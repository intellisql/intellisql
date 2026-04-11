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

import java.sql.Connection;
import java.sql.SQLException;

import com.intellisql.connector.config.DataSourceConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** Shared HikariCP-backed JDBC connection pool base for connector plugins. */
@Slf4j
@Getter
public abstract class JdbcConnectionPool {

    private final HikariDataSource dataSource;

    private final DataSourceConfig config;

    /**
     * Creates a new JDBC connection pool.
     *
     * @param config the data source configuration
     */
    protected JdbcConnectionPool(final DataSourceConfig config) {
        this.config = config;
        this.dataSource = createDataSource(config);
        log.info("{} connection pool initialized for: {}", getDatabaseName(), config.getName());
    }

    private HikariDataSource createDataSource(final DataSourceConfig config) {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(resolveJdbcUrl(config));
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(getDriverClassName());
        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
        hikariConfig.setMinimumIdle(config.getMinIdle());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setIdleTimeout(config.getIdleTimeout());
        hikariConfig.setMaxLifetime(config.getMaxLifetime());
        hikariConfig.setPoolName(getPoolNamePrefix() + config.getName());
        configureDataSourceProperties(hikariConfig, config);
        if (config.getProperties() != null) {
            config.getProperties().forEach(hikariConfig::addDataSourceProperty);
        }
        return new HikariDataSource(hikariConfig);
    }

    /**
     * Gets a JDBC connection from the pool.
     *
     * @return the JDBC connection
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Tests the connection validity.
     *
     * @return true if the connection is valid
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (final SQLException ex) {
            log.error("{} connection test failed", getDatabaseName(), ex);
            return false;
        }
    }

    /** Closes the connection pool. */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("{} connection pool closed for: {}", getDatabaseName(), config.getName());
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

    protected String resolveJdbcUrl(final DataSourceConfig config) {
        return config.getEffectiveJdbcUrl();
    }

    protected abstract String getDriverClassName();

    protected abstract String getDatabaseName();

    protected abstract String getPoolNamePrefix();

    protected void configureDataSourceProperties(
                                                 final HikariConfig hikariConfig, final DataSourceConfig config) {
    }
}
