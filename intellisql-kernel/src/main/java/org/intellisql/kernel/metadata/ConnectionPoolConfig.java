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

package org.intellisql.kernel.metadata;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Connection pool configuration for HikariCP. */
@Getter
@RequiredArgsConstructor
@Builder
public final class ConnectionPoolConfig {

    public static final int DEFAULT_MAXIMUM_POOL_SIZE = 20;

    public static final int DEFAULT_MINIMUM_IDLE = 5;

    public static final long DEFAULT_CONNECTION_TIMEOUT = 30000L;

    public static final long DEFAULT_IDLE_TIMEOUT = 600000L;

    public static final long DEFAULT_MAX_LIFETIME = 1800000L;

    @Builder.Default
    private final int maximumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;

    @Builder.Default
    private final int minimumIdle = DEFAULT_MINIMUM_IDLE;

    @Builder.Default
    private final long connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    @Builder.Default
    private final long idleTimeout = DEFAULT_IDLE_TIMEOUT;

    @Builder.Default
    private final long maxLifetime = DEFAULT_MAX_LIFETIME;
}
