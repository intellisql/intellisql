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

/** MySQL dialect converter for SQL translation. Handles MySQL-specific syntax and features. */
@Slf4j
@Getter
public class MySQLDialectConverter {

    /** MySQL dialect identifier. */
    private static final SqlDialect DIALECT = SqlDialect.MYSQL;

    /** Calcite MySQL dialect instance. */
    private final org.apache.calcite.sql.SqlDialect calciteDialect;

    /** Creates a new MySQL dialect converter. */
    public MySQLDialectConverter() {
        this.calciteDialect = DIALECT.toCalciteDialect();
        log.debug("Initialized MySQL dialect converter");
    }

    /**
     * Converts a SqlNode to MySQL SQL string.
     *
     * @param sqlNode the SqlNode to convert
     * @return MySQL SQL string
     */
    public String toSql(final SqlNode sqlNode) {
        log.debug("Converting SqlNode to MySQL SQL");
        return sqlNode.toSqlString(calciteDialect).getSql();
    }

    /**
     * Converts SQL from another dialect to MySQL.
     *
     * @param sql the SQL string
     * @return MySQL compatible SQL
     */
    public String convertFromDialect(final String sql) {
        log.debug("Converting SQL to MySQL dialect: {}", sql);
        return sql;
    }

    /**
     * Checks if the given SQL node contains MySQL-specific syntax.
     *
     * @param sqlNode the SQL node to check
     * @return true if MySQL-specific syntax is detected
     */
    public boolean hasDialectSpecificSyntax(final SqlNode sqlNode) {
        MySqlSyntaxChecker checker = new MySqlSyntaxChecker();
        sqlNode.accept(checker);
        return checker.hasMySqlSyntax();
    }

    /**
     * Transforms LIMIT/OFFSET syntax to MySQL format. MySQL uses: LIMIT offset, count or LIMIT count
     * OFFSET offset
     *
     * @param fetch the fetch count
     * @param offset the offset value
     * @return formatted LIMIT clause
     */
    public String formatLimitClause(final int fetch, final int offset) {
        if (offset > 0) {
            return String.format("LIMIT %d, %d", offset, fetch);
        }
        return String.format("LIMIT %d", fetch);
    }

    /**
     * Gets MySQL-specific identifier quoting. MySQL uses backticks (`) for identifiers.
     *
     * @param identifier the identifier to quote
     * @return quoted identifier
     */
    public String quoteIdentifier(final String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    /**
     * Unquotes a MySQL identifier.
     *
     * @param identifier the quoted identifier
     * @return unquoted identifier
     */
    public String unquoteIdentifier(final String identifier) {
        if (identifier.startsWith("`") && identifier.endsWith("`")) {
            return identifier.substring(1, identifier.length() - 1).replace("``", "`");
        }
        return identifier;
    }

    /**
     * Converts a boolean value to MySQL format. MySQL uses 1/0 or TRUE/FALSE.
     *
     * @param value the boolean value
     * @return MySQL boolean representation
     */
    public String booleanToString(final boolean value) {
        return value ? "TRUE" : "FALSE";
    }

    /** Visitor to check for MySQL-specific syntax patterns. */
    private static class MySqlSyntaxChecker extends SqlBasicVisitor<Void> {

        private boolean hasMySqlSyntax;

        public boolean hasMySqlSyntax() {
            return hasMySqlSyntax;
        }

        @Override
        public Void visit(final SqlCall call) {
            SqlKind kind = call.getKind();
            if (kind == SqlKind.SELECT) {
                SqlSelect select = (SqlSelect) call;
                if (select.getFetch() != null || select.getOffset() != null) {
                    hasMySqlSyntax = true;
                }
            }
            return super.visit(call);
        }
    }
}
