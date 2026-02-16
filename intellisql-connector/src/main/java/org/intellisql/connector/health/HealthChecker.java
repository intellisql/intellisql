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

package org.intellisql.connector.health;

import org.intellisql.connector.config.DataSourceConfig;

/**
 * Interface for performing health checks on data sources. Implementations provide data source
 * specific health check logic.
 */
public interface HealthChecker {

    /**
     * Performs a health check on the data source.
     *
     * @param config the data source configuration
     * @return the health check result
     */
    HealthCheckResult check(DataSourceConfig config);

    /**
     * Gets the name of this health checker.
     *
     * @return the health checker name
     */
    String getName();

    /**
     * Gets the timeout in milliseconds for health checks.
     *
     * @return the timeout in milliseconds
     */
    default long getTimeoutMs() {
        return 5000;
    }
}
