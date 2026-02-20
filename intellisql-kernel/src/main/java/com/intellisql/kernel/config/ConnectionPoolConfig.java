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

package com.intellisql.kernel.config;

import lombok.Builder;
import lombok.Getter;

/**
 * Connection pool configuration using HikariCP. Defines parameters for database connection pooling.
 */
@Getter
@Builder
public class ConnectionPoolConfig {

    /** Maximum number of connections in the pool. */
    @Builder.Default
    private final int maximumPoolSize = 10;

    /** Minimum number of idle connections to maintain. */
    @Builder.Default
    private final int minimumIdle = 2;

    /** Connection timeout in milliseconds. */
    @Builder.Default
    private final long connectionTimeout = 30000L;

    /** Maximum time a connection can sit idle in the pool in milliseconds. */
    @Builder.Default
    private final long idleTimeout = 600000L;

    /** Maximum lifetime of a connection in the pool in milliseconds. */
    @Builder.Default
    private final long maxLifetime = 1800000L;
}
