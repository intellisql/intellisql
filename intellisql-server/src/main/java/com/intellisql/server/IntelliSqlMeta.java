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

import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.intellisql.common.metadata.Column;
import com.intellisql.common.metadata.Table;
import com.intellisql.common.metadata.enums.DataType;
import com.intellisql.connector.model.QueryResult;
import com.intellisql.federation.IntelliSqlKernel;
import com.intellisql.federation.metadata.MetadataManager;
import org.apache.calcite.avatica.AvaticaParameter;
import org.apache.calcite.avatica.ColumnMetaData;
import org.apache.calcite.avatica.Meta;
import org.apache.calcite.avatica.MissingResultsException;
import org.apache.calcite.avatica.NoSuchConnectionException;
import org.apache.calcite.avatica.NoSuchStatementException;
import org.apache.calcite.avatica.QueryState;
import org.apache.calcite.avatica.remote.TypedValue;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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
        h.signature = createSignature(sql);
        if (isShowTables(sql)) {
            return handleShowTables(h);
        }
        if (isShowColumns(sql)) {
            return handleShowColumns(h, sql);
        }
        return isUpdateStatement(sql) ? executeUpdate(h, sql) : executeQuery(h, sql, maxRowCount);
    }

    @Override
    public ExecuteResult prepareAndExecute(final StatementHandle h, final String sql, final long maxRowCount, final int batchSize, final PrepareCallback callback) throws NoSuchStatementException {
        return prepareAndExecute(h, sql, maxRowCount, callback);
    }

    private ExecuteResult handleShowTables(final StatementHandle h) {
        log.info("handleShowTables called, metadataManager={}", metadataManager);
        List<ColumnMetaData> columns = new ArrayList<>();
        columns.add(createStringColumn(0, "TABLE_NAME"));
        List<Object> rows = new ArrayList<>();
        if (metadataManager != null) {
            log.info("Getting tables from metadataManager, current count: {}", metadataManager.getAllTables().size());
            for (Table table : metadataManager.getAllTables()) {
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

    private ExecuteResult handleShowColumns(final StatementHandle h, final String sql) {
        List<ColumnMetaData> columns = createShowColumnsMetadata();
        List<Object> rows = new ArrayList<>();
        String tableName = extractShowColumnsTable(sql);
        if (metadataManager != null && tableName != null) {
            for (Table table : metadataManager.getAllTables()) {
                if (tableName.equalsIgnoreCase(table.getName())) {
                    addColumnRows(rows, table);
                    break;
                }
            }
        }
        Meta.Signature signature = new Meta.Signature(
                columns, sql, Collections.emptyList(),
                null, Meta.CursorFactory.ARRAY, Meta.StatementType.SELECT);
        Meta.Frame frame = new Meta.Frame(0, true, rows);
        MetaResultSet resultSet =
                MetaResultSet.create(h.connectionId, h.id, true, signature, frame, -1L);
        return new ExecuteResult(Collections.singletonList(resultSet));
    }

    private List<ColumnMetaData> createShowColumnsMetadata() {
        List<ColumnMetaData> result = new ArrayList<>();
        result.add(createStringColumn(0, "COLUMN_NAME"));
        result.add(createStringColumn(1, "DATA_TYPE"));
        result.add(createStringColumn(2, "TYPE_NAME"));
        result.add(createStringColumn(3, "ORDINAL_POSITION"));
        result.add(createStringColumn(4, "IS_NULLABLE"));
        return result;
    }

    private ColumnMetaData createStringColumn(final int index, final String name) {
        ColumnMetaData.Rep rep = ColumnMetaData.Rep.of(String.class);
        ColumnMetaData.AvaticaType avaticaType =
                new ColumnMetaData.AvaticaType(Types.VARCHAR, "VARCHAR", rep);
        return new ColumnMetaData(index, false, false, false, false,
                ResultSetMetaData.columnNullableUnknown, true,
                -1, name, name, "", 0, 0, "", "", avaticaType, true, false, false, "");
    }

    private void addColumnRows(final List<Object> rows, final Table table) {
        int position = 1;
        for (Column each : table.getColumns()) {
            List<Object> row = new ArrayList<>(5);
            row.add(each.getName());
            row.add(String.valueOf(Types.VARCHAR));
            row.add(each.getDataType() == null ? "VARCHAR" : each.getDataType().name());
            row.add(String.valueOf(position));
            row.add(each.isNullable() ? "YES" : "NO");
            rows.add(row);
            position++;
        }
    }

    @Override
    public ExecuteResult execute(final StatementHandle h, final List<TypedValue> parameterValues, final long maxRowCount) throws NoSuchStatementException {
        StatementInfo info = statements.get(h.connectionId + ":" + h.id);
        if (info == null) {
            throw new NoSuchStatementException(h);
        }
        info.getConnection().incrementQueryCount();
        info.getConnection().touch();
        String sql = null;
        if (h.signature != null) {
            sql = h.signature.sql;
        }
        log.info("Execute called with SQL: {}", sql);
        if (sql != null) {
            String executableSql = bindParameters(sql, parameterValues);
            if (isShowTables(executableSql)) {
                return handleShowTables(h);
            }
            if (isShowColumns(executableSql)) {
                return handleShowColumns(h, executableSql);
            }
            return isUpdateStatement(executableSql) ? executeUpdate(h, executableSql) : executeQuery(h, executableSql, maxRowCount);
        }
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

    private boolean isShowTables(final String sql) {
        return sql.trim().toUpperCase(Locale.ENGLISH).startsWith("SHOW TABLES");
    }

    private boolean isShowColumns(final String sql) {
        return sql.trim().toUpperCase(Locale.ENGLISH).startsWith("SHOW COLUMNS FROM ");
    }

    private String extractShowColumnsTable(final String sql) {
        String cleanSql = cleanSql(sql);
        String prefix = "SHOW COLUMNS FROM ";
        if (cleanSql.length() <= prefix.length()) {
            return null;
        }
        String result = cleanSql.substring(prefix.length()).trim();
        if (isQuotedIdentifier(result, "\"") || isQuotedIdentifier(result, "`")) {
            return result.substring(1, result.length() - 1);
        }
        return result;
    }

    private boolean isQuotedIdentifier(final String value, final String quote) {
        return value.startsWith(quote) && value.endsWith(quote);
    }

    private boolean isUpdateStatement(final String sql) {
        String normalizedSql = sql.trim().toUpperCase(Locale.ENGLISH);
        return normalizedSql.startsWith("INSERT ")
                || normalizedSql.startsWith("UPDATE ")
                || normalizedSql.startsWith("DELETE ");
    }

    private String bindParameters(final String sql, final List<TypedValue> parameterValues) {
        if (parameterValues == null || parameterValues.isEmpty()) {
            return sql;
        }
        List<Object> values = toJdbcParameterValues(parameterValues);
        StringBuilder result = new StringBuilder(sql.length() + values.size() * 8);
        int parameterIndex = 0;
        boolean quoted = false;
        int i = 0;
        while (i < sql.length()) {
            char each = sql.charAt(i);
            if ('\'' == each) {
                result.append(each);
                if (quoted && i + 1 < sql.length() && '\'' == sql.charAt(i + 1)) {
                    result.append(sql.charAt(i + 1));
                    i += 2;
                } else {
                    quoted = !quoted;
                    i++;
                }
            } else if ('?' == each && !quoted) {
                if (parameterIndex >= values.size()) {
                    throw new IllegalArgumentException("Missing parameter value for index " + (parameterIndex + 1));
                }
                result.append(toSqlLiteral(values.get(parameterIndex)));
                parameterIndex++;
                i++;
            } else {
                result.append(each);
                i++;
            }
        }
        if (parameterIndex < values.size()) {
            throw new IllegalArgumentException("Too many parameter values: " + values.size());
        }
        return result.toString();
    }

    private List<Object> toJdbcParameterValues(final List<TypedValue> parameterValues) {
        List<Object> result = new ArrayList<>(parameterValues.size());
        Calendar calendar = Calendar.getInstance();
        for (TypedValue each : parameterValues) {
            result.add(each == null ? null : each.toJdbc(calendar));
        }
        return result;
    }

    private String toSqlLiteral(final Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return Boolean.TRUE.equals(value) ? "TRUE" : "FALSE";
        }
        if (value instanceof Date || value instanceof Time || value instanceof Timestamp) {
            return quote(value.toString());
        }
        return quote(value.toString());
    }

    private String quote(final String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private ExecuteResult executeQuery(final StatementHandle h, final String sql, final long maxRowCount) {
        if (kernel == null) {
            log.warn("Kernel not initialized, returning empty result");
            Meta.Frame frame = new Meta.Frame(0, true, Collections.emptyList());
            MetaResultSet resultSet =
                    MetaResultSet.create(h.connectionId, h.id, true, h.signature, frame, -1L);
            return new ExecuteResult(Collections.singletonList(resultSet));
        }
        try {
            String cleanSql = cleanSql(sql);
            log.info("Executing query via kernel: {}", cleanSql);
            QueryResult queryResult = kernel.query(cleanSql);
            log.info("Query result - success: {}, columns: {}, rows: {}",
                    queryResult.isSuccess(),
                    queryResult.getColumnNames() != null ? queryResult.getColumnNames().size() : 0,
                    queryResult.getRows() != null ? queryResult.getRows().size() : 0);
            if (!queryResult.isSuccess()) {
                log.error("Query failed: {}", queryResult.getErrorMessage());
            }
            List<ColumnMetaData> columns = new ArrayList<>();
            if (queryResult.getColumnNames() != null) {
                int colIndex = 0;
                for (String columnName : queryResult.getColumnNames()) {
                    columns.add(createQueryColumnMetaData(colIndex, columnName, getQueryColumnType(queryResult, colIndex)));
                    colIndex++;
                }
            }
            List<Object> rows = new ArrayList<>();
            if (queryResult.getRows() != null) {
                for (List<Object> row : queryResult.getRows()) {
                    rows.add(convertQueryRow(queryResult, row));
                }
            }
            log.info("Returning {} columns and {} rows", columns.size(), rows.size());
            Meta.Signature signature = new Meta.Signature(
                    columns, cleanSql, Collections.emptyList(),
                    null, Meta.CursorFactory.ARRAY, Meta.StatementType.SELECT);
            h.signature = signature;
            StatementInfo info = statements.get(h.connectionId + ":" + h.id);
            if (info != null) {
                info.setResult(signature, rows);
            }
            Meta.Frame frame = createFrame(rows, 0L, maxRowCount);
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

    private DataType getQueryColumnType(final QueryResult queryResult, final int columnIndex) {
        return queryResult.getColumnTypes() != null && columnIndex < queryResult.getColumnTypes().size()
                ? queryResult.getColumnTypes().get(columnIndex)
                : DataType.STRING;
    }

    private ColumnMetaData createQueryColumnMetaData(final int index, final String name, final DataType dataType) {
        ColumnMetaData.Rep rep = ColumnMetaData.Rep.of(getColumnJavaClass(dataType));
        ColumnMetaData.AvaticaType avaticaType = new ColumnMetaData.AvaticaType(getColumnSqlType(dataType), getColumnTypeName(dataType), rep);
        return new ColumnMetaData(index, false, false, false, false,
                ResultSetMetaData.columnNullableUnknown, true,
                getColumnPrecision(dataType), name, name, "", getColumnScale(dataType), 0, "", "",
                avaticaType, true, false, false, getColumnJavaClass(dataType).getName());
    }

    private Class<?> getColumnJavaClass(final DataType dataType) {
        if (dataType == null) {
            return String.class;
        }
        switch (dataType) {
            case INTEGER:
                return Integer.class;
            case LONG:
                return Long.class;
            case DOUBLE:
                return Double.class;
            case BOOLEAN:
                return Boolean.class;
            case DATE:
                return Date.class;
            case TIMESTAMP:
                return Timestamp.class;
            case BINARY:
                return byte[].class;
            default:
                return String.class;
        }
    }

    private int getColumnSqlType(final DataType dataType) {
        if (dataType == null) {
            return Types.VARCHAR;
        }
        switch (dataType) {
            case INTEGER:
                return Types.INTEGER;
            case LONG:
                return Types.BIGINT;
            case DOUBLE:
                return Types.DOUBLE;
            case BOOLEAN:
                return Types.BOOLEAN;
            case DATE:
                return Types.DATE;
            case TIMESTAMP:
                return Types.TIMESTAMP;
            case BINARY:
                return Types.VARBINARY;
            default:
                return Types.VARCHAR;
        }
    }

    private String getColumnTypeName(final DataType dataType) {
        if (dataType == null) {
            return "VARCHAR";
        }
        switch (dataType) {
            case INTEGER:
                return "INTEGER";
            case LONG:
                return "BIGINT";
            case DOUBLE:
                return "DOUBLE";
            case BOOLEAN:
                return "BOOLEAN";
            case DATE:
                return "DATE";
            case TIMESTAMP:
                return "TIMESTAMP";
            case BINARY:
                return "VARBINARY";
            default:
                return "VARCHAR";
        }
    }

    private int getColumnPrecision(final DataType dataType) {
        if (DataType.DATE == dataType) {
            return 10;
        }
        if (DataType.TIMESTAMP == dataType) {
            return 26;
        }
        return -1;
    }

    private int getColumnScale(final DataType dataType) {
        return DataType.DOUBLE == dataType ? 9 : 0;
    }

    private List<Object> convertQueryRow(final QueryResult queryResult, final List<Object> row) {
        List<Object> result = new ArrayList<>(row.size());
        for (int i = 0; i < row.size(); i++) {
            result.add(convertQueryValue(row.get(i), getQueryColumnType(queryResult, i)));
        }
        return result;
    }

    private Object convertQueryValue(final Object value, final DataType dataType) {
        if (value == null) {
            return null;
        }
        if (DataType.INTEGER == dataType && value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (DataType.LONG == dataType && value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (DataType.DOUBLE == dataType && value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (DataType.DATE == dataType && value instanceof Date) {
            return (int) ((Date) value).toLocalDate().toEpochDay();
        }
        if (DataType.TIMESTAMP == dataType && value instanceof Timestamp) {
            return ((Timestamp) value).getTime();
        }
        if (DataType.TIMESTAMP == dataType && value instanceof Time) {
            return (int) (((Time) value).toLocalTime().toSecondOfDay() * 1000L);
        }
        return value;
    }

    private ExecuteResult executeUpdate(final StatementHandle h, final String sql) {
        if (kernel == null) {
            log.warn("Kernel not initialized, returning empty update count");
            return new ExecuteResult(Collections.singletonList(MetaResultSet.count(h.connectionId, h.id, 0L)));
        }
        try {
            String cleanSql = cleanSql(sql);
            int updateCount = kernel.executeUpdate(cleanSql);
            Meta.Signature signature = new Meta.Signature(
                    Collections.emptyList(), cleanSql, Collections.emptyList(),
                    null, Meta.CursorFactory.ARRAY, getStatementType(cleanSql));
            h.signature = signature;
            StatementInfo info = statements.get(h.connectionId + ":" + h.id);
            if (info != null) {
                info.setResult(signature, Collections.emptyList());
            }
            return new ExecuteResult(Collections.singletonList(MetaResultSet.count(h.connectionId, h.id, updateCount)));
            // CHECKSTYLE:OFF IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            log.error("Update execution failed: {}", ex.getMessage(), ex);
            return new ExecuteResult(Collections.singletonList(MetaResultSet.count(h.connectionId, h.id, -1L)));
        }
    }

    private String cleanSql(final String sql) {
        String result = sql.trim();
        return result.endsWith(";") ? result.substring(0, result.length() - 1).trim() : result;
    }

    @Override
    public Meta.Frame fetch(final StatementHandle h, final long offset, final int fetchMaxRowCount) throws NoSuchStatementException, MissingResultsException {
        StatementInfo info = statements.get(h.connectionId + ":" + h.id);
        if (info == null) {
            throw new MissingResultsException(h);
        }
        info.getConnection().touch();
        return createFrame(info.getRows(), offset, fetchMaxRowCount);
    }

    private Meta.Frame createFrame(final List<Object> rows, final long offset, final long fetchMaxRowCount) {
        if (rows == null || rows.isEmpty() || offset >= rows.size()) {
            return new Meta.Frame(offset, true, Collections.emptyList());
        }
        int start = (int) Math.max(0L, offset);
        int end = getFrameEnd(rows.size(), start, fetchMaxRowCount);
        List<Object> frameRows = new ArrayList<>(Math.max(end - start, 0));
        for (int i = start; i < end; i++) {
            frameRows.add(rows.get(i));
        }
        return new Meta.Frame(offset, end >= rows.size(), frameRows);
    }

    private int getFrameEnd(final int rowCount, final int start, final long fetchMaxRowCount) {
        if (fetchMaxRowCount <= 0) {
            return rowCount;
        }
        long requestedEnd = start + fetchMaxRowCount;
        return requestedEnd > rowCount ? rowCount : (int) requestedEnd;
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
        List<AvaticaParameter> parameters = createParameters(sql);
        return new Meta.Signature(columns, sql, parameters, null, Meta.CursorFactory.ARRAY, getStatementType(sql));
    }

    private List<AvaticaParameter> createParameters(final String sql) {
        int parameterCount = countParameters(sql);
        List<AvaticaParameter> result = new ArrayList<>(parameterCount);
        for (int i = 0; i < parameterCount; i++) {
            result.add(new AvaticaParameter(false, 0, 0, Types.VARCHAR, "VARCHAR", String.class.getName(), "p" + (i + 1)));
        }
        return result;
    }

    private int countParameters(final String sql) {
        int result = 0;
        boolean quoted = false;
        int i = 0;
        while (i < sql.length()) {
            char each = sql.charAt(i);
            if ('\'' == each) {
                if (quoted && i + 1 < sql.length() && '\'' == sql.charAt(i + 1)) {
                    i += 2;
                } else {
                    quoted = !quoted;
                    i++;
                }
            } else if ('?' == each && !quoted) {
                result++;
                i++;
            } else {
                i++;
            }
        }
        return result;
    }

    private Meta.StatementType getStatementType(final String sql) {
        String normalizedSql = sql.trim().toUpperCase(Locale.ENGLISH);
        if (normalizedSql.startsWith("INSERT ")) {
            return Meta.StatementType.INSERT;
        }
        if (normalizedSql.startsWith("UPDATE ")) {
            return Meta.StatementType.UPDATE;
        }
        if (normalizedSql.startsWith("DELETE ")) {
            return Meta.StatementType.DELETE;
        }
        return Meta.StatementType.SELECT;
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

        private Meta.Signature signature;

        private List<Object> rows = Collections.emptyList();

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

        void setResult(final Meta.Signature signature, final List<Object> rows) {
            this.signature = signature;
            this.rows = rows;
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
