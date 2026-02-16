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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.MDC;

/**
 * Manager for query context storage and MDC management. Provides thread-local storage for query
 * context and MDC integration.
 */
public final class QueryContextManager {

    private static final ThreadLocal<QueryContext> CURRENT_CONTEXT = new ThreadLocal<>();

    private static final Map<String, QueryContext> ACTIVE_CONTEXTS = new ConcurrentHashMap<>();

    private QueryContextManager() {
    }

    /**
     * Gets the current query context for this thread.
     *
     * @return the current query context, or null if not set
     */
    public static QueryContext getCurrentContext() {
        return CURRENT_CONTEXT.get();
    }

    /**
     * Sets the current query context for this thread. Also updates MDC with context information.
     *
     * @param context the query context to set
     */
    public static void setContext(final QueryContext context) {
        CURRENT_CONTEXT.set(context);
        if (context != null) {
            ACTIVE_CONTEXTS.put(context.getQueryId(), context);
            MDC.put("queryId", context.getQueryId());
            if (context.getUser() != null) {
                MDC.put("user", context.getUser());
            }
            if (context.getConnectionId() != null) {
                MDC.put("connectionId", context.getConnectionId());
            }
        }
    }

    /**
     * Clears the current query context for this thread. Also removes context from MDC and active
     * contexts map.
     */
    public static void clearContext() {
        final QueryContext context = CURRENT_CONTEXT.get();
        if (context != null) {
            ACTIVE_CONTEXTS.remove(context.getQueryId());
        }
        CURRENT_CONTEXT.remove();
        MDC.remove("queryId");
        MDC.remove("user");
        MDC.remove("connectionId");
    }

    /**
     * Gets a query context by its ID from the active contexts.
     *
     * @param queryId the query ID
     * @return the query context, or null if not found
     */
    public static QueryContext getContextById(final String queryId) {
        return ACTIVE_CONTEXTS.get(queryId);
    }

    /**
     * Returns the number of active query contexts.
     *
     * @return the number of active contexts
     */
    public static int getActiveContextCount() {
        return ACTIVE_CONTEXTS.size();
    }

    /**
     * Executes a task with the given query context set.
     *
     * @param context the query context
     * @param runnable the task to execute
     */
    public static void runWithContext(final QueryContext context, final Runnable runnable) {
        setContext(context);
        try {
            runnable.run();
        } finally {
            clearContext();
        }
    }
}
