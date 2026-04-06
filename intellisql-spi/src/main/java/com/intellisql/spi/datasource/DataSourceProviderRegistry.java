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

package com.intellisql.spi.datasource;

import com.intellisql.spi.loader.IntelliSqlServiceLoader;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registry facade for discovering data source providers.
 */
public final class DataSourceProviderRegistry {

    private DataSourceProviderRegistry() {
    }

    /**
     * Get provider by type or alias.
     *
     * @param type data source type
     * @return provider
     */
    public static DataSourceProvider getProvider(final String type) {
        return IntelliSqlServiceLoader.getService(DataSourceProvider.class, type);
    }

    /**
     * Canonicalize a provider type or alias to its declared type.
     *
     * @param type data source type or alias
     * @return canonical type
     */
    public static String canonicalize(final String type) {
        return getProvider(type).getType();
    }

    /**
     * Check whether a provider exists for the given type or alias.
     *
     * @param type data source type or alias
     * @return true if a provider exists
     */
    public static boolean hasProvider(final String type) {
        try {
            getProvider(type);
            return true;
        } catch (final IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * Get all registered providers.
     *
     * @return providers
     */
    public static Collection<DataSourceProvider> getProviders() {
        return IntelliSqlServiceLoader.getAllServices(DataSourceProvider.class);
    }

    /**
     * Get registered provider types in sorted order.
     *
     * @return types
     */
    public static List<String> getRegisteredTypes() {
        List<String> result = getProviders().stream()
                .map(DataSourceProvider::getType)
                .sorted()
                .collect(Collectors.toList());
        return Collections.unmodifiableList(result);
    }
}
