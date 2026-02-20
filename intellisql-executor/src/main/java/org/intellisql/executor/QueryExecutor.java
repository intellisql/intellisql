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

package org.intellisql.executor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.intellisql.kernel.metadata.enums.DataType;
import org.intellisql.optimizer.plan.ExecutionPlan;

import lombok.extern.slf4j.Slf4j;

/**
 * Query executor for executing SQL queries against data sources. Handles result set processing and
 * conversion to Row objects.
 */
@Slf4j
public class QueryExecutor {

    /** Maximum number of retries for transient failures. */
    private static final int MAX_RETRIES = 3;

    /** Initial retry delay in milliseconds. */
    private static final long INITIAL_RETRY_DELAY_MS = 100;

    /** Retry delay multiplier. */
    private static final double RETRY_MULTIPLIER = 2.0;

    /**
     * Executes a query using the provided connection and execution plan.
     *
     * @param query the query to execute
     * @param connection the database connection
     * @param executionPlan the execution plan
     * @return the query result
     */
    public QueryResult execute(
                               final Query query, final Connection connection, final ExecutionPlan executionPlan) {
        final long startTime = System.currentTimeMillis();
        int retryCount = 0;
        QueryError lastError = null;
        while (retryCount <= MAX_RETRIES) {
            try {
                return doExecute(query, connection, executionPlan, startTime, retryCount);
                // CHECKSTYLE:OFF
            } catch (final SQLException ex) {
                // CHECKSTYLE:ON
                lastError = handleSQLException(ex, retryCount);
                if (!isRetryable(ex) || retryCount >= MAX_RETRIES) {
                    break;
                }
                retryCount++;
                sleepWithBackoff(retryCount);
            }
        }
        query.markFailed(lastError);
        return QueryResult.failure(
                query.getId(), lastError, System.currentTimeMillis() - startTime, retryCount);
    }

    /**
     * Internal method to execute the query.
     *
     * @param query the query to execute
     * @param connection the database connection
     * @param executionPlan the execution plan
     * @param startTime the start time in milliseconds
     * @param retryCount the current retry count
     * @return the query result
     * @throws SQLException if database error occurs
     */
    private QueryResult doExecute(
                                  final Query query,
                                  final Connection connection,
                                  final ExecutionPlan executionPlan,
                                  final long startTime,
                                  final int retryCount) throws SQLException {
        query.markStarted();
        log.info("Executing query {} on thread {}", query.getId(), Thread.currentThread().getName());
        try (PreparedStatement stmt = connection.prepareStatement(query.getSql())) {
            if (executionPlan != null && executionPlan.getIntermediateResultLimit() > 0) {
                stmt.setFetchSize(executionPlan.getIntermediateResultLimit());
            }
            boolean hasResultSet = stmt.execute();
            if (!hasResultSet) {
                long updateCount = stmt.getUpdateCount();
                query.markCompleted(updateCount);
                return QueryResult.success(
                        query.getId(),
                        Collections.emptyIterator(),
                        Collections.emptyList(),
                        updateCount,
                        System.currentTimeMillis() - startTime,
                        retryCount);
            }
            return processResultSet(query, stmt.getResultSet(), startTime, retryCount);
        }
    }

    /**
     * Processes the result set and converts to Row objects.
     *
     * @param query the query being executed
     * @param rs the result set to process
     * @param startTime the start time in milliseconds
     * @param retryCount the current retry count
     * @return the query result
     * @throws SQLException if database error occurs
     */
    private QueryResult processResultSet(
                                         final Query query, final ResultSet rs, final long startTime, final int retryCount) throws SQLException {
        final ResultSetMetaData metaData = rs.getMetaData();
        final List<ColumnMetadata> columnMetadata = extractColumnMetadata(metaData);
        final int columnCount = metaData.getColumnCount();
        final int limit =
                query.getExecutionPlan() != null
                        ? query.getExecutionPlan().getIntermediateResultLimit()
                        : ExecutionPlan.DEFAULT_INTERMEDIATE_RESULT_LIMIT;
        final List<Row> rows = new ArrayList<>();
        boolean truncated = false;
        String warning = null;
        while (rs.next()) {
            if (rows.size() >= limit) {
                truncated = true;
                warning = "Result truncated at " + limit + " rows due to intermediate result limit";
                log.warn("Query {} result truncated at {} rows", query.getId(), limit);
                break;
            }
            final Object[] values = new Object[columnCount];
            for (int i = 0; i < columnCount; i++) {
                values[i] = rs.getObject(i + 1);
            }
            rows.add(new Row(values));
        }
        final long rowCount = rows.size();
        query.markCompleted(rowCount);
        final Iterator<Row> iterator = rows.iterator();
        final QueryResult result;
        if (truncated) {
            result =
                    QueryResult.truncated(
                            query.getId(),
                            iterator,
                            columnMetadata,
                            rowCount,
                            warning,
                            System.currentTimeMillis() - startTime);
        } else {
            result =
                    QueryResult.success(
                            query.getId(),
                            iterator,
                            columnMetadata,
                            rowCount,
                            System.currentTimeMillis() - startTime,
                            retryCount);
        }
        log.info(
                "Query {} completed with {} rows in {}ms",
                query.getId(),
                rowCount,
                result.getExecutionTimeMs());
        return result;
    }

