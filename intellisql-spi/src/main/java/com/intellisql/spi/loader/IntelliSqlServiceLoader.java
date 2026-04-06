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

package com.intellisql.spi.loader;

import com.intellisql.spi.TypedSPI;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPI service loader and cacher.
 *
 * @param <T> SPI type
 */
public final class IntelliSqlServiceLoader<T extends TypedSPI> {

    private static final Map<Class<?>, Map<String, TypedSPI>> SPI_INSTANCES_MAP = new ConcurrentHashMap<>();

    private IntelliSqlServiceLoader() {
    }

    /**
     * Register SPI interfaces.
     *
     * @param spiInterface SPI interface class
     * @param <T> type
     */
    public static <T extends TypedSPI> void register(final Class<T> spiInterface) {
        if (!SPI_INSTANCES_MAP.containsKey(spiInterface)) {
            Map<String, TypedSPI> instances = new ConcurrentHashMap<>();
            for (T spiInstance : ServiceLoader.load(spiInterface)) {
                instances.put(spiInstance.getType().toUpperCase(), spiInstance);
                for (String alias : spiInstance.getAliases()) {
                    instances.put(alias.toUpperCase(), spiInstance);
                }
            }
            SPI_INSTANCES_MAP.put(spiInterface, instances);
        }
    }

    /**
     * Get service by type.
     *
     * @param spiInterface SPI interface class
     * @param type type
     * @param <T> type
     * @return SPI instance
     * @throws IllegalArgumentException if type is null or unsupported
     */
    @SuppressWarnings("unchecked")
    public static <T extends TypedSPI> T getService(final Class<T> spiInterface, final String type) {
        if (type == null) {
            throw new IllegalArgumentException(String.format("SPI type cannot be null for interface '%s'", spiInterface.getName()));
        }
        Map<String, TypedSPI> instances = SPI_INSTANCES_MAP.get(spiInterface);
        if (instances == null) {
            register(spiInterface);
            instances = SPI_INSTANCES_MAP.get(spiInterface);
        }
        T instance = (T) instances.get(type.toUpperCase());
        if (instance == null) {
            throw new IllegalArgumentException(String.format("Unsupported SPI type: '%s' for interface '%s'", type, spiInterface.getName()));
        }
        return instance;
    }

    /**
     * Get all services for an interface.
     *
     * @param spiInterface SPI interface class
     * @param <T> type
     * @return collection of SPI instances
     */
    @SuppressWarnings("unchecked")
    public static <T extends TypedSPI> Collection<T> getAllServices(final Class<T> spiInterface) {
        register(spiInterface);
        return (Collection<T>) new LinkedHashSet<>(SPI_INSTANCES_MAP.get(spiInterface).values());
    }
}
