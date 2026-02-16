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
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MySQL connector. Tests basic connectivity, query execution, and data
 * retrieval.
 */
public class MySQLConnectorIT extends AbstractIntegrationTest {

    private static final String MYSQL_IMAGE = "mysql:8.0";

    @Container
    private static final MySQLContainer<?> MYSQL_CONTAINER =
            new MySQLContainer<>(DockerImageName.parse(MYSQL_IMAGE))
                    .withNetwork(NETWORK)
                    .withNetworkAliases("mysql")
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpass")
                    .withReuse(true);

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = createConnection(MYSQL_CONTAINER);
        dropTableIfExists(connection, "users");
        createTestTable(
                connection,
                "users",
                "id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), email VARCHAR(100)");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Should connect to MySQL container successfully")
    void shouldConnectToMySQLContainer() throws Exception {
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
    @DisplayName("Should insert and retrieve data from MySQL")
    void shouldInsertAndRetrieveData() throws Exception {
        insertTestData(connection, "users", "name, email", "'John Doe', 'john@example.com'");
        insertTestData(connection, "users", "name, email", "'Jane Smith', 'jane@example.com'");
        int count = executeQueryCount(connection, "SELECT COUNT(*) FROM users");
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should execute SELECT query with WHERE clause")
    void shouldExecuteSelectWithWhereClause() throws Exception {
        insertTestData(connection, "users", "name, email", "'John Doe', 'john@example.com'");
        insertTestData(connection, "users", "name, email", "'Jane Smith', 'jane@example.com'");
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery("SELECT * FROM users WHERE name = 'John Doe'")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("name")).isEqualTo("John Doe");
            assertThat(resultSet.getString("email")).isEqualTo("john@example.com");
            assertThat(resultSet.next()).isFalse();
        }
    }

    @Test
    @DisplayName("Should execute UPDATE query")
    void shouldExecuteUpdateQuery() throws Exception {
        insertTestData(connection, "users", "name, email", "'John Doe', 'john@example.com'");
        String updateSql = "UPDATE users SET email = 'john.doe@example.com' WHERE name = 'John Doe'";
        try (Statement statement = connection.createStatement()) {
            int rowsUpdated = statement.executeUpdate(updateSql);
            assertThat(rowsUpdated).isEqualTo(1);
        }
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery("SELECT email FROM users WHERE name = 'John Doe'")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("email")).isEqualTo("john.doe@example.com");
        }
    }

    @Test
    @DisplayName("Should execute DELETE query")
    void shouldExecuteDeleteQuery() throws Exception {
        insertTestData(connection, "users", "name, email", "'John Doe', 'john@example.com'");
        insertTestData(connection, "users", "name, email", "'Jane Smith', 'jane@example.com'");
        String deleteSql = "DELETE FROM users WHERE name = 'John Doe'";
        try (Statement statement = connection.createStatement()) {
            int rowsDeleted = statement.executeUpdate(deleteSql);
            assertThat(rowsDeleted).isEqualTo(1);
        }
        int count = executeQueryCount(connection, "SELECT COUNT(*) FROM users");
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should execute aggregation query")
    void shouldExecuteAggregationQuery() throws Exception {
        insertTestData(connection, "users", "name, email", "'John Doe', 'john@example.com'");
        insertTestData(connection, "users", "name, email", "'Jane Smith', 'jane@example.com'");
        insertTestData(connection, "users", "name, email", "'Bob Wilson', 'bob@example.com'");
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) AS total FROM users")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("total")).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("Should handle NULL values correctly")
    void shouldHandleNullValues() throws Exception {
        createTestTable(connection, "nullable_test", "id INT PRIMARY KEY, value VARCHAR(100)");
        insertTestData(connection, "nullable_test", "id, value", "1, NULL");
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM nullable_test WHERE id = 1")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("value")).isNull();
        }
    }
}
