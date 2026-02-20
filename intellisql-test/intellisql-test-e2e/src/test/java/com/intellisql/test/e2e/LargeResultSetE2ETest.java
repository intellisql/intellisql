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

package com.intellisql.test.e2e;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end tests for handling large result sets. Tests performance with 1 million rows. */
@Testcontainers
public class LargeResultSetE2ETest {

    private static final Network NETWORK = Network.newNetwork();

    private static final String POSTGRES_IMAGE = "postgres:15-alpine";

    private static final int LARGE_DATA_COUNT = 100000;

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

    @BeforeAll
    static void setupLargeDataSet() throws Exception {
        // Load PostgreSQL driver
        Class.forName("org.postgresql.Driver");
        Connection conn =
                DriverManager.getConnection(
                        POSTGRES_CONTAINER.getJdbcUrl(),
                        POSTGRES_CONTAINER.getUsername(),
                        POSTGRES_CONTAINER.getPassword());
        try {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS large_data");
                stmt.execute(
                        "CREATE TABLE large_data ("
                                + "id SERIAL PRIMARY KEY, "
                                + "name VARCHAR(50), "
                                + "value INT, "
                                + "category VARCHAR(20), "
                                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            }
            conn.setAutoCommit(false);
            String insertSql = "INSERT INTO large_data (name, value, category) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (int i = 1; i <= LARGE_DATA_COUNT; i++) {
                    pstmt.setString(1, "Name_" + i);
                    pstmt.setInt(2, i % 1000);
                    pstmt.setString(3, "Category_" + (i % 10));
                    pstmt.addBatch();
                    if (i % 10000 == 0) {
                        pstmt.executeBatch();
                        conn.commit();
                    }
                }
                pstmt.executeBatch();
                conn.commit();
            }
        } finally {
            conn.close();
        }
    }

    @AfterAll
    static void cleanupLargeDataSet() throws Exception {
        Connection conn =
                DriverManager.getConnection(
                        POSTGRES_CONTAINER.getJdbcUrl(),
                        POSTGRES_CONTAINER.getUsername(),
                        POSTGRES_CONTAINER.getPassword());
        try {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS large_data");
            }
        } finally {
            conn.close();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        connection =
                DriverManager.getConnection(
                        POSTGRES_CONTAINER.getJdbcUrl(),
                        POSTGRES_CONTAINER.getUsername(),
                        POSTGRES_CONTAINER.getPassword());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Should count total records correctly")
    void shouldCountTotalRecords() throws Exception {
        try (
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM large_data")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(LARGE_DATA_COUNT);
        }
    }

    @Test
    @DisplayName("Should iterate through large result set with default fetch size")
    void shouldIterateWithDefaultFetchSize() throws Exception {
        int count = 0;
        try (
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM large_data ORDER BY id")) {
            while (rs.next()) {
                count++;
            }
        }
        assertThat(count).isEqualTo(LARGE_DATA_COUNT);
    }

    @Test
    @DisplayName("Should iterate through large result set with custom fetch size")
    void shouldIterateWithCustomFetchSize() throws Exception {
        int count = 0;
        int fetchSize = 1000;
        try (Statement stmt = connection.createStatement()) {
            stmt.setFetchSize(fetchSize);
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM large_data ORDER BY id")) {
                while (rs.next()) {
                    count++;
                }
            }
        }
        assertThat(count).isEqualTo(LARGE_DATA_COUNT);
    }

    @Test
    @DisplayName("Should handle pagination on large data set")
    void shouldHandlePagination() throws Exception {
        int pageSize = 1000;
        for (int page = 0; page < 3; page++) {
            int offset = page * pageSize;
            String sql =
                    String.format(
                            "SELECT * FROM large_data ORDER BY id LIMIT %d OFFSET %d", pageSize, offset);
            int count = 0;
            try (
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    count++;
                    int expectedId = offset + count;
                    assertThat(rs.getInt("id")).isEqualTo(expectedId);
                }
            }
            assertThat(count).isEqualTo(pageSize);
        }
    }

    @Test
    @DisplayName("Should perform aggregation on large data set")
    void shouldPerformAggregation() throws Exception {
        try (
                Statement stmt = connection.createStatement();
                ResultSet rs =
                        stmt.executeQuery(
                                "SELECT category, COUNT(*) as cnt, AVG(value) as avg_value, SUM(value) as sum_value"
                                        + " FROM large_data GROUP BY category ORDER BY category")) {
            int categoryCount = 0;
            while (rs.next()) {
                categoryCount++;
                String category = rs.getString("category");
                int count = rs.getInt("cnt");
                assertThat(category).startsWith("Category_");
                assertThat(count).isEqualTo(LARGE_DATA_COUNT / 10);
            }
            assertThat(categoryCount).isEqualTo(10);
        }
    }

    @Test
    @DisplayName("Should filter large data set efficiently")
    void shouldFilterLargeDataSet() throws Exception {
        String sql = "SELECT * FROM large_data WHERE value > 990 ORDER BY id";
        int count = 0;
        try (
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int value = rs.getInt("value");
                assertThat(value).isGreaterThan(990);
                count++;
            }
        }
        int expectedCount = (LARGE_DATA_COUNT / 1000) * 9;
        assertThat(count).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("Should execute ORDER BY on large data set")
    void shouldExecuteOrderBy() throws Exception {
        String sql = "SELECT * FROM large_data ORDER BY value DESC LIMIT 10";
        int previousValue = Integer.MAX_VALUE;
        int count = 0;
        try (
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int currentValue = rs.getInt("value");
                assertThat(currentValue).isLessThanOrEqualTo(previousValue);
                previousValue = currentValue;
                count++;
            }
        }
        assertThat(count).isEqualTo(10);
    }

    @Test
    @DisplayName("Should execute subquery on large data set")
    void shouldExecuteSubquery() throws Exception {
        String sql =
                "SELECT * FROM large_data WHERE value > " + "(SELECT AVG(value) FROM large_data) LIMIT 100";
        int count = 0;
        try (
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                count++;
            }
        }
        assertThat(count).isEqualTo(100);
    }

    @Test
    @DisplayName("Should handle distinct query on large data set")
    void shouldHandleDistinctQuery() throws Exception {
        String sql = "SELECT DISTINCT category FROM large_data ORDER BY category";
        List<String> categories = new ArrayList<>();
        try (
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        }
        assertThat(categories).hasSize(10);
    }

    @Test
    @DisplayName("Should handle JOIN on large data set")
    void shouldHandleJoin() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                    "CREATE TEMP TABLE category_info AS "
                            + "SELECT DISTINCT category, 'Description of ' || category as description "
                            + "FROM large_data");
        }
        String sql =
                "SELECT l.category, l.name, c.description "
                        + "FROM large_data l "
                        + "JOIN category_info c ON l.category = c.category "
                        + "LIMIT 1000";
        int count = 0;
        try (
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String category = rs.getString("category");
                String description = rs.getString("description");
                assertThat(category).startsWith("Category_");
                assertThat(description).contains("Description of");
                count++;
            }
        }
        assertThat(count).isEqualTo(1000);
    }

    @Test
    @DisplayName("Should stream large result set without loading all into memory")
    void shouldStreamLargeResultSet() throws Exception {
        connection.setAutoCommit(false);
        int processedCount = 0;
        int batchSize = 5000;
        try (Statement stmt = connection.createStatement()) {
            stmt.setFetchSize(batchSize);
            try (ResultSet rs = stmt.executeQuery("SELECT id, value FROM large_data ORDER BY id")) {
                while (rs.next()) {
                    processedCount++;
                    if (processedCount % batchSize == 0) {
                        assertThat(rs.getInt("id")).isPositive();
                    }
                }
            }
        } finally {
            connection.setAutoCommit(true);
        }
        assertThat(processedCount).isEqualTo(LARGE_DATA_COUNT);
    }

    @Test
    @DisplayName("Should measure query execution time for large data set")
    void shouldMeasureQueryExecutionTime() throws Exception {
        long startTime = System.currentTimeMillis();
        try (
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM large_data WHERE value > 500")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isPositive();
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(10000);
    }
}
