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

package com.intellisql.connector.mysql;

import com.intellisql.connector.config.IntelliSQLDataSourceConfig;
import com.intellisql.connector.jdbc.JdbcConnectionPool;
import com.zaxxer.hikari.HikariConfig;

/** Manages MySQL connection pool using HikariCP. */
public class MySQLConnectionPool extends JdbcConnectionPool {

    /**
     * Creates a new MySQL connection pool.
     *
     * @param config the data source configuration
     */
    public MySQLConnectionPool(final IntelliSQLDataSourceConfig config) {
        super(config);
    }

    @Override
    protected String getDriverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    protected String getDatabaseName() {
        return "MySQL";
    }

    @Override
    protected String getPoolNamePrefix() {
        return "intellisql-mysql-";
    }

    @Override
    protected void configureDataSourceProperties(final HikariConfig hikariConfig, final IntelliSQLDataSourceConfig config) {
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
    }
}
