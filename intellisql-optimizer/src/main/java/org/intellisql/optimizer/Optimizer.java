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

package org.intellisql.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.hep.HepMatchOrder;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rules.CoreRules;
import org.intellisql.optimizer.plan.ExecutionPlan;
import org.intellisql.optimizer.plan.ExecutionStage;
import org.intellisql.optimizer.rule.PredicatePushDownRule;
import org.intellisql.optimizer.rule.ProjectionPushDownRule;

import lombok.extern.slf4j.Slf4j;

/**
 * Core SQL query optimizer using Apache Calcite's RelOptPlanner. Optimizes logical query plans and
 * generates physical execution plans.
 */
@Slf4j
public class Optimizer {

    /** The Calcite planner used for query optimization. */
    private final RelOptPlanner planner;

    /** List of optimization rules to apply. */
    private final List<RelOptRule> rules;

    /** Default intermediate result limit. */
    private final int intermediateResultLimit;

    /** Constructs a new Optimizer with default configuration. */
    public Optimizer() {
        this(ExecutionPlan.DEFAULT_INTERMEDIATE_RESULT_LIMIT);
    }

    /**
     * Constructs a new Optimizer with the specified intermediate result limit.
     *
     * @param intermediateResultLimit the maximum number of rows for intermediate results
     */
    public Optimizer(final int intermediateResultLimit) {
        this.intermediateResultLimit = intermediateResultLimit;
        this.rules = buildDefaultRules();
        this.planner = createPlanner();
    }

    /**
     * Builds the default set of optimization rules.
     *
     * @return the list of default optimization rules
     */
    private List<RelOptRule> buildDefaultRules() {
        final List<RelOptRule> defaultRules = new ArrayList<>();
        defaultRules.add(PredicatePushDownRule.INSTANCE);
        defaultRules.add(ProjectionPushDownRule.INSTANCE);
        defaultRules.add(CoreRules.FILTER_INTO_JOIN);
        defaultRules.add(CoreRules.JOIN_PUSH_EXPRESSIONS);
        defaultRules.add(CoreRules.AGGREGATE_REDUCE_FUNCTIONS);
        defaultRules.add(CoreRules.PROJECT_MERGE);
        defaultRules.add(CoreRules.PROJECT_REMOVE);
        defaultRules.add(CoreRules.FILTER_MERGE);
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
     * Optimizes the given logical plan and returns the optimized plan.
     *
     * @param logicalPlan the logical plan to optimize
     * @return the optimized relational plan
     */
    public RelNode optimize(final RelNode logicalPlan) {
        log.debug("Starting optimization for logical plan: {}", logicalPlan);
        planner.setRoot(logicalPlan);
        final RelNode optimizedPlan = planner.findBestExp();
        log.debug("Optimization completed. Optimized plan: {}", optimizedPlan);
        return optimizedPlan;
    }

    /**
     * Generates a physical execution plan from the optimized logical plan.
     *
     * @param optimizedPlan the optimized logical plan
     * @param queryId the query identifier
     * @return the execution plan
     */
    public ExecutionPlan generateExecutionPlan(final RelNode optimizedPlan, final String queryId) {
        log.debug("Generating execution plan for query: {}", queryId);
        final ExecutionPlan executionPlan =
                ExecutionPlan.builder()
                        .id(UUID.randomUUID().toString())
                        .queryId(queryId)
                        .intermediateResultLimit(intermediateResultLimit)
                        .estimatedCost(estimateCost(optimizedPlan))
                        .build();
        final List<ExecutionStage> stages = extractStages(optimizedPlan);
        for (final ExecutionStage stage : stages) {
            executionPlan.addStage(stage);
        }
        log.debug("Execution plan generated with {} stages", stages.size());
        return executionPlan;
    }

    /**
     * Extracts execution stages from the optimized plan.
     *
     * @param relNode the root of the optimized plan
     * @return the list of execution stages
     */
    private List<ExecutionStage> extractStages(final RelNode relNode) {
        final List<ExecutionStage> stages = new ArrayList<>();
        extractStagesRecursive(relNode, stages, 0);
        return stages;
    }

    /**
     * Recursively extracts execution stages from the plan tree.
     *
     * @param relNode the current relational node
     * @param stageList the list to collect stages
     * @param depth the current depth in the tree
     */
    private void extractStagesRecursive(
                                        final RelNode relNode, final List<ExecutionStage> stageList, final int depth) {
        if (relNode == null) {
            return;
        }
        final ExecutionStage stage =
                ExecutionStage.builder()
                        .id(UUID.randomUUID().toString())
                        .dataSourceId(extractDataSourceId(relNode))
                        .operation(relNode)
                        .estimatedRows(estimateRows(relNode))
                        .build();
        stageList.add(stage);
        for (final RelNode input : relNode.getInputs()) {
            extractStagesRecursive(input, stageList, depth + 1);
        }
    }

    /**
     * Extracts the data source identifier from a relational node.
     *
     * @param relNode the relational node
     * @return the data source identifier or "default" if not found
     */
    private String extractDataSourceId(final RelNode relNode) {
        if (relNode.getTable() != null) {
            return relNode.getTable().getQualifiedName().toString();
        }
        return "default";
    }

    /**
     * Estimates the number of rows for a relational node.
     *
     * @param relNode the relational node
     * @return the estimated row count
     */
    private double estimateRows(final RelNode relNode) {
        return relNode.estimateRowCount(relNode.getCluster().getMetadataQuery());
    }

    /**
     * Estimates the total cost of executing the plan.
     *
     * @param relNode the root of the plan
     * @return the estimated cost
     */
    private double estimateCost(final RelNode relNode) {
        if (relNode == null) {
            return 0.0;
        }
        double cost = estimateRows(relNode);
        for (final RelNode input : relNode.getInputs()) {
            cost += estimateCost(input);
        }
        return cost;
    }

    /**
     * Adds a custom optimization rule to the optimizer.
     *
     * @param rule the rule to add
     */
    public void addRule(final RelOptRule rule) {
        rules.add(rule);
        log.debug("Added optimization rule: {}", rule);
    }
}
