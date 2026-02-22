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

/**
 * Federated cost implementation for cross-source query optimization.
 * Considers CPU, I/O, network, and memory costs across heterogeneous data sources.
 * Reference: ShardingSphere SQL Federation cost model.
 */
public class FederatedCost implements RelOptCost {

    /** The row count estimate. */
    private final double rows;

    /** The CPU cost component. */
    private final double cpu;

    /** The I/O cost component. */
    private final double io;

    /** The network cost component (for federated queries). */
    private final double network;

    /** The memory cost component. */
    private final double memory;

    /**
     * Creates a new federated cost with the specified components.
     *
     * @param rows    the estimated row count
     * @param cpu     the CPU cost
     * @param io      the I/O cost
     * @param network the network cost
     * @param memory  the memory cost
     */
    public FederatedCost(
                         final double rows, final double cpu, final double io, final double network, final double memory) {
        this.rows = rows;
        this.cpu = cpu;
        this.io = io;
        this.network = network;
        this.memory = memory;
    }

    /**
     * Creates a simple cost with rows, CPU, and I/O.
     *
     * @param rows the estimated row count
     * @param cpu  the CPU cost
     * @param io   the I/O cost
     */
    public FederatedCost(final double rows, final double cpu, final double io) {
        this(rows, cpu, io, 0, 0);
    }

    @Override
    public double getRows() {
        return rows;
    }

    @Override
    public double getCpu() {
        return cpu;
    }

    @Override
    public double getIo() {
        return io;
    }

    /**
     * Gets the network cost component (for cross-source queries).
     *
     * @return the network cost
     */
    public double getNetwork() {
        return network;
    }

    /**
     * Gets the memory cost component.
     *
     * @return the memory cost
     */
    public double getMemory() {
        return memory;
    }

    /**
     * Calculates the total weighted cost.
     * Total = CPU * w1 + IO * w2 + Network * w3 + Memory * w4
     *
     * @return the total weighted cost
     */
    public double getTotalCost() {
        return cpu * CostFactor.CPU.getDefaultWeight()
                + io * CostFactor.IO.getDefaultWeight()
                + network * CostFactor.NETWORK.getDefaultWeight()
                + memory * CostFactor.MEMORY.getDefaultWeight();
    }

    @Override
    public boolean isInfinite() {
        return Double.isInfinite(rows) || Double.isInfinite(cpu)
                || Double.isInfinite(io) || Double.isInfinite(network)
                || Double.isInfinite(memory);
    }

    @Override
    public boolean equals(final RelOptCost other) {
        if (other == null) {
            return false;
        }
        if (other instanceof FederatedCost) {
            final FederatedCost that = (FederatedCost) other;
            return Math.abs(this.getTotalCost() - that.getTotalCost()) < 1e-10;
        }
        // Compare with other RelOptCost implementations using rows and cpu
        return Math.abs(this.rows - other.getRows()) < 1e-10
                && Math.abs(this.cpu - other.getCpu()) < 1e-10;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RelOptCost) {
            return equals((RelOptCost) obj);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Double.hashCode(getTotalCost());
    }

    @Override
    public boolean isEqWithEpsilon(final RelOptCost other) {
        if (other == null) {
            return false;
        }
        if (other instanceof FederatedCost) {
            final FederatedCost that = (FederatedCost) other;
            return Math.abs(this.getTotalCost() - that.getTotalCost()) < 1e-5;
        }
        return Math.abs(this.rows - other.getRows()) < 1e-5
                && Math.abs(this.cpu - other.getCpu()) < 1e-5;
    }

    @Override
    public boolean isLt(final RelOptCost other) {
        if (other instanceof FederatedCost) {
            final FederatedCost that = (FederatedCost) other;
            return this.getTotalCost() < that.getTotalCost() - 1e-10;
        }
        // Compare with other implementations
        return this.rows < other.getRows();
    }

    @Override
    public boolean isLe(final RelOptCost other) {
        if (other instanceof FederatedCost) {
            final FederatedCost that = (FederatedCost) other;
            return this.getTotalCost() <= that.getTotalCost() + 1e-10;
        }
        return this.rows <= other.getRows();
    }

    @Override
    public RelOptCost plus(final RelOptCost other) {
        if (other instanceof FederatedCost) {
            final FederatedCost that = (FederatedCost) other;
            return new FederatedCost(
                    this.rows + that.rows,
                    this.cpu + that.cpu,
                    this.io + that.io,
                    this.network + that.network,
                    this.memory + that.memory);
        }
        return new FederatedCost(this.rows + other.getRows(), this.cpu + other.getCpu(), this.io + other.getIo());
    }

    @Override
    public RelOptCost minus(final RelOptCost other) {
        if (other instanceof FederatedCost) {
            final FederatedCost that = (FederatedCost) other;
            return new FederatedCost(
                    this.rows - that.rows,
                    this.cpu - that.cpu,
                    this.io - that.io,
                    this.network - that.network,
                    this.memory - that.memory);
        }
        return new FederatedCost(this.rows - other.getRows(), this.cpu - other.getCpu(), this.io - other.getIo());
    }

    @Override
    public RelOptCost multiplyBy(final double factor) {
        return new FederatedCost(
                this.rows * factor,
                this.cpu * factor,
                this.io * factor,
                this.network * factor,
                this.memory * factor);
    }

    @Override
    public double divideBy(final RelOptCost cost) {
        if (cost instanceof FederatedCost) {
            final FederatedCost that = (FederatedCost) cost;
            final double thisTotal = this.getTotalCost();
            final double thatTotal = that.getTotalCost();
            if (thatTotal == 0) {
                return Double.POSITIVE_INFINITY;
            }
            return thisTotal / thatTotal;
        }
        if (cost.getRows() == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return this.rows / cost.getRows();
    }

    @Override
    public String toString() {
        return String.format(
                "FederatedCost{rows=%.0f, cpu=%.2f, io=%.2f, network=%.2f, memory=%.2f, total=%.2f}",
                rows, cpu, io, network, memory, getTotalCost());
    }

    /**
     * Creates an infinite cost.
     *
     * @return an infinite cost instance
     */
    public static FederatedCost infinite() {
        return new FederatedCost(
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY);
    }

    /**
     * Creates a zero cost.
     *
     * @return a zero cost instance
     */
    public static FederatedCost zero() {
        return new FederatedCost(0, 0, 0, 0, 0);
    }
}
