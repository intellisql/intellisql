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

package org.intellisql.kernel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.intellisql.kernel.config.ModelConfig;
import org.intellisql.kernel.logger.QueryContext;
import org.intellisql.kernel.metadata.MetadataManager;
import org.intellisql.optimizer.Optimizer;
import org.intellisql.parser.SqlParserFactory;
import org.intellisql.parser.dialect.SqlDialect;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Main entry point for the IntelliSql kernel. Initializes all components and provides query
 * execution and SQL translation capabilities.
 */
@Slf4j
public final class IntelliSqlKernel implements AutoCloseable {

    @Getter
    private final ModelConfig config;

    private final DataSourceManager dataSourceManager;

    private final QueryProcessor queryProcessor;

    private final MetadataManager metadataManager;

    private final Optimizer optimizer;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a new IntelliSqlKernel instance with the given configuration.
     *
     * @param config the model configuration
     */
    public IntelliSqlKernel(final ModelConfig config) {
        this.config = config;
        this.dataSourceManager = new DataSourceManager(config);
        this.optimizer = new Optimizer(config.getProps().getMaxIntermediateRows());
        this.metadataManager = new MetadataManager();
        this.queryProcessor =
                new QueryProcessor(dataSourceManager, metadataManager, optimizer, config.getProps());
    }

    /**
     * Creates a new IntelliSqlKernel instance by loading configuration from a file.
     *
     * @param configPath the path to the configuration file
     * @return a new IntelliSqlKernel instance
     * @throws IOException if the configuration file cannot be read
     */
    public static IntelliSqlKernel create(final Path configPath) throws IOException {
        final ModelConfig config = org.intellisql.kernel.config.ConfigLoader.load(configPath);
        return new IntelliSqlKernel(config);
    }

    /**
     * Initializes the kernel and all its components. This method must be called before executing
     * queries.
     */
    public synchronized void initialize() {
        if (initialized.get()) {
            log.warn("Kernel already initialized");
            return;
        }
        log.info("Initializing IntelliSql kernel...");
        final long startTime = System.currentTimeMillis();
        dataSourceManager.initialize();
        initializeMetadata();
        initialized.set(true);
        final long duration = System.currentTimeMillis() - startTime;
        log.info("IntelliSql kernel initialized successfully in {}ms", duration);
    }

    private void initializeMetadata() {
        final java.util.Map<org.intellisql.connector.api.DataSourceConnector, org.intellisql.connector.config.DataSourceConfig> connectorMap = new java.util.HashMap<>();
        for (final String dsName : dataSourceManager.getDataSourceNames()) {
            final org.intellisql.kernel.config.DataSourceConfig kernelConfig =
                    dataSourceManager.getDataSourceConfig(dsName);
            final org.intellisql.connector.enums.DataSourceType connectorType =
                    org.intellisql.connector.enums.DataSourceType.valueOf(kernelConfig.getType().name());
            final org.intellisql.connector.api.DataSourceConnector connector =
                    org.intellisql.connector.ConnectorRegistry.getInstance().getConnector(connectorType);
            final org.intellisql.connector.config.DataSourceConfig connectorConfig =
                    org.intellisql.connector.config.DataSourceConfig.builder()
                            .name(dsName)
                            .type(connectorType)
                            .jdbcUrl(kernelConfig.getUrl())
                            .username(kernelConfig.getUsername())
                            .password(kernelConfig.getPassword())
                            .maxPoolSize(kernelConfig.getConnectionPool().getMaximumPoolSize())
                            .minIdle(kernelConfig.getConnectionPool().getMinimumIdle())
                            .connectionTimeout(kernelConfig.getConnectionPool().getConnectionTimeout())
                            .idleTimeout(kernelConfig.getConnectionPool().getIdleTimeout())
                            .maxLifetime(kernelConfig.getConnectionPool().getMaxLifetime())
                            .build();
            connectorMap.put(connector, connectorConfig);
        }
        metadataManager.initialize(connectorMap);
    }

    /**
     * Executes a SQL query and returns the result.
     *
     * @param sql the SQL query to execute
     * @return the query result
     */
    public org.intellisql.connector.model.QueryResult query(final String sql) {
        ensureInitialized();
        final QueryContext context = QueryContext.create(sql, "system", "kernel");
        return queryProcessor.process(sql, context);
    }

