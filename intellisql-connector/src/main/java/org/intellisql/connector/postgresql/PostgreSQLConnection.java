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

package org.intellisql.connector.postgresql;

import java.sql.SQLException;

import org.intellisql.connector.api.Connection;
import org.intellisql.connector.model.QueryResult;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * PostgreSQL implementation of Connection interface. Wraps a JDBC connection from the connection
 * pool.
 */
@Slf4j
public class PostgreSQLConnection implements Connection {

    @Getter
    private final java.sql.Connection jdbcConnection;

    private final PostgreSQLConnectionPool connectionPool;

    private final PostgreSQLQueryExecutor queryExecutor;

    private volatile boolean closed;

    /**
     * Creates a new PostgreSQL connection.
     *
     * @param jdbcConnection the JDBC connection
     * @param connectionPool the parent connection pool
     */
    public PostgreSQLConnection(
                                final java.sql.Connection jdbcConnection, final PostgreSQLConnectionPool connectionPool) {
        this.jdbcConnection = jdbcConnection;
        this.connectionPool = connectionPool;
        this.queryExecutor = new PostgreSQLQueryExecutor();
    }

    @Override
    public QueryResult executeQuery(final String sql) throws Exception {
        checkNotClosed();
        return queryExecutor.executeQuery(jdbcConnection, sql);
    }

    @Override
    public int executeUpdate(final String sql) throws Exception {
        checkNotClosed();
        return queryExecutor.executeUpdate(jdbcConnection, sql);
    }

    @Override
    public boolean isValid() {
        if (closed) {
            return false;
        }
        try {
            return jdbcConnection.isValid(5);
        } catch (final SQLException ex) {
            log.error("PostgreSQL connection validity check failed", ex);
            return false;
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        try {
            jdbcConnection.close();
            closed = true;
            log.debug("PostgreSQL connection closed");
        } catch (final SQLException ex) {
            log.error("Error closing PostgreSQL connection", ex);
        }
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("PostgreSQL connection is already closed");
        }
    }
}
