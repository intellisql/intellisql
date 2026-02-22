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

package com.intellisql.common.retry;

import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Detector for transient errors that can be safely retried. Identifies temporary failures that may
 * succeed on subsequent attempts.
 */
public class TransientErrorDetector {

    private static final Set<String> TRANSIENT_SQL_STATES =
            new HashSet<>(
                    Arrays.asList(
                            "08001", "08004", "08006", "08007", "08502", "08506", "40001", "40XL1", "40XL2",
                            "57014", "57P01", "57P02", "57P03"));

    private static final Set<Integer> TRANSIENT_SQL_CODES =
            new HashSet<>(
                    Arrays.asList(1040, 1205, 1213, 2006, 2013, 2061, 2062, 2063, 2064, 2066, 2067, 2068));

    /**
     * Determines if an exception represents a transient error that can be retried.
     *
     * @param exception the exception to check
     * @return true if the error is transient and can be retried
     */
    public boolean isTransient(final Exception exception) {
        if (exception instanceof SocketTimeoutException || exception instanceof TimeoutException) {
            return true;
        }
        if (exception instanceof SQLException) {
            return isTransientSQLException((SQLException) exception);
        }
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof SocketTimeoutException || cause instanceof TimeoutException) {
                return true;
            }
            if (cause instanceof SQLException) {
                return isTransientSQLException((SQLException) cause);
            }
            cause = cause.getCause();
        }
        return isTransientByMessage(exception.getMessage());
    }

    /**
     * Checks if a SQLException is transient based on SQL state or error code.
     *
     * @param sqlException the SQL exception
     * @return true if the exception is transient
     */
    private boolean isTransientSQLException(final SQLException sqlException) {
        final String sqlState = sqlException.getSQLState();
        final int errorCode = sqlException.getErrorCode();
        if (sqlState != null && TRANSIENT_SQL_STATES.contains(sqlState)) {
            return true;
        }
        if (TRANSIENT_SQL_CODES.contains(errorCode)) {
            return true;
        }
        return isTransientByMessage(sqlException.getMessage());
    }

    /**
     * Checks if an exception message indicates a transient error.
     *
     * @param message the exception message
     * @return true if the message indicates a transient error
     */
    private boolean isTransientByMessage(final String message) {
        if (message == null) {
            return false;
        }
        final String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("connection reset")
                || lowerMessage.contains("connection closed")
                || lowerMessage.contains("connection timed out")
                || lowerMessage.contains("timeout")
                || lowerMessage.contains("deadlock")
                || lowerMessage.contains("lock wait timeout")
                || lowerMessage.contains("too many connections")
                || lowerMessage.contains("temporarily unavailable")
                || lowerMessage.contains("resource temporarily unavailable")
                || lowerMessage.contains("try again")
                || lowerMessage.contains("retry");
    }
}
