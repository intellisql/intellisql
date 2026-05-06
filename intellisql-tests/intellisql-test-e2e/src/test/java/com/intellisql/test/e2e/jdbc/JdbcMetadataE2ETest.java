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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.intellisql.test.e2e.framework.environment.E2EEnvironment;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** JDBC DatabaseMetaData E2E tests. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class JdbcMetadataE2ETest {

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
    void assertGetTables() throws Exception {
        try (
                Connection connection = environment.createIntelliSqlConnection();
                ResultSet resultSet = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            assertThat(readColumn(resultSet, "TABLE_NAME"), hasItems("customers", "e2e_table_names"));
        }
    }

    @Test
    void assertGetColumns() throws Exception {
        try (
                Connection connection = environment.createIntelliSqlConnection();
                ResultSet resultSet = connection.getMetaData().getColumns(null, null, "customers", "%")) {
            assertThat(readColumn(resultSet, "COLUMN_NAME"), hasItems("id", "customer_name", "status", "score"));
        }
    }

    @Test
    void assertGetSchemas() throws Exception {
        try (
                Connection connection = environment.createIntelliSqlConnection();
                ResultSet resultSet = connection.getMetaData().getSchemas()) {
            assertTrue(resultSet.next());
            assertThat(resultSet.getString("TABLE_SCHEM"), is("intellisql"));
        }
    }

    @Test
    void assertGetCatalogs() throws Exception {
        try (
                Connection connection = environment.createIntelliSqlConnection();
                ResultSet resultSet = connection.getMetaData().getCatalogs()) {
            assertTrue(resultSet.next());
            assertThat(resultSet.getString("TABLE_CAT"), is("intellisql"));
        }
    }

    @Test
    void assertMetadataSupportsOnlyForwardReadOnlyResultSets() throws Exception {
        try (Connection connection = environment.createIntelliSqlConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            assertThat(metaData.supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY), is(true));
            assertThat(metaData.supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE), is(false));
            assertThat(metaData.supportsResultSetConcurrency(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY), is(true));
            assertThat(metaData.supportsResultSetConcurrency(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE), is(false));
        }
    }

    private List<String> readColumn(final ResultSet resultSet, final String columnLabel) throws Exception {
        List<String> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(resultSet.getString(columnLabel));
        }
        return result;
    }
}
