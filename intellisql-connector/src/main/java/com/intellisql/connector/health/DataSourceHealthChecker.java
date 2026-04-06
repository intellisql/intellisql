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

package com.intellisql.connector.health;

import com.intellisql.connector.ConnectorRegistry;
import com.intellisql.connector.api.DataSourceConnector;

import com.intellisql.connector.config.DataSourceConfig;

/**
 * Implementation of HealthChecker backed by connector SPI implementations.
 */
public class DataSourceHealthChecker implements HealthChecker {

    @Override
    public HealthCheckResult check(final DataSourceConfig config) {
        long startTime = System.currentTimeMillis();
        boolean healthy = performHealthCheck(config);
        long responseTime = System.currentTimeMillis() - startTime;
        if (healthy) {
            if (responseTime > 1000) {
                return HealthCheckResult.degraded(
                        config.getName(), "Connection is slow: " + responseTime + "ms", responseTime);
            }
            return HealthCheckResult.healthy(config.getName(), responseTime);
        }
        return HealthCheckResult.unhealthy(config.getName(), "Health check returned false");
    }

    @Override
    public String getName() {
        return "DataSourceHealthChecker";
    }

    private boolean performHealthCheck(final DataSourceConfig config) {
        DataSourceConnector connector = ConnectorRegistry.getInstance().getConnector(config.getType());
        return connector.testConnection(config);
    }

    /** Clears the connection cache. */
    public void clearCache() {
        // No-op. Connector-specific caches are owned by the connector implementation itself.
    }
}
