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

package com.intellisql.connector.postgresql;

import java.sql.Connection;
import java.sql.SQLException;

import com.intellisql.connector.config.DataSourceConfig;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages PostgreSQL connection pool using HikariCP. Configured with sslmode=require for production
 * environments.
 */
@Slf4j
@Getter
public class PostgreSQLConnectionPool {

    private final HikariDataSource dataSource;

    private final DataSourceConfig config;

    /**
     * Creates a new PostgreSQL connection pool.
     *
     * @param config the data source configuration
     */
    public PostgreSQLConnectionPool(final DataSourceConfig config) {
        this.config = config;
        this.dataSource = createDataSource(config);
        log.info("PostgreSQL connection pool initialized for: {}", config.getName());
    }

    private HikariDataSource createDataSource(final DataSourceConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        String jdbcUrl = buildJdbcUrl(config);
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
        hikariConfig.setMinimumIdle(config.getMinIdle());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setIdleTimeout(config.getIdleTimeout());
        hikariConfig.setMaxLifetime(config.getMaxLifetime());
        hikariConfig.setPoolName("intellisql-pg-" + config.getName());
        hikariConfig.addDataSourceProperty("preparedStatementCacheQueries", "256");
        hikariConfig.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
        hikariConfig.addDataSourceProperty("stringtype", "unspecified");
        hikariConfig.addDataSourceProperty("reWriteBatchedInserts", "true");
        if (config.getProperties() != null) {
            config.getProperties().forEach(hikariConfig::addDataSourceProperty);
        }
        return new HikariDataSource(hikariConfig);
    }

    private String buildJdbcUrl(final DataSourceConfig config) {
        if (config.getJdbcUrl() != null && !config.getJdbcUrl().isEmpty()) {
            String url = config.getJdbcUrl();
            if (!url.contains("sslmode=")) {
                url = url + (url.contains("?") ? "&" : "?") + "sslmode=require";
            }
            return url;
        }
        StringBuilder url = new StringBuilder("jdbc:postgresql://");
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

    /**
     * Gets a connection from the pool.
     *
     * @return the connection
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Tests the connection validity.
     *
     * @return true if valid
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (final SQLException ex) {
            log.error("PostgreSQL connection test failed", ex);
            return false;
        }
    }

    /**
     * Closes the connection pool.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("PostgreSQL connection pool closed for: {}", config.getName());
        }
    }

    /**
     * Gets active connections count.
     *
     * @return count
     */
    public int getActiveConnections() {
        return dataSource.getHikariPoolMXBean().getActiveConnections();
    }

    /**
     * Gets idle connections count.
     *
     * @return count
     */
    public int getIdleConnections() {
        return dataSource.getHikariPoolMXBean().getIdleConnections();
    }

    /**
     * Gets total connections count.
     *
     * @return count
     */
    public int getTotalConnections() {
        return dataSource.getHikariPoolMXBean().getTotalConnections();
    }
}
