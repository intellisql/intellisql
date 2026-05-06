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

package com.intellisql.test.e2e.framework.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/** Loads JDBC E2E runner configuration from YAML resources. */
public final class E2ERunnerConfigLoader {

    private static final String DEFAULT_RESOURCE = "e2e/runner.yaml";

    /**
     * Loads the default JDBC E2E runner configuration.
     *
     * @return loaded runner configuration
     */
    public E2ERunnerConfig loadDefault() {
        return load(DEFAULT_RESOURCE);
    }

    /**
     * Loads a JDBC E2E runner configuration resource.
     *
     * @param resourcePath the classpath resource path
     * @return loaded runner configuration
     * @throws IllegalStateException if the resource cannot be loaded
     */
    public E2ERunnerConfig load(final String resourcePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Runner config resource not found: " + resourcePath);
            }
            return parse(new Yaml().load(inputStream));
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to load runner config: " + resourcePath, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private E2ERunnerConfig parse(final Object rawConfig) {
        Map<String, Object> root = rawConfig instanceof Map ? (Map<String, Object>) rawConfig : new HashMap<String, Object>(0);
        return E2ERunnerConfig.builder()
                .execution(parseExecution(getMap(root, "execution")))
                .containers(parseContainers(getMap(root, "containers")))
                .assertion(parseAssertion(getMap(root, "assertion")))
                .build();
    }

    private E2ERunnerConfig.ExecutionConfig parseExecution(final Map<String, Object> config) {
        return E2ERunnerConfig.ExecutionConfig.builder()
                .mode(getString(config, "mode", "docker"))
                .serverPort(getInt(config, "serverPort", 0))
                .jdbcDatabase(getString(config, "jdbcDatabase", "intellisql"))
                .caseRoot(getString(config, "caseRoot", "e2e/cases"))
                .defaultModel(getString(config, "defaultModel", "basic"))
                .defaultFetchSize(getInt(config, "defaultFetchSize", 1000))
                .timeoutSeconds(getInt(config, "timeoutSeconds", 60))
                .build();
    }

    private Map<String, E2ERunnerConfig.ContainerConfig> parseContainers(final Map<String, Object> config) {
        Map<String, E2ERunnerConfig.ContainerConfig> result = new HashMap<>(Math.max(config.size(), 1));
        if (config.isEmpty()) {
            result.put("postgresql", E2ERunnerConfig.ContainerConfig.builder().build());
            return result;
        }
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            result.put(entry.getKey(), parseContainer(getObjectMap(entry.getValue())));
        }
        return result;
    }

    private E2ERunnerConfig.ContainerConfig parseContainer(final Map<String, Object> config) {
        return E2ERunnerConfig.ContainerConfig.builder()
                .enabled(getBoolean(config, "enabled", true))
                .image(getString(config, "image", "postgres:15-alpine"))
                .database(getString(config, "database", "testdb"))
                .username(getString(config, "username", "testuser"))
                .password(getString(config, "password", "testpass"))
                .build();
    }

    private E2ERunnerConfig.AssertionConfig parseAssertion(final Map<String, Object> config) {
        return E2ERunnerConfig.AssertionConfig.builder()
                .orderMode(getString(config, "orderMode", "auto"))
                .nullToken(getString(config, "nullToken", "<NULL>"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(final Map<String, Object> config, final String key) {
        Object value = config.get(key);
        return value instanceof Map ? (Map<String, Object>) value : new HashMap<String, Object>(0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getObjectMap(final Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new HashMap<String, Object>(0);
    }

    private String getString(final Map<String, Object> config, final String key, final String defaultValue) {
        Object value = config.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private int getInt(final Map<String, Object> config, final String key, final int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (final NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean getBoolean(final Map<String, Object> config, final String key, final boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            return Boolean.parseBoolean(String.valueOf(value));
        }
        return defaultValue;
    }
}
