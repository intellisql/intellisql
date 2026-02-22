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

import com.intellisql.connector.model.QueryResult;
import com.intellisql.federation.IntelliSqlKernel;
import com.intellisql.federation.metadata.MetadataManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.avatica.AvaticaParameter;
import org.apache.calcite.avatica.ColumnMetaData;
import org.apache.calcite.avatica.Meta;
import org.apache.calcite.avatica.MissingResultsException;
import org.apache.calcite.avatica.NoSuchConnectionException;
import org.apache.calcite.avatica.NoSuchStatementException;
import org.apache.calcite.avatica.QueryState;
import org.apache.calcite.avatica.remote.TypedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Avatica Meta implementation for IntelliSql. Provides metadata and query execution services.
 */
@Slf4j
@Getter
public class IntelliSqlMeta implements Meta {

    private final ConnectionManager connectionManager;

    @Setter
    private MetadataManager metadataManager;

    @Setter
    private IntelliSqlKernel kernel;

    private final Map<String, ServerConnection> connections = new ConcurrentHashMap<>();

    private final Map<String, StatementInfo> statements = new ConcurrentHashMap<>();

    private final AtomicInteger statementIdGenerator = new AtomicInteger(0);

    /**
     * Constructs a new IntelliSqlMeta.
     */
    public IntelliSqlMeta() {
        this.connectionManager = new ConnectionManager();
        this.metadataManager = new MetadataManager();
    }

    /**
     * Constructs a new IntelliSqlMeta with a specified MetadataManager.
     *
     * @param metadataManager the metadata manager
     */
    public IntelliSqlMeta(final MetadataManager metadataManager) {
        this.connectionManager = new ConnectionManager();
        this.metadataManager = metadataManager != null ? metadataManager : new MetadataManager();
    }

    @Override
    public Map<DatabaseProperty, Object> getDatabaseProperties(final ConnectionHandle ch) {
        return Collections.emptyMap();
    }

    @Override
    public MetaResultSet getTables(final ConnectionHandle ch, final String catalog, final Pat schemaPattern, final Pat tableNamePattern, final List<String> tableTypes) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getColumns(final ConnectionHandle ch, final String catalog, final Pat schemaPattern, final Pat tableNamePattern, final Pat columnNamePattern) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getSchemas(final ConnectionHandle ch, final String catalog, final Pat schemaPattern) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getCatalogs(final ConnectionHandle ch) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getTableTypes(final ConnectionHandle ch) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getProcedures(final ConnectionHandle ch, final String catalog, final Pat schemaPattern, final Pat procedureNamePattern) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getProcedureColumns(final ConnectionHandle ch, final String catalog, final Pat schemaPattern, final Pat procedureNamePattern, final Pat columnNamePattern) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getColumnPrivileges(final ConnectionHandle ch, final String catalog, final String schema, final String table, final Pat columnNamePattern) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getTablePrivileges(final ConnectionHandle ch, final String catalog, final Pat schemaPattern, final Pat tableNamePattern) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getBestRowIdentifier(final ConnectionHandle ch, final String catalog, final String schema, final String table, final int scope, final boolean nullable) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getVersionColumns(final ConnectionHandle ch, final String catalog, final String schema, final String table) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getPrimaryKeys(final ConnectionHandle ch, final String catalog, final String schema, final String table) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getImportedKeys(final ConnectionHandle ch, final String catalog, final String schema, final String table) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getExportedKeys(final ConnectionHandle ch, final String catalog, final String schema, final String table) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getCrossReference(final ConnectionHandle ch, final String parentCatalog, final String parentSchema, final String parentTable, final String foreignCatalog,
                                           final String foreignSchema, final String foreignTable) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getTypeInfo(final ConnectionHandle ch) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getIndexInfo(final ConnectionHandle ch, final String catalog, final String schema, final String table, final boolean unique, final boolean approximate) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getUDTs(final ConnectionHandle ch, final String catalog, final Pat schemaPattern, final Pat typeNamePattern, final int[] types) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getSuperTypes(final ConnectionHandle ch, final String catalog, final Pat schemaPattern, final Pat typeNamePattern) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getSuperTables(final ConnectionHandle ch, final String catalog, final Pat schemaPattern, final Pat tableNamePattern) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getAttributes(final ConnectionHandle ch, final String catalog, final Pat schemaPattern, final Pat typeNamePattern, final Pat attributeNamePattern) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getClientInfoProperties(final ConnectionHandle ch) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getFunctions(final ConnectionHandle ch, final String catalog, final Pat schemaPattern, final Pat functionNamePattern) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getFunctionColumns(final ConnectionHandle ch, final String catalog, final Pat schemaPattern, final Pat functionNamePattern, final Pat columnNamePattern) {
        return emptyMetaResultSet();
    }

