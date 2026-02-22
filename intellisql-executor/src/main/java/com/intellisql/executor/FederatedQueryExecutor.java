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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.intellisql.optimizer.plan.ExecutionPlan;
import com.intellisql.optimizer.plan.ExecutionStage;

import lombok.extern.slf4j.Slf4j;

/**
 * Federated query executor for executing cross-source JOIN operations. Merges results in memory
 * from multiple data sources.
 */
@Slf4j
public class FederatedQueryExecutor {

    /** Maximum concurrent sub-queries. */
    private static final int MAX_CONCURRENT_QUERIES = 10;

    /** Default timeout for federated queries in seconds. */
    private static final long DEFAULT_TIMEOUT_SECONDS = 300;

    /** Thread pool for parallel execution. */
    private final ExecutorService executorService;

    /** Query executor for individual data sources. */
    private final QueryExecutor queryExecutor;

    /** Intermediate result limiter. */
    private final IntermediateResultLimiter resultLimiter;

    /** Connection provider interface. */
    private final ConnectionProvider connectionProvider;

    /**
     * Creates a new FederatedQueryExecutor with default settings.
     *
     * @param connectionProvider the connection provider
     */
    public FederatedQueryExecutor(final ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
        this.queryExecutor = new QueryExecutor();
        this.resultLimiter = new IntermediateResultLimiter();
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_QUERIES);
    }

    /**
     * Creates a new FederatedQueryExecutor with custom settings.
     *
     * @param connectionProvider the connection provider
     * @param maxConcurrentQueries maximum concurrent sub-queries
     */
    public FederatedQueryExecutor(final ConnectionProvider connectionProvider, final int maxConcurrentQueries) {
        this.connectionProvider = connectionProvider;
        this.queryExecutor = new QueryExecutor();
        this.resultLimiter = new IntermediateResultLimiter();
        this.executorService = Executors.newFixedThreadPool(maxConcurrentQueries);
    }

    /**
     * Executes a federated query across multiple data sources.
     *
     * @param query the query to execute
     * @param executionPlan the execution plan
     * @return the merged query result
     */
    public QueryResult execute(final Query query, final ExecutionPlan executionPlan) {
        final long startTime = System.currentTimeMillis();
        query.markStarted();
        log.info(
                "Executing federated query {} with {} stages",
                query.getId(),
                executionPlan.getStages().size());
        try {
            List<StageResult> stageResults = executeStages(query, executionPlan);
            QueryResult mergedResult = mergeResults(query, stageResults, executionPlan, startTime);
            if (mergedResult.isSuccess()) {
                query.markCompleted(mergedResult.getRowCount());
            } else {
                query.markFailed(mergedResult.getError());
            }
            return mergedResult;
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            log.error("Federated query {} failed: {}", query.getId(), ex.getMessage(), ex);
            QueryError error = QueryError.executionError("Federated query execution failed", ex);
            query.markFailed(error);
            return QueryResult.failure(query.getId(), error, System.currentTimeMillis() - startTime, 0);
        }
    }

    /**
     * Executes all stages of the execution plan.
     *
     * @param query the query to execute
     * @param executionPlan the execution plan
     * @return the list of stage results
     */
    private List<StageResult> executeStages(final Query query, final ExecutionPlan executionPlan) {
        List<StageResult> results = new ArrayList<>();
        for (ExecutionStage stage : executionPlan.getStages()) {
            log.debug("Executing stage {} for query {}", stage.getId(), query.getId());
            StageResult stageResult = executeStage(query, stage, executionPlan);
            results.add(stageResult);
            if (!stageResult.isSuccess()) {
                log.warn("Stage {} failed for query {}", stage.getId(), query.getId());
                break;
            }
        }
        return results;
    }

    /**
     * Executes a single stage.
     *
     * @param query the query to execute
     * @param stage the execution stage
     * @param executionPlan the execution plan
     * @return the stage result
     */
    private StageResult executeStage(final Query query, final ExecutionStage stage, final ExecutionPlan executionPlan) {
        String dataSourceId = stage.getDataSourceId();
        try {
            Connection connection = connectionProvider.getConnection(dataSourceId);
            if (connection == null) {
                return StageResult.failure(dataSourceId, QueryError.dataSourceNotFound(dataSourceId));
            }
            String stageSql = convertRelNodeToSql(stage.getOperation());
            Query stageQuery =
                    Query.builder()
                            .id(query.getId() + "-" + stage.getId())
                            .sql(stageSql)
                            .sourceDialect(query.getSourceDialect())
                            .executionPlan(executionPlan)
                            .build();
            QueryResult result = queryExecutor.execute(stageQuery, connection, executionPlan);
            List<Row> rows = collectRows(result.getResultSet());
            return StageResult.success(dataSourceId, rows, result.getColumnMetadata());
        } catch (final SQLException ex) {
            log.error(
                    "Failed to execute stage {} on datasource {}: {}",
                    stage.getId(),
                    dataSourceId,
                    ex.getMessage());
            return StageResult.failure(
                    dataSourceId, QueryError.executionError("Stage execution failed", ex));
        }
    }

    /**
     * Converts a RelNode to SQL string. This is a placeholder implementation - actual conversion
     * should use Calcite's SqlDialect.
     *
     * @param relNode the relational expression
     * @return the SQL string
     */
    private String convertRelNodeToSql(final org.apache.calcite.rel.RelNode relNode) {
        if (relNode == null) {
            return "";
        }
        return relNode.explain();
    }

    /**
     * Executes sub-queries in parallel.
     *
     * @param query the query to execute
     * @param dataSourceSqlMap map of data source IDs to SQL statements
     * @param executionPlan the execution plan
     * @return map of data source IDs to query results
     */
    public Map<String, QueryResult> executeParallel(
                                                    final Query query,
                                                    final Map<String, String> dataSourceSqlMap,
                                                    final ExecutionPlan executionPlan) {
        Map<String, QueryResult> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Map.Entry<String, String> entry : dataSourceSqlMap.entrySet()) {
            String dataSourceId = entry.getKey();
            String sql = entry.getValue();
            CompletableFuture<Void> future =
                    CompletableFuture.runAsync(
                            () -> executeParallelSubQuery(query, dataSourceId, sql, executionPlan, results),
                            executorService);
            futures.add(future);
        }
        CompletableFuture<Void> allFutures =
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            allFutures.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            // CHECKSTYLE:OFF
        } catch (final TimeoutException ex) {
            // CHECKSTYLE:ON
            log.warn("Parallel query execution timed out after {} seconds", DEFAULT_TIMEOUT_SECONDS);
            futures.forEach(f -> f.cancel(true));
            // CHECKSTYLE:OFF
        } catch (final InterruptedException ex) {
            // CHECKSTYLE:ON
            Thread.currentThread().interrupt();
            log.warn("Parallel query execution interrupted");
            // CHECKSTYLE:OFF
        } catch (final ExecutionException ex) {
            // CHECKSTYLE:ON
            log.error("Parallel query execution failed: {}", ex.getMessage());
        }
        return results;
    }

    /**
     * Executes a single sub-query in parallel.
     *
     * @param query the parent query
     * @param dataSourceId the data source ID
     * @param sql the SQL to execute
     * @param executionPlan the execution plan
     * @param results the results map to populate
     */
    private void executeParallelSubQuery(
                                         final Query query,
                                         final String dataSourceId,
                                         final String sql,
                                         final ExecutionPlan executionPlan,
                                         final Map<String, QueryResult> results) {
        try {
            Connection connection = connectionProvider.getConnection(dataSourceId);
            if (connection == null) {
                results.put(
                        dataSourceId,
                        QueryResult.failure(
                                query.getId(), QueryError.dataSourceNotFound(dataSourceId)));
                return;
            }
            Query subQuery =
                    Query.builder()
                            .id(query.getId() + "-" + dataSourceId)
                            .sql(sql)
                            .sourceDialect(query.getSourceDialect())
                            .executionPlan(executionPlan)
                            .build();
            QueryResult result = queryExecutor.execute(subQuery, connection, executionPlan);
            results.put(dataSourceId, result);
            // CHECKSTYLE:OFF
        } catch (final SQLException ex) {
            // CHECKSTYLE:ON
            log.error(
                    "Parallel query failed for datasource {}: {}", dataSourceId, ex.getMessage());
            results.put(
                    dataSourceId,
                    QueryResult.failure(
                            query.getId(), QueryError.executionError("Parallel query failed", ex)));
        }
    }

    /**
     * Merges results from multiple stages.
     *
     * @param query the query being executed
     * @param stageResults the list of stage results
     * @param executionPlan the execution plan
     * @param startTime the start time in milliseconds
     * @return the merged query result
     */
    private QueryResult mergeResults(
                                     final Query query,
                                     final List<StageResult> stageResults,
                                     final ExecutionPlan executionPlan,
                                     final long startTime) {
        for (StageResult result : stageResults) {
            if (!result.isSuccess()) {
                return QueryResult.failure(
                        query.getId(), result.getError(), System.currentTimeMillis() - startTime, 0);
            }
        }
        if (stageResults.isEmpty()) {
            return QueryResult.success(
                    query.getId(),
                    Collections.emptyIterator(),
                    Collections.emptyList(),
                    0,
                    System.currentTimeMillis() - startTime);
        }
        List<Row> mergedRows = new ArrayList<>();
        List<ColumnMetadata> mergedColumns = new ArrayList<>();
        for (StageResult result : stageResults) {
            if (result.getRows() != null) {
                mergedRows.addAll(result.getRows());
            }
            if (result.getColumnMetadata() != null && mergedColumns.isEmpty()) {
                mergedColumns.addAll(result.getColumnMetadata());
            }
        }
        IntermediateResultLimiter.LimitedResult limitedResult =
                resultLimiter.limit(mergedRows, executionPlan.getIntermediateResultLimit());
        Iterator<Row> iterator = limitedResult.getRows().iterator();
        if (limitedResult.isTruncated()) {
            return QueryResult.truncated(
                    query.getId(),
                    iterator,
                    mergedColumns,
                    limitedResult.getRowCount(),
                    limitedResult.getWarning(),
                    System.currentTimeMillis() - startTime);
        }
        return QueryResult.success(
                query.getId(),
                iterator,
                mergedColumns,
                limitedResult.getRowCount(),
                System.currentTimeMillis() - startTime);
    }

    /**
     * Performs in-memory JOIN between two result sets.
     *
     * @param leftRows left side rows
     * @param rightRows right side rows
     * @param leftKeyExtractor key extractor for left side
     * @param rightKeyExtractor key extractor for right side
     * @param joinType the join type (INNER, LEFT, RIGHT, FULL)
     * @return joined rows
     */
    public List<Row> join(
                          final List<Row> leftRows,
                          final List<Row> rightRows,
                          final KeyExtractor leftKeyExtractor,
                          final KeyExtractor rightKeyExtractor,
                          final JoinType joinType) {
        List<Row> result = new ArrayList<>();
        Map<Object, List<Row>> rightIndex = buildIndex(rightRows, rightKeyExtractor);
        for (Row leftRow : leftRows) {
            Object leftKey = leftKeyExtractor.extract(leftRow);
            List<Row> matchingRightRows = rightIndex.getOrDefault(leftKey, Collections.emptyList());
            if (matchingRightRows.isEmpty()) {
                if (joinType == JoinType.LEFT || joinType == JoinType.FULL) {
                    result.add(
                            createNullExtendedRow(
                                    leftRow, rightRows.isEmpty() ? 0 : rightRows.get(0).getColumnCount()));
                }
            } else {
                for (Row rightRow : matchingRightRows) {
                    result.add(mergeRows(leftRow, rightRow));
                }
            }
        }
        if (joinType == JoinType.RIGHT || joinType == JoinType.FULL) {
            Map<Object, List<Row>> leftIndex = buildIndex(leftRows, leftKeyExtractor);
            for (Row rightRow : rightRows) {
                Object rightKey = rightKeyExtractor.extract(rightRow);
                if (!leftIndex.containsKey(rightKey)) {
                    result.add(
                            createNullExtendedRow(
                                    rightRow, leftRows.isEmpty() ? 0 : leftRows.get(0).getColumnCount()));
                }
            }
        }
        return result;
    }

    /**
     * Builds a hash index for efficient join lookup.
     *
     * @param rows the rows to index
     * @param keyExtractor the key extractor
     * @return the hash index
     */
    private Map<Object, List<Row>> buildIndex(final List<Row> rows, final KeyExtractor keyExtractor) {
        Map<Object, List<Row>> index = new HashMap<>();
        for (Row row : rows) {
            Object key = keyExtractor.extract(row);
            index.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }
        return index;
    }

    /**
     * Merges two rows into one.
     *
     * @param left the left row
     * @param right the right row
     * @return the merged row
     */
    private Row mergeRows(final Row left, final Row right) {
        List<Object> leftValues = left.getValues();
        List<Object> rightValues = right.getValues();
        List<Object> merged = new ArrayList<>(leftValues.size() + rightValues.size());
        merged.addAll(leftValues);
        merged.addAll(rightValues);
        return new Row(merged, new ArrayList<>());
    }

    /**
     * Creates a row with null values for the specified column count.
     *
     * @param source the source row
     * @param nullColumnCount the number of null columns to add
     * @return the extended row
     */
    private Row createNullExtendedRow(final Row source, final int nullColumnCount) {
        List<Object> sourceValues = source.getValues();
        List<Object> extended = new ArrayList<>(sourceValues.size() + nullColumnCount);
        extended.addAll(sourceValues);
        for (int i = 0; i < nullColumnCount; i++) {
            extended.add(null);
        }
        return new Row(extended, new ArrayList<>());
    }

    /**
     * Collects all rows from an iterator into a list.
     *
     * @param iterator the row iterator
     * @return the list of rows
     */
    private List<Row> collectRows(final Iterator<Row> iterator) {
        List<Row> rows = new ArrayList<>();
        if (iterator != null) {
            while (iterator.hasNext()) {
                rows.add(iterator.next());
            }
        }
        return rows;
    }

    /** Shuts down the executor service. */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            // CHECKSTYLE:OFF
        } catch (final InterruptedException ex) {
            // CHECKSTYLE:ON
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Interface for providing database connections.
     */
    public interface ConnectionProvider {

        /**
         * Gets a connection for the specified data source.
         *
         * @param dataSourceId the data source ID
         * @return the connection, or null if not available
         * @throws SQLException if connection fails
         */
        Connection getConnection(String dataSourceId) throws SQLException;
    }

    /**
     * Interface for extracting join keys from rows.
     */
    @FunctionalInterface
    public interface KeyExtractor {

        /**
         * Extracts a key from the given row.
         *
         * @param row the row to extract the key from
         * @return the extracted key
         */
        Object extract(Row row);
    }

    /**
     * Enum for join types.
     */
    public enum JoinType {
        /** Inner join. */
        INNER,
        /** Left outer join. */
        LEFT,
        /** Right outer join. */
        RIGHT,
        /** Full outer join. */
        FULL
    }

    /**
     * Internal class to hold stage execution results.
     */
    private static final class StageResult {

        /** Whether the stage succeeded. */
        private final boolean success;

        /** The data source ID. */
        private final String dataSourceId;

        /** The result rows. */
        private final List<Row> rows;

        /** The column metadata. */
        private final List<ColumnMetadata> columnMetadata;

        /** The error if failed. */
        private final QueryError error;

        /**
         * Constructs a StageResult.
         *
         * @param success whether the stage succeeded
         * @param dataSourceId the data source ID
         * @param rows the result rows
         * @param columnMetadata the column metadata
         * @param error the error if failed
         */
        private StageResult(
                            final boolean success,
                            final String dataSourceId,
                            final List<Row> rows,
                            final List<ColumnMetadata> columnMetadata,
                            final QueryError error) {
            this.success = success;
            this.dataSourceId = dataSourceId;
            this.rows = rows;
            this.columnMetadata = columnMetadata;
            this.error = error;
        }

        /**
         * Creates a successful StageResult.
         *
         * @param dataSourceId the data source ID
         * @param rows the result rows
         * @param columnMetadata the column metadata
         * @return the StageResult
         */
        static StageResult success(
                                   final String dataSourceId, final List<Row> rows, final List<ColumnMetadata> columnMetadata) {
            return new StageResult(true, dataSourceId, rows, columnMetadata, null);
        }

        /**
         * Creates a failed StageResult.
         *
         * @param dataSourceId the data source ID
         * @param error the error
         * @return the StageResult
         */
        static StageResult failure(final String dataSourceId, final QueryError error) {
            return new StageResult(false, dataSourceId, null, null, error);
        }

        /**
         * Gets the success status.
         *
         * @return true if successful
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * Gets the data source ID.
         *
         * @return the data source ID
         */
        public String getDataSourceId() {
            return dataSourceId;
        }

        /**
         * Gets the result rows.
         *
         * @return the rows
         */
        public List<Row> getRows() {
            return rows;
        }

        /**
         * Gets the column metadata.
         *
         * @return the column metadata
         */
        public List<ColumnMetadata> getColumnMetadata() {
            return columnMetadata;
        }

        /**
         * Gets the error.
         *
         * @return the error
         */
        public QueryError getError() {
            return error;
        }
    }
}
