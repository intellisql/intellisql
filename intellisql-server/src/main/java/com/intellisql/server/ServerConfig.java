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

package com.intellisql.server;

import lombok.Builder;
import lombok.Getter;

/**
 * Server configuration.
 */
@Getter
@Builder
public class ServerConfig {

    private static final int DEFAULT_PORT = 8765;

    private static final int DEFAULT_MAX_CONNECTIONS = 100;

    private static final int DEFAULT_IDLE_TIMEOUT_MS = 300000;

    @Builder.Default
    private int port = DEFAULT_PORT;

    @Builder.Default
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;

    @Builder.Default
    private long idleTimeoutMs = DEFAULT_IDLE_TIMEOUT_MS;

    @Builder.Default
    private String host = "0.0.0.0";

    /**
     * Creates a default server configuration.
     *
     * @return the default ServerConfig instance
     */
    public static ServerConfig defaultConfig() {
        return ServerConfig.builder().build();
    }

    /**
     * Creates a server configuration with the specified port.
     *
     * @param port the server port
     * @return the ServerConfig instance with the specified port
     */
    public static ServerConfig fromPort(final int port) {
        return ServerConfig.builder().port(port).build();
    }
}
