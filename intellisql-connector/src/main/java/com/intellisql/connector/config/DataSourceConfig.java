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

package com.intellisql.connector.config;

import java.util.Map;

import com.intellisql.connector.enums.DataSourceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for a data source connection. Contains all necessary parameters to establish and
 * manage a connection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceConfig {

    private String name;

    private DataSourceType type;

    private String host;

    private int port;

    private String database;

    private String username;

    private String password;

    private String schema;

    private String jdbcUrl;

    private String driverClassName;

    @Builder.Default
    private int maxPoolSize = 10;

    @Builder.Default
    private int minIdle = 2;

    @Builder.Default
    private long connectionTimeout = 30000;

    @Builder.Default
    private long idleTimeout = 600000;

    @Builder.Default
    private long maxLifetime = 1800000;

    private Map<String, String> properties;

    /**
     * Gets the effective JDBC URL, either from configured jdbcUrl or by building one from components.
     *
     * @return the effective JDBC URL
     */
    public String getEffectiveJdbcUrl() {
        if (jdbcUrl != null && !jdbcUrl.isEmpty()) {
            return jdbcUrl;
        }
        return buildJdbcUrl();
    }

    /**
     * Builds a JDBC URL from the configuration components.
     *
     * @return the built JDBC URL
     * @throws IllegalArgumentException if the data source type is unsupported
     */
    private String buildJdbcUrl() {
        StringBuilder url = new StringBuilder("jdbc:");
        switch (type) {
            case MYSQL:
                url.append("mysql://").append(host).append(":").append(port);
                if (database != null && !database.isEmpty()) {
                    url.append("/").append(database);
                }
                break;
            case POSTGRESQL:
                url.append("postgresql://").append(host).append(":").append(port);
                if (database != null && !database.isEmpty()) {
                    url.append("/").append(database);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported data source type: " + type);
        }
        return url.toString();
    }
}
