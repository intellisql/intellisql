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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for federated query (cross-source JOIN) operations. Measures performance of
 * queries spanning multiple data sources.
 */
@Testcontainers
public class FederatedQueryPerformanceTest {

    private static final Network NETWORK = Network.newNetwork();

    private static final String MYSQL_IMAGE = "mysql:8.0";

    private static final String POSTGRES_IMAGE = "postgres:15-alpine";

    private static final int FEDERATED_DATA_COUNT = 10000;

    @Container
    private static final MySQLContainer<?> MYSQL_CONTAINER =
            new MySQLContainer<>(DockerImageName.parse(MYSQL_IMAGE))
                    .withNetwork(NETWORK)
                    .withNetworkAliases("mysql")
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpass")
                    .withReuse(true);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
                    .withNetwork(NETWORK)
                    .withNetworkAliases("postgres")
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpass")
                    .withReuse(true);

    private Connection mysqlConnection;

    private Connection postgresConnection;

    @BeforeAll
    static void setupFederatedData() throws Exception {
        // Load drivers
        Class.forName("com.mysql.cj.jdbc.Driver");
        Class.forName("org.postgresql.Driver");
        setupMySQLData();
        setupPostgreSQLData();
    }

    private static void setupMySQLData() throws Exception {
        Connection conn =
                DriverManager.getConnection(
                        MYSQL_CONTAINER.getJdbcUrl(),
                        MYSQL_CONTAINER.getUsername(),
                        MYSQL_CONTAINER.getPassword());
        try {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS customers");
                stmt.execute(
                        "CREATE TABLE customers ("
                                + "id INT PRIMARY KEY, "
                                + "name VARCHAR(100), "
                                + "region VARCHAR(50), "
                                + "tier VARCHAR(20))");
            }
            conn.setAutoCommit(false);
            String insertSql = "INSERT INTO customers (id, name, region, tier) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (int i = 1; i <= FEDERATED_DATA_COUNT; i++) {
                    pstmt.setInt(1, i);
                    pstmt.setString(2, "Customer_" + i);
                    pstmt.setString(3, "Region_" + (i % 10));
                    pstmt.setString(4, i % 3 == 0 ? "GOLD" : (i % 3 == 1 ? "SILVER" : "BRONZE"));
                    pstmt.addBatch();
                    if (i % 2000 == 0) {
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

    private static void setupPostgreSQLData() throws Exception {
        Connection conn =
                DriverManager.getConnection(
                        POSTGRES_CONTAINER.getJdbcUrl(),
                        POSTGRES_CONTAINER.getUsername(),
                        POSTGRES_CONTAINER.getPassword());
        try {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS orders");
                stmt.execute(
                        "CREATE TABLE orders ("
                                + "id SERIAL PRIMARY KEY, "
                                + "customer_id INT, "
                                + "amount DECIMAL(10,2), "
                                + "status VARCHAR(20))");
                stmt.execute("CREATE INDEX idx_customer_id ON orders(customer_id)");
            }
            conn.setAutoCommit(false);
            String insertSql = "INSERT INTO orders (customer_id, amount, status) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (int i = 1; i <= FEDERATED_DATA_COUNT * 3; i++) {
                    pstmt.setInt(1, ((i - 1) % FEDERATED_DATA_COUNT) + 1);
                    pstmt.setDouble(2, (i % 1000) * 10.0);
                    pstmt.setString(3, i % 4 == 0 ? "CANCELLED" : (i % 4 == 1 ? "PENDING" : "COMPLETED"));
                    pstmt.addBatch();
                    if (i % 5000 == 0) {
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
    static void cleanupFederatedData() throws Exception {
        try (
                Connection conn =
                        DriverManager.getConnection(
                                MYSQL_CONTAINER.getJdbcUrl(),
                                MYSQL_CONTAINER.getUsername(),
                                MYSQL_CONTAINER.getPassword())) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS customers");
            }
        }
        try (
                Connection conn =
                        DriverManager.getConnection(
                                POSTGRES_CONTAINER.getJdbcUrl(),
                                POSTGRES_CONTAINER.getUsername(),
                                POSTGRES_CONTAINER.getPassword())) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS orders");
            }
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        mysqlConnection =
                DriverManager.getConnection(
                        MYSQL_CONTAINER.getJdbcUrl(),
                        MYSQL_CONTAINER.getUsername(),
                        MYSQL_CONTAINER.getPassword());
        postgresConnection =
                DriverManager.getConnection(
                        POSTGRES_CONTAINER.getJdbcUrl(),
                        POSTGRES_CONTAINER.getUsername(),
                        POSTGRES_CONTAINER.getPassword());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mysqlConnection != null && !mysqlConnection.isClosed()) {
            mysqlConnection.close();
        }
        if (postgresConnection != null && !postgresConnection.isClosed()) {
            postgresConnection.close();
        }
    }

