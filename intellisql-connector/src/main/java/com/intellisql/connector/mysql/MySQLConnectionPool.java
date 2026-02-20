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

import java.sql.Connection;
import java.sql.SQLException;

import com.intellisql.connector.config.DataSourceConfig;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** Manages MySQL connection pool using HikariCP. */
@Slf4j
@Getter
public class MySQLConnectionPool {

    private final HikariDataSource dataSource;

    private final DataSourceConfig config;

    /**
     * Creates a new MySQL connection pool.
     *
     * @param config the data source configuration
     */
    public MySQLConnectionPool(final DataSourceConfig config) {
        this.config = config;
        this.dataSource = createDataSource(config);
        log.info("MySQL connection pool initialized for: {}", config.getName());
    }

    private HikariDataSource createDataSource(final DataSourceConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getEffectiveJdbcUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
        hikariConfig.setMinimumIdle(config.getMinIdle());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setIdleTimeout(config.getIdleTimeout());
        hikariConfig.setMaxLifetime(config.getMaxLifetime());
        hikariConfig.setPoolName("intellisql-mysql-" + config.getName());
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
        if (config.getProperties() != null) {
            config.getProperties().forEach(hikariConfig::addDataSourceProperty);
        }
        return new HikariDataSource(hikariConfig);
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
            log.error("Connection test failed", ex);
            return false;
        }
    }

    /**
     * Closes the connection pool.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("MySQL connection pool closed for: {}", config.getName());
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
