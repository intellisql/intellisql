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

package com.intellisql.common.config;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/** Data source configuration containing connection details and pool settings. */
@Getter
@Builder
public class DataSourceConfig {

    /** Canonical data source type resolved by SPI. */
    private final String type;

    /** Data source host. */
    private final String host;

    /** Data source port. */
    private final Integer port;

    /** Logical database or catalog name. */
    private final String database;

    /** Default schema name. */
    private final String schema;

    /** JDBC connection URL. */
    private final String url;

    /** Database username. */
    private final String username;

    /** Database password. */
    private final String password;

    /** Connector-specific properties. */
    private final Map<String, String> properties;

    /** Connection pool configuration. */
    @Builder.Default
    private final ConnectionPoolConfig connectionPool = ConnectionPoolConfig.builder().build();

    /** Health check configuration. */
    @Builder.Default
    private final HealthCheckConfig healthCheck = HealthCheckConfig.builder().build();
}
