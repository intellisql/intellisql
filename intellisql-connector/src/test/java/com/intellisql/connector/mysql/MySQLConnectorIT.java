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

package com.intellisql.connector.mysql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.intellisql.connector.config.DataSourceConfig;
import com.intellisql.connector.enums.DataSourceType;

/**
 * Integration tests for MySQLConnector.
 * Requires a running MySQL instance.
 */
class MySQLConnectorIT {

    private MySQLConnector connector;

    private DataSourceConfig config;

    @BeforeEach
    void setUp() {
        final String mysqlUrl = System.getenv("MYSQL_TEST_URL");
        assumeTrue(mysqlUrl != null, "MySQL test URL not set, skipping test");
        config = DataSourceConfig.builder()
                .type(DataSourceType.MYSQL)
                .jdbcUrl(mysqlUrl)
                .username(System.getenv().getOrDefault("MYSQL_TEST_USER", "root"))
                .password(System.getenv().getOrDefault("MYSQL_TEST_PASSWORD", ""))
                .maxPoolSize(5)
                .build();
        connector = new MySQLConnector();
    }

    @Test
    void testConnectorType() {
        assertNotNull(connector);
        assertTrue(connector instanceof MySQLConnector);
    }

    @Test
    void testConnect() throws Exception {
        assumeTrue(config != null, "Config not initialized");
        // This test requires actual MySQL connection
    }
}
