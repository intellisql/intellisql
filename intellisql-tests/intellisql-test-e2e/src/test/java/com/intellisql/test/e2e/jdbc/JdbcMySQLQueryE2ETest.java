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
import java.util.List;

import com.intellisql.test.e2e.framework.casefile.E2ETestCase;
import com.intellisql.test.e2e.framework.environment.E2EEnvironment;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/** JDBC MySQL query E2E tests using mirror assertions against MySQL baseline. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class JdbcMySQLQueryE2ETest {

    private E2EEnvironment environment;

    private JdbcE2ECaseRunner caseRunner;

    private List<E2ETestCase> testCases;

    @BeforeAll
    void setUp(@TempDir final Path tempDirectory) {
        environment = new E2EEnvironment();
        environment.start("mysql", tempDirectory);
        caseRunner = new JdbcE2ECaseRunner(environment);
        testCases = caseRunner.scan("e2e/cases/mysql");
    }

    @AfterAll
    void tearDown() {
        if (environment != null) {
            environment.close();
        }
    }

    @Test
    void assertExecuteMySQLSelect() throws Exception {
        assertCase("mysql-orders-select");
    }

    @Test
    void assertExecuteMySQLFilter() throws Exception {
        assertCase("mysql-orders-filter");
    }

    @Test
    void assertExecuteMySQLOrderBy() throws Exception {
        assertCase("mysql-orders-order-by");
    }

    @Test
    void assertExecuteMySQLLimit() throws Exception {
        assertCase("mysql-orders-limit");
    }

    @Test
    void assertExecuteMySQLAggregate() throws Exception {
        assertCase("mysql-orders-aggregate");
    }

    @Test
    void assertExecuteMySQLStar() throws Exception {
        assertCase("mysql-orders-star");
    }

    @Test
    void assertExecuteMySQLDistinct() throws Exception {
        assertCase("mysql-orders-distinct");
    }

    @Test
    void assertExecuteMySQLBetween() throws Exception {
        assertCase("mysql-orders-between");
    }

    @Test
    void assertExecuteMySQLIn() throws Exception {
        assertCase("mysql-orders-in");
    }

    @Test
    void assertExecuteMySQLLike() throws Exception {
        assertCase("mysql-orders-like");
    }

    @Test
    void assertExecuteMySQLNull() throws Exception {
        assertCase("mysql-orders-null");
    }

    @Test
    void assertExecuteMySQLNotNull() throws Exception {
        assertCase("mysql-orders-not-null");
    }

    @Test
    void assertExecuteMySQLBoolean() throws Exception {
        assertCase("mysql-orders-boolean");
    }

    @Test
    void assertExecuteMySQLDate() throws Exception {
        assertCase("mysql-orders-date");
    }

    @Test
    void assertExecuteMySQLOffset() throws Exception {
        assertCase("mysql-orders-offset");
    }

    @Test
    void assertExecuteMySQLMultiOrder() throws Exception {
        assertCase("mysql-orders-multi-order");
    }

    @Test
    void assertExecuteMySQLExpressions() throws Exception {
        assertCase("mysql-orders-expressions");
    }

    @Test
    void assertExecuteMySQLFunctions() throws Exception {
        assertCase("mysql-orders-functions");
    }

    @Test
    void assertExecuteMySQLCaseExpression() throws Exception {
        assertCase("mysql-orders-case-expression");
    }

    @Test
    void assertExecuteMySQLAggregateFunctions() throws Exception {
        assertCase("mysql-orders-aggregate-functions");
    }

    @Test
    void assertExecuteMySQLCountDistinct() throws Exception {
        assertCase("mysql-orders-count-distinct");
    }

    @Test
    void assertExecuteMySQLHaving() throws Exception {
        assertCase("mysql-orders-having");
    }

    @Test
    void assertExecuteMySQLInnerJoin() throws Exception {
        assertCase("mysql-orders-inner-join");
    }

    @Test
    void assertExecuteMySQLLeftJoin() throws Exception {
        assertCase("mysql-orders-left-join");
    }

    @Test
    void assertExecuteMySQLSubqueryIn() throws Exception {
        assertCase("mysql-orders-subquery-in");
    }

    @Test
    void assertExecuteMySQLExists() throws Exception {
        assertCase("mysql-customers-exists");
    }

    @Test
    void assertExecuteMySQLUnion() throws Exception {
        assertCase("mysql-orders-union");
    }

    @Test
    void assertExecuteMySQLQuotedIdentifiers() throws Exception {
        assertCase("mysql-orders-quoted-identifiers");
    }

    @Test
    void assertExecuteMySQLComparisonOperators() throws Exception {
        assertCase("mysql-orders-comparison-operators");
    }

    @Test
    void assertExecuteMySQLNotPredicates() throws Exception {
        assertCase("mysql-orders-not-predicates");
    }

    @Test
    void assertExecuteMySQLOrderAlias() throws Exception {
        assertCase("mysql-orders-order-alias");
    }

    @Test
    void assertExecuteMySQLOrderOrdinal() throws Exception {
        assertCase("mysql-orders-order-ordinal");
    }

    @Test
    void assertExecuteMySQLGroupMultiple() throws Exception {
        assertCase("mysql-orders-group-multiple");
    }

    @Test
    void assertExecuteMySQLHavingCount() throws Exception {
        assertCase("mysql-orders-having-count");
    }

    @Test
    void assertExecuteMySQLCoalesceCast() throws Exception {
        assertCase("mysql-orders-coalesce-cast");
    }

    @Test
    void assertExecuteMySQLStringFunctions() throws Exception {
        assertCase("mysql-orders-string-functions");
    }

    @Test
    void assertExecuteMySQLDerivedTable() throws Exception {
        assertCase("mysql-orders-derived-table");
    }

    @Test
    void assertExecuteMySQLScalarSubquery() throws Exception {
        assertCase("mysql-orders-scalar-subquery");
    }

    @Test
    void assertExecuteMySQLUnionAll() throws Exception {
        assertCase("mysql-orders-union-all");
    }

    @Test
    void assertExecuteMySQLLimitOffsetComma() throws Exception {
        assertCase("mysql-orders-limit-offset-comma");
    }

    @Test
    void assertExecuteMySQLWindowRowNumber() throws Exception {
        assertCase("mysql-orders-window-row-number");
    }

    @Test
    void assertExecuteMySQLCte() throws Exception {
        assertCase("mysql-orders-cte");
    }

    @Test
    void assertExecuteMySQLRightJoin() throws Exception {
        assertCase("mysql-orders-right-join");
    }

    @Test
    void assertExecuteMySQLConditionalAggregation() throws Exception {
        assertCase("mysql-orders-conditional-aggregation");
    }

    @Test
    void assertExecuteMySQLNotBetween() throws Exception {
        assertCase("mysql-orders-not-between");
    }

    @Test
    void assertExecuteMySQLJoinOn() throws Exception {
        assertCase("mysql-orders-join-on");
    }

    @Test
    void assertExecuteMySQLCrossJoin() throws Exception {
        assertCase("mysql-orders-cross-join");
    }

    @Test
    void assertExecuteMySQLNaturalJoin() throws Exception {
        assertCase("mysql-orders-natural-join");
    }

    @Test
    void assertExecuteMySQLCorrelatedExists() throws Exception {
        assertCase("mysql-customers-correlated-exists");
    }

    @Test
    void assertExecuteMySQLNotExists() throws Exception {
        assertCase("mysql-customers-not-exists");
    }

    @Test
    void assertExecuteMySQLQuantifiedAll() throws Exception {
        assertCase("mysql-orders-quantified-all");
    }

    @Test
    void assertExecuteMySQLQuantifiedAny() throws Exception {
        assertCase("mysql-orders-quantified-any");
    }

    @Test
    void assertExecuteMySQLSubqueryNotIn() throws Exception {
        assertCase("mysql-orders-subquery-not-in");
    }

    @Test
    void assertExecuteMySQLConcatDateExtract() throws Exception {
        assertCase("mysql-orders-concat-date-extract");
    }

    @Test
    void assertExecuteMySQLNullif() throws Exception {
        assertCase("mysql-orders-nullif");
    }

    @Test
    void assertExecuteMySQLSimpleCase() throws Exception {
        assertCase("mysql-orders-simple-case");
    }

    @Test
    void assertExecuteMySQLRecursiveCte() throws Exception {
        assertCase("mysql-orders-recursive-cte");
    }

    @Test
    void assertExecuteMySQLDateFunctions() throws Exception {
        assertCase("mysql-orders-date-functions");
    }

    @Test
    void assertExecuteMySQLTimestampdiff() throws Exception {
        assertCase("mysql-orders-timestampdiff");
    }

    @Test
    void assertExecuteMySQLStringExtraFunctions() throws Exception {
        assertCase("mysql-orders-string-extra-functions");
    }

    @Test
    void assertExecuteMySQLStringPadLocate() throws Exception {
        assertCase("mysql-orders-string-pad-locate");
    }

    @Test
    void assertExecuteMySQLNumericFunctions() throws Exception {
        assertCase("mysql-orders-numeric-functions");
    }

    @Test
    void assertExecuteMySQLBitwiseOperators() throws Exception {
        assertCase("mysql-orders-bitwise-operators");
    }

    @Test
    void assertExecuteMySQLRegexpLike() throws Exception {
        assertCase("mysql-orders-regexp-like");
    }

    @Test
    void assertExecuteMySQLIsBooleanPredicate() throws Exception {
        assertCase("mysql-orders-is-boolean-predicate");
    }

    @Test
    void assertExecuteMySQLGroupConcat() throws Exception {
        assertCase("mysql-orders-group-concat");
    }

    @Test
    void assertExecuteMySQLWindowAggregateFrame() throws Exception {
        assertCase("mysql-orders-window-aggregate-frame");
    }

    @Test
    void assertExecuteMySQLWindowLagLead() throws Exception {
        assertCase("mysql-orders-window-lag-lead");
    }

    @Test
    void assertExecuteMySQLIntersect() throws Exception {
        assertCase("mysql-orders-intersect");
    }

    @Test
    void assertExecuteMySQLExcept() throws Exception {
        assertCase("mysql-orders-except");
    }

    @Test
    void assertExecuteMySQLHavingAlias() throws Exception {
        assertCase("mysql-orders-having-alias");
    }

    @Test
    void assertExecuteMySQLNullSafeEquals() throws Exception {
        assertCase("mysql-orders-null-safe-equals");
    }

    @Test
    void assertExecuteMySQLPreparedStringParameter() throws Exception {
        try (
                Connection connection = environment.createIntelliSqlConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT order_id, amount FROM mysql_orders WHERE status = ? ORDER BY order_id")) {
            statement.setString(1, "PAID");
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getString("order_id"), is("M001"));
                assertThat(resultSet.getInt("amount"), is(10));
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getString("order_id"), is("M003"));
                assertThat(resultSet.getInt("amount"), is(30));
                assertThat(resultSet.next(), is(false));
            }
        }
    }

    @Test
    void assertExecuteMySQLPreparedNumericParameter() throws Exception {
        try (
                Connection connection = environment.createIntelliSqlConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT order_id, amount FROM mysql_orders WHERE amount >= ? ORDER BY amount")) {
            statement.setInt(1, 30);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getString("order_id"), is("M003"));
                assertThat(resultSet.getInt("amount"), is(30));
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getString("order_id"), is("M004"));
                assertThat(resultSet.getInt("amount"), is(40));
                assertThat(resultSet.next(), is(false));
            }
        }
    }

    private void assertCase(final String caseId) throws Exception {
        caseRunner.assertCase(caseRunner.find(testCases, caseId));
    }
}
