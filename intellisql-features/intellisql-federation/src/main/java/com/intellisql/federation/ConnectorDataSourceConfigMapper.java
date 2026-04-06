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

package com.intellisql.federation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps kernel data source configuration into connector-facing configuration.
 */
final class ConnectorDataSourceConfigMapper {

    private ConnectorDataSourceConfigMapper() {
    }

    /**
     * Convert a kernel data source config to a connector config.
     *
     * @param name data source name
     * @param kernelConfig kernel config
     * @return connector config
     */
    static com.intellisql.connector.config.DataSourceConfig toConnectorConfig(
                                                                              final String name, final com.intellisql.common.config.DataSourceConfig kernelConfig) {
        return com.intellisql.connector.config.DataSourceConfig.builder()
                .name(name)
                .type(kernelConfig.getType())
                .host(kernelConfig.getHost())
                .port(null == kernelConfig.getPort() ? 0 : kernelConfig.getPort())
                .database(kernelConfig.getDatabase())
                .schema(kernelConfig.getSchema())
                .jdbcUrl(kernelConfig.getUrl())
                .username(kernelConfig.getUsername())
                .password(kernelConfig.getPassword())
                .properties(copyProperties(kernelConfig.getProperties()))
                .maxPoolSize(kernelConfig.getConnectionPool().getMaximumPoolSize())
                .minIdle(kernelConfig.getConnectionPool().getMinimumIdle())
                .connectionTimeout(kernelConfig.getConnectionPool().getConnectionTimeout())
                .idleTimeout(kernelConfig.getConnectionPool().getIdleTimeout())
                .maxLifetime(kernelConfig.getConnectionPool().getMaxLifetime())
                .build();
    }

    private static Map<String, String> copyProperties(final Map<String, String> properties) {
        return null == properties ? null : new LinkedHashMap<>(properties);
    }
}
