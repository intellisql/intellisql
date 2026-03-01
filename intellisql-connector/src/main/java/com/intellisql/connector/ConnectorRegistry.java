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

package com.intellisql.connector;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import com.intellisql.connector.api.DataSourceConnector;
import com.intellisql.connector.enums.DataSourceType;

import lombok.extern.slf4j.Slf4j;

/**
 * Registry for data source connectors. Uses SPI (ServiceLoader) to discover and register
 * connectors.
 */
@Slf4j
public final class ConnectorRegistry {

    private static final ConnectorRegistry INSTANCE = new ConnectorRegistry();

    private final Map<DataSourceType, DataSourceConnector> connectors = new ConcurrentHashMap<>();

    private ConnectorRegistry() {
        loadConnectors();
    }

    /**
     * Gets the singleton instance of the connector registry.
     *
     * @return the connector registry instance
     */
    public static ConnectorRegistry getInstance() {
        return INSTANCE;
    }

    private void loadConnectors() {
        ServiceLoader<DataSourceConnector> loader = ServiceLoader.load(DataSourceConnector.class);
        for (DataSourceConnector connector : loader) {
            registerConnector(connector);
            log.info("Loaded connector for data source type: {}", connector.getDataSourceType());
        }
    }

    /**
     * Registers a connector with the registry.
     *
     * @param connector the connector to register
     */
    public void registerConnector(final DataSourceConnector connector) {
        connectors.put(connector.getDataSourceType(), connector);
    }

    /**
     * Gets the connector for the specified data source type.
     *
     * @param type the data source type
     * @return the connector for the specified type
     * @throws IllegalArgumentException if no connector is registered for the type
     */
    public DataSourceConnector getConnector(final DataSourceType type) {
        DataSourceConnector connector = connectors.get(type);
        if (connector == null) {
            throw new IllegalArgumentException("No connector registered for type: " + type);
        }
        return connector;
    }

    /**
     * Checks if a connector is registered for the specified data source type.
     *
     * @param type the data source type
     * @return true if a connector is registered, false otherwise
     */
    public boolean hasConnector(final DataSourceType type) {
        return connectors.containsKey(type);
    }

    /**
     * Unregisters and closes the connector for the specified data source type.
     *
     * @param type the data source type
     */
    public void unregisterConnector(final DataSourceType type) {
        DataSourceConnector connector = connectors.remove(type);
        if (connector != null) {
            connector.close();
        }
    }

    /** Closes all registered connectors and clears the registry. */
    public void closeAll() {
        connectors.values().forEach(DataSourceConnector::close);
        connectors.clear();
    }
}
