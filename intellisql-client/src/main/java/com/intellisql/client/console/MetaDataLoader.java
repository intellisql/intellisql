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

package com.intellisql.client.console;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronously loads database metadata for auto-completion.
 */
public class MetaDataLoader {

    private final Set<String> tables = ConcurrentHashMap.newKeySet();

    private final Set<String> columns = ConcurrentHashMap.newKeySet();

    private final Set<String> schemas = ConcurrentHashMap.newKeySet();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(this::createDaemonThread);

    /**
     * Asynchronously loads metadata from the given connection.
     *
     * @param connection the database connection
     */
    public void load(final Connection connection) {
        if (connection == null) {
            return;
        }
        executor.submit(() -> loadMetadataInternal(connection));
    }

    /**
     * Creates a daemon thread for background metadata loading.
     *
     * @param runnable the runnable to execute
     * @return the daemon thread
     */
    private Thread createDaemonThread(final Runnable runnable) {
        Thread t = new Thread(runnable, "metadata-loader");
        t.setDaemon(true);
        return t;
    }

    /**
     * Internal method to load metadata from the connection.
     *
     * @param connection the database connection
     */
    private void loadMetadataInternal(final Connection connection) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            loadSchemas(metaData);
            loadTables(metaData);
            loadColumns(metaData);
        } catch (final SQLException ex) {
            // Log or ignore
        }
    }

    /**
     * Loads schema names from the database.
     *
     * @param metaData the database metadata
     * @throws SQLException if a database error occurs
     */
    private void loadSchemas(final DatabaseMetaData metaData) throws SQLException {
        try (ResultSet rs = metaData.getSchemas()) {
            while (rs.next()) {
                schemas.add(rs.getString("TABLE_SCHEM"));
            }
        }
    }

    /**
     * Loads table names from the database.
     *
     * @param metaData the database metadata
     * @throws SQLException if a database error occurs
     */
    private void loadTables(final DatabaseMetaData metaData) throws SQLException {
        try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                tables.add(tableName);
            }
        }
    }

    /**
     * Loads column names from the database.
     *
     * @param metaData the database metadata
     * @throws SQLException if a database error occurs
     */
    private void loadColumns(final DatabaseMetaData metaData) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, null, "%", "%")) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
    }

    /**
     * Returns the set of table names.
     *
     * @return the table names
     */
    public Set<String> getTables() {
        return tables;
    }

    /**
     * Returns the set of column names.
     *
     * @return the column names
     */
    public Set<String> getColumns() {
        return columns;
    }

    /**
     * Returns the set of schema names.
     *
     * @return the schema names
     */
    public Set<String> getSchemas() {
        return schemas;
    }

    /**
     * Clears all loaded metadata.
     */
    public void clear() {
        tables.clear();
        columns.clear();
        schemas.clear();
    }
}
