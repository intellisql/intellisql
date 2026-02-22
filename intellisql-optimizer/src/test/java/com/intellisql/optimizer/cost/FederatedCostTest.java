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

package com.intellisql.optimizer.cost;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for FederatedCost.
 */
class FederatedCostTest {

    private FederatedCostFactory factory;

    @BeforeEach
    void setUp() {
        factory = FederatedCostFactory.INSTANCE;
    }

    @Test
    void testMakeCost() {
        final FederatedCost cost = (FederatedCost) factory.makeCost(100, 10, 5);
        Assertions.assertNotNull(cost);
        Assertions.assertEquals(100.0, cost.getRows(), 0.001);
    }

    @Test
    void testMakeZeroCost() {
        final FederatedCost cost = (FederatedCost) factory.makeZeroCost();
        Assertions.assertNotNull(cost);
        Assertions.assertEquals(0.0, cost.getRows(), 0.001);
    }

    @Test
    void testMakeInfiniteCost() {
        final FederatedCost cost = (FederatedCost) factory.makeInfiniteCost();
        Assertions.assertNotNull(cost);
        Assertions.assertTrue(cost.isInfinite());
    }

    @Test
    void testMakeHugeCost() {
        final FederatedCost cost = (FederatedCost) factory.makeHugeCost();
        Assertions.assertNotNull(cost);
        Assertions.assertTrue(cost.getRows() > 0);
    }

    @Test
    void testCostComparison() {
        final FederatedCost cost1 = (FederatedCost) factory.makeCost(100, 10, 5);
        final FederatedCost cost2 = (FederatedCost) factory.makeCost(200, 20, 10);

        Assertions.assertTrue(cost1.isLt(cost2));
        Assertions.assertTrue(!cost2.isLe(cost1));
    }

    @Test
    void testCostAddition() {
        final FederatedCost cost1 = (FederatedCost) factory.makeCost(100, 10, 5);
        final FederatedCost cost2 = (FederatedCost) factory.makeCost(100, 10, 5);
        final FederatedCost sum = (FederatedCost) cost1.plus(cost2);

        Assertions.assertEquals(200.0, sum.getRows(), 0.001);
    }

    @Test
    void testCostMultiplication() {
        final FederatedCost cost = (FederatedCost) factory.makeCost(100, 10, 5);
        final FederatedCost multiplied = (FederatedCost) cost.multiplyBy(2);

        Assertions.assertEquals(200.0, multiplied.getRows(), 0.001);
    }

    @Test
    void testCostFactors() {
        Assertions.assertEquals(1.0, CostFactor.CPU.getDefaultWeight(), 0.001);
        Assertions.assertEquals(10.0, CostFactor.IO.getDefaultWeight(), 0.001);
        Assertions.assertEquals(100.0, CostFactor.NETWORK.getDefaultWeight(), 0.001);
        Assertions.assertEquals(0.1, CostFactor.MEMORY.getDefaultWeight(), 0.001);
    }
}
