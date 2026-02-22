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

import com.intellisql.federation.IntelliSqlKernel;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler for IntelliSql server queries.
 * Bridges query requests to IntelliSql kernel.
 */
@Slf4j
public class IntelliSqlHandler {

    private final IntelliSqlKernel kernel;

    private final ConnectionManager connectionManager;

    private final StatementManager statementManager;

    /**
     * Creates a new IntelliSqlHandler.
     *
     * @param kernel the IntelliSql kernel
     */
    public IntelliSqlHandler(final IntelliSqlKernel kernel) {
        this.kernel = kernel;
        this.connectionManager = new ConnectionManager();
        this.statementManager = new StatementManager();
    }

    /**
     * Executes a SQL query.
     *
     * @param sql the SQL query
     * @return the query result
     * @throws SQLException if execution fails
     */
    public Object executeQuery(final String sql) throws SQLException {
        log.debug("Executing query: {}", sql);
        // CHECKSTYLE:OFF
        try {
            return kernel.query(sql);
        } catch (final RuntimeException ex) {
            log.error("Query execution failed: {}", ex.getMessage());
            throw new SQLException("Query execution failed", ex);
        }
        // CHECKSTYLE:ON
    }

    /**
     * Gets the connection manager.
     *
     * @return the connection manager
     */
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    /**
     * Gets the statement manager.
     *
     * @return the statement manager
     */
    public StatementManager getStatementManager() {
        return statementManager;
    }
}
