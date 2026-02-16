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

import org.apache.calcite.sql.SqlNode;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating dialect-specific converters. Provides a unified interface for SQL dialect
 * conversion.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DialectConverterFactory {

    /**
     * Creates a dialect converter for the specified dialect.
     *
     * @param dialect the SQL dialect
     * @return dialect converter instance
     * @throws IllegalArgumentException if the dialect is not supported
     */
    public static Object createConverter(final SqlDialect dialect) {
        log.debug("Creating converter for dialect: {}", dialect);
        switch (dialect) {
            case MYSQL:
                return new MySQLDialectConverter();
            case POSTGRESQL:
                return new PostgreSQLDialectConverter();
            case ORACLE:
                return new OracleDialectConverter();
            case SQLSERVER:
                return new SQLServerDialectConverter();
            case HIVE:
                return new HiveDialectConverter();
            case STANDARD:
                return new StandardDialectConverter();
            default:
                throw new IllegalArgumentException("Unsupported dialect: " + dialect);
        }
    }

    /**
     * Gets a MySQL dialect converter.
     *
     * @return MySQLDialectConverter instance
     */
    public static MySQLDialectConverter getMySQLConverter() {
        return new MySQLDialectConverter();
    }

    /**
     * Gets a PostgreSQL dialect converter.
     *
     * @return PostgreSQLDialectConverter instance
     */
    public static PostgreSQLDialectConverter getPostgreSQLConverter() {
        return new PostgreSQLDialectConverter();
    }

    /**
     * Gets an Oracle dialect converter.
     *
     * @return OracleDialectConverter instance
     */
    public static OracleDialectConverter getOracleConverter() {
        return new OracleDialectConverter();
    }

    /**
     * Gets a SQL Server dialect converter.
     *
     * @return SQLServerDialectConverter instance
     */
    public static SQLServerDialectConverter getSQLServerConverter() {
        return new SQLServerDialectConverter();
    }

    /**
     * Gets a Hive dialect converter.
     *
     * @return HiveDialectConverter instance
     */
    public static HiveDialectConverter getHiveConverter() {
        return new HiveDialectConverter();
    }

    /**
     * Converts a SqlNode to SQL string for the specified dialect.
     *
     * @param sqlNode the SqlNode to convert
     * @param targetDialect the target SQL dialect
     * @return SQL string in the target dialect
     */
    public static String toSql(final SqlNode sqlNode, final SqlDialect targetDialect) {
        log.debug("Converting SqlNode to {} dialect", targetDialect);
        switch (targetDialect) {
            case MYSQL:
                return getMySQLConverter().toSql(sqlNode);
            case POSTGRESQL:
                return getPostgreSQLConverter().toSql(sqlNode);
            case ORACLE:
                return getOracleConverter().toSql(sqlNode);
            case SQLSERVER:
                return getSQLServerConverter().toSql(sqlNode);
            case HIVE:
                return getHiveConverter().toSql(sqlNode);
            case STANDARD:
            default:
                return getStandardConverter().toSql(sqlNode);
        }
    }

    /**
     * Gets a Standard SQL dialect converter.
     *
     * @return StandardDialectConverter instance
     */
    public static StandardDialectConverter getStandardConverter() {
        return new StandardDialectConverter();
    }

    /** Standard SQL dialect converter for ANSI SQL. */
    public static class StandardDialectConverter {

        private final org.apache.calcite.sql.SqlDialect calciteDialect;

        /** Creates a new standard dialect converter. */
        public StandardDialectConverter() {
            this.calciteDialect = SqlDialect.STANDARD.toCalciteDialect();
        }

        /**
         * Converts a SqlNode to SQL string.
         *
         * @param sqlNode the SqlNode to convert
         * @return SQL string
         */
        public String toSql(final SqlNode sqlNode) {
            return sqlNode.toSqlString(calciteDialect).getSql();
        }

        /**
         * Quotes an identifier.
         *
         * @param identifier the identifier to quote
         * @return quoted identifier
         */
        public String quoteIdentifier(final String identifier) {
            return "\"" + identifier.replace("\"", "\"\"") + "\"";
        }

        /**
         * Converts a boolean value to string.
         *
         * @param value the boolean value
         * @return string representation
         */
        public String booleanToString(final boolean value) {
            return value ? "TRUE" : "FALSE";
        }
    }
}
