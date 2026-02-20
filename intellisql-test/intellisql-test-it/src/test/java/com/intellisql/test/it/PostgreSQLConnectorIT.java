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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PostgreSQL connector. Tests basic connectivity, query execution, and data
 * retrieval.
 */
public class PostgreSQLConnectorIT extends AbstractIntegrationTest {

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
        dropTableIfExists(connection, "products");
        createTestTable(
                connection, "products", "id SERIAL PRIMARY KEY, name VARCHAR(100), price DECIMAL(10,2)");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Should connect to PostgreSQL container successfully")
    void shouldConnectToPostgreSQLContainer() throws Exception {
        assertThat(connection).isNotNull();
        assertThat(connection.isValid(5)).isTrue();
    }

    @Test
    @DisplayName("Should execute simple SELECT query")
    void shouldExecuteSimpleSelectQuery() throws Exception {
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT 1 AS result")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("result")).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Should insert and retrieve data from PostgreSQL")
    void shouldInsertAndRetrieveData() throws Exception {
        insertTestData(connection, "products", "name, price", "'Laptop', 999.99");
        insertTestData(connection, "products", "name, price", "'Mouse', 29.99");
        int count = executeQueryCount(connection, "SELECT COUNT(*) FROM products");
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should execute SELECT query with WHERE clause")
    void shouldExecuteSelectWithWhereClause() throws Exception {
        insertTestData(connection, "products", "name, price", "'Laptop', 999.99");
        insertTestData(connection, "products", "name, price", "'Mouse', 29.99");
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM products WHERE price > 50")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("name")).isEqualTo("Laptop");
            assertThat(resultSet.getBigDecimal("price")).isEqualByComparingTo("999.99");
            assertThat(resultSet.next()).isFalse();
        }
    }

    @Test
    @DisplayName("Should execute UPDATE query")
    void shouldExecuteUpdateQuery() throws Exception {
        insertTestData(connection, "products", "name, price", "'Laptop', 999.99");
        String updateSql = "UPDATE products SET price = 899.99 WHERE name = 'Laptop'";
        try (Statement statement = connection.createStatement()) {
            int rowsUpdated = statement.executeUpdate(updateSql);
            assertThat(rowsUpdated).isEqualTo(1);
        }
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery("SELECT price FROM products WHERE name = 'Laptop'")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getBigDecimal("price")).isEqualByComparingTo("899.99");
        }
    }

    @Test
    @DisplayName("Should execute DELETE query")
    void shouldExecuteDeleteQuery() throws Exception {
        insertTestData(connection, "products", "name, price", "'Laptop', 999.99");
        insertTestData(connection, "products", "name, price", "'Mouse', 29.99");
        String deleteSql = "DELETE FROM products WHERE price < 50";
        try (Statement statement = connection.createStatement()) {
            int rowsDeleted = statement.executeUpdate(deleteSql);
            assertThat(rowsDeleted).isEqualTo(1);
        }
        int count = executeQueryCount(connection, "SELECT COUNT(*) FROM products");
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should execute aggregation query with GROUP BY")
    void shouldExecuteAggregationQuery() throws Exception {
        insertTestData(connection, "products", "name, price", "'Laptop', 999.99");
        insertTestData(connection, "products", "name, price", "'Mouse', 29.99");
        insertTestData(connection, "products", "name, price", "'Keyboard', 79.99");
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "SELECT COUNT(*) AS total, SUM(price) AS total_price FROM products")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("total")).isEqualTo(3);
            assertThat(resultSet.getBigDecimal("total_price")).isNotNull();
        }
    }

    @Test
    @DisplayName("Should execute ORDER BY query")
    void shouldExecuteOrderByQuery() throws Exception {
        insertTestData(connection, "products", "name, price", "'Laptop', 999.99");
        insertTestData(connection, "products", "name, price", "'Mouse', 29.99");
        insertTestData(connection, "products", "name, price", "'Keyboard', 79.99");
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery("SELECT * FROM products ORDER BY price DESC")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("name")).isEqualTo("Laptop");
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("name")).isEqualTo("Keyboard");
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("name")).isEqualTo("Mouse");
        }
    }

    @Test
    @DisplayName("Should handle transaction correctly")
    void shouldHandleTransaction() throws Exception {
        connection.setAutoCommit(false);
        try {
            insertTestData(connection, "products", "name, price", "'Monitor', 299.99");
            connection.rollback();
            int count = executeQueryCount(connection, "SELECT COUNT(*) FROM products");
            assertThat(count).isEqualTo(0);
            insertTestData(connection, "products", "name, price", "'Monitor', 299.99");
            connection.commit();
            count = executeQueryCount(connection, "SELECT COUNT(*) FROM products");
            assertThat(count).isEqualTo(1);
        } finally {
            connection.setAutoCommit(true);
        }
    }

    @Test
    @DisplayName("Should execute complex query with JOIN")
    void shouldExecuteComplexJoinQuery() throws Exception {
        dropTableIfExists(connection, "orders");
        createTestTable(connection, "orders", "id SERIAL PRIMARY KEY, product_id INT, quantity INT");
        insertTestData(connection, "products", "name, price", "'Laptop', 999.99");
        insertTestData(connection, "orders", "product_id, quantity", "1, 5");
        String joinSql =
                "SELECT p.name, p.price, o.quantity FROM products p JOIN orders o ON p.id = o.product_id";
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(joinSql)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("name")).isEqualTo("Laptop");
            assertThat(resultSet.getInt("quantity")).isEqualTo(5);
        }
    }
}
