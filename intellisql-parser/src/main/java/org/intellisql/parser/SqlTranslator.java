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

package org.intellisql.parser;

import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.sql.SqlNode;
import org.intellisql.parser.dialect.DialectConverterFactory;
import org.intellisql.parser.dialect.SqlDialect;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Core SQL translator that converts SQL between different database dialects. Supports both online
 * (with metadata) and offline (syntax-only) translation modes.
 */
@Slf4j
@Getter
public class SqlTranslator {

    private final OnlineTranslationService onlineService;

    private final OfflineTranslationService offlineService;

    /**
     * Creates a new SQL translator.
     */
    public SqlTranslator() {
        this.onlineService = new OnlineTranslationService();
        this.offlineService = new OfflineTranslationService();
    }

    /**
     * Translates SQL from source dialect to target dialect.
     *
     * @param translation the translation request containing source SQL and dialects
     * @return the translation result with target SQL or error
     */
    public Translation translate(final Translation translation) {
        log.debug(
                "Translating SQL from {} to {}, mode: {}",
                translation.getSourceDialect(),
                translation.getTargetDialect(),
                translation.getMode());
        try {
            String targetSql;
            List<String> unsupportedFeatures = new ArrayList<>();
            if (translation.getMode() == TranslationMode.ONLINE) {
                targetSql =
                        onlineService.translate(
                                translation.getSourceSql(),
                                translation.getSourceDialect(),
                                translation.getTargetDialect(),
                                unsupportedFeatures);
            } else {
                targetSql =
                        offlineService.translate(
                                translation.getSourceSql(),
                                translation.getSourceDialect(),
                                translation.getTargetDialect(),
                                unsupportedFeatures);
            }
            log.info("Successfully translated SQL ({} unsupported features)", unsupportedFeatures.size());
            return translation.withResult(targetSql, unsupportedFeatures);
        } catch (final TranslationException ex) {
            log.error("Translation failed: {}", ex.getMessage());
            return translation.withError(ex.getError());
            // CHECKSTYLE:OFF: IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON: IllegalCatch
            log.error("Unexpected error during translation", ex);
            return translation.withError(TranslationError.of("INTERNAL_ERROR", ex.getMessage()));
        }
    }

    /**
     * Convenience method for offline translation.
     *
     * @param sourceSql the source SQL
     * @param sourceDialect the source dialect
     * @param targetDialect the target dialect
     * @return the translation result
     */
    public Translation translateOffline(
                                        final String sourceSql,
                                        final SqlDialect sourceDialect,
                                        final SqlDialect targetDialect) {
        Translation request =
                Translation.create(sourceSql, sourceDialect, targetDialect, TranslationMode.OFFLINE);
        return translate(request);
    }

    /**
     * Convenience method for online translation.
     *
     * @param sourceSql the source SQL
     * @param sourceDialect the source dialect
     * @param targetDialect the target dialect
     * @return the translation result
     */
    public Translation translateOnline(
                                       final String sourceSql,
                                       final SqlDialect sourceDialect,
                                       final SqlDialect targetDialect) {
        Translation request =
                Translation.create(sourceSql, sourceDialect, targetDialect, TranslationMode.ONLINE);
        return translate(request);
    }

    /**
     * Validates if the SQL can be parsed without translation.
     *
     * @param sql the SQL to validate
     * @param dialect the dialect of the SQL
     * @return true if the SQL is syntactically correct
     */
    public boolean validateSyntax(final String sql, final SqlDialect dialect) {
        try {
            parse(sql, dialect);
            return true;
        } catch (final TranslationException ex) {
            log.debug("Syntax validation failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Parses SQL into a dialect-agnostic AST using Babel parser.
     *
     * @param sql the SQL to parse
     * @param dialect the dialect of the SQL
     * @return the SQL node
     * @throws TranslationException if parsing fails
     */
    public SqlNode parse(final String sql, final SqlDialect dialect) throws TranslationException {
        try {
            return SqlParserFactory.parse(sql, dialect);
            // CHECKSTYLE:OFF: IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON: IllegalCatch
            throw TranslationException.syntaxError("Failed to parse SQL: " + ex.getMessage());
        }
    }

    /**
     * Formats a parsed SQL node back to a specific dialect.
     *
     * @param node the SQL node
     * @param targetDialect the target dialect
     * @return the formatted SQL string
     */
    public String format(final SqlNode node, final SqlDialect targetDialect) {
        return DialectConverterFactory.toSql(node, targetDialect);
    }
}
