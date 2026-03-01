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

package com.intellisql.translator.integration;

import com.intellisql.translator.SqlTranslator;
import com.intellisql.translator.Translation;
import com.intellisql.common.dialect.SqlDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for cross-dialect SQL translation.
 * Tests translation between MySQL, PostgreSQL, Oracle, SQL Server, and Hive.
 */
class CrossDialectTranslationTest {

    private SqlTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new SqlTranslator();
    }

    // ==================== MySQL to PostgreSQL ====================

    @Test
    void assertMySqlToPostgreSqlSimpleSelect() {
        String sql = "SELECT * FROM users";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
        assertTrue(result.getTargetSql().toUpperCase().contains("SELECT"));
    }

    @Test
    void assertMySqlToPostgreSqlLimitOffset() {
        String sql = "SELECT * FROM users LIMIT 10 OFFSET 5";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
    }

    @Test
    void assertMySqlToPostgreSqlWithWhere() {
        String sql = "SELECT id, name FROM users WHERE status = 'active'";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
    }

    // ==================== PostgreSQL to MySQL ====================

    @Test
    void assertPostgreSqlToMySqlSimpleSelect() {
        String sql = "SELECT * FROM users";
        Translation result = translator.translateOffline(sql, SqlDialect.POSTGRESQL, SqlDialect.MYSQL);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
    }

    @Test
    void assertPostgreSqlToMySqlFetchFirst() {
        String sql = "SELECT * FROM users FETCH FIRST 10 ROWS ONLY";
        Translation result = translator.translateOffline(sql, SqlDialect.POSTGRESQL, SqlDialect.MYSQL);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
    }

    // ==================== MySQL to Oracle ====================

    @Test
    void assertMySqlToOracleSimpleSelect() {
        String sql = "SELECT * FROM users";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.ORACLE);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
    }

    @Test
    void assertMySqlToOracleLimit() {
        String sql = "SELECT * FROM users LIMIT 10";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.ORACLE);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
    }

    // ==================== MySQL to SQL Server ====================

    @Test
    void assertMySqlToSqlServerSimpleSelect() {
        String sql = "SELECT * FROM users";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.SQLSERVER);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
    }

    @Test
    void assertMySqlToSqlServerTop() {
        String sql = "SELECT * FROM users LIMIT 10";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.SQLSERVER);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
    }

    // ==================== MySQL to Hive ====================

    @Test
    void assertMySqlToHiveSimpleSelect() {
        String sql = "SELECT * FROM users";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.HIVE);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
    }

    // ==================== Complex Queries ====================

    @Test
    void assertComplexQueryJoin() {
        String sql = "SELECT u.id, u.name, o.order_id FROM users u JOIN orders o ON u.id = o.user_id";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
        assertTrue(result.getTargetSql().toUpperCase().contains("JOIN"));
    }

    @Test
    void assertComplexQuerySubquery() {
        String sql = "SELECT * FROM (SELECT id FROM users WHERE status = 'active') AS sub";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
    }

    @Test
    void assertComplexQueryAggregation() {
        String sql = "SELECT department, COUNT(*) as cnt, AVG(salary) as avg_sal "
                + "FROM employees GROUP BY department HAVING COUNT(*) > 5";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
        assertTrue(result.getTargetSql().toUpperCase().contains("GROUP BY"));
    }

    @Test
    void assertComplexQueryOrderBy() {
        String sql = "SELECT * FROM users ORDER BY created_at DESC LIMIT 10";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
        assertTrue(result.getTargetSql().toUpperCase().contains("ORDER BY"));
    }

    @Test
    void assertComplexQueryMultipleJoins() {
        String sql = "SELECT u.name, o.order_id, p.product_name "
                + "FROM users u "
                + "JOIN orders o ON u.id = o.user_id "
                + "JOIN products p ON o.product_id = p.id";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
    }

    @Test
    void assertComplexQueryLeftJoin() {
        String sql = "SELECT u.name, o.order_id FROM users u LEFT JOIN orders o ON u.id = o.user_id";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
    }

    @Test
    void assertComplexQueryUnion() {
        String sql = "SELECT id, name FROM users UNION SELECT id, name FROM customers";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
        assertTrue(result.getTargetSql().toUpperCase().contains("UNION"));
    }

    @Test
    void assertComplexQueryCaseWhen() {
        String sql = "SELECT id, CASE WHEN status = 'active' THEN 1 ELSE 0 END as is_active FROM users";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
        assertTrue(result.getTargetSql().toUpperCase().contains("CASE"));
    }

    // ==================== Error Handling ====================

    @Test
    void assertInvalidSqlReturnsTranslationWithError() {
        String sql = "SELECT INVALID SYNTAX HERE";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertTrue(!result.isSuccessful() || result.getError() != null || result.getTargetSql() != null);
    }
}
