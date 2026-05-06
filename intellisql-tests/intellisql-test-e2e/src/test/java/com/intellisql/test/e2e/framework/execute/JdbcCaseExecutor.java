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

package com.intellisql.test.e2e.framework.execute;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.intellisql.test.e2e.framework.assertion.ResultSetSnapshotReader;
import com.intellisql.test.e2e.framework.casefile.E2ETestCase;

/** Executes JDBC E2E SQL cases against IntelliSQL. */
public final class JdbcCaseExecutor {

    private final ResultSetSnapshotReader snapshotReader;

    /**
     * Creates a JDBC case executor.
     *
     * @param snapshotReader the result set snapshot reader
     */
    public JdbcCaseExecutor(final ResultSetSnapshotReader snapshotReader) {
        this.snapshotReader = snapshotReader;
    }

    /**
     * Executes a JDBC E2E case.
     *
     * @param connection the IntelliSQL JDBC connection
     * @param testCase the parsed test case
     * @return execution result
     */
    public ExecutionResult execute(final Connection connection, final E2ETestCase testCase) {
        validateStatementMode(testCase);
        if ("state".equalsIgnoreCase(testCase.getAssertion().getType())) {
            return executeState(connection, testCase);
        }
        long startTime = System.currentTimeMillis();
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(testCase.getSql())) {
            return ExecutionResult.builder()
                    .snapshot(snapshotReader.read(resultSet))
                    .durationMillis(System.currentTimeMillis() - startTime)
                    .build();
        } catch (final SQLException ex) {
            return ExecutionResult.builder()
                    .exception(ex)
                    .durationMillis(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private ExecutionResult executeState(final Connection connection, final E2ETestCase testCase) {
        long startTime = System.currentTimeMillis();
        String expectedSql = testCase.getAssertion().getExpectedSql();
        if (expectedSql == null || expectedSql.isEmpty()) {
            throw new IllegalArgumentException("State assertion requires @expected-sql in case: " + testCase.getId());
        }
        try (Statement statement = connection.createStatement()) {
            int updateCount = statement.executeUpdate(testCase.getSql());
            try (ResultSet resultSet = statement.executeQuery(expectedSql)) {
                return ExecutionResult.builder()
                        .snapshot(snapshotReader.read(resultSet))
                        .updateCount(updateCount)
                        .durationMillis(System.currentTimeMillis() - startTime)
                        .build();
            }
        } catch (final SQLException ex) {
            return ExecutionResult.builder()
                    .exception(ex)
                    .durationMillis(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private void validateStatementMode(final E2ETestCase testCase) {
        if (!"statement".equalsIgnoreCase(testCase.getStatement().getMode())) {
            throw new IllegalArgumentException("Unsupported statement mode in P0: " + testCase.getStatement().getMode());
        }
    }
}
