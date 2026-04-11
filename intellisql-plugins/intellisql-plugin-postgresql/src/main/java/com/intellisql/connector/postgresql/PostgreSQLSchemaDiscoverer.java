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

package com.intellisql.connector.postgresql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.intellisql.common.metadata.Column;
import com.intellisql.common.metadata.Index;
import com.intellisql.common.metadata.Schema;
import com.intellisql.common.metadata.Table;
import com.intellisql.common.metadata.enums.DataType;
import com.intellisql.common.metadata.enums.IndexType;
import com.intellisql.common.metadata.enums.SchemaType;
import com.intellisql.common.metadata.enums.TableType;
import com.intellisql.connector.api.SchemaDiscoverer;

import lombok.extern.slf4j.Slf4j;

/**
 * PostgreSQL implementation of SchemaDiscoverer. Uses pg_catalog and DatabaseMetaData to discover
 * schema information.
 */
@Slf4j
public class PostgreSQLSchemaDiscoverer implements SchemaDiscoverer {

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema discoverSchema(final Connection connection, final String schemaName,
                                 final String dataSourceName) throws Exception {
        final DatabaseMetaData metaData = connection.getMetaData();
        final String effectiveSchema = schemaName != null ? schemaName : "public";
        final Schema schema = discoverTables(connection, effectiveSchema, null);
        final Map<String, Table> discoveredTables = new LinkedHashMap<>();
        for (final Table table : schema.getTables().values()) {
            Table discoveredTable = discoverColumnsForTable(metaData, effectiveSchema, table);
            discoveredTable = discoverPrimaryKeys(metaData, effectiveSchema, discoveredTable);
            discoveredTable = discoverIndexes(metaData, effectiveSchema, discoveredTable);
            discoveredTables.put(
                    discoveredTable.getName(),
                    discoveredTable.toBuilder().dataSourceId(dataSourceName).build());
        }
        return schema.toBuilder().dataSourceId(dataSourceName).tables(discoveredTables).build();
    }

    @Override
    public Schema discoverTables(final Connection connection, final String schemaName, final String tableNamePattern) throws Exception {
        final DatabaseMetaData metaData = connection.getMetaData();
        final String effectiveSchema = schemaName != null ? schemaName : "public";
        final String[] tableTypes = {"TABLE", "VIEW"};
        final Map<String, Table> tables = new LinkedHashMap<>();
        try (ResultSet rs = metaData.getTables(null, effectiveSchema, tableNamePattern, tableTypes)) {
            while (rs.next()) {
                final String tableName = rs.getString("TABLE_NAME");
                final String tableType = rs.getString("TABLE_TYPE");
                final String remarks = rs.getString("REMARKS");
                final Table table =
                        Table.builder()
                                .name(tableName)
                                .schemaName(effectiveSchema)
                                .columns(new ArrayList<Column>())
                                .primaryKey(new ArrayList<String>())
                                .indexes(new ArrayList<Index>())
                                .type("VIEW".equalsIgnoreCase(tableType) ? TableType.VIEW : TableType.TABLE)
                                .metadata(tableMetadata(connection.getCatalog(), remarks))
                                .build();
                tables.put(tableName, table);
                log.debug("Discovered PostgreSQL table: {}", tableName);
            }
        }
        return Schema.builder()
                .name(effectiveSchema)
                .type(SchemaType.PHYSICAL)
                .tables(tables)
                .metadata(schemaMetadata(connection.getCatalog()))
                .build();
    }

    @Override
    public Schema discoverColumns(final Connection connection, final String schemaName, final String tableName) throws Exception {
        final DatabaseMetaData metaData = connection.getMetaData();
        final String effectiveSchema = schemaName != null ? schemaName : "public";
        final Schema schema = discoverTables(connection, effectiveSchema, tableName);
        final Map<String, Table> discoveredTables = new LinkedHashMap<>();
        for (final Table table : schema.getTables().values()) {
            final Table discoveredTable = discoverColumnsForTable(metaData, effectiveSchema, table);
            discoveredTables.put(discoveredTable.getName(), discoveredTable);
        }
        return schema.toBuilder().tables(discoveredTables).build();
    }

