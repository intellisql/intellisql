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

package com.intellisql.kernel;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.intellisql.connector.ConnectorRegistry;
import com.intellisql.connector.api.DataSourceConnector;
import com.intellisql.kernel.config.DataSourceConfig;
import com.intellisql.kernel.config.HealthCheckConfig;
import com.intellisql.kernel.config.ModelConfig;
import com.intellisql.kernel.logger.StructuredLogger;
import com.intellisql.kernel.metadata.enums.DataSourceStatus;
import com.intellisql.kernel.metadata.enums.DataSourceType;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages data source connections and lifecycle. Handles connection pool initialization, health
 * checks, and cleanup.
 */
@Slf4j
public final class DataSourceManager implements AutoCloseable {

    @Getter
    private final ModelConfig config;

    private final Map<String, DataSourceConfig> dataSources;

    private final Map<String, DataSourceStatus> dataSourceStatuses;

    private final ScheduledExecutorService healthCheckScheduler;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final StructuredLogger structuredLogger =
            StructuredLogger.getLogger(DataSourceManager.class);

    /**
     * Creates a new DataSourceManager with the given configuration.
     *
     * @param config the model configuration
     */
    public DataSourceManager(final ModelConfig config) {
        this.config = config;
        this.dataSources = new ConcurrentHashMap<>(config.getDataSources());
        this.dataSourceStatuses = new ConcurrentHashMap<>();
        this.healthCheckScheduler =
                Executors.newSingleThreadScheduledExecutor(
                        r -> {
                            final Thread thread = new Thread(r, "datasource-health-check");
                            thread.setDaemon(true);
                            return thread;
                        });
    }

    /**
     * Initializes all data sources from the configuration. Loads connection pools and starts health
     * check scheduling.
     */
    public synchronized void initialize() {
        if (initialized.get()) {
            log.warn("DataSourceManager already initialized");
            return;
        }
        log.info("Initializing DataSourceManager with {} data sources", dataSources.size());
        for (final Map.Entry<String, DataSourceConfig> entry : dataSources.entrySet()) {
            final String name = entry.getKey();
            final DataSourceConfig dsConfig = entry.getValue();
            initializeDataSource(name, dsConfig);
        }
        startHealthCheckScheduler();
        initialized.set(true);
        log.info("DataSourceManager initialized successfully");
    }

    /**
     * Initializes a single data source.
     *
     * @param name the data source name
     * @param config the data source configuration
     */
    private void initializeDataSource(final String name, final DataSourceConfig config) {
        log.info("Initializing data source: {}", name);
        try {
            final DataSourceConnector connector = getConnectorForType(config.getType());
            final boolean connectionTest = connector.testConnection(convertToConnectorConfig(name, config));
            if (connectionTest) {
                dataSourceStatuses.put(name, DataSourceStatus.ACTIVE);
                log.info("Data source {} initialized successfully", name);
            } else {
                dataSourceStatuses.put(name, DataSourceStatus.FAILED);
                log.error("Data source {} connection test failed", name);
            }
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            dataSourceStatuses.put(name, DataSourceStatus.FAILED);
            log.error("Failed to initialize data source {}: {}", name, ex.getMessage(), ex);
        }
    }

    /**
     * Gets the connector for a specific data source type.
     *
     * @param type the data source type
     * @return the connector
     */
    private DataSourceConnector getConnectorForType(final DataSourceType type) {
        final com.intellisql.connector.enums.DataSourceType connectorType =
                com.intellisql.connector.enums.DataSourceType.valueOf(type.name());
        return ConnectorRegistry.getInstance().getConnector(connectorType);
    }

