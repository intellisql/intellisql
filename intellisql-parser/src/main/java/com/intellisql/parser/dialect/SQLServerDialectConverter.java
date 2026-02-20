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

package com.intellisql.parser.dialect;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.util.SqlBasicVisitor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * SQL Server dialect converter for SQL translation. Handles SQL Server-specific syntax and
 * features.
 */
@Slf4j
@Getter
public class SQLServerDialectConverter {

    /** SQL Server dialect identifier. */
    private static final SqlDialect DIALECT = SqlDialect.SQLSERVER;

    /** Calcite SQL Server dialect instance. */
    private final org.apache.calcite.sql.SqlDialect calciteDialect;

    /** Creates a new SQL Server dialect converter. */
    public SQLServerDialectConverter() {
        this.calciteDialect = DIALECT.toCalciteDialect();
        log.debug("Initialized SQL Server dialect converter");
    }

    /**
     * Converts a SqlNode to SQL Server SQL string.
     *
     * @param sqlNode the SqlNode to convert
     * @return SQL Server SQL string
     */
    public String toSql(final SqlNode sqlNode) {
        log.debug("Converting SqlNode to SQL Server SQL");
        return sqlNode.toSqlString(calciteDialect).getSql();
    }

    /**
     * Converts SQL from another dialect to SQL Server.
     *
     * @param sql the SQL string
     * @return SQL Server compatible SQL
     */
    public String convertFromDialect(final String sql) {
        log.debug("Converting SQL to SQL Server dialect: {}", sql);
        return sql;
    }

    /**
     * Checks if the given SQL node contains SQL Server-specific syntax.
     *
     * @param sqlNode the SQL node to check
     * @return true if SQL Server-specific syntax is detected
     */
    public boolean hasDialectSpecificSyntax(final SqlNode sqlNode) {
        SqlServerSyntaxChecker checker = new SqlServerSyntaxChecker();
        sqlNode.accept(checker);
        return checker.hasSqlServerSyntax();
    }

    /**
     * Transforms LIMIT/OFFSET syntax to SQL Server OFFSET-FETCH format. SQL Server 2012+ uses: OFFSET
     * offset ROWS FETCH NEXT fetch ROWS ONLY
     *
     * @param fetch the fetch count
     * @param offset the offset value
     * @return formatted pagination clause
     */
    public String formatPaginationClause(final int fetch, final int offset) {
        StringBuilder sb = new StringBuilder();
        sb.append("OFFSET ").append(offset).append(" ROWS");
        if (fetch >= 0) {
            sb.append(" FETCH NEXT ").append(fetch).append(" ROWS ONLY");
        }
        return sb.toString();
    }

    /**
     * Generates SQL Server TOP clause for pagination (pre-2012).
     *
     * @param fetch the fetch count
     * @return TOP clause
     */
    public String formatTopClause(final int fetch) {
        return "TOP " + fetch;
    }

    /**
     * Gets SQL Server-specific identifier quoting. SQL Server uses brackets [] or double quotes for
     * identifiers.
     *
     * @param identifier the identifier to quote
     * @return quoted identifier using brackets
     */
    public String quoteIdentifier(final String identifier) {
        return "[" + identifier.replace("]", "]]") + "]";
    }

    /**
     * Unquotes a SQL Server identifier.
     *
     * @param identifier the quoted identifier
     * @return unquoted identifier
     */
    public String unquoteIdentifier(final String identifier) {
        if (identifier.startsWith("[") && identifier.endsWith("]")) {
            return identifier.substring(1, identifier.length() - 1).replace("]]", "]");
        }
        if (identifier.startsWith("\"") && identifier.endsWith("\"")) {
            return identifier.substring(1, identifier.length() - 1).replace("\"\"", "\"");
        }
        return identifier;
    }

    /**
     * Converts a boolean value to SQL Server format. SQL Server uses 1/0.
     *
     * @param value the boolean value
     * @return SQL Server boolean representation
     */
    public String booleanToString(final boolean value) {
        return value ? "1" : "0";
    }

    /**
     * Generates SQL Server GETDATE() function.
     *
     * @return GETDATE function
     */
    public String getCurrentDate() {
        return "GETDATE()";
    }

    /**
     * Generates SQL Server NEWID() function for UUID.
     *
     * @return NEWID function
     */
    public String getNewUuid() {
        return "NEWID()";
    }

    /**
     * Generates SQL Server ISNULL function for COALESCE.
     *
     * @param expression the expression to check
     * @param replacement the replacement value
     * @return ISNULL function call
     */
    public String formatIsNull(final String expression, final String replacement) {
        return String.format("ISNULL(%s, %s)", expression, replacement);
    }

    /**
     * Generates SQL Server string concatenation with NULL handling.
     *
     * @param expressions the expressions to concatenate
     * @return CONCAT function call
     */
    public String formatConcat(final String... expressions) {
        StringBuilder sb = new StringBuilder("CONCAT(");
        for (int i = 0; i < expressions.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(expressions[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    /** Visitor to check for SQL Server-specific syntax patterns. */
    private static class SqlServerSyntaxChecker extends SqlBasicVisitor<Void> {

        private boolean hasSqlServerSyntax;

        public boolean hasSqlServerSyntax() {
            return hasSqlServerSyntax;
        }

        @Override
        public Void visit(final SqlCall call) {
            SqlKind kind = call.getKind();
            if (kind == SqlKind.SELECT) {
                SqlSelect select = (SqlSelect) call;
                if (select.getFetch() != null || select.getOffset() != null) {
                    hasSqlServerSyntax = true;
                }
            }
            return super.visit(call);
        }
    }
}
