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

package org.intellisql.test.it;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    private HttpClient httpClient;

    private String elasticsearchUrl;

    @BeforeEach
    void setUp() throws Exception {
        elasticsearchUrl =
                String.format(
                        "http://%s:%d",
                        ELASTICSEARCH_CONTAINER.getHost(),
                        ELASTICSEARCH_CONTAINER.getMappedPort(ELASTICSEARCH_PORT));
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @AfterEach
    void tearDown() throws Exception {
        deleteIndex("test_index");
    }

    @Test
    @DisplayName("Should connect to Elasticsearch container successfully")
    void shouldConnectToElasticsearchContainer() throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(elasticsearchUrl))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("name", "cluster_name", "version");
    }

    @Test
    @DisplayName("Should check cluster health")
    void shouldCheckClusterHealth() throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(elasticsearchUrl + "/_cluster/health"))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("status", "cluster_name");
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
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(elasticsearchUrl + "/test_index"))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(indexMapping))
                        .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isIn(200, 201);
        assertThat(response.body()).contains("acknowledged\":true");
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
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(elasticsearchUrl + "/test_index/_doc/1"))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(document))
                        .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isIn(200, 201);
        assertThat(response.body()).contains("\"result\":\"created\"");
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
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(elasticsearchUrl + "/test_index/_search"))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(searchQuery))
                        .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"hits\":");
    }

    @Test
    @DisplayName("Should delete document successfully")
    void shouldDeleteDocument() throws Exception {
        createIndex("test_index");
        indexDocument("test_index", "1", "{\"title\": \"Test\", \"content\": \"Test content\"}");
        Thread.sleep(500);
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(elasticsearchUrl + "/test_index/_doc/1"))
                        .timeout(Duration.ofSeconds(10))
                        .DELETE()
                        .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"result\":\"deleted\"");
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
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(elasticsearchUrl + "/test_index/_search"))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(aggQuery))
                        .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("aggregations");
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
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(elasticsearchUrl + "/" + indexName))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(indexMapping))
                        .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void indexDocument(final String indexName, final String docId, final String document) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(elasticsearchUrl + "/" + indexName + "/_doc/" + docId))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(document))
                        .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void deleteIndex(final String indexName) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(elasticsearchUrl + "/" + indexName))
                        .timeout(Duration.ofSeconds(10))
                        .DELETE()
                        .build();
        // CHECKSTYLE:OFF
        try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (final Exception ignored) {
            // Ignore exception when deleting index
        }
        // CHECKSTYLE:ON
    }
}
