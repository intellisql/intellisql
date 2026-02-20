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
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for complete JDBC workflow. Tests the full lifecycle of database operations.
 */
@Testcontainers
public class JdbcE2ETest {

    private static final Network NETWORK = Network.newNetwork();

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

    @BeforeAll
    static void loadDriver() throws ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
    }

    @BeforeEach
    void setUp() throws Exception {
        connection =
                DriverManager.getConnection(
                        POSTGRES_CONTAINER.getJdbcUrl(),
                        POSTGRES_CONTAINER.getUsername(),
                        POSTGRES_CONTAINER.getPassword());
        setupDatabaseSchema();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            cleanupDatabase();
            connection.close();
        }
    }

    private void setupDatabaseSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS order_items");
            stmt.execute("DROP TABLE IF EXISTS orders");
            stmt.execute("DROP TABLE IF EXISTS customers");
            stmt.execute("DROP TABLE IF EXISTS products");
            stmt.execute(
                    "CREATE TABLE customers ("
                            + "id SERIAL PRIMARY KEY, "
                            + "name VARCHAR(100) NOT NULL, "
                            + "email VARCHAR(100) UNIQUE, "
                            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute(
                    "CREATE TABLE products ("
                            + "id SERIAL PRIMARY KEY, "
                            + "name VARCHAR(100) NOT NULL, "
                            + "price DECIMAL(10,2) NOT NULL, "
                            + "stock INT DEFAULT 0)");
            stmt.execute(
                    "CREATE TABLE orders ("
                            + "id SERIAL PRIMARY KEY, "
                            + "customer_id INT REFERENCES customers(id), "
                            + "order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                            + "status VARCHAR(20) DEFAULT 'pending')");
            stmt.execute(
                    "CREATE TABLE order_items ("
                            + "id SERIAL PRIMARY KEY, "
                            + "order_id INT REFERENCES orders(id), "
                            + "product_id INT REFERENCES products(id), "
                            + "quantity INT NOT NULL, "
                            + "unit_price DECIMAL(10,2) NOT NULL)");
        }
    }

    private void cleanupDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS order_items");
            stmt.execute("DROP TABLE IF EXISTS orders");
            stmt.execute("DROP TABLE IF EXISTS customers");
            stmt.execute("DROP TABLE IF EXISTS products");
        }
    }

    private int insertCustomer(final String name, final String email) throws SQLException {
        String sql = "INSERT INTO customers (name, email) VALUES (?, ?) RETURNING id";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to insert customer");
    }

    private int insertProduct(final String name, final double price, final int stock) throws SQLException {
        String sql = "INSERT INTO products (name, price, stock) VALUES (?, ?, ?) RETURNING id";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setDouble(2, price);
            pstmt.setInt(3, stock);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to insert product");
    }

    private int insertOrder(final int customerId, final String status) throws SQLException {
        String sql = "INSERT INTO orders (customer_id, status) VALUES (?, ?) RETURNING id";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            pstmt.setString(2, status);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to insert order");
    }

    private void insertOrderItem(
                                 final int orderId, final int productId, final int quantity, final double unitPrice) throws SQLException {
        String sql =
                "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            pstmt.setInt(2, productId);
            pstmt.setInt(3, quantity);
            pstmt.setDouble(4, unitPrice);
            pstmt.executeUpdate();
        }
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperations {

        @Test
        @DisplayName("Should create and read customer")
        void shouldCreateAndReadCustomer() throws SQLException {
            String insertSql = "INSERT INTO customers (name, email) VALUES (?, ?) RETURNING id";
            int customerId;
            try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
                pstmt.setString(1, "John Doe");
                pstmt.setString(2, "john@example.com");
                try (ResultSet rs = pstmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    customerId = rs.getInt(1);
                    assertThat(customerId).isPositive();
                }
            }
            String selectSql = "SELECT * FROM customers WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(selectSql)) {
                pstmt.setInt(1, customerId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("name")).isEqualTo("John Doe");
                    assertThat(rs.getString("email")).isEqualTo("john@example.com");
                }
            }
        }

        @Test
        @DisplayName("Should update customer")
        void shouldUpdateCustomer() throws SQLException {
            int customerId = insertCustomer("Jane Smith", "jane@example.com");
            String updateSql = "UPDATE customers SET name = ?, email = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                pstmt.setString(1, "Jane Doe");
                pstmt.setString(2, "jane.doe@example.com");
                pstmt.setInt(3, customerId);
                int rowsUpdated = pstmt.executeUpdate();
                assertThat(rowsUpdated).isEqualTo(1);
            }
            try (
                    PreparedStatement pstmt =
                            connection.prepareStatement("SELECT * FROM customers WHERE id = ?")) {
                pstmt.setInt(1, customerId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("name")).isEqualTo("Jane Doe");
                    assertThat(rs.getString("email")).isEqualTo("jane.doe@example.com");
                }
            }
        }

        @Test
        @DisplayName("Should delete customer")
        void shouldDeleteCustomer() throws SQLException {
            int customerId = insertCustomer("Bob Wilson", "bob@example.com");
            String deleteSql = "DELETE FROM customers WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteSql)) {
                pstmt.setInt(1, customerId);
                int rowsDeleted = pstmt.executeUpdate();
                assertThat(rowsDeleted).isEqualTo(1);
            }
            try (
                    PreparedStatement pstmt =
                            connection.prepareStatement("SELECT COUNT(*) FROM customers WHERE id = ?")) {
                pstmt.setInt(1, customerId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(0);
                }
            }
        }
    }

    @Nested
    @DisplayName("Transaction Operations")
    class TransactionOperations {

        @Test
        @DisplayName("Should commit transaction successfully")
        void shouldCommitTransaction() throws SQLException {
            connection.setAutoCommit(false);
            try {
                insertCustomer("Alice", "alice@example.com");
                insertCustomer("Bob", "bob@example.com");
                connection.commit();
            } finally {
                connection.setAutoCommit(true);
            }
            try (
                    Statement stmt = connection.createStatement();
                    ResultSet rs =
                            stmt.executeQuery(
                                    "SELECT COUNT(*) FROM customers WHERE email LIKE '%@example.com'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(2);
            }
        }

        @Test
        @DisplayName("Should rollback transaction on error")
        void shouldRollbackTransaction() throws SQLException {
            connection.setAutoCommit(false);
            try {
                insertCustomer("Charlie", "charlie@example.com");
                connection.rollback();
            } finally {
                connection.setAutoCommit(true);
            }
            try (
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0);
            }
        }

        @Test
        @DisplayName("Should handle savepoints correctly")
        void shouldHandleSavepoints() throws SQLException {
            connection.setAutoCommit(false);
            try {
                insertCustomer("David", "david@example.com");
                Savepoint savepoint = connection.setSavepoint("after_david");
                insertCustomer("Eve", "eve@example.com");
                connection.rollback(savepoint);
                insertCustomer("Frank", "frank@example.com");
                connection.commit();
            } finally {
                connection.setAutoCommit(true);
            }
            try (
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT name FROM customers ORDER BY name")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("name")).isEqualTo("David");
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("name")).isEqualTo("Frank");
                assertThat(rs.next()).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("Batch Operations")
    class BatchOperations {

        @Test
        @DisplayName("Should execute batch insert")
        void shouldExecuteBatchInsert() throws SQLException {
            String insertSql = "INSERT INTO products (name, price, stock) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
                for (int i = 1; i <= 10; i++) {
                    pstmt.setString(1, "Product " + i);
                    pstmt.setDouble(2, i * 10.0);
                    pstmt.setInt(3, i * 5);
                    pstmt.addBatch();
                }
                int[] results = pstmt.executeBatch();
                assertThat(results).hasSize(10);
            }
            try (
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(10);
            }
        }

        @Test
        @DisplayName("Should execute batch update")
        void shouldExecuteBatchUpdate() throws SQLException {
            for (int i = 1; i <= 5; i++) {
                insertProduct("Product " + i, 100.0, 10);
            }
            String updateSql = "UPDATE products SET price = price * 1.1 WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                for (int i = 1; i <= 5; i++) {
                    pstmt.setInt(1, i);
                    pstmt.addBatch();
                }
                int[] results = pstmt.executeBatch();
                assertThat(results).hasSize(5);
            }
            try (
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT price FROM products WHERE id = 1")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getDouble("price")).isEqualTo(110.0);
            }
        }
    }

    @Nested
    @DisplayName("Complex Query Operations")
    class ComplexQueryOperations {

        @BeforeEach
        void setupTestData() throws SQLException {
            int customerId1 = insertCustomer("John", "john@test.com");
            int customerId2 = insertCustomer("Jane", "jane@test.com");
            int productId1 = insertProduct("Laptop", 999.99, 10);
            int productId2 = insertProduct("Mouse", 29.99, 50);
            int orderId1 = insertOrder(customerId1, "completed");
            int orderId2 = insertOrder(customerId2, "pending");
            insertOrderItem(orderId1, productId1, 1, 999.99);
            insertOrderItem(orderId1, productId2, 2, 29.99);
            insertOrderItem(orderId2, productId2, 1, 29.99);
        }

        @Test
        @DisplayName("Should execute JOIN query")
        void shouldExecuteJoinQuery() throws SQLException {
            String sql =
                    "SELECT c.name AS customer_name, o.id AS order_id, o.status "
                            + "FROM customers c "
                            + "JOIN orders o ON c.id = o.customer_id "
                            + "ORDER BY c.name";
            try (
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("customer_name")).isEqualTo("Jane");
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("customer_name")).isEqualTo("John");
            }
        }

        @Test
        @DisplayName("Should execute multi-table JOIN query")
        void shouldExecuteMultiTableJoinQuery() throws SQLException {
            String sql =
                    "SELECT c.name, p.name AS product_name, oi.quantity, oi.unit_price "
                            + "FROM customers c "
                            + "JOIN orders o ON c.id = o.customer_id "
                            + "JOIN order_items oi ON o.id = oi.order_id "
                            + "JOIN products p ON oi.product_id = p.id "
                            + "WHERE c.name = 'John'";
            try (
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    assertThat(rs.getString("name")).isEqualTo("John");
                }
                assertThat(count).isEqualTo(2);
            }
        }

        @Test
        @DisplayName("Should execute aggregate query with GROUP BY")
        void shouldExecuteAggregateQuery() throws SQLException {
            String sql =
                    "SELECT c.name, COUNT(DISTINCT o.id) AS order_count, SUM(oi.quantity * oi.unit_price) AS total "
                            + "FROM customers c "
                            + "LEFT JOIN orders o ON c.id = o.customer_id "
                            + "LEFT JOIN order_items oi ON o.id = oi.order_id "
                            + "GROUP BY c.id, c.name "
                            + "ORDER BY c.name";
            try (
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("name")).isEqualTo("Jane");
                assertThat(rs.getInt("order_count")).isEqualTo(1);
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("name")).isEqualTo("John");
                assertThat(rs.getInt("order_count")).isEqualTo(1);
            }
        }

        @Test
        @DisplayName("Should execute subquery")
        void shouldExecuteSubquery() throws SQLException {
            String sql = "SELECT * FROM products WHERE price > " + "(SELECT AVG(price) FROM products)";
            try (
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("name")).isEqualTo("Laptop");
                assertThat(rs.getDouble("price")).isEqualTo(999.99);
            }
        }
    }
}
