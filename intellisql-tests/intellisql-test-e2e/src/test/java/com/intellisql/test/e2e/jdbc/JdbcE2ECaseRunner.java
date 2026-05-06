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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.intellisql.test.e2e.framework.assertion.ComparisonResult;
import com.intellisql.test.e2e.framework.assertion.OrderMode;
import com.intellisql.test.e2e.framework.assertion.ResultSetComparator;
import com.intellisql.test.e2e.framework.assertion.ResultSetSnapshot;
import com.intellisql.test.e2e.framework.assertion.ResultSetSnapshotReader;
import com.intellisql.test.e2e.framework.assertion.SqlOrderAnalyzer;
import com.intellisql.test.e2e.framework.assertion.ValueNormalizer;
import com.intellisql.test.e2e.framework.casefile.E2ECaseParser;
import com.intellisql.test.e2e.framework.casefile.E2ECaseScanner;
import com.intellisql.test.e2e.framework.casefile.E2ETestCase;
import com.intellisql.test.e2e.framework.environment.E2EEnvironment;
import com.intellisql.test.e2e.framework.execute.BaselineExecutor;
import com.intellisql.test.e2e.framework.execute.ExecutionResult;
import com.intellisql.test.e2e.framework.execute.JdbcCaseExecutor;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JdbcE2ECaseRunner {

    private final E2EEnvironment environment;

    private final JdbcCaseExecutor jdbcCaseExecutor;

    private final BaselineExecutor baselineExecutor;

    private final ResultSetComparator comparator = new ResultSetComparator();

    private final SqlOrderAnalyzer orderAnalyzer = new SqlOrderAnalyzer();

    JdbcE2ECaseRunner(final E2EEnvironment environment) {
        this.environment = environment;
        ValueNormalizer valueNormalizer = new ValueNormalizer(environment.getConfig().getAssertion().getNullToken());
        ResultSetSnapshotReader snapshotReader = new ResultSetSnapshotReader(valueNormalizer);
        jdbcCaseExecutor = new JdbcCaseExecutor(snapshotReader);
        baselineExecutor = new BaselineExecutor(snapshotReader);
    }

    List<E2ETestCase> scan(final String caseRoot) {
        E2ECaseParser parser = new E2ECaseParser(environment.getConfig().getExecution().getDefaultModel());
        return new E2ECaseScanner(parser).scan(caseRoot);
    }

    E2ETestCase find(final List<E2ETestCase> testCases, final String caseId) {
        for (E2ETestCase each : testCases) {
            if (caseId.equals(each.getId())) {
                return each;
            }
        }
        throw new IllegalArgumentException("Case not found: " + caseId);
    }

    void assertCase(final E2ETestCase testCase) throws SQLException {
        try (Connection intelliSqlConnection = environment.createIntelliSqlConnection()) {
            ExecutionResult actual = jdbcCaseExecutor.execute(intelliSqlConnection, testCase);
            assertNull(actual.getException(), getExecutionFailureMessage(actual));
            if ("state".equalsIgnoreCase(testCase.getAssertion().getType())) {
                assertTrue(actual.getUpdateCount() >= 0, "Update count should be returned for case: " + testCase.getId());
            }
            ResultSetSnapshot expected = loadExpected(testCase);
            OrderMode orderMode = orderAnalyzer.analyze(testCase.getSql(), testCase.getAssertion().getOrder());
            ComparisonResult comparisonResult = comparator.compare(actual.getSnapshot(), expected, orderMode);
            assertTrue(comparisonResult.isMatched(), getComparisonFailureMessage(testCase, comparisonResult));
        }
    }

    private String getExecutionFailureMessage(final ExecutionResult result) {
        return result.getException() == null ? "Execution succeeded" : result.getException().getMessage();
    }

    private ResultSetSnapshot loadExpected(final E2ETestCase testCase) throws SQLException {
        if ("file".equalsIgnoreCase(testCase.getAssertion().getType())) {
            return baselineExecutor.loadExpectedFile(testCase);
        }
        if ("mysql".equalsIgnoreCase(testCase.getAssertion().getTarget())) {
            try (Connection mySQLConnection = environment.createMySQLConnection()) {
                return baselineExecutor.executeMirror(mySQLConnection, testCase);
            }
        }
        try (Connection postgreSQLConnection = environment.createPostgreSQLConnection()) {
            return baselineExecutor.executeMirror(postgreSQLConnection, testCase);
        }
    }

    private String getComparisonFailureMessage(final E2ETestCase testCase, final ComparisonResult result) {
        return "Case " + testCase.getId() + " failed: " + result.getMessage() + " " + result.getDetails();
    }
}
