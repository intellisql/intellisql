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

import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptCostFactory;

/**
 * Factory for creating FederatedCost instances.
 * Used by the VolcanoPlanner to create cost objects during optimization.
 * Reference: ShardingSphere SQL Federation cost factory.
 */
public final class FederatedCostFactory implements RelOptCostFactory {

    /** Singleton instance. */
    public static final FederatedCostFactory INSTANCE = new FederatedCostFactory();

    /** Private constructor for singleton. */
    private FederatedCostFactory() {
    }

    @Override
    public RelOptCost makeCost(final double rows, final double cpu, final double io) {
        return new FederatedCost(rows, cpu, io, 0, 0);
    }

    /**
     * Creates a federated cost with all components.
     *
     * @param rows    the estimated row count
     * @param cpu     the CPU cost
     * @param io      the I/O cost
     * @param network the network cost (for cross-source queries)
     * @param memory  the memory cost
     * @return the federated cost instance
     */
    public RelOptCost makeCost(
                               final double rows, final double cpu, final double io, final double network, final double memory) {
        return new FederatedCost(rows, cpu, io, network, memory);
    }

    @Override
    public RelOptCost makeHugeCost() {
        return new FederatedCost(
                Double.MAX_VALUE / 2,
                Double.MAX_VALUE / 2,
                Double.MAX_VALUE / 2,
                Double.MAX_VALUE / 2,
                Double.MAX_VALUE / 2);
    }

    @Override
    public RelOptCost makeInfiniteCost() {
        return FederatedCost.infinite();
    }

    @Override
    public RelOptCost makeZeroCost() {
        return FederatedCost.zero();
    }

    @Override
    public RelOptCost makeTinyCost() {
        return new FederatedCost(0.001, 0.001, 0, 0, 1);
    }

    /**
     * Creates a cost for a table scan operation.
     *
     * @param rows         the estimated row count
     * @param bytesPerRow  the average bytes per row
     * @param isRemote     whether the scan is from a remote source
     * @return the cost for the table scan
     */
    public RelOptCost makeTableScanCost(final double rows, final double bytesPerRow, final boolean isRemote) {
        // Minimal CPU per row
        final double cpu = rows * 0.1;
        // I/O in pages
        final double io = rows * bytesPerRow / 8192.0;
        // Network in KB if remote
        final double network = isRemote ? rows * bytesPerRow / 1024.0 : 0;
        // Memory in KB
        final double memory = rows * bytesPerRow / 1024.0;
        return makeCost(rows, cpu, io, network, memory);
    }

    /**
     * Creates a cost for a join operation.
     *
     * @param leftRows     the left input row count
     * @param rightRows    the right input row count
     * @param outputRows   the estimated output row count
     * @param isCrossJoin  whether this is a cross join
     * @return the cost for the join
     */
    public RelOptCost makeJoinCost(
                                   final double leftRows, final double rightRows, final double outputRows, final boolean isCrossJoin) {
        // For hash join: build hash table on smaller side + probe
        final double buildSide = Math.min(leftRows, rightRows);
        final double probeSide = Math.max(leftRows, rightRows);

        final double cpu = isCrossJoin ? leftRows * rightRows : buildSide + probeSide;
        // 64 bytes per entry in hash table
        final double memory = buildSide * 64;
        // Join is done locally after data fetch
        final double network = 0;

        return makeCost(outputRows, cpu, 0, network, memory);
    }

    /**
     * Creates a cost for an aggregate operation.
     *
     * @param inputRows  the input row count
     * @param groupCount the number of groups
     * @param outputRows the estimated output row count
     * @return the cost for the aggregate
     */
    public RelOptCost makeAggregateCost(final double inputRows, final int groupCount, final double outputRows) {
        // Hash computation per row
        final double cpu = inputRows * groupCount * 0.1;
        // Memory for hash table
        final double memory = outputRows * groupCount * 16;
        return makeCost(outputRows, cpu, 0, 0, memory);
    }

    /**
     * Creates a cost for a sort operation.
     *
     * @param rows       the input row count
     * @param memoryUsed whether memory is used for sorting
     * @return the cost for the sort
     */
    public RelOptCost makeSortCost(final double rows, final boolean memoryUsed) {
        // N log N comparisons
        final double cpu = rows * Math.log(rows + 1) * 0.1;
        // Memory for sorting
        final double memory = memoryUsed ? rows * 64 : 0;
        return makeCost(rows, cpu, 0, 0, memory);
    }
}
