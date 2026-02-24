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

package com.intellisql.optimizer.metadata;

import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * Statistics information for a table.
 * Used by the cost-based optimizer for query planning.
 * Reference: Calcite RelOptTable statistics.
 */
@Data
@Builder
public class TableStatistics {

    /** The table name (qualified). */
    private final String tableName;

    /** Total number of rows in the table. */
    private final long rowCount;

    /** Average row size in bytes. */
    @Builder.Default
    private final double averageRowSize = 100.0;

    /** Number of distinct values per column. */
    @Builder.Default
    private final Map<String, Long> columnDistinctCount = new HashMap<>();

    /** Column null count. */
    @Builder.Default
    private final Map<String, Long> columnNullCount = new HashMap<>();

    /** Minimum values per column. */
    @Builder.Default
    private final Map<String, Object> columnMinValues = new HashMap<>();

    /** Maximum values per column. */
    @Builder.Default
    private final Map<String, Object> columnMaxValues = new HashMap<>();

    /** When the statistics were last updated. */
    @Builder.Default
    private final long lastUpdatedTime = System.currentTimeMillis();

    /**
     * Gets the selectivity for a column predicate.
     * Selectivity is the fraction of rows that match a predicate.
     *
     * @param columnName the column name
     * @param operator   the comparison operator
     * @param value      the comparison value
     * @return the estimated selectivity (0.0 to 1.0)
     */
    public double getSelectivity(final String columnName, final String operator, final Object value) {
        final Long distinctCount = columnDistinctCount.get(columnName);
        if (distinctCount == null || distinctCount <= 0) {
            // Default 10% selectivity
            return 0.1;
        }
        // Equality predicates have selectivity = 1/distinct
        if ("=".equals(operator) || "==".equals(operator)) {
            return 1.0 / distinctCount;
        }
        // Range predicates typically select ~30% of data
        if (">".equals(operator) || "<".equals(operator) || ">=".equals(operator) || "<=".equals(operator)) {
            return 0.3;
        }
        // LIKE predicates vary widely
        if ("LIKE".equalsIgnoreCase(operator)) {
            return 0.1;
        }
        // IN predicate selectivity depends on number of values
        return 0.2;
    }

    /**
     * Gets the distinct count for a column.
     *
     * @param columnName the column name
     * @return the distinct count, or -1 if unknown
     */
    public long getDistinctCount(final String columnName) {
        return columnDistinctCount.getOrDefault(columnName, -1L);
    }

    /**
     * Gets the null fraction for a column.
     *
     * @param columnName the column name
     * @return the fraction of null values (0.0 to 1.0)
     */
    public double getNullFraction(final String columnName) {
        final Long nullCount = columnNullCount.get(columnName);
        if (nullCount == null || rowCount <= 0) {
            return 0.0;
        }
        return (double) nullCount / rowCount;
    }

    /**
     * Estimates the output row count after applying a predicate.
     *
     * @param selectivity the predicate selectivity
     * @return the estimated output row count
     */
    public long estimateOutputRows(final double selectivity) {
        return Math.max(1, (long) (rowCount * selectivity));
    }

    /**
     * Creates default statistics for a table with only row count.
     *
     * @param tableName the table name
     * @param rowCount  the row count
     * @return the table statistics
     */
    public static TableStatistics of(final String tableName, final long rowCount) {
        return TableStatistics.builder()
                .tableName(tableName)
                .rowCount(rowCount)
                .build();
    }
}