    /**
     * Executes a SQL query with a specific user and connection context.
     *
     * @param sql the SQL query to execute
     * @param user the user executing the query
     * @param connectionId the connection identifier
     * @return the query result
     */
    public org.intellisql.connector.model.QueryResult query(
                                                            final String sql, final String user, final String connectionId) {
        ensureInitialized();
        final QueryContext context = QueryContext.create(sql, user, connectionId);
        return queryProcessor.process(sql, context);
    }

    /**
     * Translates SQL from one dialect to another.
     *
     * @param sql the SQL to translate
     * @param sourceDialect the source SQL dialect
     * @param targetDialect the target SQL dialect
     * @return the translated SQL
     * @throws RuntimeException if translation fails
     */
    public String translate(
                            final String sql, final SqlDialect sourceDialect, final SqlDialect targetDialect) {
        ensureInitialized();
        log.debug("Translating SQL from {} to {}: {}", sourceDialect, targetDialect, sql);
        try {
            final org.apache.calcite.sql.SqlNode parsed = SqlParserFactory.parse(sql, sourceDialect);
            final org.apache.calcite.sql.SqlDialect targetCalciteDialect =
                    toCalciteDialect(targetDialect);
            final String translated = parsed.toSqlString(targetCalciteDialect).toString();
            log.debug("Translation completed: {}", translated);
            return translated;
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            log.error("Failed to translate SQL: {}", ex.getMessage(), ex);
            throw new RuntimeException("SQL translation failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Gets the query processor instance.
     *
     * @return the query processor
     */
    public QueryProcessor getQueryProcessor() {
        return queryProcessor;
    }

    /**
     * Gets the data source manager instance.
     *
     * @return the data source manager
     */
    public DataSourceManager getDataSourceManager() {
        return dataSourceManager;
    }

    /**
     * Gets the metadata manager instance.
     *
     * @return the metadata manager
     */
    public MetadataManager getMetadataManager() {
        return metadataManager;
    }

    /**
     * Checks if the kernel is initialized.
     *
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * Checks if the kernel is closed.
     *
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Ensures the kernel is initialized before performing operations.
     *
     * @throws IllegalStateException if the kernel is not initialized
     */
    private void ensureInitialized() {
        if (!initialized.get()) {
            throw new IllegalStateException("Kernel not initialized. Call initialize() first.");
        }
        if (closed.get()) {
            throw new IllegalStateException("Kernel has been closed.");
        }
    }

    /**
     * Converts a SqlDialect enum to Calcite's SqlDialect.
     *
     * @param dialect the IntelliSql dialect
     * @return the Calcite SqlDialect
     */
    private org.apache.calcite.sql.SqlDialect toCalciteDialect(final SqlDialect dialect) {
        switch (dialect) {
            case MYSQL:
                return org.apache.calcite.sql.dialect.MysqlSqlDialect.DEFAULT;
            case POSTGRESQL:
                return org.apache.calcite.sql.dialect.PostgresqlSqlDialect.DEFAULT;
            case ORACLE:
                return org.apache.calcite.sql.dialect.OracleSqlDialect.DEFAULT;
            case SQLSERVER:
                return org.apache.calcite.sql.dialect.MssqlSqlDialect.DEFAULT;
            case HIVE:
                return org.apache.calcite.sql.dialect.HiveSqlDialect.DEFAULT;
            case STANDARD:
            default:
                return org.apache.calcite.sql.dialect.AnsiSqlDialect.DEFAULT;
        }
    }

    @Override
    public synchronized void close() {
        if (closed.get()) {
            return;
        }
        log.info("Shutting down IntelliSql kernel...");
        final long startTime = System.currentTimeMillis();
        try {
            dataSourceManager.close();
            metadataManager.close();
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            log.error("Error during kernel shutdown: {}", ex.getMessage(), ex);
        }
        closed.set(true);
        initialized.set(false);
        final long duration = System.currentTimeMillis() - startTime;
        log.info("IntelliSql kernel shutdown completed in {}ms", duration);
    }
}
