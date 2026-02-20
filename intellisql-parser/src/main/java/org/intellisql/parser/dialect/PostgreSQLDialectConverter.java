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

package org.intellisql.parser.dialect;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.util.SqlBasicVisitor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * PostgreSQL dialect converter for SQL translation. Handles PostgreSQL-specific syntax and
 * features.
 */
@Slf4j
@Getter
public class PostgreSQLDialectConverter {

    /** PostgreSQL dialect identifier. */
    private static final SqlDialect DIALECT = SqlDialect.POSTGRESQL;

    /** Calcite PostgreSQL dialect instance. */
    private final org.apache.calcite.sql.SqlDialect calciteDialect;

    /** Creates a new PostgreSQL dialect converter. */
    public PostgreSQLDialectConverter() {
        this.calciteDialect = DIALECT.toCalciteDialect();
        log.debug("Initialized PostgreSQL dialect converter");
    }

    /**
     * Converts a SqlNode to PostgreSQL SQL string.
     *
     * @param sqlNode the SqlNode to convert
     * @return PostgreSQL SQL string
     */
    public String toSql(final SqlNode sqlNode) {
        log.debug("Converting SqlNode to PostgreSQL SQL");
        return sqlNode.toSqlString(calciteDialect).getSql();
    }

    /**
     * Converts SQL from another dialect to PostgreSQL.
     *
     * @param sql the SQL string
     * @return PostgreSQL compatible SQL
     */
    public String convertFromDialect(final String sql) {
        log.debug("Converting SQL to PostgreSQL dialect: {}", sql);
        return sql;
    }

    /**
     * Checks if the given SQL node contains PostgreSQL-specific syntax.
     *
     * @param sqlNode the SQL node to check
     * @return true if PostgreSQL-specific syntax is detected
     */
    public boolean hasDialectSpecificSyntax(final SqlNode sqlNode) {
        PostgresSyntaxChecker checker = new PostgresSyntaxChecker();
        sqlNode.accept(checker);
        return checker.hasPostgresSyntax();
    }

    /**
     * Transforms LIMIT/OFFSET syntax to PostgreSQL format. PostgreSQL uses: LIMIT count OFFSET offset
     *
     * @param fetch the fetch count
     * @param offset the offset value
     * @return formatted LIMIT clause
     */
    public String formatLimitClause(final int fetch, final int offset) {
        StringBuilder sb = new StringBuilder();
        if (fetch >= 0) {
            sb.append("LIMIT ").append(fetch);
        }
        if (offset > 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("OFFSET ").append(offset);
        }
        return sb.toString();
    }

    /**
     * Gets PostgreSQL-specific identifier quoting. PostgreSQL uses double quotes (") for identifiers.
     *
     * @param identifier the identifier to quote
     * @return quoted identifier
     */
    public String quoteIdentifier(final String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    /**
     * Unquotes a PostgreSQL identifier.
     *
     * @param identifier the quoted identifier
     * @return unquoted identifier
     */
    public String unquoteIdentifier(final String identifier) {
        if (identifier.startsWith("\"") && identifier.endsWith("\"")) {
            return identifier.substring(1, identifier.length() - 1).replace("\"\"", "\"");
        }
        return identifier;
    }

    /**
     * Converts a boolean value to PostgreSQL format. PostgreSQL uses TRUE/FALSE.
     *
     * @param value the boolean value
     * @return PostgreSQL boolean representation
     */
    public String booleanToString(final boolean value) {
        return value ? "TRUE" : "FALSE";
    }

    /**
     * Generates PostgreSQL-specific type cast syntax. PostgreSQL uses: value::type or CAST(value AS
     * type)
     *
     * @param value the value to cast
     * @param targetType the target type
     * @return PostgreSQL cast expression
     */
    public String formatCast(final String value, final String targetType) {
        return value + "::" + targetType;
    }

    /**
     * Generates a PostgreSQL string literal.
     *
     * @param value the string value
     * @return quoted string literal
     */
    public String formatStringLiteral(final String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    /** Visitor to check for PostgreSQL-specific syntax patterns. */
    private static class PostgresSyntaxChecker extends SqlBasicVisitor<Void> {

        private boolean hasPostgresSyntax;

        public boolean hasPostgresSyntax() {
            return hasPostgresSyntax;
        }

        @Override
        public Void visit(final SqlCall call) {
            SqlKind kind = call.getKind();
            if (kind == SqlKind.SELECT) {
                SqlSelect select = (SqlSelect) call;
                if (select.getFetch() != null || select.getOffset() != null) {
                    hasPostgresSyntax = true;
                }
            }
            return super.visit(call);
        }
    }
}
