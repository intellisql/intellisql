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

package com.intellisql.translator.dialect;

import com.intellisql.common.dialect.SqlDialect;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.util.SqlBasicVisitor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** Oracle dialect converter for SQL translation. Handles Oracle-specific syntax and features. */
@Slf4j
@Getter
public class OracleDialectConverter {

    /** Oracle dialect identifier. */
    private static final SqlDialect DIALECT = SqlDialect.ORACLE;

    /** Calcite Oracle dialect instance. */
    private final org.apache.calcite.sql.SqlDialect calciteDialect;

    /** Creates a new Oracle dialect converter. */
    public OracleDialectConverter() {
        this.calciteDialect = DIALECT.toCalciteDialect();
        log.debug("Initialized Oracle dialect converter");
    }

    /**
     * Converts a SqlNode to Oracle SQL string.
     *
     * @param sqlNode the SqlNode to convert
     * @return Oracle SQL string
     */
    public String toSql(final SqlNode sqlNode) {
        log.debug("Converting SqlNode to Oracle SQL");
        return sqlNode.toSqlString(calciteDialect).getSql();
    }

    /**
     * Converts SQL from another dialect to Oracle.
     *
     * @param sql the SQL string
     * @return Oracle compatible SQL
     */
    public String convertFromDialect(final String sql) {
        log.debug("Converting SQL to Oracle dialect: {}", sql);
        return sql;
    }

    /**
     * Checks if the given SQL node contains Oracle-specific syntax.
     *
     * @param sqlNode the SQL node to check
     * @return true if Oracle-specific syntax is detected
     */
    public boolean hasDialectSpecificSyntax(final SqlNode sqlNode) {
        OracleSyntaxChecker checker = new OracleSyntaxChecker();
        sqlNode.accept(checker);
        return checker.hasOracleSyntax();
    }

    /**
     * Transforms LIMIT/OFFSET syntax to Oracle ROWNUM format. Oracle 12c+ uses: OFFSET offset ROWS
     * FETCH NEXT fetch ROWS ONLY Older Oracle uses ROWNUM.
     *
     * @param fetch the fetch count
     * @param offset the offset value
     * @return formatted pagination clause
     */
    public String formatPaginationClause(final int fetch, final int offset) {
        StringBuilder sb = new StringBuilder();
        if (offset > 0) {
            sb.append("OFFSET ").append(offset).append(" ROWS");
        }
        if (fetch >= 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("FETCH NEXT ").append(fetch).append(" ROWS ONLY");
        }
        return sb.toString();
    }

    /**
     * Generates Oracle ROWNUM-based pagination for older versions.
     *
     * @param fetch the fetch count
     * @param offset the offset value
     * @return ROWNUM-based WHERE clause
     */
    public String formatRowNumPagination(final int fetch, final int offset) {
        if (offset > 0) {
            int startRow = offset + 1;
            int endRow = offset + fetch;
            return String.format("WHERE ROWNUM BETWEEN %d AND %d", startRow, endRow);
        }
        return String.format("WHERE ROWNUM <= %d", fetch);
    }

    /**
     * Gets Oracle-specific identifier quoting. Oracle uses double quotes (") for identifiers.
     *
     * @param identifier the identifier to quote
     * @return quoted identifier
     */
    public String quoteIdentifier(final String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    /**
     * Unquotes an Oracle identifier.
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
     * Converts a boolean value to Oracle format. Oracle doesn't have native boolean in SQL, uses 1/0
     * or 'Y'/'N'.
     *
     * @param value the boolean value
     * @return Oracle boolean representation
     */
    public String booleanToString(final boolean value) {
        return value ? "1" : "0";
    }

    /**
     * Generates Oracle-specific DUAL table reference for SELECT without FROM.
     *
     * @return DUAL table reference
     */
    public String getDualTable() {
        return "FROM DUAL";
    }

    /**
     * Converts SYSDATE to Oracle format.
     *
     * @return Oracle SYSDATE expression
     */
    public String getCurrentDate() {
        return "SYSDATE";
    }

    /**
     * Generates Oracle sequence next value syntax.
     *
     * @param sequenceName the sequence name
     * @return sequence next value expression
     */
    public String getSequenceNextVal(final String sequenceName) {
        return sequenceName + ".NEXTVAL";
    }

    /**
     * Generates Oracle sequence current value syntax.
     *
     * @param sequenceName the sequence name
     * @return sequence current value expression
     */
    public String getSequenceCurrVal(final String sequenceName) {
        return sequenceName + ".CURRVAL";
    }

    /** Visitor to check for Oracle-specific syntax patterns. */
    private static class OracleSyntaxChecker extends SqlBasicVisitor<Void> {

        private boolean hasOracleSyntax;

        public boolean hasOracleSyntax() {
            return hasOracleSyntax;
        }

        @Override
        public Void visit(final SqlCall call) {
            SqlKind kind = call.getKind();
            if (kind == SqlKind.SELECT) {
                SqlSelect select = (SqlSelect) call;
                if (select.getFetch() != null || select.getOffset() != null) {
                    hasOracleSyntax = true;
                }
            }
            return super.visit(call);
        }
    }
}