    private Table discoverColumnsForTable(final DatabaseMetaData metaData, final String schemaName, final Table table) throws Exception {
        final List<Column> columns = new ArrayList<>();
        try (ResultSet rs = metaData.getColumns(null, schemaName, table.getName(), null)) {
            int position = 0;
            while (rs.next()) {
                final String nativeType = rs.getString("TYPE_NAME");
                final int columnSize = rs.getInt("COLUMN_SIZE");
                final int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                final Column column =
                        Column.builder()
                                .name(rs.getString("COLUMN_NAME"))
                                .dataType(mapPostgreSQLTypeToDataType(nativeType, rs.getInt("DATA_TYPE")))
                                .nullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")))
                                .defaultValue(rs.getString("COLUMN_DEF"))
                                .comment(rs.getString("REMARKS"))
                                .size(columnSize)
                                .precision(columnSize)
                                .scale(decimalDigits)
                                .metadata(columnMetadata(
                                        table.getName(),
                                        schemaName,
                                        nativeType,
                                        false,
                                        isAutoIncrement(rs),
                                        ++position))
                                .build();
                columns.add(column);
            }
        }
        return table.toBuilder().columns(columns).build();
    }

    private Table discoverPrimaryKeys(final DatabaseMetaData metaData, final String schemaName, final Table table) throws Exception {
        final List<String> primaryKey = new ArrayList<>();
        try (ResultSet rs = metaData.getPrimaryKeys(null, schemaName, table.getName())) {
            while (rs.next()) {
                primaryKey.add(rs.getString("COLUMN_NAME"));
            }
        }

        final List<Column> columns = new ArrayList<>();
        for (final Column column : table.getColumns()) {
            final boolean isPrimaryKey = primaryKey.contains(column.getName());
            columns.add(column.toBuilder().metadata(
                    updateMetadata(column.getMetadata(), "primaryKey", String.valueOf(isPrimaryKey))).build());
        }
        return table.toBuilder().columns(columns).primaryKey(primaryKey).build();
    }

    private Table discoverIndexes(final DatabaseMetaData metaData, final String schemaName, final Table table) throws Exception {
        final Map<String, List<String>> indexColumns = new LinkedHashMap<>();
        final Map<String, IndexType> indexTypes = new LinkedHashMap<>();
        final Map<String, Boolean> uniqueFlags = new LinkedHashMap<>();

        try (ResultSet rs = metaData.getIndexInfo(null, schemaName, table.getName(), false, true)) {
            while (rs.next()) {
                final String indexName = rs.getString("INDEX_NAME");
                if (indexName == null) {
                    continue;
                }
                indexColumns.computeIfAbsent(indexName, ignored -> new ArrayList<String>());
                final String columnName = rs.getString("COLUMN_NAME");
                if (columnName != null) {
                    indexColumns.get(indexName).add(columnName);
                }
                uniqueFlags.put(indexName, !rs.getBoolean("NON_UNIQUE"));
                indexTypes.put(indexName, mapIndexType(rs.getString("INDEX_TYPE")));
            }
        }

        final List<Index> indexes = new ArrayList<>();
        for (final String indexName : indexColumns.keySet()) {
            indexes.add(Index.builder()
                    .name(indexName)
                    .columns(indexColumns.get(indexName))
                    .unique(Boolean.TRUE.equals(uniqueFlags.get(indexName)))
                    .type(indexTypes.get(indexName))
                    .build());
        }
        return table.toBuilder().indexes(indexes).build();
    }

