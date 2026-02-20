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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a structured error from query execution. Contains error code, message, and diagnostic
 * information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryError {

    /** Error code for programmatic error handling. */
    private String code;

    /** Human-readable error message. */
    private String message;

    /** ID of the data source where the error occurred (if applicable). */
    private String dataSourceId;

    /** Whether the error is retryable. */
    private boolean retryable;

    /** Stack trace for debugging (optional). */
    private String stackTrace;

    /**
     * Creates a simple QueryError with code and message.
     *
     * @param code the error code
     * @param message the error message
     * @return a new QueryError instance
     */
    public static QueryError of(final String code, final String message) {
        return QueryError.builder().code(code).message(message).retryable(false).build();
    }

    /**
     * Creates a QueryError from an exception.
     *
     * @param code the error code
     * @param message the error message
     * @param cause the exception that caused the error
     * @return a new QueryError instance
     */
    public static QueryError of(final String code, final String message, final Throwable cause) {
        QueryErrorBuilder builder = QueryError.builder().code(code).message(message);
        if (cause != null) {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement element : cause.getStackTrace()) {
                sb.append(element.toString()).append("\n");
            }
            builder.stackTrace(sb.toString());
        }
        return builder.build();
    }

    /**
     * Creates a connection timeout error.
     *
     * @param dataSourceId the data source ID
     * @return a new QueryError instance
     */
    public static QueryError connectionTimeout(final String dataSourceId) {
        return QueryError.builder()
                .code("CONN_TIMEOUT")
                .message("Connection timeout while connecting to data source: " + dataSourceId)
                .dataSourceId(dataSourceId)
                .retryable(true)
                .build();
    }

    /**
     * Creates a connection refused error.
     *
     * @param dataSourceId the data source ID
     * @return a new QueryError instance
     */
    public static QueryError connectionRefused(final String dataSourceId) {
        return QueryError.builder()
                .code("CONN_REFUSED")
                .message("Connection refused by data source: " + dataSourceId)
                .dataSourceId(dataSourceId)
                .retryable(true)
                .build();
    }

    /**
     * Creates a syntax error.
     *
     * @param message the error message
     * @return a new QueryError instance
     */
    public static QueryError syntaxError(final String message) {
        return QueryError.builder().code("SYNTAX_ERROR").message(message).retryable(false).build();
    }

    /**
     * Creates an execution error.
     *
     * @param message the error message
     * @param cause the exception that caused the error
     * @return a new QueryError instance
     */
    public static QueryError executionError(final String message, final Throwable cause) {
        return QueryError.builder()
                .code("EXEC_ERROR")
                .message(message)
                .retryable(false)
                .stackTrace(getStackTraceString(cause))
                .build();
    }

    /**
     * Creates a data source not found error.
     *
     * @param dataSourceId the data source ID
     * @return a new QueryError instance
     */
    public static QueryError dataSourceNotFound(final String dataSourceId) {
        return QueryError.builder()
                .code("DS_NOT_FOUND")
                .message("Data source not found: " + dataSourceId)
                .dataSourceId(dataSourceId)
                .retryable(false)
                .build();
    }

    /**
     * Creates an intermediate result limit exceeded error.
     *
     * @param limit the limit that was exceeded
     * @return a new QueryError instance
     */
    public static QueryError intermediateLimitExceeded(final int limit) {
        return QueryError.builder()
                .code("LIMIT_EXCEEDED")
                .message("Intermediate result limit exceeded: " + limit + " rows")
                .retryable(false)
                .build();
    }

    /**
     * Extracts stack trace as string from a throwable.
     *
     * @param throwable the throwable
     * @return the stack trace as string
     */
    private static String getStackTraceString(final Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }
        return sb.toString();
    }
}
