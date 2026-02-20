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

package org.intellisql.test.e2e;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
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

/**
 * Performance tests for query execution. Measures and validates query performance metrics.
 */
@Testcontainers
public class QueryPerformanceTest {

    private static final Network NETWORK = Network.newNetwork();

    private static final String POSTGRES_IMAGE = "postgres:15-alpine";

    private static final int PERFORMANCE_DATA_COUNT = 50000;

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
    static void setupPerformanceData() throws Exception {
        // Load PostgreSQL driver
        Class.forName("org.postgresql.Driver");
        Connection conn =
                DriverManager.getConnection(
                        POSTGRES_CONTAINER.getJdbcUrl(),
                        POSTGRES_CONTAINER.getUsername(),
                        POSTGRES_CONTAINER.getPassword());
        try {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS perf_data");
                stmt.execute(
                        "CREATE TABLE perf_data ("
                                + "id SERIAL PRIMARY KEY, "
                                + "name VARCHAR(50), "
                                + "value INT, "
                                + "category VARCHAR(20), "
                                + "status VARCHAR(10), "
                                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            }
            conn.setAutoCommit(false);
            String insertSql =
                    "INSERT INTO perf_data (name, value, category, status) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (int i = 1; i <= PERFORMANCE_DATA_COUNT; i++) {
                    pstmt.setString(1, "Name_" + i);
                    pstmt.setInt(2, i % 1000);
                    pstmt.setString(3, "Category_" + (i % 100));
                    pstmt.setString(4, i % 2 == 0 ? "active" : "inactive");
                    pstmt.addBatch();
                    if (i % 5000 == 0) {
                        pstmt.executeBatch();
                        conn.commit();
                    }
                }
                pstmt.executeBatch();
                conn.commit();
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE INDEX idx_perf_category ON perf_data(category)");
                stmt.execute("CREATE INDEX idx_perf_value ON perf_data(value)");
                stmt.execute("CREATE INDEX idx_perf_status ON perf_data(status)");
            }
        } finally {
            conn.close();
        }
    }

    @AfterAll
    static void cleanupPerformanceData() throws Exception {
        Connection conn =
                DriverManager.getConnection(
                        POSTGRES_CONTAINER.getJdbcUrl(),
                        POSTGRES_CONTAINER.getUsername(),
                        POSTGRES_CONTAINER.getPassword());
        try {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS perf_data");
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
    @DisplayName("Should measure simple SELECT performance")
    void shouldMeasureSimpleSelectPerformance() throws Exception {
        long totalTime = 0;
        int iterations = 10;
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            try (
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT 1")) {
                rs.next();
            }
            long endTime = System.nanoTime();
            totalTime += endTime - startTime;
        }
        long avgTimeMs = totalTime / iterations / 1_000_000;
        assertThat(avgTimeMs).isLessThan(50);
    }

    @Test
    @DisplayName("Should measure indexed query performance")
    void shouldMeasureIndexedQueryPerformance() throws Exception {
        String category = "Category_50";
        long startTime = System.currentTimeMillis();
        try (
                PreparedStatement pstmt =
                        connection.prepareStatement("SELECT * FROM perf_data WHERE category = ?")) {
            pstmt.setString(1, category);
            try (ResultSet rs = pstmt.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                }
                assertThat(count).isEqualTo(PERFORMANCE_DATA_COUNT / 100);
            }
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(1000);
    }

    @Test
    @DisplayName("Should measure aggregation query performance")
    void shouldMeasureAggregationQueryPerformance() throws Exception {
        long startTime = System.currentTimeMillis();
        try (
                Statement stmt = connection.createStatement();
                ResultSet rs =
                        stmt.executeQuery(
                                "SELECT category, COUNT(*) as cnt, AVG(value) as avg_val, MAX(value) as max_val "
                                        + "FROM perf_data GROUP BY category ORDER BY category")) {
            int categoryCount = 0;
            while (rs.next()) {
                categoryCount++;
            }
            assertThat(categoryCount).isEqualTo(100);
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(3000);
    }

    @Test
    @DisplayName("Should measure batch insert performance")
    void shouldMeasureBatchInsertPerformance() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TEMP TABLE perf_temp (id INT, name VARCHAR(50), value INT)");
        }
        int batchSize = 1000;
        long startTime = System.currentTimeMillis();
        try (
                PreparedStatement pstmt =
                        connection.prepareStatement("INSERT INTO perf_temp (id, name, value) VALUES (?, ?, ?)")) {
            for (int i = 1; i <= batchSize; i++) {
                pstmt.setInt(1, i);
                pstmt.setString(2, "Temp_" + i);
                pstmt.setInt(3, i);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(2000);
    }

    @Test
    @DisplayName("Should measure prepared statement vs statement performance")
    void shouldMeasurePreparedStatementPerformance() throws Exception {
        int iterations = 100;
        long statementTime = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            try (
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM perf_data WHERE id = " + (i + 1))) {
                rs.next();
            }
            statementTime += System.nanoTime() - start;
        }
        long preparedTime = 0;
        try (
                PreparedStatement pstmt =
                        connection.prepareStatement("SELECT * FROM perf_data WHERE id = ?")) {
            for (int i = 0; i < iterations; i++) {
                pstmt.setInt(1, i + 1);
                long start = System.nanoTime();
                try (ResultSet rs = pstmt.executeQuery()) {
                    rs.next();
                }
                preparedTime += System.nanoTime() - start;
            }
        }
        assertThat(statementTime).isGreaterThan(0);
        assertThat(preparedTime).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should measure ORDER BY performance")
    void shouldMeasureOrderByPerformance() throws Exception {
        int limit = 1000;
        long startTime = System.currentTimeMillis();
        try (
                Statement stmt = connection.createStatement();
                ResultSet rs =
                        stmt.executeQuery(
                                String.format("SELECT * FROM perf_data ORDER BY value DESC LIMIT %d", limit))) {
            int count = 0;
            while (rs.next()) {
                count++;
            }
            assertThat(count).isEqualTo(limit);
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(2000);
    }

    @Test
    @DisplayName("Should measure complex query performance")
    void shouldMeasureComplexQueryPerformance() throws Exception {
        long startTime = System.currentTimeMillis();
        String sql =
                "SELECT category, status, COUNT(*) as cnt, SUM(value) as total "
                        + "FROM perf_data "
                        + "WHERE value > 100 "
                        + "GROUP BY category, status "
                        + "HAVING COUNT(*) > 10 "
                        + "ORDER BY total DESC "
                        + "LIMIT 50";
        try (
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            int count = 0;
            while (rs.next()) {
                count++;
            }
            assertThat(count).isLessThanOrEqualTo(50);
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(3000);
    }

    @Test
    @DisplayName("Should measure subquery performance")
    void shouldMeasureSubqueryPerformance() throws Exception {
        long startTime = System.currentTimeMillis();
        String sql =
                "SELECT * FROM perf_data "
                        + "WHERE value > (SELECT AVG(value) FROM perf_data) "
                        + "LIMIT 100";
        try (
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            int count = 0;
            while (rs.next()) {
                count++;
            }
            assertThat(count).isEqualTo(100);
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(2000);
    }

    @Test
    @DisplayName("Should measure concurrent query performance")
    void shouldMeasureConcurrentQueryPerformance() throws Exception {
        int threadCount = 10;
        List<Thread> threads = new ArrayList<>();
        List<Long> durations = Collections.synchronizedList(new ArrayList<>());
        List<SQLException> exceptions = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> executeConcurrentQuery(durations, exceptions));
            threads.add(thread);
        }
        long totalStart = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        long totalEnd = System.currentTimeMillis();
        long totalDuration = totalEnd - totalStart;
        assertThat(exceptions).isEmpty();
        assertThat(durations).hasSize(threadCount);
        assertThat(totalDuration).isLessThan(10000);
    }

    /**
     * Executes a concurrent query and records the duration.
     *
     * @param durations the list to record query durations
     * @param exceptions the list to record any SQL exceptions
     */
    private void executeConcurrentQuery(final List<Long> durations, final List<SQLException> exceptions) {
        long start = System.currentTimeMillis();
        try (
                Connection conn =
                        DriverManager.getConnection(
                                POSTGRES_CONTAINER.getJdbcUrl(),
                                POSTGRES_CONTAINER.getUsername(),
                                POSTGRES_CONTAINER.getPassword());
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM perf_data")) {
            rs.next();
        } catch (final SQLException ex) {
            exceptions.add(ex);
        }
        long end = System.currentTimeMillis();
        durations.add(end - start);
    }

    @Test
    @DisplayName("Should measure connection acquisition performance")
    void shouldMeasureConnectionAcquisitionPerformance() throws Exception {
        int iterations = 20;
        long totalTime = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();
            try (
                    Connection conn =
                            DriverManager.getConnection(
                                    POSTGRES_CONTAINER.getJdbcUrl(),
                                    POSTGRES_CONTAINER.getUsername(),
                                    POSTGRES_CONTAINER.getPassword())) {
                assertThat(conn.isValid(3)).isTrue();
            }
            long end = System.currentTimeMillis();
            totalTime += end - start;
        }
        long avgTime = totalTime / iterations;
        assertThat(avgTime).isLessThan(100);
    }

    @Test
    @DisplayName("Should measure pagination performance")
    void shouldMeasurePaginationPerformance() throws Exception {
        int pageSize = 1000;
        int pages = 10;
        long totalTime = 0;
        for (int page = 0; page < pages; page++) {
            int offset = page * pageSize;
            long start = System.currentTimeMillis();
            try (
                    Statement stmt = connection.createStatement();
                    ResultSet rs =
                            stmt.executeQuery(
                                    String.format(
                                            "SELECT * FROM perf_data ORDER BY id LIMIT %d OFFSET %d",
                                            pageSize, offset))) {
                int count = 0;
                while (rs.next()) {
                    count++;
                }
                assertThat(count).isEqualTo(pageSize);
            }
            totalTime += System.currentTimeMillis() - start;
        }
        assertThat(totalTime).isLessThan(5000);
    }
}
