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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.intellisql.common.dialect.SqlDialect;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Represents a SQL translation task and its result. */
@Getter
@Builder
@RequiredArgsConstructor
public class Translation {

    private final String id;

    private final String sourceSql;

    private final SqlDialect sourceDialect;

    private final SqlDialect targetDialect;

    private final TranslationMode mode;

    private final String targetSql;

    private final List<String> unsupportedFeatures;

    private final TranslationError error;

    /**
     * Creates a new translation task.
     *
     * @param sourceSql the source SQL
     * @param sourceDialect the source dialect
     * @param targetDialect the target dialect
     * @param mode the translation mode
     * @return the translation task
     */
    public static Translation create(
                                     final String sourceSql,
                                     final SqlDialect sourceDialect,
                                     final SqlDialect targetDialect,
                                     final TranslationMode mode) {
        return Translation.builder()
                .id(UUID.randomUUID().toString())
                .sourceSql(sourceSql)
                .sourceDialect(sourceDialect)
                .targetDialect(targetDialect)
                .mode(mode)
                .build();
    }

    /**
     * Returns a new translation with the result.
     *
     * @param targetSql the translated SQL
     * @param unsupportedFeatures the unsupported features
     * @return the translation with result
     */
    public Translation withResult(final String targetSql, final List<String> unsupportedFeatures) {
        return Translation.builder()
                .id(this.id)
                .sourceSql(this.sourceSql)
                .sourceDialect(this.sourceDialect)
                .targetDialect(this.targetDialect)
                .mode(this.mode)
                .targetSql(targetSql)
                .unsupportedFeatures(
                        unsupportedFeatures != null ? unsupportedFeatures : Collections.emptyList())
                .build();
    }

    /**
     * Returns a new translation with the error.
     *
     * @param error the translation error
     * @return the translation with error
     */
    public Translation withError(final TranslationError error) {
        return Translation.builder()
                .id(this.id)
                .sourceSql(this.sourceSql)
                .sourceDialect(this.sourceDialect)
                .targetDialect(this.targetDialect)
                .mode(this.mode)
                .unsupportedFeatures(Collections.emptyList())
                .error(error)
                .build();
    }

    /**
     * Checks if the translation was successful.
     *
     * @return true if successful
     */
    public boolean isSuccessful() {
        return error == null && targetSql != null;
    }

    /**
     * Checks if there are unsupported features.
     *
     * @return true if there are unsupported features
     */
    public boolean hasUnsupportedFeatures() {
        return unsupportedFeatures != null && !unsupportedFeatures.isEmpty();
    }

    /**
     * Returns a new translation builder.
     *
     * @return the builder
     */
    public static TranslationBuilder builder() {
        return new TranslationBuilder();
    }

    /**
     * Builder for {@link Translation}.
     */
    public static class TranslationBuilder {

        private String id = UUID.randomUUID().toString();

        private List<String> unsupportedFeatures = new ArrayList<>();

        /**
         * Sets the ID.
         *
         * @param id the ID
         * @return the builder
         */
        public TranslationBuilder id(final String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the source SQL.
         *
         * @param sourceSql the source SQL
         * @return the builder
         */
        public TranslationBuilder sourceSql(final String sourceSql) {
            this.sourceSql = sourceSql;
            return this;
        }

        /**
         * Sets the source dialect.
         *
         * @param sourceDialect the source dialect
         * @return the builder
         */
        public TranslationBuilder sourceDialect(final SqlDialect sourceDialect) {
            this.sourceDialect = sourceDialect;
            return this;
        }

        /**
         * Sets the target dialect.
         *
         * @param targetDialect the target dialect
         * @return the builder
         */
        public TranslationBuilder targetDialect(final SqlDialect targetDialect) {
            this.targetDialect = targetDialect;
            return this;
        }

        /**
         * Sets the translation mode.
         *
         * @param mode the mode
         * @return the builder
         */
        public TranslationBuilder mode(final TranslationMode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Sets the target SQL.
         *
         * @param targetSql the target SQL
         * @return the builder
         */
        public TranslationBuilder targetSql(final String targetSql) {
            this.targetSql = targetSql;
            return this;
        }

        /**
         * Sets the unsupported features.
         *
         * @param unsupportedFeatures the unsupported features
         * @return the builder
         */
        public TranslationBuilder unsupportedFeatures(final List<String> unsupportedFeatures) {
            this.unsupportedFeatures = unsupportedFeatures;
            return this;
        }

        /**
         * Sets the error.
         *
         * @param error the error
         * @return the builder
         */
        public TranslationBuilder error(final TranslationError error) {
            this.error = error;
            return this;
        }

        /**
         * Builds the translation.
         *
         * @return the translation
         */
        public Translation build() {
            return new Translation(
                    id, sourceSql, sourceDialect, targetDialect, mode, targetSql, unsupportedFeatures, error);
        }
    }
}
