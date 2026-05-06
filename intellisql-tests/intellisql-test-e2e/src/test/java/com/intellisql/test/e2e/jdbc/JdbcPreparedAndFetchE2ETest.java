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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.intellisql.test.e2e.framework.environment.E2EEnvironment;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/** JDBC PreparedStatement and fetch paging E2E tests. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class JdbcPreparedAndFetchE2ETest {

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
    void assertPreparedStatementParameterBinding() throws Exception {
        try (
                Connection connection = environment.createIntelliSqlConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT id, customer_name FROM customers WHERE status = ? ORDER BY id")) {
            statement.setString(1, "ACTIVE");
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getString("id"), is("C001"));
                assertThat(resultSet.getString("customer_name"), is("Alice"));
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getString("id"), is("C003"));
                assertThat(resultSet.getString("customer_name"), is("Carol"));
                assertThat(resultSet.next(), is(false));
            }
        }
    }

    @Test
    void assertFetchesMultipleFrames() throws Exception {
        try (
                Connection connection = environment.createIntelliSqlConnection();
                Statement statement = connection.createStatement()) {
            statement.setFetchSize(1);
            try (ResultSet resultSet = statement.executeQuery("SELECT id FROM customers ORDER BY id")) {
                assertThat(readIds(resultSet), is(Arrays.asList("C001", "C002", "C003")));
            }
        }
    }

    private List<String> readIds(final ResultSet resultSet) throws Exception {
        List<String> result = new ArrayList<>(3);
        while (resultSet.next()) {
            result.add(resultSet.getString("id"));
        }
        return result;
    }
}
