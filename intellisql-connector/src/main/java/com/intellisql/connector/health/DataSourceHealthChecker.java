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

package com.intellisql.connector.health;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.intellisql.connector.config.DataSourceConfig;
import com.intellisql.connector.enums.DataSourceType;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of HealthChecker for various data sources. MySQL/PostgreSQL use SELECT 1 for
 * health checks. Elasticsearch uses GET /_cluster/health for health checks.
 */
@Slf4j
public class DataSourceHealthChecker implements HealthChecker {

    private final Map<String, Object> connectionCache = new ConcurrentHashMap<>();

    @Override
    public HealthCheckResult check(final DataSourceConfig config) {
        long startTime = System.currentTimeMillis();
        boolean healthy = performHealthCheck(config);
        long responseTime = System.currentTimeMillis() - startTime;
        if (healthy) {
            if (responseTime > 1000) {
                return HealthCheckResult.degraded(
                        config.getName(), "Connection is slow: " + responseTime + "ms", responseTime);
            }
            return HealthCheckResult.healthy(config.getName(), responseTime);
        }
        return HealthCheckResult.unhealthy(config.getName(), "Health check returned false");
    }

    @Override
    public String getName() {
        return "DataSourceHealthChecker";
    }

    private boolean performHealthCheck(final DataSourceConfig config) {
        DataSourceType type = config.getType();
        switch (type) {
            case MYSQL:
                return checkMySQL(config);
            case POSTGRESQL:
                return checkPostgreSQL(config);
            case ELASTICSEARCH:
                return checkElasticsearch(config);
            default:
                throw new IllegalArgumentException("Unsupported data source type: " + type);
        }
    }

    private boolean checkMySQL(final DataSourceConfig config) {
        String jdbcUrl = buildJdbcUrl(config, "mysql");
        try (
                Connection conn =
                        DriverManager.getConnection(jdbcUrl, config.getUsername(), config.getPassword())) {
            return conn.isValid(5) && executeHealthQuery(conn);
        } catch (final SQLException ex) {
            log.error("MySQL health check failed for '{}': {}", config.getName(), ex.getMessage());
            return false;
        }
    }

    private boolean checkPostgreSQL(final DataSourceConfig config) {
        String jdbcUrl = buildJdbcUrl(config, "postgresql");
        try (
                Connection conn =
                        DriverManager.getConnection(jdbcUrl, config.getUsername(), config.getPassword())) {
            return conn.isValid(5) && executeHealthQuery(conn);
        } catch (final SQLException ex) {
            log.error("PostgreSQL health check failed for '{}': {}", config.getName(), ex.getMessage());
            return false;
        }
    }

    private boolean executeHealthQuery(final Connection conn) throws SQLException {
        try (
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next();
        }
    }

    private boolean checkElasticsearch(final DataSourceConfig config) {
        ElasticsearchClient client = getOrCreateElasticsearchClient(config);
        HealthResponse health;
        try {
            health = client.cluster().health();
        } catch (final IOException ex) {
            log.error("Elasticsearch health check failed for '{}': {}", config.getName(), ex.getMessage());
            return false;
        }
        if (health == null) {
            return false;
        }
        String status = health.status().jsonValue();
        if ("red".equals(status)) {
            log.warn("Elasticsearch cluster '{}' is in RED state", config.getName());
            return false;
        }
        if ("yellow".equals(status)) {
            log.info("Elasticsearch cluster '{}' is in YELLOW state", config.getName());
        }
        return true;
    }

    private ElasticsearchClient getOrCreateElasticsearchClient(final DataSourceConfig config) {
        return (ElasticsearchClient) connectionCache.computeIfAbsent(
                config.getName(),
                name -> createElasticsearchClient(config));
    }

    /**
     * Creates an Elasticsearch client from the configuration.
     *
     * @param config the data source configuration
     * @return the created Elasticsearch client
     */
    private ElasticsearchClient createElasticsearchClient(final DataSourceConfig config) {
        String scheme = "http";
        if (config.getProperties() != null
                && "true".equalsIgnoreCase(config.getProperties().get("ssl"))) {
            scheme = "https";
        }
        String host = config.getHost() != null ? config.getHost() : "localhost";
        int port = config.getPort() > 0 ? config.getPort() : 9200;
        org.apache.http.HttpHost httpHost = new org.apache.http.HttpHost(host, port, scheme);
        org.elasticsearch.client.RestClientBuilder builder =
                org.elasticsearch.client.RestClient.builder(httpHost);
        if (config.getUsername() != null && config.getPassword() != null) {
            org.apache.http.impl.client.BasicCredentialsProvider credentialsProvider =
                    new org.apache.http.impl.client.BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    org.apache.http.auth.AuthScope.ANY,
                    new org.apache.http.auth.UsernamePasswordCredentials(
                            config.getUsername(), config.getPassword()));
            builder.setHttpClientConfigCallback(
                    httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }
        org.elasticsearch.client.RestClient restClient = builder.build();
        co.elastic.clients.transport.ElasticsearchTransport transport =
                new co.elastic.clients.transport.rest_client.RestClientTransport(
                        restClient, new co.elastic.clients.json.jackson.JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    private String buildJdbcUrl(final DataSourceConfig config, final String dbType) {
        if (config.getJdbcUrl() != null && !config.getJdbcUrl().isEmpty()) {
            return config.getJdbcUrl();
        }
        StringBuilder url = new StringBuilder("jdbc:").append(dbType).append("://");
        url.append(config.getHost()).append(":").append(config.getPort());
        if (config.getDatabase() != null && !config.getDatabase().isEmpty()) {
            url.append("/").append(config.getDatabase());
        }
        if ("postgresql".equals(dbType)) {
            url.append("?sslmode=require");
        }
        return url.toString();
    }

    /** Clears the connection cache. */
    public void clearCache() {
        connectionCache.clear();
    }

    /**
     * Removes a data source from the cache and closes its connection.
     *
     * @param dataSourceName the name of the data source to remove
     */
    public void removeFromCache(final String dataSourceName) {
        Object client = connectionCache.remove(dataSourceName);
        if (client instanceof ElasticsearchClient) {
            closeElasticsearchClient((ElasticsearchClient) client, dataSourceName);
        }
    }

    private void closeElasticsearchClient(final ElasticsearchClient client, final String dataSourceName) {
        try {
            client._transport().close();
        } catch (final IOException ex) {
            log.error("Error closing Elasticsearch client for: {}", dataSourceName, ex);
        }
    }
}
