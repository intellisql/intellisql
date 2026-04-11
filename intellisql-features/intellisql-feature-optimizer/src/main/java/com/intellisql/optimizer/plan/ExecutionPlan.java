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

package com.intellisql.optimizer.plan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.calcite.rex.RexNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a complete execution plan for a SQL query. Contains all execution stages, estimated
 * costs, and optimization information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionPlan {

    /** Default limit for intermediate results to prevent memory overflow. */
    public static final int DEFAULT_INTERMEDIATE_RESULT_LIMIT = 100000;

    /** Unique identifier for this execution plan. */
    private String id;

    /** Identifier of the query this plan is for. */
    private String queryId;

    /** Ordered list of execution stages to be executed. */
    @Builder.Default
    private List<ExecutionStage> stages = new ArrayList<>();

    /** Estimated total cost of executing this plan. */
    private double estimatedCost;

    /** Predicates that have been pushed down to data sources. */
    @Builder.Default
    private Set<RexNode> pushdownPredicates = new HashSet<>();

    /** Projections (column references) that have been pushed down to data sources. */
    @Builder.Default
    private Set<String> pushdownProjections = new HashSet<>();

    /** Maximum number of rows for intermediate results. */
    @Builder.Default
    private int intermediateResultLimit = DEFAULT_INTERMEDIATE_RESULT_LIMIT;

    /**
     * Adds an execution stage to the plan.
     *
     * @param stage the execution stage to add
     */
    public void addStage(final ExecutionStage stage) {
        if (stages == null) {
            stages = new ArrayList<>();
        }
        stages.add(stage);
    }

    /**
     * Adds a pushdown predicate to the plan.
     *
     * @param predicate the predicate to add
     */
    public void addPushdownPredicate(final RexNode predicate) {
        if (pushdownPredicates == null) {
            pushdownPredicates = new HashSet<>();
        }
        pushdownPredicates.add(predicate);
    }

    /**
     * Adds a pushdown projection to the plan.
     *
     * @param columnName the column name to add
     */
    public void addPushdownProjection(final String columnName) {
        if (pushdownProjections == null) {
            pushdownProjections = new HashSet<>();
        }
        pushdownProjections.add(columnName);
    }
}
