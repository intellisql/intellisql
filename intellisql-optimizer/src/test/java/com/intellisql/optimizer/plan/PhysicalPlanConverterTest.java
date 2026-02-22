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

package com.intellisql.optimizer.plan;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for ExecutionPlan and ExecutionStage.
 */
class PhysicalPlanConverterTest {

    @Test
    void testExecutionPlanBuilder() {
        final ExecutionPlan plan = ExecutionPlan.builder()
                .id("plan-1")
                .queryId("query-1")
                .intermediateResultLimit(100000)
                .estimatedCost(100.0)
                .build();

        assertNotNull(plan);
        assertNotNull(plan.getId());
        assertNotNull(plan.getQueryId());
        assertTrue(plan.getIntermediateResultLimit() > 0);
    }

    @Test
    void testExecutionPlanDefaultLimit() {
        assertTrue(ExecutionPlan.DEFAULT_INTERMEDIATE_RESULT_LIMIT > 0);
    }

    @Test
    void testExecutionStageBuilder() {
        final ExecutionStage stage = ExecutionStage.builder()
                .id("stage-1")
                .dataSourceId("ds-1")
                .estimatedRows(1000.0)
                .build();

        assertNotNull(stage);
        assertNotNull(stage.getId());
        assertNotNull(stage.getDataSourceId());
    }
}
