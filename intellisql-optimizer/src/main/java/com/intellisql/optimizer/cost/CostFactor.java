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

/**
 * Cost factors for federated query optimization.
 * These factors are used to calculate the total execution cost
 * across heterogeneous data sources.
 */
public enum CostFactor {

    /** CPU processing cost (computation, expression evaluation). */
    CPU(1.0),

    /** I/O cost (disk reads/writes, network data transfer). */
    IO(10.0),

    /** Network cost (data transfer between federated sources). */
    NETWORK(100.0),

    /** Memory cost (buffer usage, intermediate results). */
    MEMORY(0.1);

    /** The default weight for this cost factor. */
    private final double defaultWeight;

    CostFactor(final double defaultWeight) {
        this.defaultWeight = defaultWeight;
    }

    /**
     * Gets the default weight for this cost factor.
     *
     * @return the default weight
     */
    public double getDefaultWeight() {
        return defaultWeight;
    }
}
