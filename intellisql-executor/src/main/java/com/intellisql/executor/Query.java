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

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.intellisql.executor.enums.QueryStatus;
import com.intellisql.optimizer.plan.ExecutionPlan;
import com.intellisql.parser.dialect.SqlDialect;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a SQL query entity with execution context and state. Tracks the complete lifecycle
 * from submission to completion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Query {

    /** Unique identifier for this query. */
    private String id;

    /** The raw SQL statement. */
    private String sql;

    /** The source SQL dialect. */
    private SqlDialect sourceDialect;

    /** Set of target data source IDs. */
    @Builder.Default
    private Set<String> targetDataSources = new HashSet<>();

    /** The optimized execution plan. */
    private ExecutionPlan executionPlan;

    /** Current execution status. */
    @Builder.Default
    private QueryStatus status = QueryStatus.PENDING;

    /** Execution start timestamp. */
    private Instant startTime;

    /** Execution end timestamp. */
    private Instant endTime;

    /** Number of rows returned. */
    private Long rowCount;

    /** Error information if failed. */
    private QueryError error;

    /**
     * Creates a new Query with a generated ID.
     *
     * @param sql the SQL statement
     * @return a new Query instance
     */
    public static Query of(final String sql) {
        return Query.builder()
                .id(UUID.randomUUID().toString())
                .sql(sql)
                .sourceDialect(SqlDialect.STANDARD)
                .status(QueryStatus.PENDING)
                .build();
    }

    /**
     * Creates a new Query with a generated ID and dialect.
     *
     * @param sql the SQL statement
     * @param dialect the source SQL dialect
     * @return a new Query instance
     */
    public static Query of(final String sql, final SqlDialect dialect) {
        return Query.builder()
                .id(UUID.randomUUID().toString())
                .sql(sql)
                .sourceDialect(dialect)
                .status(QueryStatus.PENDING)
                .build();
    }

    /**
     * Marks the query as started (RUNNING). Sets the start time to now.
     *
     * @throws IllegalStateException if the query is not in PENDING status
     */
    public void markStarted() {
        if (status != QueryStatus.PENDING) {
            throw new IllegalStateException("Cannot start query in status: " + status);
        }
        this.status = QueryStatus.RUNNING;
        this.startTime = Instant.now();
    }

    /**
     * Marks the query as completed successfully. Sets the end time to now and records the row count.
     *
     * @param rowCount the number of rows returned
     * @throws IllegalStateException if the query is not in RUNNING status
     */
    public void markCompleted(final long rowCount) {
        if (status != QueryStatus.RUNNING) {
            throw new IllegalStateException("Cannot complete query in status: " + status);
        }
        this.status = QueryStatus.COMPLETED;
        this.endTime = Instant.now();
        this.rowCount = rowCount;
    }

    /**
     * Marks the query as failed. Sets the end time to now and records the error.
     *
     * @param error the error that caused the failure
     * @throws IllegalStateException if the query is not in RUNNING status
     */
    public void markFailed(final QueryError error) {
        if (status != QueryStatus.RUNNING) {
            throw new IllegalStateException("Cannot fail query in status: " + status);
        }
        this.status = QueryStatus.FAILED;
        this.endTime = Instant.now();
        this.error = error;
    }

    /**
     * Marks the query as cancelled. Sets the end time to now.
     *
     * @throws IllegalStateException if the query is not in RUNNING status
     */
    public void markCancelled() {
        if (status != QueryStatus.RUNNING) {
            throw new IllegalStateException("Cannot cancel query in status: " + status);
        }
        this.status = QueryStatus.CANCELLED;
        this.endTime = Instant.now();
    }

    /**
     * Calculates the execution duration in milliseconds.
     *
     * @return the duration in milliseconds, or -1 if not started or completed
     */
    public long getExecutionDurationMs() {
        if (startTime == null) {
            return -1;
        }
        Instant end = endTime != null ? endTime : Instant.now();
        return end.toEpochMilli() - startTime.toEpochMilli();
    }

    /**
     * Checks if the query is in a terminal state.
     *
     * @return true if the query is completed, failed, or cancelled
     */
    public boolean isTerminal() {
        return status == QueryStatus.COMPLETED
                || status == QueryStatus.FAILED
                || status == QueryStatus.CANCELLED;
    }

    /**
     * Checks if the query is still running.
     *
     * @return true if the query is in RUNNING status
     */
    public boolean isRunning() {
        return status == QueryStatus.RUNNING;
    }

    /**
     * Checks if the query succeeded.
     *
     * @return true if the query is in COMPLETED status
     */
    public boolean isSuccessful() {
        return status == QueryStatus.COMPLETED;
    }

    /**
     * Checks if the query failed.
     *
     * @return true if the query is in FAILED status
     */
    public boolean isFailed() {
        return status == QueryStatus.FAILED;
    }

    /**
     * Adds a target data source to the query.
     *
     * @param dataSourceId the data source ID to add
     */
    public void addTargetDataSource(final String dataSourceId) {
        if (targetDataSources == null) {
            targetDataSources = new HashSet<>();
        }
        targetDataSources.add(dataSourceId);
    }
}
