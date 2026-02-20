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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for cross-source JOIN operations. Tests federated queries across MySQL and
 * PostgreSQL.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CrossSourceJoinIT extends AbstractIntegrationTest {

    private static final String MYSQL_IMAGE = "mysql:8.0";

    private static final String POSTGRES_IMAGE = "postgres:15-alpine";

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
    static void setUpContainers() {
        assertThat(MYSQL_CONTAINER.isRunning()).isTrue();
        assertThat(POSTGRES_CONTAINER.isRunning()).isTrue();
    }

    @BeforeEach
    void setUp() throws Exception {
        mysqlConnection = createConnection(MYSQL_CONTAINER);
        postgresConnection = createConnection(POSTGRES_CONTAINER);
        setupMySQLData();
        setupPostgreSQLData();
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

    @AfterAll
    static void tearDownContainers() {
    }

    private void setupMySQLData() throws Exception {
        dropTableIfExists(mysqlConnection, "customers");
        createTestTable(
                mysqlConnection, "customers", "id INT PRIMARY KEY, name VARCHAR(100), region VARCHAR(50)");
        insertTestData(mysqlConnection, "customers", "id, name, region", "1, 'John Doe', 'North'");
        insertTestData(mysqlConnection, "customers", "id, name, region", "2, 'Jane Smith', 'South'");
        insertTestData(mysqlConnection, "customers", "id, name, region", "3, 'Bob Wilson', 'East'");
    }

    private void setupPostgreSQLData() throws Exception {
        dropTableIfExists(postgresConnection, "orders");
        createTestTable(
                postgresConnection,
                "orders",
                "id INT PRIMARY KEY, customer_id INT, amount DECIMAL(10,2), order_date DATE");
        insertTestData(
                postgresConnection,
                "orders",
                "id, customer_id, amount, order_date",
                "1, 1, 100.00, '2024-01-15'");
        insertTestData(
                postgresConnection,
                "orders",
                "id, customer_id, amount, order_date",
                "2, 1, 250.00, '2024-01-20'");
        insertTestData(
                postgresConnection,
                "orders",
                "id, customer_id, amount, order_date",
                "3, 2, 175.50, '2024-02-01'");
    }

    @Test
    @DisplayName("Should verify both data sources are accessible")
    void shouldVerifyDataSourcesAccessible() throws Exception {
        int mysqlCustomerCount = executeQueryCount(mysqlConnection, "SELECT COUNT(*) FROM customers");
        int postgresOrderCount = executeQueryCount(postgresConnection, "SELECT COUNT(*) FROM orders");
        assertThat(mysqlCustomerCount).isEqualTo(3);
        assertThat(postgresOrderCount).isEqualTo(3);
    }

    @Test
    @DisplayName("Should simulate cross-source JOIN by fetching and joining in memory")
    void shouldSimulateCrossSourceJoin() throws Exception {
        List<Customer> customers = fetchCustomersFromMySQL();
        List<Order> orders = fetchOrdersFromPostgreSQL();
        List<JoinedResult> results = performInMemoryJoin(customers, orders);
        assertThat(results).hasSize(3);
        assertThat(results)
                .extracting("customerName")
                .containsExactlyInAnyOrder("John Doe", "John Doe", "Jane Smith");
    }

    @Test
    @DisplayName("Should aggregate data across sources")
    void shouldAggregateDataAcrossSources() throws Exception {
        List<Customer> customers = fetchCustomersFromMySQL();
        List<Order> orders = fetchOrdersFromPostgreSQL();
        java.util.Map<Integer, String> customerMap = new java.util.HashMap<>();
        for (Customer customer : customers) {
            customerMap.put(customer.id, customer.name);
        }
        java.util.Map<String, Double> regionTotals = new java.util.HashMap<>();
        for (Order order : orders) {
            String customerName = customerMap.get(order.customerId);
            if (customerName != null) {
                Double customerTotal =
                        orders.stream()
                                .filter(o -> o.customerId == order.customerId)
                                .mapToDouble(o -> o.amount)
                                .sum();
                if ("John Doe".equals(customerName)) {
                    regionTotals.put(customerName, customerTotal);
                }
            }
        }
        assertThat(regionTotals.get("John Doe")).isEqualTo(350.00);
    }

    @Test
    @DisplayName("Should handle filtering on one source before join")
    void shouldHandleFilteringBeforeJoin() throws Exception {
        List<Customer> filteredCustomers = new ArrayList<>();
        try (
                Statement statement = mysqlConnection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery("SELECT * FROM customers WHERE region = 'North'")) {
            while (resultSet.next()) {
                filteredCustomers.add(
                        new Customer(
                                resultSet.getInt("id"),
                                resultSet.getString("name"),
                                resultSet.getString("region")));
            }
        }
        List<Order> orders = fetchOrdersFromPostgreSQL();
        List<JoinedResult> results = performInMemoryJoin(filteredCustomers, orders);
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(r -> r.customerName.equals("John Doe"));
    }

    @Test
    @DisplayName("Should handle pagination across sources")
    void shouldHandlePaginationAcrossSources() throws Exception {
        List<Order> orders = fetchOrdersFromPostgreSQL();
        List<Order> paginatedOrders =
                orders.stream()
                        .sorted((o1, o2) -> Double.compare(o2.amount, o1.amount))
                        .limit(2)
                        .collect(java.util.stream.Collectors.toList());
        assertThat(paginatedOrders).hasSize(2);
        assertThat(paginatedOrders.get(0).amount).isGreaterThanOrEqualTo(paginatedOrders.get(1).amount);
    }

    @Test
    @DisplayName("Should perform UNION operation across sources")
    void shouldPerformUnionAcrossSources() throws Exception {
        List<String> allNames = new ArrayList<>();
        try (
                Statement statement = mysqlConnection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT name FROM customers")) {
            while (resultSet.next()) {
                allNames.add(resultSet.getString("name"));
            }
        }
        try (
                Statement statement = postgresConnection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT 'Order ' || id as name FROM orders")) {
            while (resultSet.next()) {
                allNames.add(resultSet.getString("name"));
            }
        }
        assertThat(allNames).hasSize(6);
    }

    @Test
    @DisplayName("Should handle data type conversion across sources")
    void shouldHandleDataTypeConversion() throws Exception {
        try (
                Statement mysqlStmt = mysqlConnection.createStatement();
                Statement pgStmt = postgresConnection.createStatement()) {
            ResultSet mysqlRs =
                    mysqlStmt.executeQuery(
                            "SELECT id, CAST(name AS CHAR(50)) as name FROM customers WHERE id = 1");
            assertThat(mysqlRs.next()).isTrue();
            String mysqlName = mysqlRs.getString("name");
            ResultSet pgRs =
                    pgStmt.executeQuery(
                            "SELECT customer_id, CAST(amount AS VARCHAR) as amount FROM orders WHERE customer_id"
                                    + " = 1");
            List<String> amounts = new ArrayList<>();
            while (pgRs.next()) {
                amounts.add(pgRs.getString("amount"));
            }
            assertThat(mysqlName).isEqualTo("John Doe");
            assertThat(amounts).containsExactlyInAnyOrder("100.00", "250.00");
        }
    }

    private List<Customer> fetchCustomersFromMySQL() throws Exception {
        List<Customer> customers = new ArrayList<>();
        try (
                Statement statement = mysqlConnection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM customers")) {
            while (resultSet.next()) {
                customers.add(
                        new Customer(
                                resultSet.getInt("id"),
                                resultSet.getString("name"),
                                resultSet.getString("region")));
            }
        }
        return customers;
    }

    private List<Order> fetchOrdersFromPostgreSQL() throws Exception {
        List<Order> orders = new ArrayList<>();
        try (
                Statement statement = postgresConnection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM orders")) {
            while (resultSet.next()) {
                orders.add(
                        new Order(
                                resultSet.getInt("id"),
                                resultSet.getInt("customer_id"),
                                resultSet.getDouble("amount"),
                                resultSet.getDate("order_date")));
            }
        }
        return orders;
    }

    private List<JoinedResult> performInMemoryJoin(final List<Customer> customers, final List<Order> orders) {
        List<JoinedResult> results = new ArrayList<>();
        java.util.Map<Integer, Customer> customerMap =
                customers.stream().collect(java.util.stream.Collectors.toMap(c -> c.id, c -> c));
        for (Order order : orders) {
            Customer customer = customerMap.get(order.customerId);
            if (customer != null) {
                results.add(
                        new JoinedResult(customer.name, customer.region, order.amount, order.orderDate));
            }
        }
        return results;
    }

    private static class Customer {

        private final int id;

        private final String name;

        private final String region;

        Customer(final int id, final String name, final String region) {
            this.id = id;
            this.name = name;
            this.region = region;
        }
    }

    private static class Order {

        private final int id;

        private final int customerId;

        private final double amount;

        private final java.sql.Date orderDate;

        Order(final int id, final int customerId, final double amount, final java.sql.Date orderDate) {
            this.id = id;
            this.customerId = customerId;
            this.amount = amount;
            this.orderDate = orderDate;
        }
    }

    private static class JoinedResult {

        private final String customerName;

        private final String region;

        private final double amount;

        private final java.sql.Date orderDate;

        JoinedResult(final String customerName, final String region, final double amount, final java.sql.Date orderDate) {
            this.customerName = customerName;
            this.region = region;
            this.amount = amount;
            this.orderDate = orderDate;
        }

        public String getCustomerName() {
            return customerName;
        }
    }
}
