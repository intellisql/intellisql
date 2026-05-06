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

package com.intellisql.test.e2e.framework.environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.intellisql.test.e2e.framework.config.E2ERunnerConfig;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Testcontainer fixture for MySQL datasource E2E tests. */
public final class MySQLContainerFixture implements AutoCloseable {

    private final E2ERunnerConfig.ContainerConfig config;

    private MySQLContainer<?> container;

    /**
     * Creates a MySQL container fixture.
     *
     * @param config the MySQL container configuration
     */
    public MySQLContainerFixture(final E2ERunnerConfig.ContainerConfig config) {
        this.config = config;
    }

    /** Starts the MySQL container. */
    public void start() {
        container = new MySQLContainer<>(DockerImageName.parse(config.getImage()))
                .withDatabaseName(config.getDatabase())
                .withUsername(config.getUsername())
                .withPassword(config.getPassword());
        container.start();
    }

    /**
     * Creates a JDBC connection to the MySQL container.
     *
     * @return JDBC connection
     * @throws SQLException if the connection cannot be created
     */
    public Connection createConnection() throws SQLException {
        return DriverManager.getConnection(container.getJdbcUrl(), container.getUsername(), container.getPassword());
    }

    /**
     * Converts container settings to model template variables.
     *
     * @return model template variables
     */
    public Map<String, String> toModelVariables() {
        Map<String, String> result = new HashMap<>(5);
        result.put("MYSQL_HOST", container.getHost());
        result.put("MYSQL_PORT", String.valueOf(container.getMappedPort(MySQLContainer.MYSQL_PORT)));
        result.put("MYSQL_DATABASE", container.getDatabaseName());
        result.put("MYSQL_USERNAME", container.getUsername());
        result.put("MYSQL_PASSWORD", container.getPassword());
        return result;
    }

    @Override
    public void close() {
        if (container != null) {
            container.stop();
        }
    }
}
