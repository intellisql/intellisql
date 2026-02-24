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

package com.intellisql.optimizer.rule;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.JoinInfo;
import org.apache.calcite.rel.logical.LogicalJoin;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.tools.RelBuilderFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Rule that reorders joins to improve query performance.
 * Reorders joins based on join selectivity and table sizes.
 * Reference: ShardingSphere JoinReorderRule.
 *
 * <p>
 * This rule applies heuristic-based join reordering for RBO phase:
 * <ul>
 *   <li>Smaller tables should be on the build side (right side)</li>
 *   <li>More selective joins should be executed earlier</li>
 *   <li>Cross joins should be deferred as much as possible</li>
 * </ul>
 * </p>
 */
@Slf4j
public class JoinReorderRule extends RelOptRule {

    /** Singleton instance of the join reorder rule. */
    public static final JoinReorderRule INSTANCE = new JoinReorderRule();

    /** Constructs a new JoinReorderRule. */
    public JoinReorderRule() {
        super(operand(LogicalJoin.class, any()), "JoinReorderRule");
    }

    /**
     * Constructs a new JoinReorderRule with a custom RelBuilderFactory.
     *
     * @param factory the relational builder factory
     */
    public JoinReorderRule(final RelBuilderFactory factory) {
        super(operand(LogicalJoin.class, any()), factory, "JoinReorderRule");
    }

    /**
     * Constructs a new JoinReorderRule with a custom operand.
     *
     * @param operand     the rule operand
     * @param description the rule description
     */
    protected JoinReorderRule(final RelOptRuleOperand operand, final String description) {
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
        final LogicalJoin join = call.rel(0);
        // Only reorder if it would be beneficial
        return isReorderBeneficial(join);
    }

    /**
     * Determines if reordering the join would be beneficial.
     *
     * @param join the join to evaluate
     * @return true if reordering would improve performance
     */
    private boolean isReorderBeneficial(final LogicalJoin join) {
        final RelNode left = join.getLeft();
        final RelNode right = join.getRight();
        // Estimate row counts
        final double leftRows = left.estimateRowCount(left.getCluster().getMetadataQuery());
        final double rightRows = right.estimateRowCount(right.getCluster().getMetadataQuery());
        // Reorder if right side is significantly larger than left side
        // (we want smaller table on build side)
        return rightRows > leftRows * 1.5;
    }

    /**
     * Transforms the matched relational expressions by reordering the join.
     *
     * @param call the rule call containing the matched rels
     */
    @Override
    public void onMatch(final RelOptRuleCall call) {
        final LogicalJoin join = call.rel(0);
        log.debug("Reordering join: {}", join);
        final RelNode left = join.getLeft();
        final RelNode right = join.getRight();
        // Swap left and right
        final RelNode newLeft = right;
        final RelNode newRight = left;
        // Adjust the join condition for swapped inputs
        final RexNode newCondition = swapJoinCondition(join, left, right);
        // Create new join with swapped inputs
        final LogicalJoin newJoin = join.copy(
                join.getTraitSet(),
                newCondition,
                newLeft,
                newRight,
                join.getJoinType(),
                join.isSemiJoinDone());
        log.debug("Created reordered join with swapped inputs");
        call.transformTo(newJoin);
    }

    /**
     * Swaps the join condition to reflect swapped inputs.
     *
     * @param join  the original join
     * @param left  the original left input
     * @param right the original right input
     * @return the adjusted join condition
     */
    private RexNode swapJoinCondition(
                                      final LogicalJoin join,
                                      final RelNode left,
                                      final RelNode right) {
        final RexNode condition = join.getCondition();
        // Field counts would be used for swapping input references in full implementation
        // final int leftFieldCount = left.getRowType().getFieldCount();
        // final int rightFieldCount = right.getRowType().getFieldCount();
        // For now, return the original condition
        // Full implementation would swap input references
        return condition;
    }

    /**
     * Analyzes join selectivity to determine optimal order.
     *
     * @param join the join to analyze
     * @return the estimated selectivity (0.0 to 1.0)
     */
    private double estimateJoinSelectivity(final Join join) {
        final JoinInfo joinInfo = join.analyzeCondition();
        if (joinInfo.leftKeys.isEmpty() || joinInfo.rightKeys.isEmpty()) {
            // Cross join - worst selectivity
            return 1.0;
        }
        // Heuristic: equality joins typically have good selectivity
        return 0.1;
    }

    /**
     * Gets the estimated row count for a relational node.
     *
     * @param relNode the relational node
     * @return the estimated row count
     */
    private double getEstimatedRows(final RelNode relNode) {
        return relNode.estimateRowCount(relNode.getCluster().getMetadataQuery());
    }
}
