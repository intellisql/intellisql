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

import com.intellisql.spi.database.DatabaseDialectRegistry;

import lombok.extern.slf4j.Slf4j;

import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;

/**
 * Converts SqlNode to SQL string for different database dialects. Uses Calcite's built-in SQL
 * generation capabilities.
 */
@Slf4j
public final class SqlNodeToStringConverter {

    private final SqlDialect targetDialect;

    /**
     * Creates a converter for the specified target dialect.
     *
     * @param targetDialect the target SQL dialect
     */
    public SqlNodeToStringConverter(final String targetDialect) {
        this.targetDialect = toCalciteDialect(targetDialect);
    }

    /**
     * Converts a SqlNode to SQL string.
     *
     * @param sqlNode the SqlNode to convert
     * @return SQL string in the target dialect
     */
    public String convert(final SqlNode sqlNode) {
        log.debug(
                "Converting SqlNode to SQL string for dialect: {}", targetDialect.getDatabaseProduct());
        return sqlNode.toSqlString(targetDialect).getSql();
    }

    /**
     * Converts a SqlNode to formatted SQL string with custom options.
     *
     * @param sqlNode the SqlNode to convert
     * @param forceParens whether to force parentheses around expressions
     * @param quoteAllIdentifiers whether to quote all identifiers
     * @return formatted SQL string
     */
    public String convert(final SqlNode sqlNode, final boolean forceParens, final boolean quoteAllIdentifiers) {
        log.debug(
                "Converting SqlNode with options - forceParens: {}, quoteAllIdentifiers: {}",
                forceParens,
                quoteAllIdentifiers);
        return sqlNode.toSqlString(targetDialect).getSql();
    }

    /**
     * Static method to convert SqlNode to SQL string for a specific dialect.
     *
     * @param sqlNode the SqlNode to convert
     * @param dialect the target SQL dialect
     * @return SQL string
     */
    public static String toSql(final SqlNode sqlNode, final String dialect) {
        SqlNodeToStringConverter converter = new SqlNodeToStringConverter(dialect);
        return converter.convert(sqlNode);
    }

    /**
     * Converts a registered IntelliSql dialect to Calcite SqlDialect.
     *
     * @param dialect IntelliSql dialect type
     * @return Calcite SqlDialect instance
     */
    private static SqlDialect toCalciteDialect(final String dialect) {
        return DatabaseDialectRegistry.getDialect(dialect).getCalciteDialect();
    }

    /**
     * Gets the Calcite dialect for the specified IntelliSql dialect.
     *
     * @param dialect the IntelliSql dialect
     * @return Calcite SqlDialect instance
     */
    public static SqlDialect getCalciteDialect(final String dialect) {
        return toCalciteDialect(dialect);
    }
}