    private boolean isAutoIncrement(final ResultSet rs) throws SQLException {
        return "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"));
    }

    private Map<String, String> schemaMetadata(final String catalog) {
        return singleMetadata("catalog", catalog);
    }

    private Map<String, String> tableMetadata(final String catalog, final String remarks) {
        final Map<String, String> metadata = new LinkedHashMap<>();
        putIfNotBlank(metadata, "catalog", catalog);
        putIfNotBlank(metadata, "remarks", remarks);
        return metadata.isEmpty() ? null : metadata;
    }

    private Map<String, String> columnMetadata(
                                               final String tableName,
                                               final String schemaName,
                                               final String nativeType,
                                               final boolean primaryKey,
                                               final boolean autoIncrement,
                                               final int ordinalPosition) {
        final Map<String, String> metadata = new LinkedHashMap<>();
        putIfNotBlank(metadata, "tableName", tableName);
        putIfNotBlank(metadata, "schemaName", schemaName);
        putIfNotBlank(metadata, "nativeType", nativeType);
        metadata.put("primaryKey", String.valueOf(primaryKey));
        metadata.put("autoIncrement", String.valueOf(autoIncrement));
        metadata.put("ordinalPosition", String.valueOf(ordinalPosition));
        return metadata;
    }

    private Map<String, String> updateMetadata(
                                               final Map<String, String> metadata, final String key, final String value) {
        final Map<String, String> updated = new LinkedHashMap<>();
        if (metadata != null) {
            updated.putAll(metadata);
        }
        updated.put(key, value);
        return updated;
    }

    private Map<String, String> singleMetadata(final String key, final String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        final Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put(key, value);
        return metadata;
    }

    private void putIfNotBlank(final Map<String, String> metadata, final String key, final String value) {
        if (value != null && !value.isEmpty()) {
            metadata.put(key, value);
        }
    }

    private DataType mapPostgreSQLTypeToDataType(final String nativeType, final int sqlType) {
        if (nativeType == null) {
            return mapSqlTypeToDataType(sqlType);
        }
        final String upperType = nativeType.toUpperCase();
        if (upperType.startsWith("_")) {
            return DataType.ARRAY;
        }
        switch (upperType) {
            case "INT4":
            case "INTEGER":
            case "SMALLINT":
            case "INT2":
                return DataType.INTEGER;
            case "INT8":
            case "BIGINT":
                return DataType.LONG;
            case "FLOAT4":
            case "REAL":
                return DataType.DOUBLE;
            case "FLOAT8":
            case "DOUBLE PRECISION":
            case "NUMERIC":
            case "DECIMAL":
                return DataType.DOUBLE;
            case "BOOL":
            case "BOOLEAN":
                return DataType.BOOLEAN;
            case "DATE":
                return DataType.DATE;
            case "TIMESTAMP":
            case "TIMESTAMPTZ":
            case "TIME":
            case "TIMETZ":
                return DataType.TIMESTAMP;
            case "BYTEA":
                return DataType.BINARY;
            case "JSON":
            case "JSONB":
                return DataType.JSON;
            case "VARCHAR":
            case "CHAR":
            case "TEXT":
            case "BPCHAR":
            case "NAME":
            default:
                return mapSqlTypeToDataType(sqlType);
        }
    }

    private DataType mapSqlTypeToDataType(final int sqlType) {
        switch (sqlType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
                return DataType.STRING;
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
                return DataType.INTEGER;
            case Types.BIGINT:
                return DataType.LONG;
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
                return DataType.DOUBLE;
            case Types.BIT:
            case Types.BOOLEAN:
                return DataType.BOOLEAN;
            case Types.DATE:
                return DataType.DATE;
            case Types.TIMESTAMP:
            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return DataType.TIMESTAMP;
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
                return DataType.BINARY;
            case Types.ARRAY:
                return DataType.ARRAY;
            default:
                return DataType.STRING;
        }
    }

    private IndexType mapIndexType(final String indexType) {
        if (indexType == null) {
            return IndexType.BTREE;
        }
        final String upperType = indexType.toUpperCase();
        if (upperType.contains("HASH")) {
            return IndexType.HASH;
        } else if (upperType.contains("GIN") || upperType.contains("GI")) {
            return IndexType.GI;
        } else if (upperType.contains("SPATIAL") || upperType.contains("GIST")) {
            return IndexType.SPATIAL;
        } else {
            return IndexType.BTREE;
        }
    }
}
