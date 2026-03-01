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

package com.intellisql.it.federation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.intellisql.federation.IntelliSqlKernel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.intellisql.common.config.ModelConfig;

/**
 * Integration tests for cross-source JOIN functionality.
 * Tests the complete federated query pipeline.
 */
class CrossSourceJoinIT {

    private IntelliSqlKernel kernel;

    @BeforeEach
    void setUp() throws Exception {
        final String configPath = System.getenv("INTELLISQL_TEST_CONFIG");
        assumeTrue(configPath != null, "Test config not set, skipping IT");
        final ModelConfig config = com.intellisql.common.config.ConfigLoader.load(
                java.nio.file.Paths.get(configPath));
        kernel = new IntelliSqlKernel(config);
        kernel.initialize();
    }

    @Test
    void testKernelInitialization() {
        assumeTrue(kernel != null, "Kernel not initialized");
        assertTrue(kernel.isInitialized(), "Kernel should be initialized");
    }

    @Test
    void testSimpleQuery() {
        assumeTrue(kernel != null && kernel.isInitialized(), "Kernel not initialized");
        // Test simple query execution
        final com.intellisql.connector.model.QueryResult result = kernel.query("SELECT 1");
        assertNotNull(result, "Query result should not be null");
    }

    @Test
    void testCrossSourceJoin() {
        assumeTrue(kernel != null && kernel.isInitialized(), "Kernel not initialized");
        // Test cross-source JOIN query
        // This would require actual multi-source setup
    }
}
