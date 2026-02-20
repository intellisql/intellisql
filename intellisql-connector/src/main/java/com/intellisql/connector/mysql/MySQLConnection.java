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

package com.intellisql.connector.mysql;

import java.sql.SQLException;

import com.intellisql.connector.api.Connection;
import com.intellisql.connector.model.QueryResult;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * MySQL implementation of Connection interface. Wraps a JDBC connection from the connection pool.
 */
@Slf4j
public class MySQLConnection implements Connection {

    @Getter
    private final java.sql.Connection jdbcConnection;

    private final MySQLConnectionPool connectionPool;

    private final MySQLQueryExecutor queryExecutor;

    private volatile boolean closed;

    /**
     * Creates a new MySQL connection.
     *
     * @param jdbcConnection the JDBC connection
     * @param connectionPool the parent connection pool
     */
    public MySQLConnection(final java.sql.Connection jdbcConnection, final MySQLConnectionPool connectionPool) {
        this.jdbcConnection = jdbcConnection;
        this.connectionPool = connectionPool;
        this.queryExecutor = new MySQLQueryExecutor();
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
            log.error("Connection validity check failed", ex);
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
            log.debug("MySQL connection closed");
        } catch (final SQLException ex) {
            log.error("Error closing MySQL connection", ex);
        }
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Connection is already closed");
        }
    }
}
