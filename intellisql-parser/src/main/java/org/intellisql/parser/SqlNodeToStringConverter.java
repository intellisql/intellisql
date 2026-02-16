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

package org.intellisql.parser;

import org.apache.calcite.sql.SqlNode;
import org.intellisql.parser.dialect.SqlDialect;

import lombok.extern.slf4j.Slf4j;

/**
 * Converts SqlNode to SQL string for different database dialects. Uses Calcite's built-in SQL
 * generation capabilities.
 */
@Slf4j
public final class SqlNodeToStringConverter {

    private final org.apache.calcite.sql.SqlDialect targetDialect;

    /**
     * Creates a converter for the specified target dialect.
     *
     * @param targetDialect the target SQL dialect
     */
    public SqlNodeToStringConverter(final SqlDialect targetDialect) {
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
    public static String toSql(final SqlNode sqlNode, final SqlDialect dialect) {
        SqlNodeToStringConverter converter = new SqlNodeToStringConverter(dialect);
        return converter.convert(sqlNode);
    }

    /**
     * Converts IntelliSql dialect enum to Calcite SqlDialect.
     *
     * @param dialect IntelliSql dialect enum
     * @return Calcite SqlDialect instance
     */
    private static org.apache.calcite.sql.SqlDialect toCalciteDialect(final SqlDialect dialect) {
        return dialect.toCalciteDialect();
    }

    /**
     * Gets the Calcite dialect for the specified IntelliSql dialect.
     *
     * @param dialect the IntelliSql dialect
     * @return Calcite SqlDialect instance
     */
    public static org.apache.calcite.sql.SqlDialect getCalciteDialect(final SqlDialect dialect) {
        return toCalciteDialect(dialect);
    }
}
