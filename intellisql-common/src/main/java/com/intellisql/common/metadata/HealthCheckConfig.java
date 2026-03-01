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

package com.intellisql.common.metadata;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Health check configuration for data source monitoring. */
@Getter
@RequiredArgsConstructor
@Builder
public final class HealthCheckConfig {

    public static final boolean DEFAULT_ENABLED = true;

    public static final int DEFAULT_INTERVAL_SECONDS = 30;

    public static final int DEFAULT_TIMEOUT_SECONDS = 5;

    public static final int DEFAULT_FAILURE_THRESHOLD = 3;

    @Builder.Default
    private final boolean enabled = DEFAULT_ENABLED;

    @Builder.Default
    private final int intervalSeconds = DEFAULT_INTERVAL_SECONDS;

    @Builder.Default
    private final int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

    @Builder.Default
    private final int failureThreshold = DEFAULT_FAILURE_THRESHOLD;
}
