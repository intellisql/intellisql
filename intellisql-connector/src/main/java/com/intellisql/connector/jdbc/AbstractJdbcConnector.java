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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.intellisql.common.metadata.Schema;
import com.intellisql.connector.api.Connection;
import com.intellisql.connector.api.DataSourceConnector;
import com.intellisql.connector.api.QueryExecutor;
import com.intellisql.connector.api.SchemaDiscoverer;
import com.intellisql.connector.config.DataSourceConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Shared JDBC connector lifecycle for connector plugins backed by a JDBC pool.
 *
 * @param <P> concrete JDBC connection pool type
 */
@Slf4j
public abstract class AbstractJdbcConnector<P extends JdbcConnectionPool> implements DataSourceConnector {

    private final Map<String, P> connectionPools = new ConcurrentHashMap<>();

    private final SchemaDiscoverer schemaDiscoverer;

    private final QueryExecutor queryExecutor;

    protected AbstractJdbcConnector(final SchemaDiscoverer schemaDiscoverer, final QueryExecutor queryExecutor) {
        this.schemaDiscoverer = schemaDiscoverer;
        this.queryExecutor = queryExecutor;
    }

    @Override
    public Connection connect(final DataSourceConfig config) throws Exception {
        final P pool = getOrCreatePool(config);
        return new JdbcConnectionAdapter(pool.getConnection(), queryExecutor, getDatabaseName());
    }

    @Override
    public boolean testConnection(final DataSourceConfig config) {
        try {
            final P pool = getOrCreatePool(config);
            final boolean success = pool.testConnection();
            log.info(
                    "{} connection test for '{}': {}",
                    getDatabaseName(),
                    config.getName(),
                    success ? "SUCCESS" : "FAILED");
            return success;
            // CHECKSTYLE:OFF: IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON: IllegalCatch
            log.error(
                    "{} connection test failed for '{}': {}",
                    getDatabaseName(),
                    config.getName(),
                    ex.getMessage(),
                    ex);
            return false;
        }
    }

    @Override
    public Schema discoverSchema(final DataSourceConfig config) throws Exception {
        final P pool = getOrCreatePool(config);
        try (java.sql.Connection connection = pool.getConnection()) {
            return schemaDiscoverer.discoverSchema(connection, config.getSchema(), config.getName());
        }
    }

    @Override
    public void close() {
        log.info("Closing all {} connection pools", getDatabaseName());
        connectionPools.values().forEach(JdbcConnectionPool::close);
        connectionPools.clear();
    }

    /**
     * Closes a specific connection pool.
     *
     * @param name the data source name
     */
    public void closePool(final String name) {
        final P pool = connectionPools.remove(name);
        if (pool != null) {
            pool.close();
            log.info("Closed {} connection pool for: {}", getDatabaseName(), name);
        }
    }

    /**
     * Gets active connections count for a pool.
     *
     * @param name the data source name
     * @return count
     */
    public int getActiveConnections(final String name) {
        final P pool = connectionPools.get(name);
        return pool != null ? pool.getActiveConnections() : 0;
    }

    /**
     * Gets idle connections count for a pool.
     *
     * @param name the data source name
     * @return count
     */
    public int getIdleConnections(final String name) {
        final P pool = connectionPools.get(name);
        return pool != null ? pool.getIdleConnections() : 0;
    }

    protected P getOrCreatePool(final DataSourceConfig config) {
        return connectionPools.computeIfAbsent(
                config.getName(),
                name -> {
                    log.info("Creating new {} connection pool for: {}", getDatabaseName(), name);
                    return createConnectionPool(config);
                });
    }

    protected abstract String getDatabaseName();

    protected abstract P createConnectionPool(DataSourceConfig config);
}
