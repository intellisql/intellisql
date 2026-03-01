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

package com.intellisql.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages all client connections to the server. Implements connection limit of 100 concurrent
 * connections.
 */
@Slf4j
@Getter
public class ConnectionManager {

    private static final int MAX_CONNECTIONS = 100;

    private final Map<String, ServerConnection> connections;

    /**
     * Constructs a new ConnectionManager.
     */
    public ConnectionManager() {
        this.connections = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new connection for the specified client.
     *
     * @param clientId the unique identifier for the client
     * @return the created ServerConnection
     * @throws IllegalStateException if maximum connection limit is reached
     */
    public ServerConnection createConnection(final String clientId) {
        if (connections.size() >= MAX_CONNECTIONS) {
            log.warn("Connection limit reached: {}", MAX_CONNECTIONS);
            throw new IllegalStateException("Maximum connection limit reached: " + MAX_CONNECTIONS);
        }
        ServerConnection connection = new ServerConnection(clientId);
        connections.put(connection.getId(), connection);
        log.info("Created connection: {} (total: {})", connection.getId(), connections.size());
        return connection;
    }

    /**
     * Retrieves a connection by its identifier.
     *
     * @param connectionId the connection identifier
     * @return the ServerConnection or null if not found
     */
    public ServerConnection getConnection(final String connectionId) {
        ServerConnection connection = connections.get(connectionId);
        if (connection != null) {
            connection.touch();
        }
        return connection;
    }

    /**
     * Closes a connection by its identifier.
     *
     * @param connectionId the connection identifier to close
     */
    public void closeConnection(final String connectionId) {
        ServerConnection connection = connections.remove(connectionId);
        if (connection != null) {
            log.info("Closed connection: {} (total: {})", connectionId, connections.size());
        }
    }

    /**
     * Returns the current number of active connections.
     *
     * @return the number of connections
     */
    public int getConnectionCount() {
        return connections.size();
    }

    /**
     * Closes all connections that have been idle longer than the specified time.
     *
     * @param maxIdleTimeMs the maximum idle time in milliseconds
     */
    public void closeIdleConnections(final long maxIdleTimeMs) {
        final long currentTime = System.currentTimeMillis();
        connections.entrySet().removeIf(entry -> shouldRemoveIdleConnection(entry.getValue(), maxIdleTimeMs, currentTime));
    }

    /**
     * Determines if a connection should be removed due to being idle.
     *
     * @param conn the connection to check
     * @param maxIdleTimeMs the maximum idle time in milliseconds
     * @param currentTime the current time in milliseconds
     * @return true if the connection should be removed
     */
    private boolean shouldRemoveIdleConnection(final ServerConnection conn, final long maxIdleTimeMs, final long currentTime) {
        long idleTime = conn.getIdleTime(currentTime);
        if (idleTime > maxIdleTimeMs) {
            log.info("Closed idle connection: {} (idle time: {}ms)", conn.getId(), idleTime);
            return true;
        }
        return false;
    }

    /**
     * Closes all connections.
     */
    public void closeAll() {
        connections.clear();
        log.info("Closed all connections");
    }

    /**
     * Represents a server-side connection.
     */
    @Getter
    public static class ServerConnection {

        private final String id;

        private long lastAccessTime = System.currentTimeMillis();

        private int queryCount;

        /**
         * Constructs a new ServerConnection.
         *
         * @param id the connection identifier
         */
        public ServerConnection(final String id) {
            this.id = id;
        }

        /**
         * Updates the last access time to current time.
         */
        public void touch() {
            this.lastAccessTime = System.currentTimeMillis();
        }

        /**
         * Increments the query count for this connection.
         */
        public void incrementQueryCount() {
            this.queryCount++;
        }

        /**
         * Returns the idle time since last access.
         *
         * @param currentTime the current time in milliseconds
         * @return the idle time in milliseconds
         */
        public long getIdleTime(final long currentTime) {
            return currentTime - lastAccessTime;
        }
    }
}
