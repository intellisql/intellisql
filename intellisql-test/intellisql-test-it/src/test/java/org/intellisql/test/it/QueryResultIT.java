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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for query result handling. Tests result set metadata, type handling, and cursor
 * operations.
 */
public class QueryResultIT extends AbstractIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:15-alpine";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
                    .withNetwork(NETWORK)
                    .withNetworkAliases("postgres")
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpass")
                    .withReuse(true);

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = createConnection(POSTGRES_CONTAINER);
        dropTableIfExists(connection, "test_results");
        createTestTable(
                connection,
                "test_results",
                "id INT PRIMARY KEY, "
                        + "name VARCHAR(100), "
                        + "price DECIMAL(10,2), "
                        + "quantity INT, "
                        + "active BOOLEAN, "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "data JSONB");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Should retrieve result set metadata correctly")
    void shouldRetrieveResultSetMetadata() throws Exception {
        insertTestData(
                connection,
                "test_results",
                "id, name, price, quantity, active",
                "1, 'Product A', 99.99, 100, true");
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM test_results")) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            assertThat(metaData.getColumnCount()).isEqualTo(7);
            assertThat(metaData.getColumnName(1)).isEqualTo("id");
            assertThat(metaData.getColumnType(1)).isEqualTo(Types.INTEGER);
            assertThat(metaData.getColumnName(2)).isEqualTo("name");
            assertThat(metaData.getColumnType(2)).isEqualTo(Types.VARCHAR);
        }
    }

    @Test
    @DisplayName("Should handle different data types correctly")
    void shouldHandleDifferentDataTypes() throws Exception {
        insertTestData(
                connection,
                "test_results",
                "id, name, price, quantity, active",
                "1, 'Test', 123.45, 50, true");
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM test_results WHERE id = 1")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("id")).isEqualTo(1);
            assertThat(resultSet.getString("name")).isEqualTo("Test");
            assertThat(resultSet.getBigDecimal("price")).isEqualByComparingTo("123.45");
            assertThat(resultSet.getInt("quantity")).isEqualTo(50);
            assertThat(resultSet.getBoolean("active")).isTrue();
        }
    }

    @Test
    @DisplayName("Should handle NULL values correctly")
    void shouldHandleNullValues() throws Exception {
        insertTestData(
                connection,
                "test_results",
                "id, name, price, quantity, active",
                "1, NULL, NULL, NULL, NULL");
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM test_results WHERE id = 1")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("name")).isNull();
            assertThat(resultSet.wasNull()).isTrue();
            assertThat(resultSet.getBigDecimal("price")).isNull();
            assertThat(resultSet.wasNull()).isTrue();
            assertThat(resultSet.getObject("quantity")).isNull();
            assertThat(resultSet.wasNull()).isTrue();
        }
    }

    @Test
    @DisplayName("Should iterate through result set correctly")
    void shouldIterateThroughResultSet() throws Exception {
        for (int i = 1; i <= 5; i++) {
            insertTestData(
                    connection,
                    "test_results",
                    "id, name, price, quantity, active",
                    String.format("%d, 'Product %d', %d.00, %d, true", i, i, i * 10, i * 5));
        }
        List<Integer> ids = new ArrayList<>();
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT id FROM test_results ORDER BY id")) {
            while (resultSet.next()) {
                ids.add(resultSet.getInt("id"));
            }
        }
        assertThat(ids).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("Should handle scrollable result set")
    void shouldHandleScrollableResultSet() throws Exception {
        for (int i = 1; i <= 3; i++) {
            insertTestData(
                    connection,
                    "test_results",
                    "id, name, price, quantity, active",
                    String.format("%d, 'Product %d', %d.00, 10, true", i, i, i * 10));
        }
        try (
                Statement statement =
                        connection.createStatement(
                                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                ResultSet resultSet = statement.executeQuery("SELECT * FROM test_results ORDER BY id")) {
            assertThat(resultSet.last()).isTrue();
            assertThat(resultSet.getInt("id")).isEqualTo(3);
            assertThat(resultSet.getRow()).isEqualTo(3);
            assertThat(resultSet.first()).isTrue();
            assertThat(resultSet.getInt("id")).isEqualTo(1);
            assertThat(resultSet.absolute(2)).isTrue();
            assertThat(resultSet.getInt("id")).isEqualTo(2);
            assertThat(resultSet.previous()).isTrue();
            assertThat(resultSet.getInt("id")).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Should handle updatable result set")
    void shouldHandleUpdatableResultSet() throws Exception {
        insertTestData(
                connection,
                "test_results",
                "id, name, price, quantity, active",
                "1, 'Original', 100.00, 10, true");
        try (
                Statement statement =
                        connection.createStatement(
                                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                ResultSet resultSet = statement.executeQuery("SELECT * FROM test_results WHERE id = 1")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("name")).isEqualTo("Original");
            resultSet.updateString("name", "Updated");
            resultSet.updateRow();
        }
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery("SELECT name FROM test_results WHERE id = 1")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("name")).isEqualTo("Updated");
        }
    }

    @Test
    @DisplayName("Should handle large result set efficiently")
    void shouldHandleLargeResultSet() throws Exception {
        int rowCount = 1000;
        for (int i = 1; i <= rowCount; i++) {
            insertTestData(
                    connection,
                    "test_results",
                    "id, name, price, quantity, active",
                    String.format("%d, 'Product %d', %.2f, %d, true", i, i, i * 1.5, i % 100));
        }
        int count = 0;
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM test_results")) {
            while (resultSet.next()) {
                count++;
            }
        }
        assertThat(count).isEqualTo(rowCount);
    }

    @Test
    @DisplayName("Should handle column alias correctly")
    void shouldHandleColumnAlias() throws Exception {
        insertTestData(
                connection,
                "test_results",
                "id, name, price, quantity, active",
                "1, 'Test', 100.00, 50, true");
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "SELECT id AS product_id, name AS product_name, price * 1.1 AS price_with_tax FROM"
                                        + " test_results")) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            assertThat(metaData.getColumnLabel(1)).isEqualTo("product_id");
            assertThat(metaData.getColumnLabel(2)).isEqualTo("product_name");
            assertThat(metaData.getColumnLabel(3)).isEqualTo("price_with_tax");
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("product_id")).isEqualTo(1);
            assertThat(resultSet.getString("product_name")).isEqualTo("Test");
            assertThat(resultSet.getBigDecimal("price_with_tax")).isEqualByComparingTo("110.00");
        }
    }

    @Test
    @DisplayName("Should handle multiple result sets from stored procedure")
    void shouldHandleMultipleResultSets() throws Exception {
        try (Statement statement = connection.createStatement()) {
            boolean hasResultSet = statement.execute("SELECT 1 AS first_query");
            assertThat(hasResultSet).isTrue();
            try (ResultSet resultSet = statement.getResultSet()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt(1)).isEqualTo(1);
            }
            statement.getMoreResults();
            assertThat(statement.getResultSet()).isNull();
        }
    }

    @Test
    @DisplayName("Should handle cursor positioning correctly")
    void shouldHandleCursorPositioning() throws Exception {
        for (int i = 1; i <= 5; i++) {
            insertTestData(
                    connection,
                    "test_results",
                    "id, name, price, quantity, active",
                    String.format("%d, 'Item %d', %d.00, 10, true", i, i, i * 10));
        }
        try (
                Statement statement =
                        connection.createStatement(
                                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                ResultSet resultSet = statement.executeQuery("SELECT * FROM test_results ORDER BY id")) {
            assertThat(resultSet.isBeforeFirst()).isTrue();
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.isFirst()).isTrue();
            assertThat(resultSet.getInt("id")).isEqualTo(1);
            while (resultSet.next()) {
                // Advance through all rows
                resultSet.getRow();
            }
            assertThat(resultSet.isAfterLast()).isTrue();
            resultSet.beforeFirst();
            assertThat(resultSet.isBeforeFirst()).isTrue();
        }
    }

    @Test
    @DisplayName("Should handle fetch size for large results")
    void shouldHandleFetchSize() throws Exception {
        for (int i = 1; i <= 100; i++) {
            insertTestData(
                    connection,
                    "test_results",
                    "id, name, price, quantity, active",
                    String.format("%d, 'Item %d', %d.00, 10, true", i, i, i));
        }
        try (Statement statement = connection.createStatement()) {
            statement.setFetchSize(10);
            try (ResultSet resultSet = statement.executeQuery("SELECT * FROM test_results ORDER BY id")) {
                int count = 0;
                while (resultSet.next()) {
                    count++;
                }
                assertThat(count).isEqualTo(100);
            }
        }
    }

    @Test
    @DisplayName("Should handle aggregate functions correctly")
    void shouldHandleAggregateFunctions() throws Exception {
        insertTestData(
                connection,
                "test_results",
                "id, name, price, quantity, active",
                "1, 'A', 100.00, 10, true");
        insertTestData(
                connection,
                "test_results",
                "id, name, price, quantity, active",
                "2, 'B', 200.00, 20, true");
        insertTestData(
                connection,
                "test_results",
                "id, name, price, quantity, active",
                "3, 'C', 300.00, 30, false");
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "SELECT COUNT(*) as cnt, SUM(price) as total_price, AVG(quantity) as avg_qty, "
                                        + "MAX(price) as max_price, MIN(price) as min_price FROM test_results")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("cnt")).isEqualTo(3);
            assertThat(resultSet.getBigDecimal("total_price")).isEqualByComparingTo("600.00");
            assertThat(resultSet.getBigDecimal("avg_qty")).isEqualByComparingTo("20");
            assertThat(resultSet.getBigDecimal("max_price")).isEqualByComparingTo("300.00");
            assertThat(resultSet.getBigDecimal("min_price")).isEqualByComparingTo("100.00");
        }
    }
}
