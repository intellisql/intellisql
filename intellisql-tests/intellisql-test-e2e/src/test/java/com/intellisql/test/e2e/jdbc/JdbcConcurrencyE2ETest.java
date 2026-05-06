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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.intellisql.test.e2e.framework.environment.E2EEnvironment;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** JDBC concurrent access E2E tests. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class JdbcConcurrencyE2ETest {

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
    void assertConcurrentConnections() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        try {
            List<Future<Boolean>> futures = submitTasks(executorService, 4, this::openAndCheckConnection);
            for (Future<Boolean> each : futures) {
                assertThat(each.get(), is(true));
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    private Boolean openAndCheckConnection() throws Exception {
        try (Connection connection = environment.createIntelliSqlConnection()) {
            return !connection.isClosed() && connection.isValid(1);
        }
    }

    @Test
    void assertConcurrentReadQueries() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        try {
            List<Future<Integer>> futures = submitTasks(executorService, 4, this::queryCustomerCount);
            for (Future<Integer> each : futures) {
                assertThat(each.get(), is(3));
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    private Integer queryCustomerCount() throws Exception {
        try (
                Connection connection = environment.createIntelliSqlConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT id FROM customers ORDER BY id")) {
            int result = 0;
            while (resultSet.next()) {
                result++;
            }
            return result;
        }
    }

    @Test
    void assertConcurrentReadWriteIsolation() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        try {
            Future<Integer> firstUpdate = executorService.submit(() -> updateCustomer("C001", "RW1"));
            Future<Integer> secondUpdate = executorService.submit(() -> updateCustomer("C002", "RW2"));
            Future<Integer> read = executorService.submit(this::queryCustomerCount);
            assertThat(firstUpdate.get(), is(1));
            assertThat(secondUpdate.get(), is(1));
            assertThat(read.get(), is(3));
            assertThat(queryStatus("C001"), is("RW1"));
            assertThat(queryStatus("C002"), is("RW2"));
        } finally {
            executorService.shutdownNow();
        }
    }

    private int updateCustomer(final String id, final String status) throws Exception {
        try (
                Connection connection = environment.createIntelliSqlConnection();
                Statement statement = connection.createStatement()) {
            return statement.executeUpdate("UPDATE customers SET status = '" + status + "' WHERE id = '" + id + "'");
        }
    }

    private String queryStatus(final String id) throws Exception {
        try (
                Connection connection = environment.createIntelliSqlConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT status FROM customers WHERE id = '" + id + "'")) {
            assertTrue(resultSet.next());
            return resultSet.getString("status");
        }
    }

    private <T> List<Future<T>> submitTasks(final ExecutorService executorService, final int count, final Callable<T> task) {
        List<Future<T>> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(executorService.submit(task));
        }
        return result;
    }
}
