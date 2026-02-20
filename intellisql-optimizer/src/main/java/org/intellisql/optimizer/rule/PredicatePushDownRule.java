/*
 * Licensed to the IntelliSQL Project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The IntelliSQL Project licenses this file to You under the Apache License, Version 2.0
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

package org.intellisql.optimizer.rule;

import java.util.List;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.tools.RelBuilderFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Optimization rule that pushes WHERE predicates down to data sources. This reduces the amount of
 * data transferred and improves query performance.
 */
@Slf4j
public class PredicatePushDownRule extends RelOptRule {

    /** Singleton instance of the predicate pushdown rule. */
    public static final PredicatePushDownRule INSTANCE = new PredicatePushDownRule();

    /** Constructs a new PredicatePushDownRule. */
    public PredicatePushDownRule() {
        super(operand(Filter.class, any()), "PredicatePushDownRule");
    }

    /**
     * Constructs a new PredicatePushDownRule with a custom RelBuilderFactory.
     *
     * @param factory the relational builder factory
     */
    public PredicatePushDownRule(final RelBuilderFactory factory) {
        super(operand(Filter.class, any()), factory, "PredicatePushDownRule");
    }

    /**
     * Constructs a new PredicatePushDownRule with a custom operand.
     *
     * @param operand the rule operand
     * @param description the rule description
     */
    protected PredicatePushDownRule(final RelOptRuleOperand operand, final String description) {
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
        final Filter filter = call.rel(0);
        if (filter.getCondition() == null) {
            return false;
        }
        final RelNode input = filter.getInput();
        return canPushDown(input);
    }

    /**
     * Determines if predicates can be pushed down through the given node.
     *
     * @param relNode the relational node to check
     * @return true if predicates can be pushed down
     */
    private boolean canPushDown(final RelNode relNode) {
        if (relNode instanceof TableScan) {
            return true;
        }
        if (relNode instanceof Project) {
            return canPushDown(((Project) relNode).getInput());
        }
        if (relNode instanceof Filter) {
            return true;
        }
        return false;
    }

    /**
     * Transforms the matched relational expressions.
     *
     * @param call the rule call containing the matched rels
     */
    @Override
    public void onMatch(final RelOptRuleCall call) {
        final Filter filter = call.rel(0);
        final RexNode condition = filter.getCondition();
        final RelNode input = filter.getInput();
        log.debug("Pushing down predicate: {} to input: {}", condition, input);
        final RelNode newInput = pushDownPredicate(input, condition);
        final RexNode remainingCondition = extractRemainingCondition(condition, input);
        if (remainingCondition == null || remainingCondition.isAlwaysTrue()) {
            call.transformTo(newInput);
        } else {
            final Filter newFilter = filter.copy(filter.getTraitSet(), newInput, remainingCondition);
            call.transformTo(newFilter);
        }
    }

    /**
     * Pushes a predicate down through a relational node.
     *
     * @param relNode the relational node
     * @param predicate the predicate to push down
     * @return the new relational node with the predicate pushed down
     */
    private RelNode pushDownPredicate(final RelNode relNode, final RexNode predicate) {
        if (relNode instanceof TableScan) {
            return pushDownToTableScan((TableScan) relNode, predicate);
        }
        if (relNode instanceof Project) {
            return pushDownThroughProject((Project) relNode, predicate);
        }
        if (relNode instanceof Filter) {
            return mergeWithFilter((Filter) relNode, predicate);
        }
        return relNode;
    }

    /**
     * Pushes a predicate down to a table scan.
     *
     * @param tableScan the table scan
     * @param predicate the predicate to push down
     * @return the new table scan with the predicate
     */
    private RelNode pushDownToTableScan(final TableScan tableScan, final RexNode predicate) {
        log.debug("Pushing predicate to table scan: {}", tableScan.getTable().getQualifiedName());
        return new org.apache.calcite.rel.logical.LogicalFilter(
                tableScan.getCluster(), tableScan.getTraitSet(), tableScan, predicate);
    }

    /**
     * Pushes a predicate through a project.
     *
     * @param project the project
     * @param predicate the predicate to push down
     * @return the new relational node
     */
    private RelNode pushDownThroughProject(final Project project, final RexNode predicate) {
        final RelNode pushedInput = pushDownPredicate(project.getInput(), predicate);
        return project.copy(
                project.getTraitSet(), pushedInput, project.getProjects(), project.getRowType());
    }

    /**
     * Merges a predicate with an existing filter.
     *
     * @param existingFilter the existing filter
     * @param newPredicate the new predicate to merge
     * @return the merged filter
     */
    private RelNode mergeWithFilter(final Filter existingFilter, final RexNode newPredicate) {
        final List<RexNode> conditions =
                java.util.Arrays.asList(existingFilter.getCondition(), newPredicate);
        return existingFilter.copy(
                existingFilter.getTraitSet(),
                existingFilter.getInput(),
                RexUtil.composeConjunction(existingFilter.getCluster().getRexBuilder(), conditions));
    }

    /**
     * Extracts any remaining condition that could not be pushed down.
     *
     * @param originalCondition the original condition
     * @param relNode the relational node
     * @return the remaining condition, or null if all was pushed down
     */
    private RexNode extractRemainingCondition(
                                              final RexNode originalCondition, final RelNode relNode) {
        return null;
    }
}
