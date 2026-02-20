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

package com.intellisql.connector.postgresql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.intellisql.connector.api.SchemaDiscoverer;
import com.intellisql.connector.enums.DataType;
import com.intellisql.connector.enums.IndexType;
import com.intellisql.connector.enums.SchemaType;
import com.intellisql.connector.enums.TableType;
import com.intellisql.connector.model.Column;
import com.intellisql.connector.model.Index;
import com.intellisql.connector.model.Schema;
import com.intellisql.connector.model.Table;

import lombok.extern.slf4j.Slf4j;

/**
 * PostgreSQL implementation of SchemaDiscoverer. Uses pg_catalog and DatabaseMetaData to discover
 * schema information.
 */
@Slf4j
public class PostgreSQLSchemaDiscoverer implements SchemaDiscoverer {

    @Override
    public Schema discoverSchema(final Connection connection, final String schemaName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        String effectiveSchema = schemaName != null ? schemaName : "public";
        Schema schema = discoverTables(connection, effectiveSchema, null);
        for (Table table : schema.getTables()) {
            discoverColumnsForTable(metaData, effectiveSchema, table);
            discoverPrimaryKeys(metaData, effectiveSchema, table);
            discoverIndexes(metaData, effectiveSchema, table);
        }
        return schema;
    }

    @Override
    public Schema discoverTables(final Connection connection, final String schemaName, final String tableNamePattern) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        String effectiveSchema = schemaName != null ? schemaName : "public";
        String[] tableTypes = {"TABLE", "VIEW"};
        List<Table> tables = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(null, effectiveSchema, tableNamePattern, tableTypes)) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String tableType = rs.getString("TABLE_TYPE");
                String remarks = rs.getString("REMARKS");
                Table table =
                        Table.builder()
                                .name(tableName)
                                .schema(effectiveSchema)
                                .catalog(connection.getCatalog())
                                .type("VIEW".equalsIgnoreCase(tableType) ? TableType.VIEW : TableType.TABLE)
                                .remarks(remarks)
                                .build();
                tables.add(table);
                log.debug("Discovered PostgreSQL table: {}", tableName);
            }
        }
        return Schema.builder()
                .name(effectiveSchema)
                .catalog(connection.getCatalog())
                .type(SchemaType.PHYSICAL)
                .tables(tables)
                .build();
    }

    @Override
    public Schema discoverColumns(final Connection connection, final String schemaName, final String tableName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        String effectiveSchema = schemaName != null ? schemaName : "public";
        Schema schema = discoverTables(connection, effectiveSchema, tableName);
        for (Table table : schema.getTables()) {
            discoverColumnsForTable(metaData, effectiveSchema, table);
        }
        return schema;
    }

    private void discoverColumnsForTable(final DatabaseMetaData metaData, final String schemaName, final Table table) throws Exception {
        try (ResultSet rs = metaData.getColumns(null, schemaName, table.getName(), null)) {
            int position = 0;
            while (rs.next()) {
                String nativeType = rs.getString("TYPE_NAME");
                Column column =
                        Column.builder()
                                .name(rs.getString("COLUMN_NAME"))
                                .tableName(table.getName())
                                .schemaName(schemaName)
                                .dataType(mapPostgreSQLTypeToDataType(nativeType, rs.getInt("DATA_TYPE")))
                                .nativeType(nativeType)
                                .columnSize(rs.getInt("COLUMN_SIZE"))
                                .decimalDigits(rs.getInt("DECIMAL_DIGITS"))
                                .nullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")))
                                .defaultValue(rs.getString("COLUMN_DEF"))
                                .remarks(rs.getString("REMARKS"))
                                .ordinalPosition(++position)
                                .build();
                table.addColumn(column);
            }
        }
    }

    private void discoverPrimaryKeys(final DatabaseMetaData metaData, final String schemaName, final Table table) throws Exception {
        try (ResultSet rs = metaData.getPrimaryKeys(null, schemaName, table.getName())) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                Column column = table.getColumn(columnName);
                if (column != null) {
                    column.setPrimaryKey(true);
                }
            }
        }
    }

    private void discoverIndexes(final DatabaseMetaData metaData, final String schemaName, final Table table) throws Exception {
        Set<String> processedIndexes = new HashSet<>();
        try (ResultSet rs = metaData.getIndexInfo(null, schemaName, table.getName(), false, true)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName == null || processedIndexes.contains(indexName)) {
                    continue;
                }
                processedIndexes.add(indexName);
                Index index =
                        Index.builder()
                                .name(indexName)
                                .tableName(table.getName())
                                .schemaName(schemaName)
                                .type(mapIndexType(rs.getString("INDEX_TYPE")))
                                .unique(!rs.getBoolean("NON_UNIQUE"))
                                .build();
                addIndexColumns(metaData, schemaName, table, indexName, index);
                table.addIndex(index);
            }
        }
    }

    private void addIndexColumns(
                                 final DatabaseMetaData metaData, final String schemaName, final Table table,
                                 final String indexName, final Index index) throws Exception {
        try (ResultSet rs = metaData.getIndexInfo(null, schemaName, table.getName(), false, true)) {
            while (rs.next()) {
                if (indexName.equals(rs.getString("INDEX_NAME"))) {
                    String columnName = rs.getString("COLUMN_NAME");
                    if (columnName != null) {
                        index.addColumnName(columnName);
                    }
                }
            }
        }
    }

    private DataType mapPostgreSQLTypeToDataType(final String nativeType, final int sqlType) {
        if (nativeType == null) {
            return mapSqlTypeToDataType(sqlType);
        }
        String upperType = nativeType.toUpperCase();
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
        String upperType = indexType.toUpperCase();
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