    @Override
    public MetaResultSet getPseudoColumns(final ConnectionHandle ch, final String catalog, final Pat schemaPattern, final Pat tableNamePattern, final Pat columnNamePattern) {
        return emptyMetaResultSet();
    }

    @Override
    public Iterable<Object> createIterable(final StatementHandle handle, final QueryState state, final Signature signature, final List<TypedValue> parameterValues, final Frame firstFrame) {
        return Collections.emptyList();
    }

    @Override
    public StatementHandle prepare(final ConnectionHandle ch, final String sql, final long maxRowCount) {
        ServerConnection conn = connections.get(ch.id);
        if (conn == null) {
            throw new NoSuchConnectionException(ch.id);
        }
        int stmtId = statementIdGenerator.incrementAndGet();
        StatementHandle statement = new StatementHandle(ch.id, stmtId, createSignature(sql));
        statements.put(ch.id + ":" + stmtId, new StatementInfo(statement, conn));
        log.debug("Prepared statement: {} for connection: {}", stmtId, ch.id);
        return statement;
    }

    @Override
    public Meta.ConnectionProperties connectionSync(final ConnectionHandle ch, final Meta.ConnectionProperties connProps) {
        ServerConnection conn = connections.get(ch.id);
        if (conn == null) {
            throw new NoSuchConnectionException(ch.id);
        }
        conn.touch();
        return connProps;
    }

    @Override
    public void openConnection(final ConnectionHandle ch, final Map<String, String> info) {
        ServerConnection conn = new ServerConnection(ch.id);
        connections.put(ch.id, conn);
        log.info("Opened connection: {}", ch.id);
    }

    @Override
    public void closeConnection(final ConnectionHandle ch) {
        ServerConnection conn = connections.remove(ch.id);
        if (conn != null) {
            log.info("Closed connection: {}", ch.id);
        }
    }

    @Override
    public StatementHandle createStatement(final ConnectionHandle ch) {
        ServerConnection conn = connections.get(ch.id);
        if (conn == null) {
            throw new NoSuchConnectionException(ch.id);
        }
        int stmtId = statementIdGenerator.incrementAndGet();
        StatementHandle statement = new StatementHandle(ch.id, stmtId, null);
        statements.put(ch.id + ":" + stmtId, new StatementInfo(statement, conn));
        log.debug("Created statement: {} for connection: {}", stmtId, ch.id);
        return statement;
    }

    @Override
    public void closeStatement(final StatementHandle h) {
        statements.remove(h.connectionId + ":" + h.id);
        log.debug("Closed statement: {}", h.id);
    }

    @Override
    public ExecuteResult prepareAndExecute(final StatementHandle h, final String sql, final long maxRowCount, final PrepareCallback callback) throws NoSuchStatementException {
        StatementInfo info = statements.get(h.connectionId + ":" + h.id);
        if (info == null) {
            throw new NoSuchStatementException(h);
        }
        info.getConnection().incrementQueryCount();
        info.getConnection().touch();
        // Handle SHOW TABLES command
        String normalizedSql = sql.trim().toUpperCase();
        if (normalizedSql.startsWith("SHOW TABLES")) {
            return handleShowTables(h);
        }
        Meta.Frame frame = new Meta.Frame(0, true, Collections.emptyList());
        Meta.Signature signature = createSignature(sql);
        MetaResultSet resultSet =
                MetaResultSet.create(h.connectionId, h.id, true, signature, frame, -1L);
        return new ExecuteResult(Collections.singletonList(resultSet));
    }

    @Override
    public ExecuteResult prepareAndExecute(final StatementHandle h, final String sql, final long maxRowCount, final int batchSize, final PrepareCallback callback) throws NoSuchStatementException {
        return prepareAndExecute(h, sql, maxRowCount, callback);
    }

