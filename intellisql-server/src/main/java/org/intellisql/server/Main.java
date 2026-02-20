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

package org.intellisql.server;

import lombok.extern.slf4j.Slf4j;

/**
 * Main entry point for IntelliSql server.
 */
@Slf4j
public class Main {

    private static final String DEFAULT_HOST = "0.0.0.0";

    private static final int DEFAULT_PORT = 8765;

    /**
     * Main method that starts the IntelliSql server.
     *
     * @param args command line arguments, first argument can be port number
     */
    // suppression for UncommentedMain - this is the main entry point
    @SuppressWarnings("UncommentedMain")
    public static void main(final String[] args) {
        log.info("====================================");
        log.info("IntelliSql Server v1.0.0");
        log.info("====================================");
        int port = parsePort(args);
        ServerConfig config =
                ServerConfig.builder()
                        .host(DEFAULT_HOST)
                        .port(port)
                        .maxConnections(100)
                        .idleTimeoutMs(300000)
                        .build();
        final IntelliSqlServer server = new IntelliSqlServer(config);
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    log.info("Shutting down IntelliSql server...");
                                    server.stop();
                                    log.info("Server shutdown complete");
                                }));
        try {
            server.start();
            log.info("Server started. Listening on port {}", server.getPort());
            log.info("JDBC URL: jdbc:intellisql://localhost:{}/intellisql", server.getPort());
            Thread.currentThread().join();
        } catch (final IllegalStateException ex) {
            log.error("Failed to start server", ex);
            System.exit(1);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Server interrupted", ex);
            System.exit(1);
        }
    }

    /**
     * Parses the port number from command line arguments.
     *
     * @param args command line arguments
     * @return the parsed port number or default port if parsing fails
     */
    private static int parsePort(final String[] args) {
        if (args.length > 0) {
            try {
                return Integer.parseInt(args[0]);
            } catch (final NumberFormatException ex) {
                log.warn("Invalid port number: {}, using default port {}", args[0], DEFAULT_PORT);
            }
        }
        return DEFAULT_PORT;
    }
}
