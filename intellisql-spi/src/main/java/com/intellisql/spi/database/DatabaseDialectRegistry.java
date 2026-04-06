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

package com.intellisql.spi.database;

import com.intellisql.spi.loader.IntelliSqlServiceLoader;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registry facade for discovering database dialect plugins.
 */
public final class DatabaseDialectRegistry {

    private DatabaseDialectRegistry() {
    }

    /**
     * Get the dialect implementation for a type.
     *
     * @param type database type
     * @return registered dialect
     */
    public static DatabaseDialect getDialect(final String type) {
        return IntelliSqlServiceLoader.getService(DatabaseDialect.class, type);
    }

    /**
     * Get all registered dialect implementations.
     *
     * @return registered dialects
     */
    public static Collection<DatabaseDialect> getDialects() {
        return IntelliSqlServiceLoader.getAllServices(DatabaseDialect.class);
    }

    /**
     * Get the registered type names in sorted order.
     *
     * @return registered type names
     */
    public static List<String> getRegisteredTypes() {
        final List<String> result = getDialects().stream()
                .map(DatabaseDialect::getType)
                .sorted()
                .collect(Collectors.toList());
        return Collections.unmodifiableList(result);
    }
}