    private ExecuteResult handleShowTables(final StatementHandle h) {
        log.info("handleShowTables called, metadataManager={}", metadataManager);
        List<ColumnMetaData> columns = new ArrayList<>();
        // Create column metadata for TABLE_NAME column
        ColumnMetaData.Rep rep = ColumnMetaData.Rep.of(String.class);
        ColumnMetaData.AvaticaType avaticaType =
                new ColumnMetaData.AvaticaType(java.sql.Types.VARCHAR, "VARCHAR", rep);
        ColumnMetaData columnMetaData =
                new ColumnMetaData(0, false, false, false, false,
                        java.sql.ResultSetMetaData.columnNullableUnknown, true,
                        -1, "TABLE_NAME", "TABLE_NAME", "", 0, 0, "", "", avaticaType, true, false, false, "");
        columns.add(columnMetaData);
        // Get tables from metadata manager
        List<Object> rows = new ArrayList<>();
        if (metadataManager != null) {
            log.info("Getting tables from metadataManager, current count: {}", metadataManager.getAllTables().size());
            for (com.intellisql.common.metadata.Table table : metadataManager.getAllTables()) {
                log.info("Found table: {}", table.getName());
                rows.add(Collections.singletonList(table.getName()));
            }
        } else {
            log.warn("MetadataManager is null!");
        }
        log.info("Returning {} tables", rows.size());
        Meta.Signature signature = new Meta.Signature(
                columns, "SHOW TABLES", Collections.emptyList(),
                null, Meta.CursorFactory.ARRAY, Meta.StatementType.SELECT);
        Meta.Frame frame = new Meta.Frame(0, true, rows);
        MetaResultSet resultSet =
                MetaResultSet.create(h.connectionId, h.id, true, signature, frame, -1L);
        return new ExecuteResult(Collections.singletonList(resultSet));
    }

    @Override
    public ExecuteResult execute(final StatementHandle h, final List<TypedValue> parameterValues, final long maxRowCount) throws NoSuchStatementException {
        StatementInfo info = statements.get(h.connectionId + ":" + h.id);
        if (info == null) {
            throw new NoSuchStatementException(h);
        }
        info.getConnection().incrementQueryCount();
        info.getConnection().touch();
        // Get SQL from signature
        String sql = null;
        if (h.signature != null) {
            sql = h.signature.sql;
        }
        log.info("Execute called with SQL: {}", sql);
        // Handle SHOW TABLES command
        if (sql != null) {
            String normalizedSql = sql.trim().toUpperCase();
            if (normalizedSql.startsWith("SHOW TABLES")) {
                return handleShowTables(h);
            }
            // Execute query using kernel
            return executeQuery(h, sql);
        }
        // Return empty result for now (other queries not implemented)
        Meta.Frame frame = new Meta.Frame(0, true, Collections.emptyList());
        Meta.Signature signature = h.signature != null ? h.signature : createSignature("SELECT 1");
        MetaResultSet resultSet =
                MetaResultSet.create(h.connectionId, h.id, true, signature, frame, -1L);
        return new ExecuteResult(Collections.singletonList(resultSet));
    }

    @Override
    public ExecuteResult execute(final StatementHandle h, final List<TypedValue> parameterValues, final int batchSize) throws NoSuchStatementException {
        return execute(h, parameterValues, -1L);
    }

    /**
     * Executes a SQL query using the kernel.
     *
     * @param h the statement handle
     * @param sql the SQL query
     * @return the execute result
     */
    private ExecuteResult executeQuery(final StatementHandle h, final String sql) {
        if (kernel == null) {
            log.warn("Kernel not initialized, returning empty result");
            Meta.Frame frame = new Meta.Frame(0, true, Collections.emptyList());
            MetaResultSet resultSet =
                    MetaResultSet.create(h.connectionId, h.id, true, h.signature, frame, -1L);
            return new ExecuteResult(Collections.singletonList(resultSet));
        }

        try {
            // Remove trailing semicolon if present (Calcite doesn't like it)
            String cleanSql = sql.trim();
            if (cleanSql.endsWith(";")) {
                cleanSql = cleanSql.substring(0, cleanSql.length() - 1).trim();
            }
            log.info("Executing query via kernel: {}", cleanSql);
            QueryResult queryResult = kernel.query(cleanSql);
            log.info("Query result - success: {}, columns: {}, rows: {}",
                    queryResult.isSuccess(),
                    queryResult.getColumnNames() != null ? queryResult.getColumnNames().size() : 0,
                    queryResult.getRows() != null ? queryResult.getRows().size() : 0);
            if (!queryResult.isSuccess()) {
                log.error("Query failed: {}", queryResult.getErrorMessage());
            }

            // Build column metadata from query result
            List<ColumnMetaData> columns = new ArrayList<>();
            if (queryResult.getColumnNames() != null) {
                int colIndex = 0;
                for (String columnName : queryResult.getColumnNames()) {
                    ColumnMetaData.Rep rep = ColumnMetaData.Rep.of(String.class);
                    ColumnMetaData.AvaticaType avaticaType =
                            new ColumnMetaData.AvaticaType(java.sql.Types.VARCHAR, "VARCHAR", rep);
                    ColumnMetaData columnMetaData =
                            new ColumnMetaData(colIndex, false, false, false, false,
                                    java.sql.ResultSetMetaData.columnNullableUnknown, true,
                                    -1, columnName, columnName, "", 0, 0, "", "",
                                    avaticaType, true, false, false, "");
                    columns.add(columnMetaData);
                    colIndex++;
                }
            }

            // Build rows from query result
            List<Object> rows = new ArrayList<>();
            if (queryResult.getRows() != null) {
                for (List<Object> row : queryResult.getRows()) {
                    rows.add(new ArrayList<Object>(row));
                }
            }

            log.info("Returning {} columns and {} rows", columns.size(), rows.size());
            Meta.Signature signature = new Meta.Signature(
                    columns, cleanSql, Collections.emptyList(),
                    null, Meta.CursorFactory.ARRAY, Meta.StatementType.SELECT);
            Meta.Frame frame = new Meta.Frame(0, true, rows);
            MetaResultSet resultSet =
                    MetaResultSet.create(h.connectionId, h.id, true, signature, frame, -1L);
            return new ExecuteResult(Collections.singletonList(resultSet));
            // CHECKSTYLE:OFF IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            log.error("Query execution failed: {}", ex.getMessage(), ex);
            Meta.Frame frame = new Meta.Frame(0, true, Collections.emptyList());
            MetaResultSet resultSet =
                    MetaResultSet.create(h.connectionId, h.id, true, h.signature, frame, -1L);
            return new ExecuteResult(Collections.singletonList(resultSet));
        }
    }

