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

package com.intellisql.parser;

import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import com.intellisql.common.dialect.SqlDialect;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link SqlParserFactory}.
 */
class SqlParserFactoryTest {

    @Test
    void assertCreateParserForMySql() {
        assertDoesNotThrow(() -> SqlParserFactory.createParser("SELECT 1", SqlDialect.MYSQL));
    }

    @Test
    void assertCreateParserForPostgreSql() {
        assertDoesNotThrow(() -> SqlParserFactory.createParser("SELECT 1", SqlDialect.POSTGRESQL));
    }

    @Test
    void assertCreateParserForOracle() {
        assertDoesNotThrow(() -> SqlParserFactory.createParser("SELECT 1 FROM DUAL", SqlDialect.ORACLE));
    }

    @Test
    void assertCreateParserForSqlServer() {
        assertDoesNotThrow(() -> SqlParserFactory.createParser("SELECT 1", SqlDialect.SQLSERVER));
    }

    @Test
    void assertCreateBabelParser() {
        assertDoesNotThrow(() -> SqlParserFactory.createBabelParser("SELECT 1"));
    }

    @Test
    void assertParseSimpleSelect() throws SqlParseException {
        String sql = "SELECT * FROM users";
        SqlNode result = SqlParserFactory.parse(sql, SqlDialect.MYSQL);
        assertNotNull(result);
    }

    @Test
    void assertParseWithLimit() throws SqlParseException {
        String sql = "SELECT * FROM users LIMIT 10";
        SqlNode result = SqlParserFactory.parse(sql, SqlDialect.MYSQL);
        assertNotNull(result);
    }

    @Test
    void assertParseWithOffsetFetch() throws SqlParseException {
        String sql = "SELECT * FROM users OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY";
        SqlNode result = SqlParserFactory.parse(sql, SqlDialect.POSTGRESQL);
        assertNotNull(result);
    }

    @Test
    void assertParseWithBabel() throws SqlParseException {
        String sql = "SELECT * FROM users LIMIT 10 OFFSET 5";
        SqlNode result = SqlParserFactory.parseWithBabel(sql);
        assertNotNull(result);
    }

    @Test
    void assertParseInvalidSqlThrowsException() {
        String sql = "SELECT FROM WHERE";
        assertThrows(SqlParseException.class, () -> SqlParserFactory.parse(sql, SqlDialect.MYSQL));
    }

    @Test
    void assertParseExpression() throws SqlParseException {
        String expr = "1 + 2 * 3";
        SqlNode result = SqlParserFactory.parseExpression(expr, SqlDialect.MYSQL);
        assertNotNull(result);
    }

    @Test
    void assertParseJoinQuery() throws SqlParseException {
        String sql = "SELECT u.id, o.order_id FROM users u JOIN orders o ON u.id = o.user_id";
        SqlNode result = SqlParserFactory.parse(sql, SqlDialect.MYSQL);
        assertNotNull(result);
    }

    @Test
    void assertParseSubquery() throws SqlParseException {
        String sql = "SELECT * FROM (SELECT id FROM users) AS sub";
        SqlNode result = SqlParserFactory.parse(sql, SqlDialect.POSTGRESQL);
        assertNotNull(result);
    }

    @Test
    void assertParseAggregateQuery() throws SqlParseException {
        String sql = "SELECT COUNT(*), AVG(age) FROM users GROUP BY department HAVING COUNT(*) > 5";
        SqlNode result = SqlParserFactory.parse(sql, SqlDialect.MYSQL);
        assertNotNull(result);
    }

    @Test
    void assertParseInsertStatement() throws SqlParseException {
        String sql = "INSERT INTO users (id, name) VALUES (1, 'John')";
        SqlNode result = SqlParserFactory.parse(sql, SqlDialect.MYSQL);
        assertNotNull(result);
    }

    @Test
    void assertParseUpdateStatement() throws SqlParseException {
        String sql = "UPDATE users SET name = 'Jane' WHERE id = 1";
        SqlNode result = SqlParserFactory.parse(sql, SqlDialect.MYSQL);
        assertNotNull(result);
    }

    @Test
    void assertParseDeleteStatement() throws SqlParseException {
        String sql = "DELETE FROM users WHERE id = 1";
        SqlNode result = SqlParserFactory.parse(sql, SqlDialect.MYSQL);
        assertNotNull(result);
    }
}
