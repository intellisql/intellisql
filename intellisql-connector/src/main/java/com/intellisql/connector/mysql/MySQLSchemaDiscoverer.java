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

package com.intellisql.connector.mysql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
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
 * MySQL implementation of SchemaDiscoverer. Uses DatabaseMetaData to discover schema information.
 */
@Slf4j
public class MySQLSchemaDiscoverer implements SchemaDiscoverer {

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema discoverSchema(final Connection connection, final String schemaName,
                                 final String dataSourceName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = schemaName != null ? schemaName : connection.getCatalog();
        Schema schema = discoverTables(connection, schemaName, null);
        for (Table table : schema.getTables()) {
            discoverColumnsForTable(metaData, catalog, table);
            discoverPrimaryKeys(metaData, catalog, table);
            discoverIndexes(metaData, catalog, table);
        }
        // Use the data source configuration name, not the catalog name
        return schema.toBuilder().dataSourceName(dataSourceName).build();
    }

    @Override
    public Schema discoverTables(final Connection connection, final String schemaName, final String tableNamePattern) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = schemaName != null ? schemaName : connection.getCatalog();
        String[] tableTypes = {"TABLE", "VIEW"};
        List<Table> tables = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(catalog, schemaName, tableNamePattern, tableTypes)) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String tableType = rs.getString("TABLE_TYPE");
                String remarks = rs.getString("REMARKS");
                Table table =
                        Table.builder()
                                .name(tableName)
                                .schema(schemaName)
                                .catalog(catalog)
                                .type("VIEW".equalsIgnoreCase(tableType) ? TableType.VIEW : TableType.TABLE)
                                .remarks(remarks)
                                .build();
                tables.add(table);
                log.debug("Discovered table: {}", tableName);
            }
        }
        return Schema.builder()
                .name(schemaName != null ? schemaName : catalog)
                .catalog(catalog)
                .type(SchemaType.PHYSICAL)
                .tables(tables)
                .build();
    }

    @Override
    public Schema discoverColumns(final Connection connection, final String schemaName, final String tableName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = schemaName != null ? schemaName : connection.getCatalog();
        Schema schema = discoverTables(connection, schemaName, tableName);
        for (Table table : schema.getTables()) {
            discoverColumnsForTable(metaData, catalog, table);
        }
        return schema;
    }

    private void discoverColumnsForTable(final DatabaseMetaData metaData, final String catalog, final Table table) throws Exception {
        try (ResultSet rs = metaData.getColumns(catalog, table.getSchema(), table.getName(), null)) {
            int position = 0;
            while (rs.next()) {
                Column column =
                        Column.builder()
                                .name(rs.getString("COLUMN_NAME"))
                                .tableName(table.getName())
                                .schemaName(table.getSchema())
                                .dataType(mapSqlTypeToDataType(rs.getInt("DATA_TYPE")))
                                .nativeType(rs.getString("TYPE_NAME"))
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

    private void discoverPrimaryKeys(final DatabaseMetaData metaData, final String catalog, final Table table) throws Exception {
        try (ResultSet rs = metaData.getPrimaryKeys(catalog, table.getSchema(), table.getName())) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                Column column = table.getColumn(columnName);
                if (column != null) {
                    column.setPrimaryKey(true);
                }
            }
        }
    }

    private void discoverIndexes(final DatabaseMetaData metaData, final String catalog, final Table table) throws Exception {
        Set<String> processedIndexes = new HashSet<>();
        try (
                ResultSet rs =
                        metaData.getIndexInfo(catalog, table.getSchema(), table.getName(), false, true)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName == null || processedIndexes.contains(indexName)) {
                    continue;
                }
                processedIndexes.add(indexName);
                // INDEX_TYPE column may not exist in some MySQL versions
                String indexType = null;
                try {
                    indexType = rs.getString("INDEX_TYPE");
                } catch (final SQLException ex) {
                    // Column not found, use default
                }
                Index index =
                        Index.builder()
                                .name(indexName)
                                .tableName(table.getName())
                                .schemaName(table.getSchema())
                                .type(mapIndexType(indexType))
                                .unique(!rs.getBoolean("NON_UNIQUE"))
                                .build();
                addIndexColumns(metaData, catalog, table, indexName, index);
                table.addIndex(index);
            }
        }
    }

    private void addIndexColumns(
                                 final DatabaseMetaData metaData, final String catalog, final Table table,
                                 final String indexName, final Index index) throws Exception {
        try (
                ResultSet rs =
                        metaData.getIndexInfo(catalog, table.getSchema(), table.getName(), false, true)) {
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
        } else if (upperType.contains("FULLTEXT")) {
            return IndexType.FULLTEXT;
        } else if (upperType.contains("SPATIAL")) {
            return IndexType.SPATIAL;
        } else {
            return IndexType.BTREE;
        }
    }
}
