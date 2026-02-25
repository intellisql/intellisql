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
import java.util.UUID;

import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.volcano.VolcanoPlanner;
import org.apache.calcite.rel.RelNode;

import com.intellisql.optimizer.metadata.DataSourceAware;
import com.intellisql.optimizer.plan.ExecutionPlan;
import com.intellisql.optimizer.plan.ExecutionStage;

import lombok.extern.slf4j.Slf4j;

/**
 * Hybrid Optimizer that combines Rule-Based and Cost-Based optimization.
 * First applies RBO for heuristic transformations, then CBO for cost-based join ordering.
 * Reference: ShardingSphere hybrid optimizer pattern.
 *
 * <p>
 * Optimization pipeline:
 * <ol>
 *   <li>RBO Phase: Apply predicate pushdown, projection pushdown, and other heuristic rules</li>
 *   <li>CBO Phase: Explore join orderings and select the lowest cost plan</li>
 * </ol>
 * </p>
 */
@Slf4j
public class HybridOptimizer {

    /** The RBO optimizer for heuristic transformations. */
    private final RboOptimizer rboOptimizer;

    /** Whether to enable CBO phase. */
    private final boolean enableCbo;

    /** Default intermediate result limit. */
    private final int intermediateResultLimit;

    /** Creates a new HybridOptimizer with both RBO and CBO enabled. */
    public HybridOptimizer() {
        this(true, ExecutionPlan.DEFAULT_INTERMEDIATE_RESULT_LIMIT);
    }

    /**
     * Creates a new HybridOptimizer.
     *
     * @param enableCbo               whether to enable CBO phase
     * @param intermediateResultLimit the maximum intermediate result rows
     */
    public HybridOptimizer(final boolean enableCbo, final int intermediateResultLimit) {
        this.enableCbo = enableCbo;
        this.intermediateResultLimit = intermediateResultLimit;
        this.rboOptimizer = new RboOptimizer();
        log.info("HybridOptimizer initialized with CBO enabled: {}", enableCbo);
    }

    /**
     * Optimizes the given logical plan using the hybrid approach.
     * Pipeline: RBO (heuristic rules) → CBO (cost-based optimization)
     *
     * <p>
     * Note: The CBO phase uses the planner from the RelNode's cluster directly,
     * without creating a new CboOptimizer. The rules should already be registered
     * when the planner was created (like ShardingSphere does).
     * </p>
     *
     * @param logicalPlan the logical plan to optimize
     * @return the optimized relational plan
     */
    public RelNode optimize(final RelNode logicalPlan) {
        log.debug("Starting hybrid optimization pipeline");
        // Phase 1: RBO - Apply heuristic transformation rules
        log.debug("Phase 1: Applying RBO rules");
        RelNode currentPlan = rboOptimizer.optimize(logicalPlan);
        log.debug("After RBO: {}", currentPlan);
        // Phase 2: CBO - Cost-based optimization (join ordering, etc.)
        // Use the planner from the RelNode's cluster directly (like ShardingSphere does)
        if (enableCbo) {
            log.debug("Phase 2: Applying CBO optimization");
            final RelOptPlanner planner = currentPlan.getCluster().getPlanner();
            if (planner instanceof VolcanoPlanner) {
                // Use planner directly - rules should already be registered
                planner.setRoot(currentPlan);
                currentPlan = planner.findBestExp();
                log.debug("After CBO: {}", currentPlan);
            } else {
                log.warn("Planner is not VolcanoPlanner, skipping CBO phase");
            }
        }
        log.debug("Hybrid optimization completed");
        return currentPlan;
    }

    /**
     * Optimizes and generates a physical execution plan.
     *
     * @param logicalPlan the logical plan to optimize
     * @param queryId     the query identifier
     * @return the execution plan
     */
    public ExecutionPlan optimizeAndGeneratePlan(final RelNode logicalPlan, final String queryId) {
        final RelNode optimizedPlan = optimize(logicalPlan);
        return generateExecutionPlan(optimizedPlan, queryId);
    }

    /**
     * Generates a physical execution plan from the optimized logical plan.
     *
     * @param optimizedPlan the optimized logical plan
     * @param queryId       the query identifier
     * @return the execution plan
     */
    public ExecutionPlan generateExecutionPlan(final RelNode optimizedPlan, final String queryId) {
        log.debug("Generating execution plan for query: {}", queryId);
        final ExecutionPlan executionPlan = ExecutionPlan.builder()
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
     * @param relNode    the current relational node
     * @param stageList  the list to collect stages
     * @param depth      the current depth in the tree
     */
    private void extractStagesRecursive(
                                        final RelNode relNode, final List<ExecutionStage> stageList, final int depth) {
        if (relNode == null) {
            return;
        }
        final ExecutionStage stage = ExecutionStage.builder()
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
     * Uses the {@link DataSourceAware} interface to get the data source ID
     * from the underlying table implementation.
     *
     * @param relNode the relational node
     * @return the data source identifier or "default" if not found
     */
    private String extractDataSourceId(final RelNode relNode) {
        final RelOptTable table = relNode.getTable();
        if (table != null) {
            // Try to get DataSourceAware from the table
            final DataSourceAware dataSourceAware = table.unwrap(DataSourceAware.class);
            if (dataSourceAware != null && dataSourceAware.getDataSourceId() != null) {
                return dataSourceAware.getDataSourceId();
            }
            // Fallback: try to unwrap to the table class directly
            final org.apache.calcite.schema.Table calciteTable = table.unwrap(org.apache.calcite.schema.Table.class);
            if (calciteTable instanceof DataSourceAware) {
                return ((DataSourceAware) calciteTable).getDataSourceId();
            }
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
     * Estimates the cost of the optimized plan.
     *
     * @param relNode the root of the optimized plan
     * @return the estimated cost
     */
    private double estimateCost(final RelNode relNode) {
        if (relNode == null) {
            return 0.0;
        }
        double cost = relNode.estimateRowCount(relNode.getCluster().getMetadataQuery());
        for (final RelNode input : relNode.getInputs()) {
            cost += estimateCost(input);
        }
        return cost;
    }

    /**
     * Gets the RBO optimizer.
     *
     * @return the RBO optimizer
     */
    public RboOptimizer getRboOptimizer() {
        return rboOptimizer;
    }

    /**
     * Checks if CBO is enabled.
     *
     * @return true if CBO is enabled
     */
    public boolean isCboEnabled() {
        return enableCbo;
    }
}
