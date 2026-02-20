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

package org.intellisql.kernel.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Structured logger for query execution with MDC context support. Provides structured logging with
 * query ID and other context information.
 */
@RequiredArgsConstructor
public class StructuredLogger {

    @Getter
    private final Logger logger;

    /**
     * Creates a StructuredLogger for the given class.
     *
     * @param clazz the class to create logger for
     * @return a new StructuredLogger instance
     */
    public static StructuredLogger getLogger(final Class<?> clazz) {
        return new StructuredLogger(LoggerFactory.getLogger(clazz));
    }

    /**
     * Logs an info message with query context.
     *
     * @param context the query context
     * @param message the log message
     * @param args optional message arguments
     */
    public void info(final QueryContext context, final String message, final Object... args) {
        withContext(context, () -> logger.info(message, args));
    }

    /**
     * Logs a debug message with query context.
     *
     * @param context the query context
     * @param message the log message
     * @param args optional message arguments
     */
    public void debug(final QueryContext context, final String message, final Object... args) {
        withContext(context, () -> logger.debug(message, args));
    }

    /**
     * Logs a warn message with query context.
     *
     * @param context the query context
     * @param message the log message
     * @param args optional message arguments
     */
    public void warn(final QueryContext context, final String message, final Object... args) {
        withContext(context, () -> logger.warn(message, args));
    }

    /**
     * Logs an error message with query context.
     *
     * @param context the query context
     * @param message the log message
     * @param throwable the exception to log
     */
    public void error(final QueryContext context, final String message, final Throwable throwable) {
        withContext(context, () -> logger.error(message, throwable));
    }

    /**
     * Logs an error message with query context and arguments.
     *
     * @param context the query context
     * @param message the log message
     * @param args optional message arguments
     */
    public void error(final QueryContext context, final String message, final Object... args) {
        withContext(context, () -> logger.error(message, args));
    }

    /**
     * Executes a logging operation with MDC context set.
     *
     * @param context the query context
     * @param runnable the logging operation to execute
     */
    private void withContext(final QueryContext context, final Runnable runnable) {
        if (context != null) {
            MDC.put("queryId", context.getQueryId());
            MDC.put("connectionId", context.getConnectionId());
            MDC.put("user", context.getUser());
        }
        try {
            runnable.run();
        } finally {
            if (context != null) {
                MDC.remove("queryId");
                MDC.remove("connectionId");
                MDC.remove("user");
            }
        }
    }
}
