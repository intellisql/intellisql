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

import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParser.Config;
import org.apache.calcite.sql.validate.SqlConformance;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import com.intellisql.common.dialect.SqlDialect;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating configured SqlParser instances. Supports parsing SQL in multiple dialects
 * using Apache Calcite with IntelliSql extensions.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SqlParserFactory {

    /**
     * Creates a SqlParser instance configured for the specified dialect.
     *
     * @param sql the SQL string to parse
     * @param dialect the SQL dialect to use for parsing
     * @return configured SqlParser instance
     */
    public static SqlParser createParser(final String sql, final SqlDialect dialect) {
        Config config = createParserConfig(dialect);
        return SqlParser.create(sql, config);
    }

    /**
     * Creates a SqlParser configuration for the specified dialect.
     *
     * @param dialect the SQL dialect
     * @return SqlParser configuration
     */
    public static Config createParserConfig(final SqlDialect dialect) {
        SqlParser.ConfigBuilder configBuilder = SqlParser.configBuilder();
        Lex lex = getLex(dialect);
        SqlConformance conformance = getConformance(dialect);
        configBuilder.setLex(lex);
        configBuilder.setConformance(conformance);

        return configBuilder.build();
    }

    private static Lex getLex(final SqlDialect dialect) {
        switch (dialect) {
            case MYSQL:
                return Lex.MYSQL;
            case ORACLE:
                return Lex.ORACLE;
            case SQLSERVER:
                return Lex.SQL_SERVER;
            case POSTGRESQL:
            case HIVE:
            case STANDARD:
            default:
                return Lex.JAVA;
        }
    }

    /**
     * Gets the SQL conformance level for the specified dialect.
     *
     * @param dialect the SQL dialect
     * @return SqlConformance for the dialect
     */
    private static SqlConformance getConformance(final SqlDialect dialect) {
        switch (dialect) {
            case MYSQL:
                // MySQL conformance allows LIMIT start, count and other MySQL-specific syntax
                return SqlConformanceEnum.MYSQL_5;
            case POSTGRESQL:
                return SqlConformanceEnum.PRAGMATIC_99;
            case ORACLE:
                return SqlConformanceEnum.ORACLE_10;
            case SQLSERVER:
                return SqlConformanceEnum.SQL_SERVER_2008;
            case HIVE:
            case STANDARD:
            default:
                return SqlConformanceEnum.DEFAULT;
        }
    }

    /**
     * Parses a SQL string into a SqlNode AST.
     *
     * @param sql the SQL string to parse
     * @param dialect the SQL dialect to use
     * @return parsed SqlNode
     * @throws org.apache.calcite.sql.parser.SqlParseException if parsing fails
     */
    public static SqlNode parse(final String sql, final SqlDialect dialect) throws org.apache.calcite.sql.parser.SqlParseException {
        log.debug("Parsing SQL with dialect {}: {}", dialect, sql);
        SqlParser parser = createParser(sql, dialect);
        return parser.parseQuery();
    }

    /**
     * Parses a SQL expression into a SqlNode.
     *
     * @param sql the SQL expression to parse
     * @param dialect the SQL dialect to use
     * @return parsed SqlNode
     * @throws org.apache.calcite.sql.parser.SqlParseException if parsing fails
     */
    public static SqlNode parseExpression(final String sql, final SqlDialect dialect) throws org.apache.calcite.sql.parser.SqlParseException {
        log.debug("Parsing SQL expression with dialect {}: {}", dialect, sql);
        SqlParser parser = createParser(sql, dialect);
        return parser.parseExpression();
    }

    /**
     * Creates a parser with Babel configuration for lenient multi-dialect parsing.
     *
     * @param sql the SQL string to parse
     * @return SqlParser with Babel configuration
     */
    public static SqlParser createBabelParser(final String sql) {
        Config config = BabelParserConfiguration.createConfig();
        SqlParser.ConfigBuilder configBuilder = SqlParser.configBuilder(config);
        return SqlParser.create(sql, config);
    }

    /**
     * Parses SQL using Babel parser for lenient multi-dialect support.
     *
     * @param sql the SQL string to parse
     * @return parsed SqlNode
     * @throws org.apache.calcite.sql.parser.SqlParseException if parsing fails
     */
    public static SqlNode parseWithBabel(final String sql) throws org.apache.calcite.sql.parser.SqlParseException {
        log.debug("Parsing SQL with Babel parser: {}", sql);
        SqlParser parser = createBabelParser(sql);
        return parser.parseQuery();
    }
}
