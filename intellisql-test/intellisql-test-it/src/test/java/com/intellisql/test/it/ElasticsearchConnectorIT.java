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

package com.intellisql.test.it;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Elasticsearch connector. Tests basic connectivity and search operations.
 */
public class ElasticsearchConnectorIT extends AbstractIntegrationTest {

    private static final String ELASTICSEARCH_IMAGE =
            "docker.elastic.co/elasticsearch/elasticsearch:8.12.0";

    private static final int ELASTICSEARCH_PORT = 9200;

    private static final int CONNECT_TIMEOUT = 10000;

    private static final int READ_TIMEOUT = 10000;

    @Container
    private static final GenericContainer<?> ELASTICSEARCH_CONTAINER =
            new GenericContainer<>(DockerImageName.parse(ELASTICSEARCH_IMAGE))
                    .withNetwork(NETWORK)
                    .withNetworkAliases("elasticsearch")
                    .withExposedPorts(ELASTICSEARCH_PORT)
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                    .waitingFor(
                            Wait.forHttp("/_cluster/health")
                                    .forPort(ELASTICSEARCH_PORT)
                                    .forStatusCode(200)
                                    .withStartupTimeout(Duration.ofMinutes(3)))
                    .withReuse(true);

    private String elasticsearchUrl;

    @BeforeEach
    void setUp() throws Exception {
        elasticsearchUrl =
                String.format(
                        "http://%s:%d",
                        ELASTICSEARCH_CONTAINER.getHost(),
                        ELASTICSEARCH_CONTAINER.getMappedPort(ELASTICSEARCH_PORT));
    }

    @AfterEach
    void tearDown() throws Exception {
        deleteIndex("test_index");
    }

