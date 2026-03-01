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

import org.apache.calcite.sql.SqlNode;
import com.intellisql.common.dialect.SqlDialect;

/**
 * Interface for converting SQL between different database dialects. Each dialect (MySQL,
 * PostgreSQL, Oracle, etc.) should implement this interface.
 */
public interface DialectConverter {

    /**
     * Returns the SQL dialect this converter handles.
     *
     * @return the SqlDialect enum value
     */
    SqlDialect getDialect();

    /**
     * Converts a parsed SQL node to the target dialect string representation.
     *
     * @param node the parsed SQL node (AST)
     * @return the SQL string in the target dialect
     */
    String toTargetDialect(SqlNode node);

    /**
     * Parses SQL string from the source dialect into a Calcite SqlNode.
     *
     * @param sql the SQL string in the source dialect
     * @return the parsed SqlNode (AST)
     * @throws TranslationException if parsing fails
     */
    SqlNode fromSourceDialect(String sql) throws TranslationException;

    /**
     * Returns the Calcite SqlDialect for database-specific formatting.
     *
     * @return the Calcite SqlDialect instance
     */
    org.apache.calcite.sql.SqlDialect getCalciteDialect();

    /**
     * Checks if a specific SQL feature is supported by this dialect.
     *
     * @param featureName the name of the SQL feature
     * @return true if the feature is supported, false otherwise
     */
    default boolean supportsFeature(String featureName) {
        return true;
    }

    /**
     * Gets the dialect-specific quote character for identifiers.
     *
     * @return the quote character (e.g., backtick for MySQL, double quote for PostgreSQL)
     */
    default String getQuoteCharacter() {
        return getCalciteDialect().quoteStringLiteral("");
    }
}
