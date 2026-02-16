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

package org.intellisql.connector.elasticsearch;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.intellisql.connector.api.Connection;
import org.intellisql.connector.api.DataSourceConnector;
import org.intellisql.connector.config.DataSourceConfig;
import org.intellisql.connector.enums.DataSourceType;
import org.intellisql.connector.model.Schema;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch implementation of DataSourceConnector. Provides connection management and schema
 * discovery for Elasticsearch clusters. Uses Elasticsearch Java Client 8.12.0 with HTTP/HTTPS
 * support.
 */
@Slf4j
public class ElasticsearchConnector implements DataSourceConnector {

    private final Map<String, ElasticsearchClient> clients = new ConcurrentHashMap<>();

    private final ElasticsearchSchemaDiscoverer schemaDiscoverer =
            new ElasticsearchSchemaDiscoverer();

    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.ELASTICSEARCH;
    }

    @Override
    public Connection connect(final DataSourceConfig config) throws Exception {
        ElasticsearchClient client = getOrCreateClient(config);
        return new ElasticsearchConnection(client);
    }

    @Override
    public boolean testConnection(final DataSourceConfig config) {
        try {
            ElasticsearchClient client = getOrCreateClient(config);
            HealthResponse health = client.cluster().health();
            boolean success = health != null && !"red".equals(health.status().jsonValue());
            log.info(
                    "Elasticsearch connection test for '{}': {}",
                    config.getName(),
                    success ? "SUCCESS" : "FAILED");
            return success;
            // CHECKSTYLE:OFF: IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON: IllegalCatch
            log.error(
                    "Elasticsearch connection test failed for '{}': {}", config.getName(), ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    public Schema discoverSchema(final DataSourceConfig config) throws Exception {
        ElasticsearchClient client = getOrCreateClient(config);
        return schemaDiscoverer.discoverSchema(client, config.getSchema());
    }

    private ElasticsearchClient getOrCreateClient(final DataSourceConfig config) {
        return clients.computeIfAbsent(
                config.getName(),
                name -> {
                    log.info("Creating new Elasticsearch client for: {}", name);
                    return createClient(config);
                });
    }

    private ElasticsearchClient createClient(final DataSourceConfig config) {
        String scheme = getScheme(config);
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
        if (config.getProperties() != null) {
            String timeoutStr = config.getProperties().get("connectionTimeout");
            if (timeoutStr != null) {
                int timeout = Integer.parseInt(timeoutStr);
                builder.setRequestConfigCallback(
                        requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(timeout));
            }
            String socketTimeoutStr = config.getProperties().get("socketTimeout");
            if (socketTimeoutStr != null) {
                int socketTimeout = Integer.parseInt(socketTimeoutStr);
                builder.setRequestConfigCallback(
                        requestConfigBuilder -> requestConfigBuilder.setSocketTimeout(socketTimeout));
            }
        }
        RestClient restClient = builder.build();
        ElasticsearchTransport transport =
                new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    private String getScheme(final DataSourceConfig config) {
        if (config.getProperties() != null) {
            String ssl = config.getProperties().get("ssl");
            if ("true".equalsIgnoreCase(ssl)) {
                return "https";
            }
        }
        return "http";
    }

    @Override
    public void close() {
        log.info("Closing all Elasticsearch clients");
        clients
                .values()
                .forEach(
                        client -> {
                            try {
                                client._transport().close();
                                // CHECKSTYLE:OFF: IllegalCatch
                            } catch (final Exception ex) {
                                // CHECKSTYLE:ON: IllegalCatch
                                log.error("Error closing Elasticsearch client", ex);
                            }
                        });
        clients.clear();
    }

    /**
     * Closes a specific Elasticsearch client.
     *
     * @param name the data source name
     */
    public void closeClient(final String name) {
        ElasticsearchClient client = clients.remove(name);
        if (client != null) {
            try {
                client._transport().close();
                log.info("Closed Elasticsearch client for: {}", name);
                // CHECKSTYLE:OFF: IllegalCatch
            } catch (final Exception ex) {
                // CHECKSTYLE:ON: IllegalCatch
                log.error("Error closing Elasticsearch client for: {}", name, ex);
            }
        }
    }
}
