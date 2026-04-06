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

package com.intellisql.spi.datasource;

import com.intellisql.spi.TypedSPI;

import java.util.Map;

/**
 * SPI for pluggable data source types.
 */
public interface DataSourceProvider extends TypedSPI {

    /**
     * Get the SQL dialect type used to render SQL for this data source.
     *
     * @return SQL dialect type
     */
    default String getTargetDialectType() {
        return getType();
    }

    /**
     * Build a JDBC URL from host/port/database fields.
     *
     * @param host host name
     * @param port port
     * @param database database name
     * @return JDBC URL
     */
    default String buildJdbcUrl(final String host, final Integer port, final String database) {
        return buildJdbcUrl(host, port, database, null, null);
    }

    /**
     * Build a JDBC URL from host/port/database fields and optional provider-specific settings.
     *
     * @param host host name
     * @param port port
     * @param database database name
     * @param schema default schema
     * @param properties provider-specific properties
     * @return JDBC URL
     */
    default String buildJdbcUrl(
                                final String host,
                                final Integer port,
                                final String database,
                                final String schema,
                                final Map<String, String> properties) {
        StringBuilder result = new StringBuilder("jdbc:")
                .append(getType().toLowerCase())
                .append("://")
                .append(null == host || host.isEmpty() ? "localhost" : host);
        if (null != port) {
            result.append(":").append(port);
        }
        if (null != database && !database.isEmpty()) {
            result.append("/").append(database);
        }
        return result.toString();
    }

    /**
     * Normalize an explicitly configured JDBC URL with provider-specific defaults.
     *
     * @param jdbcUrl configured JDBC URL
     * @param schema default schema
     * @param properties provider-specific properties
     * @return normalized JDBC URL
     */
    default String normalizeJdbcUrl(
                                    final String jdbcUrl, final String schema, final Map<String, String> properties) {
        return jdbcUrl;
    }
}
