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

package com.intellisql.translator;

import com.intellisql.parser.SqlParserFactory;

import java.util.List;

import org.apache.calcite.sql.SqlNode;
import com.intellisql.translator.dialect.DialectConverterFactory;
import com.intellisql.common.dialect.SqlDialect;

import lombok.extern.slf4j.Slf4j;

/**
 * Offline SQL translation service that performs syntax-only translation without requiring database
 * metadata or schema information.
 */
@Slf4j
public class OfflineTranslationService {

    /**
     * Translates SQL from source dialect to target dialect without metadata. This performs pure
     * syntactic translation using Calcite's parser.
     *
     * @param sourceSql the SQL string in the source dialect
     * @param sourceDialect the source SQL dialect
     * @param targetDialect the target SQL dialect
     * @param unsupportedFeatures list to collect unsupported features
     * @return the translated SQL string in the target dialect
     * @throws TranslationException if translation fails
     */
    public String translate(
                            final String sourceSql,
                            final SqlDialect sourceDialect,
                            final SqlDialect targetDialect,
                            final List<String> unsupportedFeatures) throws TranslationException {
        log.debug("Offline translation: {} -> {}", sourceDialect, targetDialect);
        if (sourceSql == null || sourceSql.trim().isEmpty()) {
            throw TranslationException.syntaxError("Source SQL is empty");
        }
        try {
            SqlNode ast = SqlParserFactory.parse(sourceSql, sourceDialect);
            String targetSql = DialectConverterFactory.toSql(ast, targetDialect);
            log.debug("Offline translation complete: {}", targetSql);
            return targetSql;
            // CHECKSTYLE:OFF: IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON: IllegalCatch
            throw TranslationException.syntaxError("Failed to translate: " + ex.getMessage());
        }
    }
}
