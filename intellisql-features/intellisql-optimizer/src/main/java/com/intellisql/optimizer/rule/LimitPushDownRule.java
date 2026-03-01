/*
 * Licensed to the IntelliSql Project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The IntelliSql Project licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellisql.optimizer.rule;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalSort;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.tools.RelBuilderFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Rule that pushes LIMIT down to data sources.
 * Reduces data transfer by limiting rows at the source.
 * Reference: ShardingSphere LimitPushDownRule.
 *
 * <p>
 * This rule enables:
 * <ul>
 *   <li>Pushing LIMIT to table scans</li>
 *   <li>Pushing LIMIT through projections</li>
 *   <li>Combining nested limits (min of limits)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Note: OFFSET cannot be safely pushed down in federated queries
 * without proper ordering guarantees.
 * </p>
 */
@Slf4j
public class LimitPushDownRule extends RelOptRule {

    /** Singleton instance of the limit pushdown rule. */
    public static final LimitPushDownRule INSTANCE = new LimitPushDownRule();

    /** Constructs a new LimitPushDownRule. */
    public LimitPushDownRule() {
        super(operand(LogicalSort.class, any()), "LimitPushDownRule");
    }

    /**
     * Constructs a new LimitPushDownRule with a custom RelBuilderFactory.
     *
     * @param factory the relational builder factory
     */
    public LimitPushDownRule(final RelBuilderFactory factory) {
        super(operand(LogicalSort.class, any()), factory, "LimitPushDownRule");
    }

    /**
     * Constructs a new LimitPushDownRule with a custom operand.
     *
     * @param operand     the rule operand
     * @param description the rule description
     */
    protected LimitPushDownRule(final RelOptRuleOperand operand, final String description) {
        super(operand, description);
    }

    /**
     * Checks if this rule matches the given relational expressions.
     *
     * @param call the rule call containing the matched rels
     * @return true if the rule matches and should be applied
     */
    @Override
    public boolean matches(final RelOptRuleCall call) {
        final LogicalSort sort = call.rel(0);
        return hasLimit(sort);
    }

    /**
     * Determines if the sort has a LIMIT clause.
     *
     * @param sort the sort node to check
     * @return true if there is a limit
     */
    private boolean hasLimit(final LogicalSort sort) {
        return sort.fetch != null;
    }

    /**
     * Transforms the matched relational expressions by pushing down the limit.
     *
     * @param call the rule call containing the matched rels
     */
    @Override
    public void onMatch(final RelOptRuleCall call) {
        final LogicalSort sort = call.rel(0);
        final RelNode input = sort.getInput();
        final int limit = getLimit(sort);
        final int offset = getOffset(sort);
        log.debug("Pushing down LIMIT {} OFFSET {} through: {}", limit, offset, input);
        // Check if we can push down through the input
        if (canPushDown(input, limit, offset)) {
            final RelNode newInput = pushDownLimit(input, limit, offset);
            transformSort(call, sort, newInput, offset);
            log.debug("Limit pushdown completed");
        }
    }

    /**
     * Transforms the sort node after limit pushdown.
     *
     * @param call     the rule call
     * @param sort     the original sort node
     * @param newInput the new input after pushdown
     * @param offset   the offset value
     */
    private void transformSort(
                               final RelOptRuleCall call,
                               final LogicalSort sort,
                               final RelNode newInput,
                               final int offset) {
        // If limit was fully pushed down, remove this sort
        // Otherwise, keep the sort with remaining limit
        if (offset == 0) {
            transformWithZeroOffset(call, sort, newInput);
        } else {
            // Partial push-down - push limit + offset
            final RelNode newSort = sort.copy(
                    sort.getTraitSet(),
                    newInput,
                    sort.getCollation(),
                    null,
                    sort.fetch);
            call.transformTo(newSort);
        }
    }

    /**
     * Transforms the sort when offset is zero.
     *
     * @param call     the rule call
     * @param sort     the original sort node
     * @param newInput the new input after pushdown
     */
    private void transformWithZeroOffset(
                                         final RelOptRuleCall call,
                                         final LogicalSort sort,
                                         final RelNode newInput) {
        // Full push-down possible
        if (sort.getCollation().getFieldCollations().isEmpty()) {
            // No ordering, can remove the sort entirely
            call.transformTo(newInput);
        } else {
            // Keep the sort for ordering but limit is pushed
            final RelNode newSort = sort.copy(
                    sort.getTraitSet(),
                    newInput,
                    sort.getCollation(),
                    null,
                    null);
            call.transformTo(newSort);
        }
    }

