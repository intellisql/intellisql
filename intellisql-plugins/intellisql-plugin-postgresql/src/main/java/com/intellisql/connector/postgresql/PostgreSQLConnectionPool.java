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

package com.intellisql.connector.postgresql;

import com.intellisql.connector.config.IntelliSQLDataSourceConfig;
import com.intellisql.connector.jdbc.JdbcConnectionPool;
import com.zaxxer.hikari.HikariConfig;

/**
 * Manages PostgreSQL connection pool using HikariCP. Configured with sslmode=require for production
 * environments.
 */
public class PostgreSQLConnectionPool extends JdbcConnectionPool {

    /**
     * Creates a new PostgreSQL connection pool.
     *
     * @param config the data source configuration
     */
    public PostgreSQLConnectionPool(final IntelliSQLDataSourceConfig config) {
        super(config);
    }

    @Override
    protected String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    protected String getDatabaseName() {
        return "PostgreSQL";
    }

    @Override
    protected String getPoolNamePrefix() {
        return "intellisql-pg-";
    }

    @Override
    protected String resolveJdbcUrl(final IntelliSQLDataSourceConfig config) {
        if (config.getJdbcUrl() != null && !config.getJdbcUrl().isEmpty()) {
            String url = config.getJdbcUrl();
            if (!url.contains("sslmode=")) {
                url = url + (url.contains("?") ? "&" : "?") + "sslmode=require";
            }
            return url;
        }
        final StringBuilder url = new StringBuilder("jdbc:postgresql://");
        url.append(config.getHost()).append(":").append(config.getPort());
        if (config.getDatabase() != null && !config.getDatabase().isEmpty()) {
            url.append("/").append(config.getDatabase());
        }
        url.append("?sslmode=require");
        if (config.getSchema() != null && !config.getSchema().isEmpty()) {
            url.append("&currentSchema=").append(config.getSchema());
        }
        return url.toString();
    }

    @Override
    protected void configureDataSourceProperties(final HikariConfig hikariConfig, final IntelliSQLDataSourceConfig config) {
        hikariConfig.addDataSourceProperty("preparedStatementCacheQueries", "256");
        hikariConfig.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
        hikariConfig.addDataSourceProperty("stringtype", "unspecified");
        hikariConfig.addDataSourceProperty("reWriteBatchedInserts", "true");
    }
}
