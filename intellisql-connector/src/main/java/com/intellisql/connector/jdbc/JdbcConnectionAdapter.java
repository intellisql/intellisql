/*
 * Licensed to the IntelliSql Project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The IntelliSql Project licenses this file to You under the Apache License, Version 2.0
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

package com.intellisql.connector.jdbc;

import java.sql.SQLException;

import com.intellisql.connector.api.Connection;
import com.intellisql.connector.api.QueryExecutor;
import com.intellisql.connector.model.QueryResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** Generic JDBC-backed connection adapter shared by connector plugins. */
@Slf4j
public class JdbcConnectionAdapter implements Connection {

    @Getter
    private final java.sql.Connection jdbcConnection;

    private final QueryExecutor queryExecutor;

    private final String databaseName;

    private volatile boolean closed;

    /**
     * Creates a new JDBC-backed connection adapter.
     *
     * @param jdbcConnection the JDBC connection
     * @param queryExecutor the query executor to use
     * @param databaseName the user-facing database label for logging
     */
    public JdbcConnectionAdapter(
                                 final java.sql.Connection jdbcConnection, final QueryExecutor queryExecutor, final String databaseName) {
        this.jdbcConnection = jdbcConnection;
        this.queryExecutor = queryExecutor;
        this.databaseName = databaseName;
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
            log.error("{} connection validity check failed", databaseName, ex);
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
            log.debug("{} connection closed", databaseName);
        } catch (final SQLException ex) {
            log.error("Error closing {} connection", databaseName, ex);
        }
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException(databaseName + " connection is already closed");
        }
    }
}
