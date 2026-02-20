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
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

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
 * End-to-end tests for JDBC connection handling. Tests connection establishment, configuration, and
 * lifecycle.
 */
@Testcontainers
public class JdbcConnectionIT {

    private static final Network NETWORK = Network.newNetwork();

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
    static void loadDrivers() throws ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Class.forName("org.postgresql.Driver");
    }

    @BeforeEach
    void setUp() throws Exception {
        mysqlConnection = createMySQLConnection();
        postgresConnection = createPostgreSQLConnection();
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
    @DisplayName("Should establish MySQL connection successfully")
    void shouldEstablishMySQLConnection() throws Exception {
        assertThat(mysqlConnection).isNotNull();
        assertThat(mysqlConnection.isClosed()).isFalse();
        assertThat(mysqlConnection.isValid(5)).isTrue();
    }

    @Test
    @DisplayName("Should establish PostgreSQL connection successfully")
    void shouldEstablishPostgreSQLConnection() throws Exception {
        assertThat(postgresConnection).isNotNull();
        assertThat(postgresConnection.isClosed()).isFalse();
        assertThat(postgresConnection.isValid(5)).isTrue();
    }

    @Test
    @DisplayName("Should retrieve MySQL database metadata")
    void shouldRetrieveMySQLDatabaseMetadata() throws Exception {
        DatabaseMetaData metaData = mysqlConnection.getMetaData();
        assertThat(metaData).isNotNull();
        assertThat(metaData.getDatabaseProductName()).containsIgnoringCase("MySQL");
        assertThat(metaData.getDatabaseMajorVersion()).isGreaterThan(0);
        assertThat(metaData.getDriverName()).isNotEmpty();
    }

    @Test
    @DisplayName("Should retrieve PostgreSQL database metadata")
    void shouldRetrievePostgreSQLDatabaseMetadata() throws Exception {
        DatabaseMetaData metaData = postgresConnection.getMetaData();
        assertThat(metaData).isNotNull();
        assertThat(metaData.getDatabaseProductName()).containsIgnoringCase("PostgreSQL");
        assertThat(metaData.getDatabaseMajorVersion()).isGreaterThan(0);
        assertThat(metaData.getDriverName()).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle connection with custom properties")
    void shouldHandleConnectionWithCustomProperties() throws Exception {
        Properties props = new Properties();
        props.setProperty("user", MYSQL_CONTAINER.getUsername());
        props.setProperty("password", MYSQL_CONTAINER.getPassword());
        props.setProperty("useSSL", "false");
        props.setProperty("serverTimezone", "UTC");
        Connection conn = DriverManager.getConnection(MYSQL_CONTAINER.getJdbcUrl(), props);
        try {
            assertThat(conn.isValid(5)).isTrue();
        } finally {
            conn.close();
        }
    }

    @Test
    @DisplayName("Should handle connection timeout correctly")
    void shouldHandleConnectionTimeout() throws Exception {
        DriverManager.setLoginTimeout(10);
        Connection conn =
                DriverManager.getConnection(
                        POSTGRES_CONTAINER.getJdbcUrl(),
                        POSTGRES_CONTAINER.getUsername(),
                        POSTGRES_CONTAINER.getPassword());
        try {
            assertThat(conn.isValid(5)).isTrue();
        } finally {
            conn.close();
        }
    }

    @Test
    @DisplayName("Should handle multiple connections simultaneously")
    void shouldHandleMultipleConnections() throws Exception {
        int connectionCount = 5;
        Connection[] connections = new Connection[connectionCount];
        try {
            for (int i = 0; i < connectionCount; i++) {
                connections[i] =
                        DriverManager.getConnection(
                                MYSQL_CONTAINER.getJdbcUrl(),
                                MYSQL_CONTAINER.getUsername(),
                                MYSQL_CONTAINER.getPassword());
                assertThat(connections[i].isValid(3)).isTrue();
            }
        } finally {
            for (int i = 0; i < connectionCount; i++) {
                if (connections[i] != null) {
                    connections[i].close();
                }
            }
        }
    }

    @Test
    @DisplayName("Should handle connection close and reconnect")
    void shouldHandleCloseAndReconnect() throws Exception {
        mysqlConnection.close();
        assertThat(mysqlConnection.isClosed()).isTrue();
        Connection newConnection =
                DriverManager.getConnection(
                        MYSQL_CONTAINER.getJdbcUrl(),
                        MYSQL_CONTAINER.getUsername(),
                        MYSQL_CONTAINER.getPassword());
        try {
            assertThat(newConnection.isValid(5)).isTrue();
        } finally {
            newConnection.close();
        }
    }

    @Test
    @DisplayName("Should execute simple query on MySQL")
    void shouldExecuteSimpleQueryOnMySQL() throws Exception {
        try (
                Statement statement = mysqlConnection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT 1 + 1 AS result")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("result")).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Should execute simple query on PostgreSQL")
    void shouldExecuteSimpleQueryOnPostgreSQL() throws Exception {
        try (
                Statement statement = postgresConnection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT 1 + 1 AS result")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("result")).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Should handle transaction isolation levels")
    void shouldHandleTransactionIsolationLevels() throws Exception {
        int originalLevel = mysqlConnection.getTransactionIsolation();
        mysqlConnection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        assertThat(mysqlConnection.getTransactionIsolation())
                .isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
        mysqlConnection.setTransactionIsolation(originalLevel);
        assertThat(mysqlConnection.getTransactionIsolation()).isEqualTo(originalLevel);
    }

    @Test
    @DisplayName("Should handle auto-commit mode toggle")
    void shouldHandleAutoCommitToggle() throws Exception {
        final boolean originalAutoCommit = mysqlConnection.getAutoCommit();
        mysqlConnection.setAutoCommit(false);
        assertThat(mysqlConnection.getAutoCommit()).isFalse();
        mysqlConnection.setAutoCommit(true);
        assertThat(mysqlConnection.getAutoCommit()).isTrue();
        mysqlConnection.setAutoCommit(originalAutoCommit);
    }

    @Test
    @DisplayName("Should get catalogs from MySQL")
    void shouldGetCatalogsFromMySQL() throws Exception {
        try (ResultSet catalogs = mysqlConnection.getMetaData().getCatalogs()) {
            boolean foundTestDb = false;
            while (catalogs.next()) {
                if ("testdb".equals(catalogs.getString("TABLE_CAT"))) {
                    foundTestDb = true;
                    break;
                }
            }
            assertThat(foundTestDb).isTrue();
        }
    }

    @Test
    @DisplayName("Should get schemas from PostgreSQL")
    void shouldGetSchemasFromPostgreSQL() throws Exception {
        try (ResultSet schemas = postgresConnection.getMetaData().getSchemas()) {
            boolean foundPublicSchema = false;
            while (schemas.next()) {
                if ("public".equals(schemas.getString("TABLE_SCHEM"))) {
                    foundPublicSchema = true;
                    break;
                }
            }
            assertThat(foundPublicSchema).isTrue();
        }
    }

    @Test
    @DisplayName("Should handle connection warnings")
    void shouldHandleConnectionWarnings() throws Exception {
        mysqlConnection.getWarnings();
        mysqlConnection.clearWarnings();
        assertThat((Object) mysqlConnection.getWarnings()).isNull();
    }

    private Connection createMySQLConnection() throws SQLException {
        return DriverManager.getConnection(
                MYSQL_CONTAINER.getJdbcUrl(), MYSQL_CONTAINER.getUsername(), MYSQL_CONTAINER.getPassword());
    }

    private Connection createPostgreSQLConnection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES_CONTAINER.getJdbcUrl(),
                POSTGRES_CONTAINER.getUsername(),
                POSTGRES_CONTAINER.getPassword());
    }
}