    @Test
    @DisplayName("Should measure federated data fetch performance")
    void shouldMeasureFederatedDataFetchPerformance() throws Exception {
        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> customers = new ArrayList<>();
        try (
                Statement stmt = mysqlConnection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, name, region, tier FROM customers")) {
            while (rs.next()) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("id", rs.getInt("id"));
                customer.put("name", rs.getString("name"));
                customer.put("region", rs.getString("region"));
                customer.put("tier", rs.getString("tier"));
                customers.add(customer);
            }
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        assertThat(customers).hasSize(FEDERATED_DATA_COUNT);
        assertThat(duration).isLessThan(5000);
    }

    @Test
    @DisplayName("Should measure in-memory JOIN performance")
    void shouldMeasureInMemoryJoinPerformance() throws Exception {
        Map<Integer, String> customerMap = new HashMap<>();
        try (
                Statement stmt = mysqlConnection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, name FROM customers")) {
            while (rs.next()) {
                customerMap.put(rs.getInt("id"), rs.getString("name"));
            }
        }
        long startTime = System.currentTimeMillis();
        List<JoinedResult> results = new ArrayList<>();
        try (
                Statement stmt = postgresConnection.createStatement();
                ResultSet rs =
                        stmt.executeQuery(
                                "SELECT customer_id, amount, status FROM orders WHERE status = 'COMPLETED'")) {
            while (rs.next()) {
                int customerId = rs.getInt("customer_id");
                String customerName = customerMap.get(customerId);
                if (customerName != null) {
                    results.add(
                            new JoinedResult(customerName, rs.getDouble("amount"), rs.getString("status")));
                }
            }
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        assertThat(results).isNotEmpty();
        assertThat(duration).isLessThan(10000);
    }

    @Test
    @DisplayName("Should measure filtered federated query performance")
    void shouldMeasureFilteredFederatedQueryPerformance() throws Exception {
        long startTime = System.currentTimeMillis();
        List<Integer> goldCustomerIds = new ArrayList<>();
        try (
                Statement stmt = mysqlConnection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id FROM customers WHERE tier = 'GOLD'")) {
            while (rs.next()) {
                goldCustomerIds.add(rs.getInt("id"));
            }
        }
        int orderCount = 0;
        for (Integer customerId : goldCustomerIds) {
            try (
                    PreparedStatement pstmt =
                            postgresConnection.prepareStatement(
                                    "SELECT COUNT(*) FROM orders WHERE customer_id = ? AND status = 'COMPLETED'")) {
                pstmt.setInt(1, customerId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        orderCount += rs.getInt(1);
                    }
                }
            }
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        assertThat(orderCount).isGreaterThanOrEqualTo(0);
        assertThat(duration).isLessThan(30000);
    }

    @Test
    @DisplayName("Should measure aggregation across sources performance")
    void shouldMeasureAggregationAcrossSourcesPerformance() throws Exception {
        long startTime = System.currentTimeMillis();
        Map<String, Integer> regionOrderCounts = new HashMap<>();
        Map<Integer, String> customerRegions = new HashMap<>();
        try (
                Statement stmt = mysqlConnection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, region FROM customers")) {
            while (rs.next()) {
                customerRegions.put(rs.getInt("id"), rs.getString("region"));
            }
        }
        try (
                Statement stmt = postgresConnection.createStatement();
                ResultSet rs =
                        stmt.executeQuery(
                                "SELECT customer_id, COUNT(*) as cnt FROM orders GROUP BY customer_id")) {
            while (rs.next()) {
                int customerId = rs.getInt("customer_id");
                int orderCount = rs.getInt("cnt");
                String region = customerRegions.get(customerId);
                if (region != null) {
                    regionOrderCounts.merge(region, orderCount, Integer::sum);
                }
            }
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        assertThat(regionOrderCounts).hasSize(10);
        assertThat(duration).isLessThan(10000);
    }

    @Test
    @DisplayName("Should measure parallel data fetch performance")
    void shouldMeasureParallelDataFetchPerformance() throws Exception {
        Thread mysqlThread = new Thread(() -> executeMysqlCountQuery());
        Thread postgresThread = new Thread(() -> executePostgresCountQuery());
        final long startTime = System.currentTimeMillis();
        mysqlThread.start();
        postgresThread.start();
        mysqlThread.join();
        postgresThread.join();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(5000);
    }

    private static void executeMysqlCountQuery() {
        try (
                Connection conn =
                        DriverManager.getConnection(
                                MYSQL_CONTAINER.getJdbcUrl(),
                                MYSQL_CONTAINER.getUsername(),
                                MYSQL_CONTAINER.getPassword());
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")) {
            rs.next();
            // CHECKSTYLE:OFF IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            throw new RuntimeException(ex);
        }
    }

    private static void executePostgresCountQuery() {
        try (
                Connection conn =
                        DriverManager.getConnection(
                                POSTGRES_CONTAINER.getJdbcUrl(),
                                POSTGRES_CONTAINER.getUsername(),
                                POSTGRES_CONTAINER.getPassword());
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM orders")) {
            rs.next();
            // CHECKSTYLE:OFF IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            throw new RuntimeException(ex);
        }
    }

    @Test
    @DisplayName("Should measure large result federated join performance")
    void shouldMeasureLargeResultFederatedJoinPerformance() throws Exception {
        Map<Integer, String> customerTiers = new HashMap<>();
        try (
                Statement stmt = mysqlConnection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, tier FROM customers LIMIT 1000")) {
            while (rs.next()) {
                customerTiers.put(rs.getInt("id"), rs.getString("tier"));
            }
        }
        long startTime = System.currentTimeMillis();
        Map<String, Double> tierTotalAmounts = new HashMap<>();
        try (
                Statement stmt = postgresConnection.createStatement();
                ResultSet rs =
                        stmt.executeQuery(
                                "SELECT customer_id, SUM(amount) as total FROM orders GROUP BY customer_id")) {
            while (rs.next()) {
                int customerId = rs.getInt("customer_id");
                double total = rs.getDouble("total");
                String tier = customerTiers.get(customerId);
                if (tier != null) {
                    tierTotalAmounts.merge(tier, total, Double::sum);
                }
            }
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        assertThat(tierTotalAmounts).containsKeys("GOLD", "SILVER", "BRONZE");
        assertThat(duration).isLessThan(15000);
    }

    @Test
    @DisplayName("Should measure streaming federated query performance")
    void shouldMeasureStreamingFederatedQueryPerformance() throws Exception {
        Map<Integer, String> customerNames = new HashMap<>();
        try (
                Statement stmt = mysqlConnection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, name FROM customers")) {
            while (rs.next()) {
                customerNames.put(rs.getInt("id"), rs.getString("name"));
            }
        }
        long startTime = System.currentTimeMillis();
        int processedCount = 0;
        postgresConnection.setAutoCommit(false);
        try (Statement stmt = postgresConnection.createStatement()) {
            stmt.setFetchSize(1000);
            try (ResultSet rs = stmt.executeQuery("SELECT customer_id, amount FROM orders ORDER BY id")) {
                while (rs.next()) {
                    int customerId = rs.getInt("customer_id");
                    String name = customerNames.get(customerId);
                    if (name != null) {
                        processedCount++;
                    }
                }
            }
        } finally {
            postgresConnection.setAutoCommit(true);
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        assertThat(processedCount).isEqualTo(FEDERATED_DATA_COUNT * 3);
        assertThat(duration).isLessThan(20000);
    }

    private static class JoinedResult {

        private final String customerName;

        private final double amount;

        private final String status;

        JoinedResult(final String customerName, final double amount, final String status) {
            this.customerName = customerName;
            this.amount = amount;
            this.status = status;
        }
    }
}