    /**
     * Extracts the LIMIT value from a sort node.
     *
     * @param sort the sort node
     * @return the limit value, or -1 if no limit
     */
    private int getLimit(final LogicalSort sort) {
        if (sort.fetch == null) {
            return -1;
        }
        return RexLiteral.intValue(sort.fetch);
    }

    /**
     * Extracts the OFFSET value from a sort node.
     *
     * @param sort the sort node
     * @return the offset value, or 0 if no offset
     */
    private int getOffset(final LogicalSort sort) {
        if (sort.offset == null) {
            return 0;
        }
        return RexLiteral.intValue(sort.offset);
    }

    /**
     * Checks if limit can be pushed down through the input.
     *
     * @param input  the input node
     * @param limit  the limit value
     * @param offset the offset value
     * @return true if pushdown is possible
     */
    private boolean canPushDown(final RelNode input, final int limit, final int offset) {
        // Can push down through project
        if (input instanceof org.apache.calcite.rel.core.Project) {
            return true;
        }
        // Can push down through filter
        if (input instanceof org.apache.calcite.rel.core.Filter) {
            return true;
        }
        // Can push down to table scan
        if (input instanceof org.apache.calcite.rel.core.TableScan) {
            return true;
        }
        // Cannot push down through join (without additional logic)
        return false;
    }

    /**
     * Pushes down the limit through the input node.
     *
     * @param input  the input node
     * @param limit  the limit value
     * @param offset the offset value
     * @return the new input with limit pushed down
     */
    private RelNode pushDownLimit(final RelNode input, final int limit, final int offset) {
        if (input instanceof org.apache.calcite.rel.core.Project) {
            return pushDownThroughProject(
                    (org.apache.calcite.rel.core.Project) input, limit, offset);
        }
        if (input instanceof org.apache.calcite.rel.core.Filter) {
            return pushDownThroughFilter(
                    (org.apache.calcite.rel.core.Filter) input, limit, offset);
        }
        if (input instanceof org.apache.calcite.rel.core.TableScan) {
            return createLimitOnScan(
                    (org.apache.calcite.rel.core.TableScan) input, limit, offset);
        }
        return input;
    }

    /**
     * Pushes limit through a project.
     *
     * @param project the project node
     * @param limit   the limit value
     * @param offset  the offset value
     * @return the new relational node with limit pushed down
     */
    private RelNode pushDownThroughProject(
                                           final org.apache.calcite.rel.core.Project project,
                                           final int limit,
                                           final int offset) {
        final RelNode newInput = pushDownLimit(project.getInput(), limit, offset);
        return project.copy(
                project.getTraitSet(),
                newInput,
                project.getProjects(),
                project.getRowType());
    }

    /**
     * Pushes limit through a filter.
     *
     * @param filter the filter node
     * @param limit  the limit value
     * @param offset the offset value
     * @return the new relational node with limit pushed down
     */
    private RelNode pushDownThroughFilter(
                                          final org.apache.calcite.rel.core.Filter filter,
                                          final int limit,
                                          final int offset) {
        final RelNode newInput = pushDownLimit(filter.getInput(), limit, offset);
        return filter.copy(
                filter.getTraitSet(),
                newInput,
                filter.getCondition());
    }

    /**
     * Creates a limit on top of a table scan.
     *
     * @param scan   the table scan node
     * @param limit  the limit value
     * @param offset the offset value
     * @return the new relational node with limit applied
     */
    private RelNode createLimitOnScan(
                                      final org.apache.calcite.rel.core.TableScan scan,
                                      final int limit,
                                      final int offset) {
        // Create a sort with limit on the scan
        final org.apache.calcite.rex.RexBuilder rexBuilder = scan.getCluster().getRexBuilder();
        final RexNode offsetNode = offset > 0
                ? rexBuilder.makeLiteral(offset, scan.getCluster().getTypeFactory().createSqlType(
                        org.apache.calcite.sql.type.SqlTypeName.INTEGER), false)
                : null;
        final RexNode fetchNode = rexBuilder.makeLiteral(limit,
                scan.getCluster().getTypeFactory().createSqlType(
                        org.apache.calcite.sql.type.SqlTypeName.INTEGER),
                false);
        return LogicalSort.create(
                scan,
                org.apache.calcite.rel.RelCollations.EMPTY,
                offsetNode,
                fetchNode);
    }
}