    /** Starts the health check scheduler. */
    private void startHealthCheckScheduler() {
        final HealthCheckConfig healthCheckConfig = getHealthCheckConfig();
        if (!healthCheckConfig.isEnabled()) {
            log.info("Health check scheduling is disabled");
            return;
        }
        final int intervalSeconds = healthCheckConfig.getIntervalSeconds();
        log.info("Starting health check scheduler with interval {} seconds", intervalSeconds);
        healthCheckScheduler.scheduleAtFixedRate(
                this::performHealthChecks, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    /**
     * Gets the health check configuration from the first data source or returns defaults.
     *
     * @return the health check configuration
     */
    private HealthCheckConfig getHealthCheckConfig() {
        return dataSources.values().stream()
                .findFirst()
                .map(DataSourceConfig::getHealthCheck)
                .orElse(HealthCheckConfig.builder().build());
    }

    /** Performs health checks on all data sources. */
    private void performHealthChecks() {
        log.debug("Performing health checks on {} data sources", dataSources.size());
        for (final Map.Entry<String, DataSourceConfig> entry : dataSources.entrySet()) {
            final String name = entry.getKey();
            final DataSourceConfig config = entry.getValue();
            performHealthCheck(name, config);
        }
    }

    /**
     * Performs a health check on a single data source.
     *
     * @param name the data source name
     * @param config the data source configuration
     */
    private void performHealthCheck(final String name, final DataSourceConfig config) {
        final HealthCheckConfig healthCheckConfig = config.getHealthCheck();
        if (!healthCheckConfig.isEnabled()) {
            return;
        }
        try {
            final DataSourceConnector connector = getConnectorForType(config.getType());
            final boolean isHealthy = connector.testConnection(convertToConnectorConfig(name, config));
            final DataSourceStatus newStatus =
                    isHealthy ? DataSourceStatus.ACTIVE : DataSourceStatus.FAILED;
            final DataSourceStatus oldStatus = dataSourceStatuses.put(name, newStatus);
            if (oldStatus != newStatus) {
                log.info("Data source {} status changed from {} to {}", name, oldStatus, newStatus);
            }
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            log.warn("Health check failed for data source {}: {}", name, ex.getMessage());
            dataSourceStatuses.put(name, DataSourceStatus.FAILED);
        }
    }

    /**
     * Checks if a data source exists.
     *
     * @param name the data source name
     * @return true if the data source exists
     */
    public boolean hasDataSource(final String name) {
        return dataSources.containsKey(name);
    }

    /**
     * Gets the configuration for a data source.
     *
     * @param name the data source name
     * @return the data source configuration
     * @throws IllegalArgumentException if the data source does not exist
     */
    public DataSourceConfig getDataSourceConfig(final String name) {
        final DataSourceConfig config = dataSources.get(name);
        if (config == null) {
            throw new IllegalArgumentException("Data source not found: " + name);
        }
        return config;
    }

    /**
     * Gets the status of a data source.
     *
     * @param name the data source name
     * @return the data source status
     */
    public DataSourceStatus getDataSourceStatus(final String name) {
        return dataSourceStatuses.getOrDefault(name, DataSourceStatus.UNKNOWN);
    }

    /**
     * Gets all configured data source names.
     *
     * @return the collection of data source names
     */
    public Collection<String> getDataSourceNames() {
        return dataSources.keySet();
    }

    /**
     * Gets all connectors for configured data sources.
     *
     * @return the collection of connectors
     */
    public Collection<DataSourceConnector> getConnectors() {
        return dataSources.values().stream()
                .map(config -> getConnectorForType(config.getType()))
                .collect(Collectors.toList());
    }

    /**
     * Adds a new data source at runtime.
     *
     * @param name the data source name
     * @param config the data source configuration
     * @throws IllegalArgumentException if the data source already exists.
     */
    public synchronized void addDataSource(final String name, final DataSourceConfig config) {
        if (dataSources.containsKey(name)) {
            throw new IllegalArgumentException("Data source already exists: " + name);
        }
        log.info("Adding data source: {}", name);
        dataSources.put(name, config);
        initializeDataSource(name, config);
    }

    /**
     * Removes a data source at runtime.
     *
     * @param name the data source name
     */
    public synchronized void removeDataSource(final String name) {
        if (!dataSources.containsKey(name)) {
            log.warn("Data source not found: {}", name);
            return;
        }
        log.info("Removing data source: {}", name);
        dataSources.remove(name);
        dataSourceStatuses.remove(name);
    }

    /**
     * Tests the connection to a data source.
     *
     * @param name the data source name
     * @return true if the connection is successful
     */
    public boolean testConnection(final String name) {
        final DataSourceConfig config = getDataSourceConfig(name);
        try {
            final DataSourceConnector connector = getConnectorForType(config.getType());
            return connector.testConnection(convertToConnectorConfig(name, config));
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            log.error("Connection test failed for data source {}: {}", name, ex.getMessage());
            return false;
        }
    }

    /**
     * Converts kernel DataSourceConfig to connector DataSourceConfig.
     *
     * @param name the data source name
     * @param kernelConfig the kernel configuration
     * @return the connector configuration
     */
    private com.intellisql.connector.config.DataSourceConfig convertToConnectorConfig(
                                                                                      final String name,
                                                                                      final DataSourceConfig kernelConfig) {
        return com.intellisql.connector.config.DataSourceConfig.builder()
                .name(name)
                .type(com.intellisql.connector.enums.DataSourceType.valueOf(kernelConfig.getType().name()))
                .jdbcUrl(kernelConfig.getUrl())
                .username(kernelConfig.getUsername())
                .password(kernelConfig.getPassword())
                .maxPoolSize(kernelConfig.getConnectionPool().getMaximumPoolSize())
                .minIdle(kernelConfig.getConnectionPool().getMinimumIdle())
                .connectionTimeout(kernelConfig.getConnectionPool().getConnectionTimeout())
                .idleTimeout(kernelConfig.getConnectionPool().getIdleTimeout())
                .maxLifetime(kernelConfig.getConnectionPool().getMaxLifetime())
                .build();
    }

    /**
     * Checks if the manager is initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    @Override
    public synchronized void close() {
        if (!initialized.get()) {
            return;
        }
        log.info("Shutting down DataSourceManager...");
        healthCheckScheduler.shutdown();
        try {
            if (!healthCheckScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                healthCheckScheduler.shutdownNow();
            }
        } catch (final InterruptedException ex) {
            healthCheckScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        dataSourceStatuses.clear();
        initialized.set(false);
        log.info("DataSourceManager shutdown completed");
    }
}
