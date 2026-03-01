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

package com.intellisql.common.config;

import lombok.Builder;
import lombok.Getter;

/**
 * Health check configuration for data source connections. Defines parameters for connection health
 * monitoring.
 */
@Getter
@Builder
public class HealthCheckConfig {

    /** Enable health check for this data source. */
    @Builder.Default
    private final boolean enabled = true;

    /** Interval between health checks in seconds. */
    @Builder.Default
    private final int intervalSeconds = 30;

    /** Health check timeout in seconds. */
    @Builder.Default
    private final int timeoutSeconds = 5;

    /** Number of consecutive failures before marking connection as unhealthy. */
    @Builder.Default
    private final int failureThreshold = 3;
}
