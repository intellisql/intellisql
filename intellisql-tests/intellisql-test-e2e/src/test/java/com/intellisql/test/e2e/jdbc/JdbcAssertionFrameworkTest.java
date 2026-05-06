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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.util.Arrays;

import com.intellisql.test.e2e.framework.assertion.ColumnSnapshot;
import com.intellisql.test.e2e.framework.assertion.ComparisonResult;
import com.intellisql.test.e2e.framework.assertion.OrderMode;
import com.intellisql.test.e2e.framework.assertion.ResultSetComparator;
import com.intellisql.test.e2e.framework.assertion.ResultSetSnapshot;
import com.intellisql.test.e2e.framework.assertion.RowSnapshot;
import com.intellisql.test.e2e.framework.assertion.SqlOrderAnalyzer;
import com.intellisql.test.e2e.framework.assertion.ValueNormalizer;
import com.intellisql.test.e2e.framework.casefile.E2ECaseParser;
import com.intellisql.test.e2e.framework.casefile.E2ETestCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for JDBC E2E assertion framework pieces. */
public final class JdbcAssertionFrameworkTest {

    @TempDir
    private Path tempDirectory;

    @Test
    void assertParseSqlCaseDirectives() throws Exception {
        Path casePath = tempDirectory.resolve("customer-select.sql");
        Files.write(casePath, Arrays.asList(
                "-- @case id=dql-customer-select model=basic",
                "-- @source intellisql",
                "-- @assert mirror target=postgresql order=auto",
                "-- @statement mode=statement",
                "SELECT id, customer_name FROM customers ORDER BY id;"), StandardCharsets.UTF_8);
        E2ETestCase testCase = new E2ECaseParser("basic").parse(casePath);
        assertThat(testCase.getId(), is("dql-customer-select"));
        assertThat(testCase.getModel(), is("basic"));
        assertThat(testCase.getSource(), is("intellisql"));
        assertThat(testCase.getAssertion().getType(), is("mirror"));
        assertThat(testCase.getAssertion().getTarget(), is("postgresql"));
        assertThat(testCase.getStatement().getMode(), is("statement"));
        assertTrue(testCase.getSql().contains("ORDER BY id"));
    }

    @Test
    void assertCompareUnorderedSnapshots() {
        ResultSetSnapshot actual = createSnapshot("C002", "Bob", "C001", "Alice");
        ResultSetSnapshot expected = createSnapshot("C001", "Alice", "C002", "Bob");
        ComparisonResult unorderedResult = new ResultSetComparator().compare(actual, expected, OrderMode.ANY);
        ComparisonResult strictResult = new ResultSetComparator().compare(actual, expected, OrderMode.STRICT);
        assertTrue(unorderedResult.isMatched());
        assertFalse(strictResult.isMatched());
    }

    @Test
    void assertNormalizeValues() {
        ValueNormalizer normalizer = new ValueNormalizer("<NULL>");
        assertThat(normalizer.normalize(new BigDecimal("1.2300")), is("1.23"));
        assertThat(normalizer.normalize(null), is("<NULL>"));
        assertThat(normalizer.normalize(Boolean.TRUE), is("true"));
    }

    @Test
    void assertAnalyzeOrderMode() {
        SqlOrderAnalyzer analyzer = new SqlOrderAnalyzer();
        assertThat(analyzer.analyze("SELECT id FROM customers ORDER BY id", "auto"), is(OrderMode.STRICT));
        assertThat(analyzer.analyze("SELECT id FROM customers", "auto"), is(OrderMode.ANY));
        assertThat(analyzer.analyze("SELECT id FROM customers", "strict"), is(OrderMode.STRICT));
    }

    private ResultSetSnapshot createSnapshot(final String firstId, final String firstName, final String secondId, final String secondName) {
        ColumnSnapshot idColumn = ColumnSnapshot.builder().label("id").jdbcType(Types.VARCHAR).typeName("VARCHAR").build();
        ColumnSnapshot nameColumn = ColumnSnapshot.builder().label("customer_name").jdbcType(Types.VARCHAR).typeName("VARCHAR").build();
        RowSnapshot firstRow = new RowSnapshot(Arrays.asList(firstId, firstName));
        RowSnapshot secondRow = new RowSnapshot(Arrays.asList(secondId, secondName));
        return new ResultSetSnapshot(Arrays.asList(idColumn, nameColumn), Arrays.asList(firstRow, secondRow));
    }
}
