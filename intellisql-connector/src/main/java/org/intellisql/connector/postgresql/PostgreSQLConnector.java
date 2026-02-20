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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.intellisql.connector.api.Connection;
import org.intellisql.connector.api.DataSourceConnector;
import org.intellisql.connector.config.DataSourceConfig;
import org.intellisql.connector.enums.DataSourceType;
import org.intellisql.connector.model.Schema;

import lombok.extern.slf4j.Slf4j;

/**
 * PostgreSQL implementation of DataSourceConnector. Provides connection management and schema
 * discovery for PostgreSQL databases. Uses PostgreSQL JDBC Driver 42.7.1 with sslmode=require for
 * production.
 */
@Slf4j
public class PostgreSQLConnector implements DataSourceConnector {

    private final Map<String, PostgreSQLConnectionPool> connectionPools = new ConcurrentHashMap<>();

    private final PostgreSQLSchemaDiscoverer schemaDiscoverer = new PostgreSQLSchemaDiscoverer();

    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.POSTGRESQL;
    }

    @Override
    public Connection connect(final DataSourceConfig config) throws Exception {
        PostgreSQLConnectionPool pool = getOrCreatePool(config);
        java.sql.Connection jdbcConnection = pool.getConnection();
        return new PostgreSQLConnection(jdbcConnection, pool);
    }

    @Override
    public boolean testConnection(final DataSourceConfig config) {
        try {
            PostgreSQLConnectionPool pool = getOrCreatePool(config);
            boolean success = pool.testConnection();
            log.info(
                    "PostgreSQL connection test for '{}': {}",
                    config.getName(),
                    success ? "SUCCESS" : "FAILED");
            return success;
            // CHECKSTYLE:OFF: IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON: IllegalCatch
            log.error(
                    "PostgreSQL connection test failed for '{}': {}", config.getName(), ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    public Schema discoverSchema(final DataSourceConfig config) throws Exception {
        PostgreSQLConnectionPool pool = getOrCreatePool(config);
        try (java.sql.Connection connection = pool.getConnection()) {
            return schemaDiscoverer.discoverSchema(connection, config.getSchema());
        }
    }

    private PostgreSQLConnectionPool getOrCreatePool(final DataSourceConfig config) {
        return connectionPools.computeIfAbsent(
                config.getName(),
                name -> {
                    log.info("Creating new PostgreSQL connection pool for: {}", name);
                    return new PostgreSQLConnectionPool(config);
                });
    }

    @Override
    public void close() {
        log.info("Closing all PostgreSQL connection pools");
        connectionPools.values().forEach(PostgreSQLConnectionPool::close);
        connectionPools.clear();
    }

    /**
     * Closes a specific connection pool.
     *
     * @param name the data source name
     */
    public void closePool(final String name) {
        PostgreSQLConnectionPool pool = connectionPools.remove(name);
        if (pool != null) {
            pool.close();
            log.info("Closed PostgreSQL connection pool for: {}", name);
        }
    }

    /**
     * Gets active connections count for a pool.
     *
     * @param name the data source name
     * @return count
     */
    public int getActiveConnections(final String name) {
        PostgreSQLConnectionPool pool = connectionPools.get(name);
        return pool != null ? pool.getActiveConnections() : 0;
    }

    /**
     * Gets idle connections count for a pool.
     *
     * @param name the data source name
     * @return count
     */
    public int getIdleConnections(final String name) {
        PostgreSQLConnectionPool pool = connectionPools.get(name);
        return pool != null ? pool.getIdleConnections() : 0;
    }
}
