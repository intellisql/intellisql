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

package com.intellisql.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intellisql.common.metadata.enums.DataSourceType;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * Configuration loader for YAML-based configuration files. Supports environment variable
 * substitution using ${VAR_NAME} syntax.
 */
public final class ConfigLoader {

    private static final EnvironmentVariableSubstitutor SUBSTITUTOR =
            new EnvironmentVariableSubstitutor();

    /** Private constructor to prevent instantiation. */
    private ConfigLoader() {
    }

    /**
     * Loads configuration from a YAML file at the specified path.
     *
     * @param path the path to the YAML configuration file
     * @return the loaded ModelConfig
     * @throws IOException if the file cannot be read
     */
    public static ModelConfig load(final Path path) throws IOException {
        final String content = new String(Files.readAllBytes(path));
        final String substituted = SUBSTITUTOR.substitute(content);
        final LoaderOptions loaderOptions = new LoaderOptions();
        final Yaml yaml = new Yaml(new Constructor(Map.class, loaderOptions));
        final Map<String, Object> rawConfig = yaml.load(substituted);
        return parseModelConfig(rawConfig);
    }

    /**
     * Loads configuration from an input stream.
     *
     * @param inputStream the input stream containing YAML configuration
     * @return the loaded ModelConfig
     */
    public static ModelConfig load(final InputStream inputStream) {
        final LoaderOptions loaderOptions = new LoaderOptions();
        final Yaml yaml = new Yaml(new Constructor(Map.class, loaderOptions));
        final Map<String, Object> rawConfig = yaml.load(inputStream);
        return parseModelConfig(rawConfig);
    }

    @SuppressWarnings("unchecked")
    private static ModelConfig parseModelConfig(final Map<String, Object> rawConfig) {
        final Map<String, DataSourceConfig> dataSources = new HashMap<>();
        final Object dataSourcesRaw = rawConfig.get("dataSources");
        if (dataSourcesRaw instanceof List) {
            // Handle list format: dataSources: - name: ds1 ...
            final List<Map<String, Object>> dataSourcesList = (List<Map<String, Object>>) dataSourcesRaw;
            for (final Map<String, Object> dsConfig : dataSourcesList) {
                final String name = (String) dsConfig.get("name");
                if (name != null) {
                    dataSources.put(name, parseDataSourceConfig(dsConfig));
                }
            }
        } else if (dataSourcesRaw instanceof Map) {
            // Handle map format: dataSources: ds1: ...
            final Map<String, Object> dataSourcesMap = (Map<String, Object>) dataSourcesRaw;
            for (final Map.Entry<String, Object> entry : dataSourcesMap.entrySet()) {
                final String name = entry.getKey();
                final Map<String, Object> dsConfig = (Map<String, Object>) entry.getValue();
                dataSources.put(name, parseDataSourceConfig(dsConfig));
            }
        }
        final Map<String, Object> propsRaw = (Map<String, Object>) rawConfig.get("props");
        final Props props = propsRaw != null ? parseProps(propsRaw) : Props.builder().build();
        return ModelConfig.builder().dataSources(dataSources).props(props).build();
    }

    @SuppressWarnings("unchecked")
    private static DataSourceConfig parseDataSourceConfig(final Map<String, Object> config) {
        // Support both 'connectionPool' and 'pool' keys
        Map<String, Object> poolRaw = (Map<String, Object>) config.get("connectionPool");
        if (poolRaw == null) {
            poolRaw = (Map<String, Object>) config.get("pool");
        }
        final Map<String, Object> healthRaw = (Map<String, Object>) config.get("healthCheck");
        // Build URL from host/port/database if url not provided
        String url = getString(config, "url", null);
        if (url == null) {
            url = buildJdbcUrl(config);
        }
        final String typeStr = getString(config, "type", "mysql");
        final String username = getString(config, "username", null);
        final String password = getString(config, "password", null);
        return DataSourceConfig.builder()
                .type(
                        DataSourceType.valueOf(
                                typeStr.toUpperCase()))
                .url(url)
                .username(username)
                .password(password)
                .connectionPool(
                        poolRaw != null
                                ? parseConnectionPoolConfig(poolRaw)
                                : ConnectionPoolConfig.builder().build())
                .healthCheck(
                        healthRaw != null
                                ? parseHealthCheckConfig(healthRaw)
                                : HealthCheckConfig.builder().build())
                .build();
    }

