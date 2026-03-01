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

package com.intellisql.translator;

import com.intellisql.common.dialect.SqlDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SqlTranslator}.
 */
class SqlTranslatorTest {

    private SqlTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new SqlTranslator();
    }

    @Test
    void assertTranslateMySqlToPostgreSql() {
        String sql = "SELECT * FROM users LIMIT 10 OFFSET 5";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
        assertTrue(result.getTargetSql().contains("SELECT"));
    }

    @Test
    void assertTranslatePostgreSqlToMySql() {
        String sql = "SELECT * FROM users LIMIT 10 OFFSET 5";
        Translation result = translator.translateOffline(sql, SqlDialect.POSTGRESQL, SqlDialect.MYSQL);
        assertNotNull(result);
        assertNotNull(result.getTargetSql());
    }

    @Test
    void assertTranslateJoinQuery() {
        String sql = "SELECT u.id, o.order_id FROM users u JOIN orders o ON u.id = o.user_id";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertTrue(result.getTargetSql().contains("JOIN"));
    }

    @Test
    void assertTranslateAggregateQuery() {
        String sql = "SELECT COUNT(*), AVG(age) FROM users GROUP BY department";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertTrue(result.getTargetSql().contains("COUNT"));
        assertTrue(result.getTargetSql().contains("GROUP BY"));
    }

    @Test
    void assertTranslateInsertStatement() {
        String sql = "INSERT INTO users (id, name) VALUES (1, 'John')";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertTrue(result.getTargetSql().contains("INSERT INTO"));
    }

    @Test
    void assertTranslateUpdateStatement() {
        String sql = "UPDATE users SET name = 'Jane' WHERE id = 1";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertTrue(result.getTargetSql().contains("UPDATE"));
        assertTrue(result.getTargetSql().contains("WHERE"));
    }

    @Test
    void assertTranslateDeleteStatement() {
        String sql = "DELETE FROM users WHERE id = 1";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.POSTGRESQL);
        assertNotNull(result);
        assertTrue(result.getTargetSql().contains("DELETE FROM"));
    }

    @Test
    void assertValidateSyntax() {
        String sql = "SELECT * FROM users";
        boolean result = translator.validateSyntax(sql, SqlDialect.MYSQL);
        assertTrue(result);
    }

    @Test
    void assertTranslateMySqlToOracle() {
        String sql = "SELECT * FROM users LIMIT 10";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.ORACLE);
        assertNotNull(result);
    }

    @Test
    void assertTranslateMySqlToSqlServer() {
        String sql = "SELECT * FROM users LIMIT 10";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.SQLSERVER);
        assertNotNull(result);
    }

    @Test
    void assertTranslateMySqlToHive() {
        String sql = "SELECT * FROM users LIMIT 10";
        Translation result = translator.translateOffline(sql, SqlDialect.MYSQL, SqlDialect.HIVE);
        assertNotNull(result);
    }
}