    @Test
    @DisplayName("Should connect to Elasticsearch container successfully")
    void shouldConnectToElasticsearchContainer() throws Exception {
        HttpResponse response = sendRequest("GET", elasticsearchUrl, null);
        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body).contains("name", "cluster_name", "version");
    }

    @Test
    @DisplayName("Should check cluster health")
    void shouldCheckClusterHealth() throws Exception {
        HttpResponse response = sendRequest("GET", elasticsearchUrl + "/_cluster/health", null);
        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body).contains("status", "cluster_name");
    }

    @Test
    @DisplayName("Should create index successfully")
    void shouldCreateIndex() throws Exception {
        String indexMapping =
                "{\n"
                        + "  \"settings\": {\n"
                        + "    \"number_of_shards\": 1,\n"
                        + "    \"number_of_replicas\": 0\n"
                        + "  },\n"
                        + "  \"mappings\": {\n"
                        + "    \"properties\": {\n"
                        + "      \"title\": { \"type\": \"text\" },\n"
                        + "      \"content\": { \"type\": \"text\" }\n"
                        + "    }\n"
                        + "  }\n"
                        + "}";
        HttpResponse response = sendRequest("PUT", elasticsearchUrl + "/test_index", indexMapping);
        assertThat(response.statusCode).isIn(200, 201);
        assertThat(response.body).contains("acknowledged\":true");
    }

    @Test
    @DisplayName("Should index document successfully")
    void shouldIndexDocument() throws Exception {
        createIndex("test_index");
        String document =
                "{\n"
                        + "  \"title\": \"Test Document\",\n"
                        + "  \"content\": \"This is a test document for Elasticsearch\"\n"
                        + "}";
        HttpResponse response = sendRequest("POST", elasticsearchUrl + "/test_index/_doc/1", document);
        assertThat(response.statusCode).isIn(200, 201);
        assertThat(response.body).contains("\"result\":\"created\"");
    }

    @Test
    @DisplayName("Should search documents successfully")
    void shouldSearchDocuments() throws Exception {
        createIndex("test_index");
        indexDocument(
                "test_index",
                "1",
                "{\"title\": \"Java Programming\", \"content\": \"Learn Java programming\"}");
        indexDocument(
                "test_index",
                "2",
                "{\"title\": \"Python Programming\", \"content\": \"Learn Python programming\"}");
        Thread.sleep(1000);
        String searchQuery =
                "{\n"
                        + "  \"query\": {\n"
                        + "    \"match\": {\n"
                        + "      \"title\": \"Java\"\n"
                        + "    }\n"
                        + "  }\n"
                        + "}";
        HttpResponse response = sendRequest("POST", elasticsearchUrl + "/test_index/_search", searchQuery);
        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body).contains("\"hits\":");
    }

    @Test
    @DisplayName("Should delete document successfully")
    void shouldDeleteDocument() throws Exception {
        createIndex("test_index");
        indexDocument("test_index", "1", "{\"title\": \"Test\", \"content\": \"Test content\"}");
        Thread.sleep(500);
        HttpResponse response = sendRequest("DELETE", elasticsearchUrl + "/test_index/_doc/1", null);
        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body).contains("\"result\":\"deleted\"");
    }

    @Test
    @DisplayName("Should execute aggregation query")
    void shouldExecuteAggregationQuery() throws Exception {
        createIndex("test_index");
        indexDocument(
                "test_index", "1", "{\"title\": \"Doc1\", \"category\": \"tech\", \"views\": 100}");
        indexDocument(
                "test_index", "2", "{\"title\": \"Doc2\", \"category\": \"tech\", \"views\": 200}");
        indexDocument(
                "test_index", "3", "{\"title\": \"Doc3\", \"category\": \"news\", \"views\": 150}");
        Thread.sleep(1000);
        String aggQuery =
                "{\n"
                        + "  \"size\": 0,\n"
                        + "  \"aggs\": {\n"
                        + "    \"categories\": {\n"
                        + "      \"terms\": { \"field\": \"category.keyword\" }\n"
                        + "    }\n"
                        + "  }\n"
                        + "}";
        HttpResponse response = sendRequest("POST", elasticsearchUrl + "/test_index/_search", aggQuery);
        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body).contains("aggregations");
    }

    private void createIndex(final String indexName) throws Exception {
        String indexMapping =
                "{\n"
                        + "  \"settings\": {\n"
                        + "    \"number_of_shards\": 1,\n"
                        + "    \"number_of_replicas\": 0\n"
                        + "  },\n"
                        + "  \"mappings\": {\n"
                        + "    \"properties\": {\n"
                        + "      \"title\": { \"type\": \"text\" },\n"
                        + "      \"content\": { \"type\": \"text\" },\n"
                        + "      \"category\": { \"type\": \"keyword\" },\n"
                        + "      \"views\": { \"type\": \"integer\" }\n"
                        + "    }\n"
                        + "  }\n"
                        + "}";
        sendRequest("PUT", elasticsearchUrl + "/" + indexName, indexMapping);
    }

    private void indexDocument(final String indexName, final String docId, final String document) throws Exception {
        sendRequest("POST", elasticsearchUrl + "/" + indexName + "/_doc/" + docId, document);
    }

    private void deleteIndex(final String indexName) throws Exception {
        // CHECKSTYLE:OFF
        try {
            sendRequest("DELETE", elasticsearchUrl + "/" + indexName, null);
        } catch (final Exception ignored) {
            // Ignore exception when deleting index
        }
        // CHECKSTYLE:ON
    }

    private HttpResponse sendRequest(final String method, final String url, final String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("Content-Type", "application/json");
        if (body != null && !body.isEmpty()) {
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }
        int statusCode = connection.getResponseCode();
        String responseBody;
        InputStream inputStream;
        if (statusCode >= 200 && statusCode < 300) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }
        if (inputStream != null) {
            try (
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                responseBody = response.toString();
            }
        } else {
            responseBody = "";
        }
        connection.disconnect();
        return new HttpResponse(statusCode, responseBody);
    }

    private static final class HttpResponse {

        private final int statusCode;

        private final String body;

        HttpResponse(final int statusCode, final String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }
}
