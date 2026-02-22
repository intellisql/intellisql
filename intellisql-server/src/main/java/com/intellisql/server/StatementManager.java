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

package com.intellisql.server;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

/**
 * Manages JDBC statements for the IntelliSql server.
 * Tracks statement lifecycle and resource cleanup.
 */
@Slf4j
public class StatementManager {

    /** Map of statement ID to statement info. */
    private final Map<String, StatementInfo> statements = new ConcurrentHashMap<>();

    /** Counter for generating statement IDs. */
    private final AtomicInteger statementIdCounter = new AtomicInteger(0);

    /**
     * Creates a new statement and returns its ID.
     *
     * @param connectionId the connection ID
     * @return the statement ID
     */
    public String createStatement(final String connectionId) {
        final String statementId = "stmt-" + statementIdCounter.incrementAndGet();
        final StatementInfo info = new StatementInfo(connectionId, statementId);
        statements.put(statementId, info);
        log.debug("Created statement {} for connection {}", statementId, connectionId);
        return statementId;
    }

    /**
     * Gets statement info by ID.
     *
     * @param statementId the statement ID
     * @return the statement info, or null if not found
     */
    public StatementInfo getStatement(final String statementId) {
        return statements.get(statementId);
    }

    /**
     * Closes a statement.
     *
     * @param statementId the statement ID
     * @throws SQLException if close fails
     */
    public void closeStatement(final String statementId) throws SQLException {
        final StatementInfo info = statements.remove(statementId);
        if (info != null) {
            log.debug("Closed statement {}", statementId);
        }
    }

    /**
     * Closes all statements for a connection.
     *
     * @param connectionId the connection ID
     */
    public void closeStatementsForConnection(final String connectionId) {
        statements.entrySet().removeIf(entry -> {
            if (entry.getValue().getConnectionId().equals(connectionId)) {
                log.debug("Closing statement {} for connection {}",
                        entry.getKey(), connectionId);
                return true;
            }
            return false;
        });
    }

    /**
     * Gets the number of active statements.
     *
     * @return the count
     */
    public int getActiveStatementCount() {
        return statements.size();
    }

    /**
     * Closes all statements.
     */
    public void closeAll() {
        statements.clear();
        log.info("Closed all statements");
    }

    /**
     * Statement information holder.
     */
    public static class StatementInfo {

        private final String connectionId;

        private final String statementId;

        private volatile boolean closed;

        public StatementInfo(final String connectionId, final String statementId) {
            this.connectionId = connectionId;
            this.statementId = statementId;
        }

        public String getConnectionId() {
            return connectionId;
        }

        public String getStatementId() {
            return statementId;
        }

        public boolean isClosed() {
            return closed;
        }

        /**
         * Closes the statement.
         */
        public void close() {
            this.closed = true;
        }
    }
}
