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

import com.intellisql.spi.database.DatabaseDialectRegistry;
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
     * Converts a SqlNode to SQL string for the specified dialect.
     *
     * @param sqlNode the SqlNode to convert
     * @param targetDialect the target SQL dialect
     * @return SQL string in the target dialect
     */
    public static String toSql(final SqlNode sqlNode, final String targetDialect) {
        log.debug("Converting SqlNode to {} dialect", targetDialect);
        return sqlNode.toSqlString(DatabaseDialectRegistry.getDialect(targetDialect).getCalciteDialect()).getSql();
    }
}
