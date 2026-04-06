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

import com.intellisql.connector.api.DataSourceConnector;
import com.intellisql.spi.loader.IntelliSqlServiceLoader;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * Registry for data source connectors. Uses SPI (ServiceLoader) to discover and register
 * connectors.
 */
@Slf4j
public final class ConnectorRegistry {

    private static final ConnectorRegistry INSTANCE = new ConnectorRegistry();

    private ConnectorRegistry() {
    }

    /**
     * Gets the singleton instance of the connector registry.
     *
     * @return the connector registry instance
     */
    public static ConnectorRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a connector with the registry.
     *
     * @param connector the connector to register
     */
    public void registerConnector(final DataSourceConnector connector) {
        IntelliSqlServiceLoader.register(DataSourceConnector.class);
        log.info("Connector '{}' registration is managed by ServiceLoader", connector.getType());
    }

    /**
     * Gets the connector for the specified data source type.
     *
     * @param type the data source type
     * @return the connector for the specified type
     * @throws IllegalArgumentException if no connector is registered for the type
     */
    public DataSourceConnector getConnector(final String type) {
        return IntelliSqlServiceLoader.getService(DataSourceConnector.class, type);
    }

    /**
     * Checks if a connector is registered for the specified data source type.
     *
     * @param type the data source type
     * @return true if a connector is registered, false otherwise
     */
    public boolean hasConnector(final String type) {
        try {
            getConnector(type);
            return true;
        } catch (final IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * Unregisters and closes the connector for the specified data source type.
     *
     * @param type the data source type
     */
    public void unregisterConnector(final String type) {
        DataSourceConnector connector = getConnector(type);
        connector.close();
    }

    /**
     * Get all registered connector types in sorted order.
     *
     * @return connector types
     */
    public List<String> getRegisteredTypes() {
        Collection<DataSourceConnector> connectors = IntelliSqlServiceLoader.getAllServices(DataSourceConnector.class);
        List<String> result = connectors.stream().map(DataSourceConnector::getType).sorted().collect(Collectors.toList());
        return Collections.unmodifiableList(result);
    }

    /** Closes all registered connectors and clears the registry. */
    public void closeAll() {
        IntelliSqlServiceLoader.getAllServices(DataSourceConnector.class).forEach(DataSourceConnector::close);
    }
}
