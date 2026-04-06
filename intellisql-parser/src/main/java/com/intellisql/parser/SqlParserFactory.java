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

import com.intellisql.spi.database.DatabaseDialect;
import com.intellisql.spi.database.DatabaseDialectRegistry;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParser.Config;
import org.apache.calcite.sql.validate.SqlConformance;
import com.intellisql.parser.impl.IntelliSqlParserImpl;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.calcite.sql.parser.SqlParseException;

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
    public static SqlParser createParser(final String sql, final String dialect) {
        Config config = createParserConfig(dialect);
        return SqlParser.create(sql, config);
    }

    /**
     * Creates a SqlParser configuration for the specified dialect.
     *
     * @param dialect the SQL dialect
     * @return SqlParser configuration
     */
    public static Config createParserConfig(final String dialect) {
        final SqlParser.ConfigBuilder configBuilder = SqlParser.configBuilder();
        final DatabaseDialect databaseDialect = DatabaseDialectRegistry.getDialect(dialect);
        final Lex lex = databaseDialect.getLex();
        final SqlConformance conformance = databaseDialect.getConformance();
        configBuilder.setLex(lex);
        configBuilder.setConformance(conformance);

        return configBuilder.build();
    }

    /**
     * Parses a SQL string into a SqlNode AST.
     *
     * @param sql the SQL string to parse
     * @param dialect the SQL dialect to use
     * @return parsed SqlNode
     * @throws SqlParseException if parsing fails
     */
    public static SqlNode parse(final String sql, final String dialect) throws SqlParseException {
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
     * @throws SqlParseException if parsing fails
     */
    public static SqlNode parseExpression(final String sql, final String dialect) throws SqlParseException {
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
        configBuilder.setParserFactory(IntelliSqlParserImpl.FACTORY);
        return SqlParser.create(sql, configBuilder.build());
    }

    /**
     * Parses SQL using Babel parser for lenient multi-dialect support.
     *
     * @param sql the SQL string to parse
     * @return parsed SqlNode
     * @throws SqlParseException if parsing fails
     */
    public static SqlNode parseWithBabel(final String sql) throws SqlParseException {
        log.debug("Parsing SQL with Babel parser: {}", sql);
        SqlParser parser = createBabelParser(sql);
        return parser.parseQuery();
    }
}
