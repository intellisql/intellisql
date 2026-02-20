/*
 * Licensed to the IntelliSQL Project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The IntelliSQL Project licenses this file to You under the Apache License, Version 2.0
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

package com.intellisql.parser.extension;

import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import com.intellisql.parser.SqlParserFactory;
import com.intellisql.parser.dialect.SqlDialect;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for extended SQL parser functionality.
 * Tests custom SQL statements like SHOW TABLES, SHOW SCHEMAS, USE.
 */
class ExtensionSqlParserTest {

    @Test
    void assertParseShowTables() throws SqlParseException {
        String sql = "SHOW TABLES";
        SqlNode result = SqlParserFactory.parseWithBabel(sql);
        assertNotNull(result);
    }

    @Test
    void assertParseShowTablesWithFrom() throws SqlParseException {
        String sql = "SHOW TABLES FROM mydb";
        SqlNode result = SqlParserFactory.parseWithBabel(sql);
        assertNotNull(result);
    }

    @Test
    void assertParseShowTablesWithLike() throws SqlParseException {
        String sql = "SHOW TABLES LIKE 'user%'";
        SqlNode result = SqlParserFactory.parseWithBabel(sql);
        assertNotNull(result);
    }

    @Test
    void assertParseShowTablesWithFromAndLike() throws SqlParseException {
        String sql = "SHOW TABLES FROM mydb LIKE 'user%'";
        SqlNode result = SqlParserFactory.parseWithBabel(sql);
        assertNotNull(result);
    }

    @Test
    void assertParseShowSchemas() throws SqlParseException {
        String sql = "SHOW SCHEMAS";
        SqlNode result = SqlParserFactory.parseWithBabel(sql);
        assertNotNull(result);
    }

    @Test
    void assertParseShowSchemasWithLike() throws SqlParseException {
        String sql = "SHOW SCHEMAS LIKE 'public%'";
        SqlNode result = SqlParserFactory.parseWithBabel(sql);
        assertNotNull(result);
    }

    @Test
    void assertParseShowDatabases() throws SqlParseException {
        String sql = "SHOW DATABASES";
        SqlNode result = SqlParserFactory.parseWithBabel(sql);
        assertNotNull(result);
    }

    @Test
    void assertParseShowDatabasesWithLike() throws SqlParseException {
        String sql = "SHOW DATABASES LIKE 'test%'";
        SqlNode result = SqlParserFactory.parseWithBabel(sql);
        assertNotNull(result);
    }

    @Test
    void assertParseUseSchema() throws SqlParseException {
        String sql = "USE mydb";
        SqlNode result = SqlParserFactory.parseWithBabel(sql);
        assertNotNull(result);
    }

    @Test
    void assertParseExtendedStatementsWithStandardSelect() throws SqlParseException {
        // Verify that standard SELECT still works alongside extended statements
        String sql = "SELECT * FROM users WHERE id = 1";
        SqlNode result = SqlParserFactory.parse(sql, SqlDialect.MYSQL);
        assertNotNull(result);
    }

    @Test
    void assertParseMixedStatements() {
        // Verify multiple statement types can be parsed in sequence
        assertDoesNotThrow(() -> {
            SqlParserFactory.parseWithBabel("SHOW TABLES");
            SqlParserFactory.parseWithBabel("SELECT 1");
            SqlParserFactory.parseWithBabel("USE mydb");
        });
    }
}
