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

import org.apache.calcite.sql.SqlNode;

/**
 * Interface for converting SQL between different database dialects.
 * Each database dialect has its own implementation of this interface.
 */
public interface DialectConverter {

    /**
     * Gets the SQL dialect this converter handles.
     *
     * @return the SQL dialect
     */
    SqlDialect getDialect();

    /**
     * Converts a SQL node to the target dialect's SQL string.
     *
     * @param sqlNode the SQL node to convert
     * @return the converted SQL string
     */
    String convert(SqlNode sqlNode);

    /**
     * Converts a SQL string from another dialect to this dialect.
     *
     * @param sql        the SQL string
     * @param sourceDialect the source dialect
     * @return the converted SQL string
     */
    String convertFrom(String sql, SqlDialect sourceDialect);

    /**
     * Checks if a SQL feature is supported by this dialect.
     *
     * @param feature the feature name
     * @return true if supported
     */
    boolean isSupported(String feature);

    /**
     * Gets the Calcite SqlDialect for this converter.
     *
     * @return the Calcite SqlDialect
     */
    org.apache.calcite.sql.SqlDialect getCalciteDialect();
}
