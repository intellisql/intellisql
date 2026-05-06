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
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.intellisql.test.e2e.framework.assertion.ColumnSnapshot;
import com.intellisql.test.e2e.framework.assertion.ResultSetSnapshot;
import com.intellisql.test.e2e.framework.assertion.ResultSetSnapshotReader;
import com.intellisql.test.e2e.framework.assertion.RowSnapshot;
import com.intellisql.test.e2e.framework.casefile.E2ETestCase;
import com.intellisql.test.e2e.framework.io.ResourceReader;

/** Executes or loads baseline expectations for JDBC E2E assertions. */
public final class BaselineExecutor {

    private final ResultSetSnapshotReader snapshotReader;

    private final ResourceReader resourceReader = new ResourceReader();

    /**
     * Creates a baseline executor.
     *
     * @param snapshotReader the result set snapshot reader
     */
    public BaselineExecutor(final ResultSetSnapshotReader snapshotReader) {
        this.snapshotReader = snapshotReader;
    }

    /**
     * Executes a mirror SQL query against the baseline database.
     *
     * @param connection the baseline JDBC connection
     * @param testCase the parsed test case
     * @return baseline snapshot
     * @throws IllegalStateException if baseline SQL execution fails
     */
    public ResultSetSnapshot executeMirror(final Connection connection, final E2ETestCase testCase) {
        String sql = testCase.getAssertion().getExpectedSql() == null ? testCase.getSql() : testCase.getAssertion().getExpectedSql();
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            return snapshotReader.read(resultSet);
        } catch (final SQLException ex) {
            throw new IllegalStateException("Failed to execute baseline SQL for case: " + testCase.getId(), ex);
        }
    }

    /**
     * Loads a CSV expected result snapshot.
     *
     * @param testCase the parsed test case
     * @return expected snapshot
     * @throws IllegalArgumentException if the expected file is missing
     */
    public ResultSetSnapshot loadExpectedFile(final E2ETestCase testCase) {
        String expected = testCase.getAssertion().getExpected();
        if (expected == null || expected.isEmpty()) {
            throw new IllegalArgumentException("Expected file is required for case: " + testCase.getId());
        }
        return parseCsv(readExpectedResource(expected));
    }

    private String readExpectedResource(final String expected) {
        String resourcePath = expected.startsWith("e2e/") ? expected : "e2e/" + expected;
        return resourceReader.read(resourcePath);
    }

    private ResultSetSnapshot parseCsv(final String csv) {
        String[] lines = csv.trim().split("\\R");
        if (lines.length == 0 || lines[0].trim().isEmpty()) {
            throw new IllegalArgumentException("Expected CSV must contain a header");
        }
        String[] labels = lines[0].split(",");
        List<ColumnSnapshot> columns = new ArrayList<>(labels.length);
        for (String label : labels) {
            columns.add(ColumnSnapshot.builder().label(label.trim()).jdbcType(Types.VARCHAR).typeName("VARCHAR").build());
        }
        List<RowSnapshot> rows = new ArrayList<>(Math.max(lines.length - 1, 0));
        for (int i = 1; i < lines.length; i++) {
            rows.add(parseCsvRow(lines[i]));
        }
        return new ResultSetSnapshot(columns, rows);
    }

    private RowSnapshot parseCsvRow(final String line) {
        String[] values = line.split(",", -1);
        List<String> result = new ArrayList<>(values.length);
        for (String each : values) {
            result.add(each.trim());
        }
        return new RowSnapshot(result);
    }
}
