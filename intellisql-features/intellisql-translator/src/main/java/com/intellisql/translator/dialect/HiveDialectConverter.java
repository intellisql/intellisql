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

package com.intellisql.translator.dialect;

import com.intellisql.common.dialect.SqlDialect;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.util.SqlBasicVisitor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** Hive dialect converter for SQL translation. Handles Apache Hive-specific syntax and features. */
@Slf4j
@Getter
public class HiveDialectConverter {

    /** Hive dialect identifier. */
    private static final SqlDialect DIALECT = SqlDialect.HIVE;

    /** Calcite Hive dialect instance. */
    private final org.apache.calcite.sql.SqlDialect calciteDialect;

    /** Creates a new Hive dialect converter. */
    public HiveDialectConverter() {
        this.calciteDialect = DIALECT.toCalciteDialect();
        log.debug("Initialized Hive dialect converter");
    }

    /**
     * Converts a SqlNode to Hive SQL string.
     *
     * @param sqlNode the SqlNode to convert
     * @return Hive SQL string
     */
    public String toSql(final SqlNode sqlNode) {
        log.debug("Converting SqlNode to Hive SQL");
        return sqlNode.toSqlString(calciteDialect).getSql();
    }

    /**
     * Converts SQL from another dialect to Hive.
     *
     * @param sql the SQL string
     * @return Hive compatible SQL
     */
    public String convertFromDialect(final String sql) {
        log.debug("Converting SQL to Hive dialect: {}", sql);
        return sql;
    }

    /**
     * Checks if the given SQL node contains Hive-specific syntax.
     *
     * @param sqlNode the SQL node to check
     * @return true if Hive-specific syntax is detected
     */
    public boolean hasDialectSpecificSyntax(final SqlNode sqlNode) {
        HiveSyntaxChecker checker = new HiveSyntaxChecker();
        sqlNode.accept(checker);
        return checker.hasHiveSyntax();
    }

    /**
     * Transforms LIMIT/OFFSET syntax to Hive format. Hive supports LIMIT but has limited OFFSET
     * support.
     *
     * @param fetch the fetch count
     * @param offset the offset value (note: limited support)
     * @return formatted LIMIT clause
     */
    public String formatLimitClause(final int fetch, final int offset) {
        if (offset > 0) {
            log.warn("Hive has limited OFFSET support, using LIMIT only");
        }
        return "LIMIT " + fetch;
    }

    /**
     * Gets Hive-specific identifier quoting. Hive uses backticks (`) for identifiers.
     *
     * @param identifier the identifier to quote
     * @return quoted identifier
     */
    public String quoteIdentifier(final String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    /**
     * Unquotes a Hive identifier.
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
     * Converts a boolean value to Hive format. Hive uses true/false.
     *
     * @param value the boolean value
     * @return Hive boolean representation
     */
    public String booleanToString(final boolean value) {
        return value ? "true" : "false";
    }

    /**
     * Generates Hive current date function.
     *
     * @return current_date function
     */
    public String getCurrentDate() {
        return "current_date()";
    }

    /**
     * Generates Hive current timestamp function.
     *
     * @return current_timestamp function
     */
    public String getCurrentTimestamp() {
        return "current_timestamp()";
    }

    /**
     * Generates Hive-specific NVL function for COALESCE.
     *
     * @param expression the expression to check
     * @param replacement the replacement value
     * @return NVL function call
     */
    public String formatNvl(final String expression, final String replacement) {
        return String.format("NVL(%s, %s)", expression, replacement);
    }

    /**
     * Generates Hive string concatenation.
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

    /**
     * Generates Hive-specific date format function.
     *
     * @param dateExpr the date expression
     * @param pattern the format pattern
     * @return date_format function call
     */
    public String formatDateFormat(final String dateExpr, final String pattern) {
        return String.format("date_format(%s, '%s')", dateExpr, pattern);
    }

    /**
     * Generates Hive-specific Unix timestamp function.
     *
     * @param dateExpr the date expression (optional)
     * @return unix_timestamp function call
     */
    public String formatUnixTimestamp(final String dateExpr) {
        if (dateExpr == null || dateExpr.isEmpty()) {
            return "unix_timestamp()";
        }
        return String.format("unix_timestamp('%s')", dateExpr);
    }

    /**
     * Generates Hive-specific FROM_UNIXTIME function.
     *
     * @param timestamp the Unix timestamp
     * @return from_unixtime function call
     */
    public String formatFromUnixTime(final String timestamp) {
        return String.format("from_unixtime(%s)", timestamp);
    }

    /**
     * Generates Hive-specific map function.
     *
     * @param keyValues alternating key and value pairs
     * @return map function call
     */
    public String formatMap(final String... keyValues) {
        StringBuilder sb = new StringBuilder("map(");
        for (int i = 0; i < keyValues.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(keyValues[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Generates Hive-specific array function.
     *
     * @param elements the array elements
     * @return array function call
     */
    public String formatArray(final String... elements) {
        StringBuilder sb = new StringBuilder("array(");
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(elements[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Generates Hive-specific struct function.
     *
     * @param values the struct values
     * @return struct function call
     */
    public String formatStruct(final String... values) {
        StringBuilder sb = new StringBuilder("struct(");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(values[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    /** Visitor to check for Hive-specific syntax patterns. */
    private static class HiveSyntaxChecker extends SqlBasicVisitor<Void> {

        private boolean hasHiveSyntax;

        public boolean hasHiveSyntax() {
            return hasHiveSyntax;
        }

        @Override
        public Void visit(final SqlCall call) {
            SqlKind kind = call.getKind();
            if (kind == SqlKind.SELECT) {
                SqlSelect select = (SqlSelect) call;
                if (select.getFetch() != null) {
                    hasHiveSyntax = true;
                }
            }
            return super.visit(call);
        }
    }
}
