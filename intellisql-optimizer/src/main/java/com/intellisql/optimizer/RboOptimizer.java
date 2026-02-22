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

package com.intellisql.optimizer;

import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.hep.HepMatchOrder;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rules.CoreRules;

import com.intellisql.optimizer.rule.AggregateSplitRule;
import com.intellisql.optimizer.rule.JoinReorderRule;
import com.intellisql.optimizer.rule.LimitPushDownRule;
import com.intellisql.optimizer.rule.PredicatePushDownRule;
import com.intellisql.optimizer.rule.ProjectionPushDownRule;
import com.intellisql.optimizer.rule.SubqueryRewriteRule;

import lombok.extern.slf4j.Slf4j;

/**
 * Rule-Based Optimizer using Apache Calcite's HepPlanner.
 * Applies a fixed set of transformation rules in a specified order.
 * Reference: ShardingSphere HepPlanner usage in SQL Federation.
 */
@Slf4j
public class RboOptimizer {

    /** The Calcite HepPlanner used for rule-based optimization. */
    private final RelOptPlanner planner;

    /** List of optimization rules to apply. */
    private final List<RelOptRule> rules;

    /** Creates a new RboOptimizer with default rules. */
    public RboOptimizer() {
        this.rules = buildDefaultRules();
        this.planner = createPlanner();
    }

    /**
     * Creates a new RboOptimizer with custom rules.
     *
     * @param customRules the custom rules to use
     */
    public RboOptimizer(final List<RelOptRule> customRules) {
        this.rules = new ArrayList<>(customRules);
        this.planner = createPlanner();
    }

    /**
     * Builds the default set of RBO rules.
     *
     * @return the list of default optimization rules
     */
    private List<RelOptRule> buildDefaultRules() {
        final List<RelOptRule> defaultRules = new ArrayList<>();
        // Custom federated rules
        defaultRules.add(PredicatePushDownRule.INSTANCE);
        defaultRules.add(ProjectionPushDownRule.INSTANCE);
        defaultRules.add(JoinReorderRule.INSTANCE);
        defaultRules.add(SubqueryRewriteRule.INSTANCE);
        defaultRules.add(AggregateSplitRule.INSTANCE);
        defaultRules.add(LimitPushDownRule.INSTANCE);
        // Calcite core rules
        defaultRules.add(CoreRules.FILTER_INTO_JOIN);
        defaultRules.add(CoreRules.JOIN_PUSH_EXPRESSIONS);
        defaultRules.add(CoreRules.AGGREGATE_REDUCE_FUNCTIONS);
        defaultRules.add(CoreRules.PROJECT_MERGE);
        defaultRules.add(CoreRules.PROJECT_REMOVE);
        defaultRules.add(CoreRules.FILTER_MERGE);
        defaultRules.add(CoreRules.SORT_REMOVE);
        defaultRules.add(CoreRules.SORT_JOIN_TRANSPOSE);
        defaultRules.add(CoreRules.AGGREGATE_PROJECT_MERGE);
        return defaultRules;
    }

    /**
     * Creates the HepPlanner with the configured rules.
     *
     * @return the configured planner
     */
    private RelOptPlanner createPlanner() {
        final HepProgramBuilder programBuilder = HepProgram.builder();
        programBuilder.addMatchOrder(HepMatchOrder.BOTTOM_UP);
        for (final RelOptRule rule : rules) {
            programBuilder.addRuleInstance(rule);
        }
        final HepProgram program = programBuilder.build();
        return new HepPlanner(program);
    }

    /**
     * Optimizes the given logical plan using rule-based optimization.
     *
     * @param logicalPlan the logical plan to optimize
     * @return the optimized relational plan
     */
    public RelNode optimize(final RelNode logicalPlan) {
        log.debug("Starting RBO optimization for logical plan: {}", logicalPlan);
        planner.setRoot(logicalPlan);
        final RelNode optimizedPlan = planner.findBestExp();
        log.debug("RBO optimization completed. Optimized plan: {}", optimizedPlan);
        return optimizedPlan;
    }

    /**
     * Adds a custom optimization rule to the optimizer.
     *
     * @param rule the rule to add
     */
    public void addRule(final RelOptRule rule) {
        rules.add(rule);
        log.debug("Added RBO rule: {}", rule);
    }

    /**
     * Gets the list of rules used by this optimizer.
     *
     * @return the list of rules
     */
    public List<RelOptRule> getRules() {
        return new ArrayList<>(rules);
    }
}