    /**
     * Extracts column metadata from ResultSetMetaData.
     *
     * @param metaData the result set metadata
     * @return list of column metadata
     * @throws SQLException if database error occurs
     */
    private List<ColumnMetadata> extractColumnMetadata(final ResultSetMetaData metaData) throws SQLException {
        final List<ColumnMetadata> columns = new ArrayList<>();
        final int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            final ColumnMetadata column =
                    ColumnMetadata.builder()
                            .name(metaData.getColumnName(i))
                            .label(metaData.getColumnLabel(i))
                            .dataType(mapSqlTypeToDataType(metaData.getColumnType(i)))
                            .nullable(metaData.isNullable(i) != ResultSetMetaData.columnNoNulls)
                            .precision(metaData.getPrecision(i))
                            .scale(metaData.getScale(i))
                            .build();
            columns.add(column);
        }
        return columns;
    }

    /**
     * Maps SQL type to DataType enum.
     *
     * @param sqlType the SQL type from java.sql.Types
     * @return the corresponding DataType
     */
    private DataType mapSqlTypeToDataType(final int sqlType) {
        switch (sqlType) {
            case java.sql.Types.CHAR:
            case java.sql.Types.VARCHAR:
            case java.sql.Types.LONGVARCHAR:
            case java.sql.Types.NCHAR:
            case java.sql.Types.NVARCHAR:
            case java.sql.Types.LONGNVARCHAR:
            case java.sql.Types.CLOB:
            case java.sql.Types.NCLOB:
                return DataType.STRING;
            case java.sql.Types.BIT:
            case java.sql.Types.BOOLEAN:
                return DataType.BOOLEAN;
            case java.sql.Types.TINYINT:
            case java.sql.Types.SMALLINT:
            case java.sql.Types.INTEGER:
                return DataType.INTEGER;
            case java.sql.Types.BIGINT:
                return DataType.LONG;
            case java.sql.Types.FLOAT:
            case java.sql.Types.REAL:
            case java.sql.Types.DOUBLE:
                return DataType.DOUBLE;
            case java.sql.Types.NUMERIC:
            case java.sql.Types.DECIMAL:
                return DataType.DOUBLE;
            case java.sql.Types.DATE:
                return DataType.DATE;
            case java.sql.Types.TIME:
            case java.sql.Types.TIMESTAMP:
            case java.sql.Types.TIME_WITH_TIMEZONE:
            case java.sql.Types.TIMESTAMP_WITH_TIMEZONE:
                return DataType.TIMESTAMP;
            case java.sql.Types.BINARY:
            case java.sql.Types.VARBINARY:
            case java.sql.Types.LONGVARBINARY:
            case java.sql.Types.BLOB:
                return DataType.BINARY;
            case java.sql.Types.ARRAY:
                return DataType.ARRAY;
            default:
                return DataType.STRING;
        }
    }

    /**
     * Handles SQLException and creates appropriate QueryError.
     *
     * @param ex the SQLException
     * @param retryCount the current retry count
     * @return the QueryError
     */
    private QueryError handleSQLException(final SQLException ex, final int retryCount) {
        String errorCode = "SQL_ERROR";
        if (isTimeoutException(ex)) {
            errorCode = "CONN_TIMEOUT";
        } else if (isConnectionException(ex)) {
            errorCode = "CONN_ERROR";
        } else if (isSyntaxException(ex)) {
            errorCode = "SYNTAX_ERROR";
        }
        return QueryError.builder()
                .code(errorCode)
                .message(ex.getMessage())
                .retryable(isRetryable(ex))
                .stackTrace(getStackTraceString(ex))
                .build();
    }

    /**
     * Checks if the exception is retryable.
     *
     * @param ex the SQLException
     * @return true if retryable
     */
    private boolean isRetryable(final SQLException ex) {
        return isTimeoutException(ex) || isConnectionException(ex);
    }

    /**
     * Checks if the exception is a timeout.
     *
     * @param ex the SQLException
     * @return true if timeout exception
     */
    private boolean isTimeoutException(final SQLException ex) {
        final String state = ex.getSQLState();
        return state != null && (state.startsWith("08") || "HYT00".equals(state));
    }

    /**
     * Checks if the exception is a connection error.
     *
     * @param ex the SQLException
     * @return true if connection exception
     */
    private boolean isConnectionException(final SQLException ex) {
        final String state = ex.getSQLState();
        return state != null && state.startsWith("08");
    }

    /**
     * Checks if the exception is a syntax error.
     *
     * @param ex the SQLException
     * @return true if syntax exception
     */
    private boolean isSyntaxException(final SQLException ex) {
        final String state = ex.getSQLState();
        return state != null && state.startsWith("42");
    }

    /**
     * Sleeps with exponential backoff.
     *
     * @param retryCount the current retry count
     */
    private void sleepWithBackoff(final int retryCount) {
        final long delay = (long) (INITIAL_RETRY_DELAY_MS * Math.pow(RETRY_MULTIPLIER, retryCount - 1));
        try {
            Thread.sleep(delay);
            // CHECKSTYLE:OFF
        } catch (final InterruptedException ex) {
            // CHECKSTYLE:ON
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Extracts stack trace as string.
     *
     * @param throwable the throwable
     * @return the stack trace string
     */
    private String getStackTraceString(final Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (final StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }
        return sb.toString();
    }
}
