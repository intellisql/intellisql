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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for integration tests. Provides common test infrastructure and utility
 * methods.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest {

    protected static final Network NETWORK = Network.newNetwork();

    protected Connection createConnection(final JdbcDatabaseContainer<?> container) throws SQLException {
        String jdbcUrl = container.getJdbcUrl();
        String username = container.getUsername();
        String password = container.getPassword();
        Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
        assertThat(connection).isNotNull();
        assertThat(connection.isValid(5)).isTrue();
        return connection;
    }

    protected void executeSql(final Connection connection, final String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    protected void executeUpdate(final Connection connection, final String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            int rows = statement.executeUpdate(sql);
            assertThat(rows).isGreaterThanOrEqualTo(0);
        }
    }

    protected int executeQueryCount(final Connection connection, final String sql) throws SQLException {
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        }
    }

    protected void createTestTable(final Connection connection, final String tableName, final String columns) throws SQLException {
        String createTableSql = String.format("CREATE TABLE %s (%s)", tableName, columns);
        executeUpdate(connection, createTableSql);
    }

    protected void dropTableIfExists(final Connection connection, final String tableName) throws SQLException {
        String dropTableSql = String.format("DROP TABLE IF EXISTS %s", tableName);
        executeUpdate(connection, dropTableSql);
    }

    protected void insertTestData(
                                  final Connection connection, final String tableName, final String columns, final String values) throws SQLException {
        String insertSql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, values);
        executeUpdate(connection, insertSql);
    }
}
