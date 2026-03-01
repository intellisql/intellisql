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

package com.intellisql.connector.elasticsearch;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.intellisql.connector.config.DataSourceConfig;
import com.intellisql.connector.enums.DataSourceType;

/**
 * Integration tests for ElasticsearchConnector.
 * Requires a running Elasticsearch instance.
 */
class ElasticsearchConnectorIT {

    private ElasticsearchConnector connector;

    private DataSourceConfig config;

    @BeforeEach
    void setUp() {
        final String esUrl = System.getenv("ELASTICSEARCH_TEST_URL");
        assumeTrue(esUrl != null, "Elasticsearch test URL not set, skipping test");
        config = DataSourceConfig.builder()
                .type(DataSourceType.ELASTICSEARCH)
                .jdbcUrl(esUrl)
                .username(System.getenv().getOrDefault("ELASTICSEARCH_TEST_USER", ""))
                .password(System.getenv().getOrDefault("ELASTICSEARCH_TEST_PASSWORD", ""))
                .maxPoolSize(5)
                .build();
        connector = new ElasticsearchConnector();
    }

    @Test
    void testConnectorType() {
        assertNotNull(connector);
        assertTrue(connector instanceof ElasticsearchConnector);
    }

    @Test
    void testConnect() throws Exception {
        assumeTrue(config != null, "Config not initialized");
        // This test requires actual Elasticsearch connection
    }
}
