/*
 * Licensed to the IntelliSql Project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The IntelliSql Project licenses this file to You under the Apache License, Version 2.0
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

package com.intellisql.test.e2e.framework.environment;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.intellisql.test.e2e.framework.config.E2ERunnerConfig;
import com.intellisql.test.e2e.framework.config.E2ERunnerConfigLoader;
import com.intellisql.test.e2e.framework.io.ResourceReader;

import lombok.Getter;

/** Full JDBC E2E environment containing database, server, and rendered model configuration. */
public final class E2EEnvironment implements AutoCloseable {

    private static final String POSTGRESQL = "postgresql";

    private static final String MYSQL = "mysql";

    private final ResourceReader resourceReader = new ResourceReader();

    @Getter
    private E2ERunnerConfig config;

    private PostgreSQLContainerFixture postgreSQL;

    private MySQLContainerFixture mySQL;

    private IntelliSqlServerFixture server;

    /**
     * Starts the E2E environment.
     *
     * @param model the model name
     * @param tempDirectory the temporary directory
     */
    public void start(final String model, final Path tempDirectory) {
        config = new E2ERunnerConfigLoader().loadDefault();
        postgreSQL = new PostgreSQLContainerFixture(config.getContainers().get(POSTGRESQL));
        postgreSQL.start();
        initializePostgreSQL(model);
        Map<String, String> modelVariables = new HashMap<>(postgreSQL.toModelVariables());
        if (requiresMySQL(model)) {
            mySQL = new MySQLContainerFixture(config.getContainers().get(MYSQL));
            mySQL.start();
            initializeMySQL(model);
            modelVariables.putAll(mySQL.toModelVariables());
        }
        Path modelPath = new ModelConfigRenderer().render(model, modelVariables, tempDirectory.resolve(model));
        server = new IntelliSqlServerFixture();
        server.start(modelPath, config.getExecution().getServerPort(), config.getExecution().getJdbcDatabase());
    }

    private boolean requiresMySQL(final String model) {
        return resourceReader.exists("e2e/init/" + model + "/mysql.sql");
    }

    /**
     * Creates a JDBC connection to IntelliSQL.
     *
     * @return IntelliSQL JDBC connection
     * @throws SQLException if a connection cannot be created
     */
    public Connection createIntelliSqlConnection() throws SQLException {
        return DriverManager.getConnection(server.getJdbcUrl() + "?fetchSize=" + config.getExecution().getDefaultFetchSize());
    }

    /**
     * Creates a JDBC connection to the PostgreSQL baseline database.
     *
     * @return PostgreSQL JDBC connection
     * @throws SQLException if a connection cannot be created
     */
    public Connection createPostgreSQLConnection() throws SQLException {
        return postgreSQL.createConnection();
    }

    /**
     * Creates a JDBC connection to the MySQL baseline database.
     *
     * @return MySQL JDBC connection
     * @throws SQLException if a connection cannot be created
     */
    public Connection createMySQLConnection() throws SQLException {
        return mySQL.createConnection();
    }

    private void initializePostgreSQL(final String model) {
        try (Connection connection = postgreSQL.createConnection()) {
            new SqlScriptExecutor().execute(connection, getScriptResource(model, POSTGRESQL));
        } catch (final SQLException ex) {
            throw new IllegalStateException("Failed to initialize PostgreSQL database", ex);
        }
    }

    private void initializeMySQL(final String model) {
        try (Connection connection = mySQL.createConnection()) {
            new SqlScriptExecutor().execute(connection, getScriptResource(model, MYSQL));
        } catch (final SQLException ex) {
            throw new IllegalStateException("Failed to initialize MySQL database", ex);
        }
    }

    private String getScriptResource(final String model, final String databaseType) {
        String resourcePath = "e2e/init/" + model + "/" + databaseType + ".sql";
        return resourceReader.exists(resourcePath) ? resourcePath : "e2e/init/basic/" + databaseType + ".sql";
    }

    @Override
    public void close() {
        closeServer();
        closeMySQL();
        closePostgreSQL();
    }

    private void closeServer() {
        if (server != null) {
            server.close();
        }
    }

    private void closePostgreSQL() {
        if (postgreSQL != null) {
            postgreSQL.close();
        }
    }

    private void closeMySQL() {
        if (mySQL != null) {
            mySQL.close();
        }
    }
}
