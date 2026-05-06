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

import java.nio.file.Path;

import com.intellisql.server.IntelliSqlServer;
import com.intellisql.server.ServerConfig;

/** Fixture for starting and stopping IntelliSQL Server in JDBC E2E tests. */
public final class IntelliSqlServerFixture implements AutoCloseable {

    private final PortAllocator portAllocator = new PortAllocator();

    private IntelliSqlServer server;

    private int port;

    private String database;

    /**
     * Starts IntelliSQL Server with the specified model configuration.
     *
     * @param configPath the model configuration path
     * @param requestedPort the requested port, or zero for an allocated local port
     * @param database the JDBC database name
     */
    public void start(final Path configPath, final int requestedPort, final String database) {
        this.port = requestedPort > 0 ? requestedPort : portAllocator.allocate();
        this.database = database;
        server = new IntelliSqlServer(ServerConfig.fromPortAndConfigPath(port, configPath));
        server.start();
        port = server.getPort();
    }

    /**
     * Gets the IntelliSQL JDBC URL.
     *
     * @return JDBC URL
     */
    public String getJdbcUrl() {
        return "jdbc:intellisql://localhost:" + port + "/" + database;
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop();
        }
    }
}