    @Override
    public Meta.Frame fetch(final StatementHandle h, final long offset, final int fetchMaxRowCount) throws NoSuchStatementException, MissingResultsException {
        StatementInfo info = statements.get(h.connectionId + ":" + h.id);
        if (info == null) {
            throw new MissingResultsException(h);
        }
        info.getConnection().touch();
        return new Meta.Frame(offset, true, Collections.emptyList());
    }

    @Override
    public boolean syncResults(final StatementHandle h, final QueryState state, final long offset) throws NoSuchStatementException {
        return false;
    }

    @Override
    public void commit(final ConnectionHandle ch) {
    }

    @Override
    public void rollback(final ConnectionHandle ch) {
    }

    @Override
    public ExecuteBatchResult executeBatch(final StatementHandle h, final List<List<TypedValue>> parameterValueLists) throws NoSuchStatementException {
        return new ExecuteBatchResult(new long[0]);
    }

    @Override
    public ExecuteBatchResult prepareAndExecuteBatch(final StatementHandle h, final List<String> sqlCommands) throws NoSuchStatementException {
        return new ExecuteBatchResult(new long[0]);
    }

    /**
     * Creates a signature for the given SQL query.
     *
     * @param sql the SQL query
     * @return the created Signature
     */
    private Meta.Signature createSignature(final String sql) {
        List<ColumnMetaData> columns = new ArrayList<>();
        List<AvaticaParameter> parameters = new ArrayList<>();
        return new Meta.Signature(columns, sql, parameters, null, Meta.CursorFactory.ARRAY, Meta.StatementType.SELECT);
    }

    /**
     * Creates an empty MetaResultSet.
     *
     * @return an empty MetaResultSet
     */
    private MetaResultSet emptyMetaResultSet() {
        List<ColumnMetaData> columns = new ArrayList<>();
        Meta.Signature signature = new Meta.Signature(columns, "", Collections.emptyList(), null, Meta.CursorFactory.ARRAY, Meta.StatementType.SELECT);
        Meta.Frame frame = new Meta.Frame(0, true, Collections.emptyList());
        return MetaResultSet.create("", 0, true, signature, frame, -1L);
    }

    /**
     * Holds information about a statement.
     */
    @Getter
    private static class StatementInfo {

        private final StatementHandle handle;

        private final ServerConnection connection;

        /**
         * Constructs a new StatementInfo.
         *
         * @param handle the statement handle
         * @param connection the server connection
         */
        StatementInfo(final StatementHandle handle, final ServerConnection connection) {
            this.handle = handle;
            this.connection = connection;
        }
    }

    /**
     * Represents a server-side connection.
     */
    @Getter
    private static class ServerConnection {

        private final String id;

        private long lastAccessTime = System.currentTimeMillis();

        private int queryCount;

        /**
         * Constructs a new ServerConnection.
         *
         * @param id the connection identifier
         */
        ServerConnection(final String id) {
            this.id = id;
        }

        /**
         * Updates the last access time to current time.
         */
        void touch() {
            this.lastAccessTime = System.currentTimeMillis();
        }

        /**
         * Increments the query count for this connection.
         */
        void incrementQueryCount() {
            this.queryCount++;
        }
    }
}
