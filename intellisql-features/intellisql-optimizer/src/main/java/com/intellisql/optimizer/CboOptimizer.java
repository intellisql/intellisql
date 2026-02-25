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
import org.apache.calcite.plan.volcano.VolcanoPlanner;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rules.CoreRules;

import lombok.extern.slf4j.Slf4j;

/**
 * Cost-Based Optimizer using Apache Calcite's VolcanoPlanner.
 * Explores multiple execution plans and selects the one with the lowest cost.
 * Uses FederatedCostFactory for cross-source query cost estimation.
 * Reference: ShardingSphere VolcanoPlanner usage in SQL Federation.
 */
@Slf4j
public class CboOptimizer {

    /** The Calcite VolcanoPlanner used for cost-based optimization. */
    private final VolcanoPlanner planner;

    /** List of transformation rules for the VolcanoPlanner. */
    private final List<RelOptRule> rules;

    /** Creates a new CboOptimizer with a shared planner. */
    public CboOptimizer(final VolcanoPlanner planner) {
        this.planner = planner;
        this.rules = buildDefaultRules();
        registerRules();
    }

    /**
     * Creates a new CboOptimizer with custom rules and shared planner.
     *
     * @param planner the shared planner
     * @param customRules the custom rules to use
     */
    public CboOptimizer(final VolcanoPlanner planner, final List<RelOptRule> customRules) {
        this.planner = planner;
        this.rules = new ArrayList<>(customRules);
        registerRules();
    }

    /**
     * Builds the default set of CBO transformation rules.
     *
     * @return the list of default optimization rules
     */
    private List<RelOptRule> buildDefaultRules() {
        final List<RelOptRule> defaultRules = new ArrayList<>();
        // Join optimization rules
        defaultRules.add(CoreRules.JOIN_TO_MULTI_JOIN);
        defaultRules.add(CoreRules.MULTI_JOIN_OPTIMIZE_BUSHY);
        defaultRules.add(CoreRules.FILTER_INTO_JOIN);
        defaultRules.add(CoreRules.JOIN_PUSH_EXPRESSIONS);
        // Project and filter rules
        defaultRules.add(CoreRules.PROJECT_MERGE);
        defaultRules.add(CoreRules.PROJECT_REMOVE);
        defaultRules.add(CoreRules.FILTER_MERGE);
        // Aggregate rules
        defaultRules.add(CoreRules.AGGREGATE_REDUCE_FUNCTIONS);
        defaultRules.add(CoreRules.AGGREGATE_PROJECT_MERGE);
        defaultRules.add(CoreRules.AGGREGATE_JOIN_TRANSPOSE);
        // Sort rules
        defaultRules.add(CoreRules.SORT_REMOVE);
        defaultRules.add(CoreRules.SORT_JOIN_TRANSPOSE);
        return defaultRules;
    }

    /**
     * Registers all rules with the VolcanoPlanner.
     */
    private void registerRules() {
        for (final RelOptRule rule : rules) {
            planner.addRule(rule);
            log.debug("Registered CBO rule: {}", rule);
        }
    }

    /**
     * Optimizes the given logical plan using cost-based optimization.
     *
     * @param logicalPlan the logical plan to optimize
     * @return the optimized relational plan with the lowest cost
     */
    public RelNode optimize(final RelNode logicalPlan) {
        log.debug("Starting CBO optimization for logical plan: {}", logicalPlan);
        // Set the root of the planner
        planner.setRoot(logicalPlan);
        // Find the best plan based on cost
        final RelNode optimizedPlan = planner.findBestExp();
        log.debug("CBO optimization completed. Optimized plan: {}", optimizedPlan);
        if (optimizedPlan != null) {
            log.debug("Best plan estimated rows: {}",
                    optimizedPlan.estimateRowCount(optimizedPlan.getCluster().getMetadataQuery()));
        }
        return optimizedPlan;
    }

    /**
     * Gets the VolcanoPlanner instance.
     *
     * @return the planner
     */
    public RelOptPlanner getPlanner() {
        return planner;
    }

    /**
     * Adds a custom optimization rule to the optimizer.
     *
     * @param rule the rule to add
     */
    public void addRule(final RelOptRule rule) {
        rules.add(rule);
        planner.addRule(rule);
        log.debug("Added CBO rule: {}", rule);
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
