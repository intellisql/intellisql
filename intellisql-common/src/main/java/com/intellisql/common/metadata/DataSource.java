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

package com.intellisql.common.metadata;

import java.time.Instant;
import java.util.List;

import com.intellisql.common.metadata.enums.DataSourceStatus;
import com.intellisql.common.metadata.enums.DataSourceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** DataSource definition representing a connection to an external data source. */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public final class DataSource {

    private String id;

    private String name;

    private DataSourceType type;

    private String url;

    private String username;

    private String password;

    private List<SchemaMapping> schemaMappings;

    private ConnectionPoolConfig connectionPoolConfig;

    private HealthCheckConfig healthCheckConfig;

    private Instant createdAt;

    private Instant updatedAt;

    private DataSourceStatus status;

    /**
     * Initializes the data source with CREATED status.
     *
     * @return this data source instance
     */
    public DataSource initialize() {
        this.status = DataSourceStatus.CREATED;
        return this;
    }

    /**
     * Marks the data source as CONNECTING status.
     *
     * @return this data source instance
     */
    public DataSource markConnecting() {
        this.status = DataSourceStatus.CONNECTING;
        this.updatedAt = Instant.now();
        return this;
    }

    /**
     * Marks the data source as CONNECTED status.
     *
     * @return this data source instance
     */
    public DataSource markConnected() {
        this.status = DataSourceStatus.CONNECTED;
        this.updatedAt = Instant.now();
        return this;
    }

    /**
     * Marks the data source as ACTIVE status.
     *
     * @return this data source instance
     */
    public DataSource markActive() {
        this.status = DataSourceStatus.ACTIVE;
        this.updatedAt = Instant.now();
        return this;
    }

    /**
     * Marks the data source as DISCONNECTED status.
     *
     * @return this data source instance
     */
    public DataSource markDisconnected() {
        this.status = DataSourceStatus.DISCONNECTED;
        this.updatedAt = Instant.now();
        return this;
    }

    /**
     * Marks the data source as FAILED status.
     *
     * @return this data source instance
     */
    public DataSource markFailed() {
        this.status = DataSourceStatus.FAILED;
        this.updatedAt = Instant.now();
        return this;
    }

    /**
     * Checks if the data source is active.
     *
     * @return true if the data source is active or connected
     */
    public boolean isActive() {
        return status == DataSourceStatus.ACTIVE || status == DataSourceStatus.CONNECTED;
    }

    /**
     * Checks if the data source has failed.
     *
     * @return true if the data source is in FAILED status
     */
    public boolean isFailed() {
        return status == DataSourceStatus.FAILED;
    }

    /**
     * Gets the masked password for logging purposes.
     *
     * @return the masked password or null
     */
    public String getMaskedPassword() {
        return password != null ? "******" : null;
    }
}
