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

package com.intellisql.optimizer.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.sql.type.SqlTypeName;

import lombok.extern.slf4j.Slf4j;

/**
 * Provides metadata to the Calcite optimizer for federated queries.
 * Integrates IntelliSql metadata with Calcite's schema system.
 * Reference: ShardingSphere FederatedMetadataProvider.
 */
@Slf4j
public class FederatedMetadataProvider {

    /** Map of schema name to Calcite Schema. */
    private final Map<String, Schema> calciteSchemas = new ConcurrentHashMap<>();

    /** Map of table name to table statistics. */
    private final Map<String, TableStatistics> tableStatistics = new ConcurrentHashMap<>();

    /** Map of table name to column definitions. */
    private final Map<String, List<ColumnDef>> tableColumns = new ConcurrentHashMap<>();

    /** The statistics handler. */
    private final StatisticsHandler statisticsHandler;

    /**
     * Creates a new FederatedMetadataProvider.
     */
    public FederatedMetadataProvider() {
        this.statisticsHandler = new StatisticsHandler(this);
    }

    /**
     * Registers a table with its columns.
     *
     * @param schemaName the schema name
     * @param tableName  the table name
     * @param columns    the column definitions
     * @param rowCount   the estimated row count
     */
    public void registerTable(
                              final String schemaName,
                              final String tableName,
                              final List<ColumnDef> columns,
                              final long rowCount) {
        final String qualifiedName = schemaName + "." + tableName;
        log.debug("Registering table: {}", qualifiedName);

        // Store column definitions
        tableColumns.put(qualifiedName, new ArrayList<>(columns));

        // Store statistics
        tableStatistics.put(qualifiedName, TableStatistics.of(qualifiedName, rowCount));

        // Update Calcite schema
        calciteSchemas.computeIfAbsent(schemaName, k -> new AbstractSchema() {

            @Override
            public Map<String, Table> getTableMap() {
                return new HashMap<>();
            }
        });
    }

    /**
     * Gets table statistics.
     *
     * @param tableName the table name (qualified)
     * @return the table statistics
     */
    public TableStatistics getTableStatistics(final String tableName) {
        return tableStatistics.getOrDefault(tableName, TableStatistics.of(tableName, 1000));
    }

    /**
     * Gets column definitions for a table.
     *
     * @param tableName the qualified table name
     * @return the column definitions
     */
    public List<ColumnDef> getTableColumns(final String tableName) {
        return tableColumns.getOrDefault(tableName, new ArrayList<>());
    }

    /**
     * Gets all registered Calcite schemas.
     *
     * @return the schema map
     */
    public Map<String, Schema> getCalciteSchemas() {
        return new HashMap<>(calciteSchemas);
    }

    /**
     * Gets the statistics handler.
     *
     * @return the statistics handler
     */
    public StatisticsHandler getStatisticsHandler() {
        return statisticsHandler;
    }

    /**
     * Clears all registered metadata.
     */
    public void clear() {
        calciteSchemas.clear();
        tableStatistics.clear();
        tableColumns.clear();
        log.debug("Cleared federated metadata provider");
    }

    /**
     * Column definition for table metadata.
     */
    public static class ColumnDef {

        private final String name;

        private final String typeName;

        private final boolean nullable;

        private final int precision;

        private final int scale;

        public ColumnDef(final String name, final String typeName, final boolean nullable) {
            this(name, typeName, nullable, -1, -1);
        }

        public ColumnDef(
                         final String name,
                         final String typeName,
                         final boolean nullable,
                         final int precision,
                         final int scale) {
            this.name = name;
            this.typeName = typeName;
            this.nullable = nullable;
            this.precision = precision;
            this.scale = scale;
        }

        public String getName() {
            return name;
        }

        public String getTypeName() {
            return typeName;
        }

        public boolean isNullable() {
            return nullable;
        }

        public int getPrecision() {
            return precision;
        }

        public int getScale() {
            return scale;
        }

        /**
         * Converts to Calcite RelDataType.
         *
         * @param typeFactory the type factory
         * @return the RelDataType
         */
        public RelDataType toRelDataType(final RelDataTypeFactory typeFactory) {
            final SqlTypeName sqlTypeName = convertTypeName(typeName);
            RelDataType relType;
            if (precision > 0 && scale > 0) {
                relType = typeFactory.createSqlType(sqlTypeName, precision, scale);
            } else if (precision > 0) {
                relType = typeFactory.createSqlType(sqlTypeName, precision);
            } else {
                relType = typeFactory.createSqlType(sqlTypeName);
            }
            if (nullable) {
                return typeFactory.createTypeWithNullability(relType, true);
            }
            return relType;
        }

        private SqlTypeName convertTypeName(final String typeName) {
            switch (typeName.toUpperCase()) {
                case "INTEGER":
                case "INT":
                    return SqlTypeName.INTEGER;
                case "BIGINT":
                case "INT64":
                    return SqlTypeName.BIGINT;
                case "SMALLINT":
                    return SqlTypeName.SMALLINT;
                case "TINYINT":
                    return SqlTypeName.TINYINT;
                case "FLOAT":
                case "REAL":
                    return SqlTypeName.FLOAT;
                case "DOUBLE":
                    return SqlTypeName.DOUBLE;
                case "DECIMAL":
                case "NUMERIC":
                    return SqlTypeName.DECIMAL;
                case "BOOLEAN":
                case "BOOL":
                    return SqlTypeName.BOOLEAN;
                case "DATE":
                    return SqlTypeName.DATE;
                case "DATETIME":
                case "TIMESTAMP":
                    return SqlTypeName.TIMESTAMP;
                case "TIME":
                    return SqlTypeName.TIME;
                case "VARCHAR":
                case "NVARCHAR":
                case "VARCHAR2":
                    return SqlTypeName.VARCHAR;
                case "CHAR":
                case "NCHAR":
                    return SqlTypeName.CHAR;
                case "TEXT":
                case "CLOB":
                case "LONGTEXT":
                    return SqlTypeName.VARCHAR;
                case "BLOB":
                case "BINARY":
                case "VARBINARY":
                    return SqlTypeName.VARBINARY;
                default:
                    return SqlTypeName.VARCHAR;
            }
        }
    }
}
