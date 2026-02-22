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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for HybridOptimizer.
 */
class HybridOptimizerTest {

    private HybridOptimizer optimizer;

    @BeforeEach
    void setUp() {
        optimizer = new HybridOptimizer();
    }

    @Test
    void testOptimizerCreation() {
        assertNotNull(optimizer);
    }

    @Test
    void testCboEnabledByDefault() {
        assertTrue(optimizer.isCboEnabled());
    }

    @Test
    void testRboOptimizerExists() {
        assertNotNull(optimizer.getRboOptimizer());
    }

    @Test
    void testCboIsEnabled() {
        assertTrue(optimizer.isCboEnabled());
    }

    @Test
    void testOptimizerWithoutCbo() {
        final HybridOptimizer optimizerNoCbo = new HybridOptimizer(false, 100000);
        assertTrue(!optimizerNoCbo.isCboEnabled());
    }

    @Test
    void testGetRules() {
        assertNotNull(optimizer.getRboOptimizer().getRules());
        assertTrue(!optimizer.getRboOptimizer().getRules().isEmpty());
    }
}
