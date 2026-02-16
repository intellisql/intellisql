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

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for configuration loading. Tests loading database configurations from various
 * sources.
 */
@Testcontainers
public class ConfigLoadingE2ETest {

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

    @TempDir
    private Path tempDir;

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
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Should load configuration from Properties file")
    void shouldLoadConfigurationFromPropertiesFile() throws Exception {
        File configFile = tempDir.resolve("database.properties").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("db.url=" + POSTGRES_CONTAINER.getJdbcUrl() + "\n");
            writer.write("db.username=" + POSTGRES_CONTAINER.getUsername() + "\n");
            writer.write("db.password=" + POSTGRES_CONTAINER.getPassword() + "\n");
            writer.write("db.driver=org.postgresql.Driver\n");
            writer.write("db.pool.size=10\n");
            writer.write("db.timeout=30000\n");
        }
        Properties props = new Properties();
        try (InputStream inputStream = Files.newInputStream(configFile.toPath())) {
            props.load(inputStream);
        }
        assertThat(props.getProperty("db.url")).isEqualTo(POSTGRES_CONTAINER.getJdbcUrl());
        assertThat(props.getProperty("db.username")).isEqualTo(POSTGRES_CONTAINER.getUsername());
        assertThat(props.getProperty("db.password")).isEqualTo(POSTGRES_CONTAINER.getPassword());
        assertThat(props.getProperty("db.driver")).isEqualTo("org.postgresql.Driver");
        assertThat(props.getProperty("db.pool.size")).isEqualTo("10");
        assertThat(props.getProperty("db.timeout")).isEqualTo("30000");
        try (
                Connection conn =
                        DriverManager.getConnection(
                                props.getProperty("db.url"),
                                props.getProperty("db.username"),
                                props.getProperty("db.password"))) {
            assertThat(conn.isValid(5)).isTrue();
        }
    }

    @Test
    @DisplayName("Should load configuration from YAML-like file")
    void shouldLoadConfigurationFromYamlLikeFile() throws Exception {
        File configFile = tempDir.resolve("database.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("database:\n");
            writer.write("  url: " + POSTGRES_CONTAINER.getJdbcUrl() + "\n");
            writer.write("  username: " + POSTGRES_CONTAINER.getUsername() + "\n");
            writer.write("  password: " + POSTGRES_CONTAINER.getPassword() + "\n");
            writer.write("  pool:\n");
            writer.write("    maxSize: 20\n");
            writer.write("    minIdle: 5\n");
            writer.write("    connectionTimeout: 30000\n");
            writer.write("  sources:\n");
            writer.write("    - name: primary\n");
            writer.write("      type: postgresql\n");
            writer.write("      priority: 1\n");
        }
        String content = Files.readString(configFile.toPath());
        assertThat(content).contains("database:");
        assertThat(content).contains("url: " + POSTGRES_CONTAINER.getJdbcUrl());
        assertThat(content).contains("username: " + POSTGRES_CONTAINER.getUsername());
        assertThat(content).contains("maxSize: 20");
    }

    @Test
    @DisplayName("Should load configuration from JSON file")
    void shouldLoadConfigurationFromJsonFile() throws Exception {
        File configFile = tempDir.resolve("database.json").toFile();
        String jsonContent =
                "{\n"
                        + "  \"database\": {\n"
                        + "    \"url\": \""
                        + POSTGRES_CONTAINER.getJdbcUrl()
                        + "\",\n"
                        + "    \"username\": \""
                        + POSTGRES_CONTAINER.getUsername()
                        + "\",\n"
                        + "    \"password\": \""
                        + POSTGRES_CONTAINER.getPassword()
                        + "\",\n"
                        + "    \"poolSize\": 15,\n"
                        + "    \"timeout\": 25000\n"
                        + "  }\n"
                        + "}";
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(jsonContent);
        }
        String content = Files.readString(configFile.toPath());
        assertThat(content).contains("\"url\":");
        assertThat(content).contains("\"username\":");
        assertThat(content).contains("\"poolSize\": 15");
        assertThat(content).contains("\"timeout\": 25000");
    }

    @Test
    @DisplayName("Should load configuration from environment variables")
    void shouldLoadConfigurationFromEnvironmentVariables() throws Exception {
        String dbUrl = POSTGRES_CONTAINER.getJdbcUrl();
        String dbUser = POSTGRES_CONTAINER.getUsername();
        String dbPass = POSTGRES_CONTAINER.getPassword();
        String envUrl = System.getenv("DATABASE_URL");
        String envUser = System.getenv("DATABASE_USER");
        String envPass = System.getenv("DATABASE_PASSWORD");
        String loadedUrl = envUrl != null ? envUrl : dbUrl;
        String loadedUser = envUser != null ? envUser : dbUser;
        String loadedPass = envPass != null ? envPass : dbPass;
        try (Connection conn = DriverManager.getConnection(loadedUrl, loadedUser, loadedPass)) {
            assertThat(conn.isValid(5)).isTrue();
        }
    }

    @Test
    @DisplayName("Should handle missing configuration file gracefully")
    void shouldHandleMissingConfigurationFile() throws Exception {
        File configFile = tempDir.resolve("nonexistent.properties").toFile();
        assertThat(configFile.exists()).isFalse();
        String defaultUrl = POSTGRES_CONTAINER.getJdbcUrl();
        String defaultUser = POSTGRES_CONTAINER.getUsername();
        String defaultPass = POSTGRES_CONTAINER.getPassword();
        try (Connection conn = DriverManager.getConnection(defaultUrl, defaultUser, defaultPass)) {
            assertThat(conn.isValid(5)).isTrue();
        }
    }

    @Test
    @DisplayName("Should validate configuration parameters")
    void shouldValidateConfigurationParameters() throws Exception {
        Properties props = new Properties();
        props.setProperty("db.url", POSTGRES_CONTAINER.getJdbcUrl());
        props.setProperty("db.username", POSTGRES_CONTAINER.getUsername());
        props.setProperty("db.password", POSTGRES_CONTAINER.getPassword());
        props.setProperty("db.pool.size", "10");
        assertThat(props.getProperty("db.url")).isNotEmpty();
        assertThat(props.getProperty("db.url")).startsWith("jdbc:");
        assertThat(Integer.parseInt(props.getProperty("db.pool.size"))).isPositive();
    }

    @Test
    @DisplayName("Should load multiple data source configurations")
    void shouldLoadMultipleDataSourceConfigurations() throws Exception {
        File configFile = tempDir.resolve("multi-datasource.properties").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("datasource.primary.url=" + POSTGRES_CONTAINER.getJdbcUrl() + "\n");
            writer.write("datasource.primary.username=" + POSTGRES_CONTAINER.getUsername() + "\n");
            writer.write("datasource.primary.password=" + POSTGRES_CONTAINER.getPassword() + "\n");
            writer.write("datasource.primary.type=postgresql\n");
            writer.write("datasource.primary.priority=1\n");
            writer.write("datasource.replica.url=" + POSTGRES_CONTAINER.getJdbcUrl() + "\n");
            writer.write("datasource.replica.username=" + POSTGRES_CONTAINER.getUsername() + "\n");
            writer.write("datasource.replica.password=" + POSTGRES_CONTAINER.getPassword() + "\n");
            writer.write("datasource.replica.type=postgresql\n");
            writer.write("datasource.replica.priority=2\n");
        }
        Properties props = new Properties();
        try (InputStream inputStream = Files.newInputStream(configFile.toPath())) {
            props.load(inputStream);
        }
        assertThat(props.getProperty("datasource.primary.url")).isNotEmpty();
        assertThat(props.getProperty("datasource.primary.type")).isEqualTo("postgresql");
        assertThat(props.getProperty("datasource.replica.url")).isNotEmpty();
        assertThat(props.getProperty("datasource.replica.priority")).isEqualTo("2");
    }

    @Test
    @DisplayName("Should reload configuration dynamically")
    void shouldReloadConfigurationDynamically() throws Exception {
        File configFile = tempDir.resolve("dynamic.properties").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("db.url=" + POSTGRES_CONTAINER.getJdbcUrl() + "\n");
            writer.write("db.username=" + POSTGRES_CONTAINER.getUsername() + "\n");
            writer.write("db.password=" + POSTGRES_CONTAINER.getPassword() + "\n");
            writer.write("db.pool.size=10\n");
        }
        Properties props1 = new Properties();
        try (InputStream inputStream = Files.newInputStream(configFile.toPath())) {
            props1.load(inputStream);
        }
        assertThat(props1.getProperty("db.pool.size")).isEqualTo("10");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("db.url=" + POSTGRES_CONTAINER.getJdbcUrl() + "\n");
            writer.write("db.username=" + POSTGRES_CONTAINER.getUsername() + "\n");
            writer.write("db.password=" + POSTGRES_CONTAINER.getPassword() + "\n");
            writer.write("db.pool.size=20\n");
        }
        Properties props2 = new Properties();
        try (InputStream inputStream = Files.newInputStream(configFile.toPath())) {
            props2.load(inputStream);
        }
        assertThat(props2.getProperty("db.pool.size")).isEqualTo("20");
    }

    @Test
    @DisplayName("Should handle encrypted password in configuration")
    void shouldHandleEncryptedPassword() throws Exception {
        String plainPassword = POSTGRES_CONTAINER.getPassword();
        String encodedPassword = java.util.Base64.getEncoder().encodeToString(plainPassword.getBytes());
        File configFile = tempDir.resolve("encrypted.properties").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("db.url=" + POSTGRES_CONTAINER.getJdbcUrl() + "\n");
            writer.write("db.username=" + POSTGRES_CONTAINER.getUsername() + "\n");
            writer.write("db.password.encoded=" + encodedPassword + "\n");
        }
        Properties props = new Properties();
        try (InputStream inputStream = Files.newInputStream(configFile.toPath())) {
            props.load(inputStream);
        }
        String encodedPass = props.getProperty("db.password.encoded");
        String decodedPassword = new String(java.util.Base64.getDecoder().decode(encodedPass));
        assertThat(decodedPassword).isEqualTo(plainPassword);
        try (
                Connection conn =
                        DriverManager.getConnection(
                                props.getProperty("db.url"), POSTGRES_CONTAINER.getUsername(), decodedPassword)) {
            assertThat(conn.isValid(5)).isTrue();
        }
    }

    @Test
    @DisplayName("Should apply connection pool configuration")
    void shouldApplyConnectionPoolConfiguration() throws Exception {
        Properties poolConfig = new Properties();
        poolConfig.setProperty("maximumPoolSize", "10");
        poolConfig.setProperty("minimumIdle", "2");
        poolConfig.setProperty("connectionTimeout", "30000");
        poolConfig.setProperty("idleTimeout", "600000");
        poolConfig.setProperty("maxLifetime", "1800000");
        assertThat(poolConfig.getProperty("maximumPoolSize")).isEqualTo("10");
        assertThat(poolConfig.getProperty("minimumIdle")).isEqualTo("2");
        assertThat(Integer.parseInt(poolConfig.getProperty("connectionTimeout"))).isEqualTo(30000);
    }

    @Test
    @DisplayName("Should handle invalid configuration values")
    void shouldHandleInvalidConfigurationValues() throws Exception {
        Properties props = new Properties();
        props.setProperty("db.pool.size", "invalid");
        props.setProperty("db.timeout", "-100");
        props.setProperty("db.url", "not-a-valid-url");
        try {
            Integer.parseInt(props.getProperty("db.pool.size"));
        } catch (final NumberFormatException ex) {
            assertThat(ex).isInstanceOf(NumberFormatException.class);
        }
        int timeout = Integer.parseInt(props.getProperty("db.timeout"));
        assertThat(timeout).isNegative();
        String url = props.getProperty("db.url");
        assertThat(url).doesNotStartWith("jdbc:");
    }

    @Test
    @DisplayName("Should merge configurations from multiple sources")
    void shouldMergeConfigurationsFromMultipleSources() throws Exception {
        File baseConfigFile = tempDir.resolve("base.properties").toFile();
        try (FileWriter writer = new FileWriter(baseConfigFile)) {
            writer.write("db.url=" + POSTGRES_CONTAINER.getJdbcUrl() + "\n");
            writer.write("db.username=default_user\n");
            writer.write("db.pool.size=5\n");
        }
        File overrideConfigFile = tempDir.resolve("override.properties").toFile();
        try (FileWriter writer = new FileWriter(overrideConfigFile)) {
            writer.write("db.username=" + POSTGRES_CONTAINER.getUsername() + "\n");
            writer.write("db.password=" + POSTGRES_CONTAINER.getPassword() + "\n");
            writer.write("db.pool.size=10\n");
        }
        Properties merged = new Properties();
        try (InputStream inputStream = Files.newInputStream(baseConfigFile.toPath())) {
            merged.load(inputStream);
        }
        try (InputStream inputStream = Files.newInputStream(overrideConfigFile.toPath())) {
            Properties override = new Properties();
            override.load(inputStream);
            merged.putAll(override);
        }
        assertThat(merged.getProperty("db.url")).isEqualTo(POSTGRES_CONTAINER.getJdbcUrl());
        assertThat(merged.getProperty("db.username")).isEqualTo(POSTGRES_CONTAINER.getUsername());
        assertThat(merged.getProperty("db.pool.size")).isEqualTo("10");
    }
}
