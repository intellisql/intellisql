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

package com.intellisql.parser;

import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParser.Config;
import org.apache.calcite.sql.parser.SqlParserImplFactory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Babel parser configuration for lenient multi-dialect SQL parsing. Supports parsing SQL from
 * multiple dialects with relaxed syntax rules.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BabelParserConfiguration {

    /** Default identifier maximum length. */
    private static final int DEFAULT_IDENTIFIER_MAX_LENGTH = 128;

    /** Default SQL case sensitivity. */
    private static final boolean DEFAULT_CASE_SENSITIVE = false;

    /**
     * Creates a SqlParser configuration for Babel parser. Babel parser allows lenient parsing of
     * mixed-dialect SQL.
     *
     * @return SqlParser configuration for Babel
     */
    public static Config createConfig() {
        return SqlParser.configBuilder()
                .setLex(Lex.JAVA)
                .setCaseSensitive(DEFAULT_CASE_SENSITIVE)
                .setIdentifierMaxLength(DEFAULT_IDENTIFIER_MAX_LENGTH)
                .setConformance(org.apache.calcite.sql.validate.SqlConformanceEnum.LENIENT)
                .build();
    }

    /**
     * Creates a SqlParser configuration with custom factory.
     *
     * @param factory custom parser implementation factory
     * @return SqlParser configuration
     */
    public static Config createConfig(final SqlParserImplFactory factory) {
        return SqlParser.configBuilder()
                .setLex(Lex.JAVA)
                .setCaseSensitive(DEFAULT_CASE_SENSITIVE)
                .setIdentifierMaxLength(DEFAULT_IDENTIFIER_MAX_LENGTH)
                .setConformance(org.apache.calcite.sql.validate.SqlConformanceEnum.LENIENT)
                .setParserFactory(factory)
                .build();
    }

    /**
     * Creates a lenient configuration for specific dialect parsing.
     *
     * @param lex lexical rules for the dialect
     * @return SqlParser configuration
     */
    public static Config createLenientConfig(final Lex lex) {
        return SqlParser.configBuilder()
                .setLex(lex)
                .setCaseSensitive(false)
                .setIdentifierMaxLength(DEFAULT_IDENTIFIER_MAX_LENGTH)
                .setConformance(org.apache.calcite.sql.validate.SqlConformanceEnum.LENIENT)
                .build();
    }
}
