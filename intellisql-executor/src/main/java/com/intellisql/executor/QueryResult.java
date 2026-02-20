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

package com.intellisql.executor;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the result of a query execution. Contains either success data (result set, metadata)
 * or error information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {

    /** The query ID for correlation. */
    private String queryId;

    /** Whether the query executed successfully. */
    private boolean success;

    /** Iterator over result rows (if success). */
    private Iterator<Row> resultSet;

    /** Column metadata (if success). */
    private List<ColumnMetadata> columnMetadata;

    /** Total row count (if success). */
    private long rowCount;

    /** Whether results were truncated due to limit. */
    private boolean truncated;

    /** Warning message (if truncated or partial results). */
    private String warning;

    /** Error information (if failed). */
    private QueryError error;

    /** Execution time in milliseconds. */
    private long executionTimeMs;

    /** Number of retries attempted. */
    private int retryCount;

    /**
     * Creates a successful query result.
     *
     * @param queryId the query ID
     * @param resultSet the result set iterator
     * @param columns the column metadata list
     * @param rowCount the total row count
     * @param executionTimeMs the execution time in milliseconds
     * @return a new QueryResult instance
     */
    public static QueryResult success(
                                      final String queryId,
                                      final Iterator<Row> resultSet,
                                      final List<ColumnMetadata> columns,
                                      final long rowCount,
                                      final long executionTimeMs) {
        return QueryResult.builder()
                .queryId(queryId)
                .success(true)
                .resultSet(resultSet)
                .columnMetadata(columns)
                .rowCount(rowCount)
                .truncated(false)
                .executionTimeMs(executionTimeMs)
                .retryCount(0)
                .build();
    }

    /**
     * Creates a successful query result with retry count.
     *
     * @param queryId the query ID
     * @param resultSet the result set iterator
     * @param columns the column metadata list
     * @param rowCount the total row count
     * @param executionTimeMs the execution time in milliseconds
     * @param retryCount the number of retries
     * @return a new QueryResult instance
     */
    public static QueryResult success(
                                      final String queryId,
                                      final Iterator<Row> resultSet,
                                      final List<ColumnMetadata> columns,
                                      final long rowCount,
                                      final long executionTimeMs,
                                      final int retryCount) {
        return QueryResult.builder()
                .queryId(queryId)
                .success(true)
                .resultSet(resultSet)
                .columnMetadata(columns)
                .rowCount(rowCount)
                .truncated(false)
                .executionTimeMs(executionTimeMs)
                .retryCount(retryCount)
                .build();
    }

    /**
     * Creates a truncated query result with a warning.
     *
     * @param queryId the query ID
     * @param resultSet the result set iterator
     * @param columns the column metadata list
     * @param rowCount the total row count
     * @param warning the warning message
     * @param executionTimeMs the execution time in milliseconds
     * @return a new QueryResult instance
     */
    public static QueryResult truncated(
                                        final String queryId,
                                        final Iterator<Row> resultSet,
                                        final List<ColumnMetadata> columns,
                                        final long rowCount,
                                        final String warning,
                                        final long executionTimeMs) {
        return QueryResult.builder()
                .queryId(queryId)
                .success(true)
                .resultSet(resultSet)
                .columnMetadata(columns)
                .rowCount(rowCount)
                .truncated(true)
                .warning(warning)
                .executionTimeMs(executionTimeMs)
                .retryCount(0)
                .build();
    }

    /**
     * Creates a failed query result.
     *
     * @param queryId the query ID
     * @param error the error information
     * @param executionTimeMs the execution time in milliseconds
     * @param retryCount the number of retries attempted
     * @return a new QueryResult instance
     */
    public static QueryResult failure(
                                      final String queryId, final QueryError error, final long executionTimeMs, final int retryCount) {
        return QueryResult.builder()
                .queryId(queryId)
                .success(false)
                .error(error)
                .executionTimeMs(executionTimeMs)
                .retryCount(retryCount)
                .rowCount(0)
                .resultSet(Collections.emptyIterator())
                .columnMetadata(Collections.emptyList())
                .build();
    }

    /**
     * Creates a failed query result with default retry count.
     *
     * @param queryId the query ID
     * @param error the error information
     * @return a new QueryResult instance
     */
    public static QueryResult failure(final String queryId, final QueryError error) {
        return failure(queryId, error, 0, 0);
    }

    /**
     * Checks if the result has a warning.
     *
     * @return true if there is a warning
     */
    public boolean hasWarning() {
        return warning != null && !warning.isEmpty();
    }

    /**
     * Checks if the result has an error.
     *
     * @return true if there is an error
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * Gets the column count.
     *
     * @return the number of columns
     */
    public int getColumnCount() {
        return columnMetadata != null ? columnMetadata.size() : 0;
    }
}
