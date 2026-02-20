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

package org.intellisql.kernel.config;

import lombok.Builder;
import lombok.Getter;

/**
 * Global configuration properties for IntelliSql. Contains settings for query execution, logging,
 * and resource limits.
 */
@Getter
@Builder
public class Props {

    /** Maximum number of intermediate rows to process during federated queries. */
    @Builder.Default
    private final int maxIntermediateRows = 100000;

    /** Query timeout in seconds. */
    @Builder.Default
    private final int queryTimeoutSeconds = 300;

    /** Default number of rows to fetch in a batch. */
    @Builder.Default
    private final int defaultFetchSize = 1000;

    /** Enable query logging. */
    @Builder.Default
    private final boolean enableQueryLogging = true;

    /** Log level for query execution (DEBUG, INFO, WARN, ERROR). */
    @Builder.Default
    private final String logLevel = "INFO";
}
