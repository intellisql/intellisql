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

package com.intellisql.test.e2e.jdbc;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.intellisql.test.e2e.framework.environment.E2EEnvironment;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** JDBC unsupported API contract E2E tests. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class JdbcUnsupportedApiE2ETest {

    private E2EEnvironment environment;

    @BeforeAll
    void setUp(@TempDir final Path tempDirectory) {
        environment = new E2EEnvironment();
        environment.start("basic", tempDirectory);
    }

    @AfterAll
    void tearDown() {
        if (environment != null) {
            environment.close();
        }
    }

    @Test
    void assertCallableStatementIsUnsupported() throws Exception {
        try (Connection connection = environment.createIntelliSqlConnection()) {
            assertThrows(Exception.class, () -> connection.prepareCall("CALL demo()"));
        }
    }

    @Test
    void assertSavepointIsUnsupported() throws Exception {
        try (Connection connection = environment.createIntelliSqlConnection()) {
            assertThrows(Exception.class, connection::setSavepoint);
            assertThrows(Exception.class, () -> connection.setSavepoint("sp"));
        }
    }

    @Test
    void assertLobApisAreUnsupported() throws Exception {
        try (Connection connection = environment.createIntelliSqlConnection()) {
            assertThrows(Exception.class, connection::createBlob);
            assertThrows(Exception.class, connection::createClob);
            assertThrows(Exception.class, connection::createNClob);
            assertThrows(Exception.class, connection::createSQLXML);
        }
    }

    @Test
    void assertScrollableResultSetIsUnsupported() throws Exception {
        try (Connection connection = environment.createIntelliSqlConnection()) {
            assertThrows(Exception.class, () -> connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY));
        }
    }

    @Test
    void assertUpdatableResultSetIsUnsupported() throws Exception {
        try (Connection connection = environment.createIntelliSqlConnection()) {
            assertThrows(Exception.class, () -> connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE));
        }
    }

    @Test
    void assertResultSetMutationIsUnsupported() throws Exception {
        try (
                Connection connection = environment.createIntelliSqlConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT id FROM customers ORDER BY id")) {
            assertThrows(Exception.class, resultSet::previous);
            assertThrows(Exception.class, resultSet::updateRow);
        }
    }
}
