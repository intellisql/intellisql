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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents the result of a health check operation. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthCheckResult {

    private String dataSourceName;

    private HealthStatus status;

    private String message;

    private long responseTimeMs;

    private long timestamp;

    private String details;

    /**
     * Creates a healthy health check result.
     *
     * @param dataSourceName the name of the data source
     * @param responseTimeMs the response time in milliseconds
     * @return a healthy health check result
     */
    public static HealthCheckResult healthy(final String dataSourceName, final long responseTimeMs) {
        return HealthCheckResult.builder()
                .dataSourceName(dataSourceName)
                .status(HealthStatus.HEALTHY)
                .message("Connection is healthy")
                .responseTimeMs(responseTimeMs)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Creates an unhealthy health check result.
     *
     * @param dataSourceName the name of the data source
     * @param message the error message
     * @return an unhealthy health check result
     */
    public static HealthCheckResult unhealthy(final String dataSourceName, final String message) {
        return HealthCheckResult.builder()
                .dataSourceName(dataSourceName)
                .status(HealthStatus.UNHEALTHY)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Creates a degraded health check result.
     *
     * @param dataSourceName the name of the data source
     * @param message the message describing the degraded state
     * @param responseTimeMs the response time in milliseconds
     * @return a degraded health check result
     */
    public static HealthCheckResult degraded(
                                             final String dataSourceName, final String message, final long responseTimeMs) {
        return HealthCheckResult.builder()
                .dataSourceName(dataSourceName)
                .status(HealthStatus.DEGRADED)
                .message(message)
                .responseTimeMs(responseTimeMs)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Checks if the health status is healthy.
     *
     * @return true if healthy, false otherwise
     */
    public boolean isHealthy() {
        return status == HealthStatus.HEALTHY;
    }

    /** Enumeration of possible health check statuses. */
    public enum HealthStatus {
        HEALTHY,
        UNHEALTHY,
        DEGRADED,
        UNKNOWN
    }
}
