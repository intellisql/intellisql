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

package com.intellisql.connector.api;

import com.intellisql.connector.config.DataSourceConfig;
import com.intellisql.connector.enums.DataSourceType;
import com.intellisql.connector.model.Schema;

/**
 * Main interface for data source connectors. Each supported database type should provide an
 * implementation of this interface.
 */
public interface DataSourceConnector {

    /**
     * Gets the data source type this connector handles.
     *
     * @return the data source type
     */
    DataSourceType getDataSourceType();

    /**
     * Establishes a connection to the data source.
     *
     * @param config the data source configuration
     * @return a connection to the data source
     * @throws Exception if connection fails
     */
    Connection connect(DataSourceConfig config) throws Exception;

    /**
     * Tests if a connection can be established to the data source.
     *
     * @param config the data source configuration
     * @return true if connection test succeeds, false otherwise
     */
    boolean testConnection(DataSourceConfig config);

    /**
     * Discovers and returns the schema of the data source.
     *
     * @param config the data source configuration
     * @return the discovered schema
     * @throws Exception if schema discovery fails
     */
    Schema discoverSchema(DataSourceConfig config) throws Exception;

    /** Closes the connector and releases all resources. */
    void close();
}
