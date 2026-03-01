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

package com.intellisql.federation.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Limiter for intermediate query results to prevent memory overflow. Limits results to a maximum of
 * 100000 rows by default. Returns partial results with a warning when the limit is exceeded.
 */
@Slf4j
public class IntermediateResultLimiter {

    /** Default maximum number of rows (100000 as per NFR-010). */
    public static final int DEFAULT_LIMIT = 100000;

    /** Maximum number of rows allowed. */
    private final int maxRows;

    /** Creates a new IntermediateResultLimiter with the default limit. */
    public IntermediateResultLimiter() {
        this(DEFAULT_LIMIT);
    }

    /**
     * Creates a new IntermediateResultLimiter with a custom limit.
     *
     * @param maxRows the maximum number of rows to allow
     */
    public IntermediateResultLimiter(final int maxRows) {
        if (maxRows <= 0) {
            throw new IllegalArgumentException("maxRows must be positive");
        }
        this.maxRows = maxRows;
        log.info("IntermediateResultLimiter initialized with maxRows={}", maxRows);
    }

    /**
     * Limits the input rows to the configured maximum.
     *
     * @param rows the input rows
     * @return the limited result
     */
    public LimitedResult limit(final List<Row> rows) {
        return limit(rows, maxRows);
    }

    /**
     * Limits the input rows to the specified maximum.
     *
     * @param rows the input rows
     * @param limit the maximum number of rows
     * @return the limited result
     */
    public LimitedResult limit(final List<Row> rows, final int limit) {
        if (rows == null || rows.isEmpty()) {
            return new LimitedResult(Collections.emptyList(), 0, false, null);
        }
        int totalRows = rows.size();
        int effectiveLimit = limit > 0 ? Math.min(limit, maxRows) : maxRows;
        if (totalRows <= effectiveLimit) {
            log.debug("Rows within limit: {} <= {}", totalRows, effectiveLimit);
            return new LimitedResult(rows, totalRows, false, null);
        }
        List<Row> limitedRows = new ArrayList<>(rows.subList(0, effectiveLimit));
        String warning =
                String.format(
                        "Intermediate result limited to %d rows (total: %d). "
                                + "Consider adding filters to reduce result size.",
                        effectiveLimit, totalRows);
        log.warn(warning);
        return new LimitedResult(limitedRows, effectiveLimit, true, warning);
    }

    /**
     * Checks if adding more rows would exceed the limit.
     *
     * @param currentCount the current row count
     * @return true if adding more rows would exceed the limit
     */
    public boolean wouldExceedLimit(final int currentCount) {
        return currentCount >= maxRows;
    }

    /**
     * Checks if adding rows would exceed the limit.
     *
     * @param currentCount the current row count
     * @param additionalCount the number of rows to add
     * @return true if adding the rows would exceed the limit
     */
    public boolean wouldExceedLimit(final int currentCount, final int additionalCount) {
        return currentCount + additionalCount > maxRows;
    }

    /**
     * Gets the remaining capacity before the limit is reached.
     *
     * @param currentCount the current row count
     * @return the remaining capacity
     */
    public int getRemainingCapacity(final int currentCount) {
        return Math.max(0, maxRows - currentCount);
    }

    /**
     * Gets the maximum row limit.
     *
     * @return the maximum row limit
     */
    public int getMaxRows() {
        return maxRows;
    }

    /** Result of limiting operation. */
    @Getter
    @AllArgsConstructor
    public static class LimitedResult {

        /** The limited list of rows. */
        private final List<Row> rows;

        /** The count of rows in the result. */
        private final long rowCount;

        /** Whether the result was truncated. */
        private final boolean truncated;

        /** Warning message if truncated. */
        private final String warning;

        /**
         * Checks if there are any rows.
         *
         * @return true if there are rows
         */
        public boolean hasRows() {
            return rows != null && !rows.isEmpty();
        }

        /**
         * Checks if there is a warning.
         *
         * @return true if there is a warning
         */
        public boolean hasWarning() {
            return warning != null && !warning.isEmpty();
        }
    }
}