    /**
     * Builds JDBC URL from host, port, and database configuration.
     *
     * @param config the configuration map containing connection details
     * @return the built JDBC URL string
     */
    private static String buildJdbcUrl(final Map<String, Object> config) {
        final Object typeObj = config.get("type");
        final String type = typeObj != null ? typeObj.toString().toLowerCase() : "unknown";
        final String host = getString(config, "host", "localhost");
        final Integer port = getPort(config);
        final String database = getString(config, "database", null);

        final StringBuilder url = new StringBuilder("jdbc:");
        switch (type) {
            case "mysql":
                url.append("mysql://").append(host);
                if (port != null) {
                    url.append(":").append(port);
                }
                if (database != null) {
                    url.append("/").append(database);
                }
                break;
            case "postgresql":
            case "postgres":
                url.append("postgresql://").append(host);
                if (port != null) {
                    url.append(":").append(port);
                }
                if (database != null) {
                    url.append("/").append(database);
                }
                break;
            default:
                url.append(type).append("://").append(host);
                if (port != null) {
                    url.append(":").append(port);
                }
                if (database != null) {
                    url.append("/").append(database);
                }
        }
        return url.toString();
    }

    private static Integer getPort(final Map<String, Object> config) {
        final Object value = config.get("port");
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private static String getString(final Map<String, Object> config, final String key, final String defaultValue) {
        final Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    private static ConnectionPoolConfig parseConnectionPoolConfig(final Map<String, Object> config) {
        // Support both 'maxPoolSize' and 'maximumPoolSize' keys
        int maxPoolSize = getInt(config, "maxPoolSize", 0);
        if (maxPoolSize == 0) {
            maxPoolSize = getInt(config, "maximumPoolSize", 10);
        }
        // Support both 'minIdle' and 'minimumIdle' keys
        int minIdle = getInt(config, "minIdle", -1);
        if (minIdle < 0) {
            minIdle = getInt(config, "minimumIdle", 2);
        }
        return ConnectionPoolConfig.builder()
                .maximumPoolSize(maxPoolSize)
                .minimumIdle(minIdle)
                .connectionTimeout(getLong(config, "connectionTimeout", 30000L))
                .idleTimeout(getLong(config, "idleTimeout", 600000L))
                .maxLifetime(getLong(config, "maxLifetime", 1800000L))
                .build();
    }

    private static HealthCheckConfig parseHealthCheckConfig(final Map<String, Object> config) {
        return HealthCheckConfig.builder()
                .enabled(getBoolean(config, "enabled", true))
                .intervalSeconds(getInt(config, "intervalSeconds", 30))
                .timeoutSeconds(getInt(config, "timeoutSeconds", 5))
                .failureThreshold(getInt(config, "failureThreshold", 3))
                .build();
    }

    private static Props parseProps(final Map<String, Object> config) {
        return Props.builder()
                .maxIntermediateRows(getInt(config, "maxIntermediateRows", 100000))
                .queryTimeoutSeconds(getInt(config, "queryTimeoutSeconds", 300))
                .defaultFetchSize(getInt(config, "defaultFetchSize", 1000))
                .enableQueryLogging(getBoolean(config, "enableQueryLogging", true))
                .logLevel((String) config.getOrDefault("logLevel", "INFO"))
                .build();
    }

    private static int getInt(
                              final Map<String, Object> config, final String key, final int defaultValue) {
        final Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private static long getLong(
                                final Map<String, Object> config, final String key, final long defaultValue) {
        final Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    private static boolean getBoolean(
                                      final Map<String, Object> config, final String key, final boolean defaultValue) {
        final Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}
