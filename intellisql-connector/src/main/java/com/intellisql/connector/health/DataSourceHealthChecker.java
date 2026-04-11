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

package com.intellisql.connector.health;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;

import com.intellisql.common.metadata.enums.DataSourceType;
import com.intellisql.connector.config.IntelliSQLDataSourceConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of HealthChecker for various data sources. MySQL/PostgreSQL use SELECT 1 for
 * health checks. Elasticsearch uses GET /_cluster/health for health checks.
 * Uses Elasticsearch 7.x API for JDK 8 compatibility.
 */
@Slf4j
public class DataSourceHealthChecker implements HealthChecker {

    private final Map<String, Object> connectionCache = new ConcurrentHashMap<>();

    @Override
    public HealthCheckResult check(final IntelliSQLDataSourceConfig config) {
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

    private boolean performHealthCheck(final IntelliSQLDataSourceConfig config) {
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

    private boolean checkMySQL(final IntelliSQLDataSourceConfig config) {
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

    private boolean checkPostgreSQL(final IntelliSQLDataSourceConfig config) {
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

    private boolean checkElasticsearch(final IntelliSQLDataSourceConfig config) {
        RestHighLevelClient client = getOrCreateElasticsearchClient(config);
        ClusterHealthResponse health;
        try {
            health = client.cluster().health(new ClusterHealthRequest(), RequestOptions.DEFAULT);
        } catch (final IOException ex) {
            log.error("Elasticsearch health check failed for '{}': {}", config.getName(), ex.getMessage());
            return false;
        }
        if (health == null) {
            return false;
        }
        ClusterHealthStatus status = health.getStatus();
        if (status == ClusterHealthStatus.RED) {
            log.warn("Elasticsearch cluster '{}' is in RED state", config.getName());
            return false;
        }
        if (status == ClusterHealthStatus.YELLOW) {
            log.info("Elasticsearch cluster '{}' is in YELLOW state", config.getName());
        }
        return true;
    }

    private RestHighLevelClient getOrCreateElasticsearchClient(final IntelliSQLDataSourceConfig config) {
        return (RestHighLevelClient) connectionCache.computeIfAbsent(
                config.getName(),
                name -> createElasticsearchClient(config));
    }

    /**
     * Creates an Elasticsearch client from the configuration.
     *
     * @param config the data source configuration
     * @return the created Elasticsearch RestHighLevelClient
     */
    private RestHighLevelClient createElasticsearchClient(final IntelliSQLDataSourceConfig config) {
        String scheme = "http";
        if (config.getProperties() != null
                && "true".equalsIgnoreCase(config.getProperties().get("ssl"))) {
            scheme = "https";
        }
        String host = config.getHost() != null ? config.getHost() : "localhost";
        int port = config.getPort() > 0 ? config.getPort() : 9200;
        HttpHost httpHost = new HttpHost(host, port, scheme);
        RestClientBuilder builder = RestClient.builder(httpHost);
        if (config.getUsername() != null && config.getPassword() != null) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(config.getUsername(), config.getPassword()));
            builder.setHttpClientConfigCallback(
                    httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }
        return new RestHighLevelClient(builder);
    }

    private String buildJdbcUrl(final IntelliSQLDataSourceConfig config, final String dbType) {
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
        if (client instanceof RestHighLevelClient) {
            closeElasticsearchClient((RestHighLevelClient) client, dataSourceName);
        }
    }

    private void closeElasticsearchClient(final RestHighLevelClient client, final String dataSourceName) {
        try {
            client.close();
        } catch (final IOException ex) {
            log.error("Error closing Elasticsearch client for: {}", dataSourceName, ex);
        }
    }
}
