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

package com.intellisql.kernel.metadata;

import lombok.Getter;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.schema.SchemaPlus;
import com.intellisql.connector.api.DataSourceConnector;
import com.intellisql.connector.config.DataSourceConfig;
import com.intellisql.kernel.metadata.enums.SchemaType;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Metadata manager for managing DataSource, Schema, and Table registration and queries. */
@Getter
public final class MetadataManager {

    private final Map<String, DataSource> dataSources;

    private final Map<String, Schema> schemas;

    private final Map<String, Table> tables;

    /** Creates a new MetadataManager instance. */
    public MetadataManager() {
        this.dataSources = new ConcurrentHashMap<>();
        this.schemas = new ConcurrentHashMap<>();
        this.tables = new ConcurrentHashMap<>();
    }

    /**
     * Registers a data source.
     *
     * @param dataSource the data source to register
     * @throws IllegalArgumentException if dataSource is null
     */
    public void registerDataSource(final DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("DataSource cannot be null");
        }
        dataSources.put(dataSource.getId(), dataSource);
    }

    /**
     * Unregisters a data source by ID.
     *
     * @param dataSourceId the data source ID to unregister
     */
    public void unregisterDataSource(final String dataSourceId) {
        DataSource removed = dataSources.remove(dataSourceId);
        if (removed != null) {
            schemas.entrySet().removeIf(entry -> entry.getValue().getDataSourceId().equals(dataSourceId));
        }
    }

    /**
     * Gets a data source by ID.
     *
     * @param id the data source ID
     * @return the data source, or empty if not found
     */
    public Optional<DataSource> getDataSource(final String id) {
        return Optional.ofNullable(dataSources.get(id));
    }

    /**
     * Gets a data source by name.
     *
     * @param name the data source name
     * @return the data source, or empty if not found
     */
    public Optional<DataSource> getDataSourceByName(final String name) {
        return dataSources.values().stream().filter(ds -> ds.getName().equals(name)).findFirst();
    }

    /**
     * Gets all registered data sources.
     *
     * @return unmodifiable collection of data sources
     */
    public Collection<DataSource> getAllDataSources() {
        return Collections.unmodifiableCollection(dataSources.values());
    }

    /**
     * Registers a schema.
     *
     * @param schema the schema to register
     * @throws IllegalArgumentException if schema is null
     */
    public void registerSchema(final Schema schema) {
        if (schema == null) {
            throw new IllegalArgumentException("Schema cannot be null");
        }
        schemas.put(schema.getName(), schema);
        if (schema.getTables() != null) {
            tables.putAll(schema.getTables());
        }
    }

    /**
     * Unregisters a schema by name.
     *
     * @param schemaName the schema name to unregister
     */
    public void unregisterSchema(final String schemaName) {
        Schema removed = schemas.remove(schemaName);
        if (removed != null && removed.getTables() != null) {
            removed.getTables().keySet().forEach(tables::remove);
        }
    }

    /**
     * Gets a schema by name.
     *
     * @param name the schema name
     * @return the schema, or empty if not found
     */
    public Optional<Schema> getSchema(final String name) {
        return Optional.ofNullable(schemas.get(name));
    }

    /**
     * Gets schemas by data source ID.
     *
     * @param dataSourceId the data source ID
     * @return unmodifiable collection of schemas
     */
    public Collection<Schema> getSchemasByDataSourceId(final String dataSourceId) {
        return Collections.unmodifiableCollection(
                schemas.values().stream()
                        .filter(s -> s.getDataSourceId() != null && s.getDataSourceId().equals(dataSourceId))
                        .collect(java.util.stream.Collectors.toList()));
    }

    /**
     * Gets all registered schemas.
     *
     * @return unmodifiable collection of schemas
     */
    public Collection<Schema> getAllSchemas() {
        return Collections.unmodifiableCollection(schemas.values());
    }

    /**
     * Registers a table in a schema.
     *
     * @param schemaName the schema name
     * @param table the table to register
     * @throws IllegalArgumentException if table is null
     */
    public void registerTable(final String schemaName, final Table table) {
        if (table == null) {
            throw new IllegalArgumentException("Table cannot be null");
        }
        tables.put(table.getName(), table);
        Schema schema = schemas.get(schemaName);
        if (schema != null) {
            Map<String, Table> schemaTables = new ConcurrentHashMap<>(schema.getTables());
            schemaTables.put(table.getName(), table);
            schemas.put(schemaName, schema.toBuilder().tables(schemaTables).build());
        }
    }

    /**
     * Unregisters a table by name.
     *
     * @param tableName the table name to unregister
     */
    public void unregisterTable(final String tableName) {
        Table removed = tables.remove(tableName);
        if (removed != null) {
            schemas
                    .values()
                    .forEach(
                            schema -> {
                                if (schema.getTables() != null && schema.getTables().containsKey(tableName)) {
                                    Map<String, Table> schemaTables = new ConcurrentHashMap<>(schema.getTables());
                                    schemaTables.remove(tableName);
                                    schemas.put(schema.getName(), schema.toBuilder().tables(schemaTables).build());
                                }
                            });
        }
    }

    /**
     * Gets a table by name.
     *
     * @param name the table name
     * @return the table, or empty if not found
     */
    public Optional<Table> getTable(final String name) {
        return Optional.ofNullable(tables.get(name));
    }

    /**
     * Gets a table by schema name and table name.
     *
     * @param schemaName the schema name
     * @param tableName the table name
     * @return the table, or empty if not found
     */
    public Optional<Table> getTable(final String schemaName, final String tableName) {
        return getSchema(schemaName).map(schema -> schema.getTables().get(tableName));
    }

    /**
     * Gets all registered tables.
     *
     * @return unmodifiable collection of tables
     */
    public Collection<Table> getAllTables() {
        return Collections.unmodifiableCollection(tables.values());
    }

    /**
     * Discovers schema for a data source.
     *
     * @param dataSourceId the data source ID
     */
    public void discoverSchema(final String dataSourceId) {
        getDataSource(dataSourceId)
                .ifPresent(
                        dataSource -> {
                            // Schema discovery logic would be implemented by connector layer
                            // This is a placeholder for the discovery hook
                        });
    }

    /** Clears all metadata. */
    public void clear() {
        tables.clear();
        schemas.clear();
        dataSources.clear();
    }

    /**
     * Gets the root schema for Calcite.
     *
     * @return the root schema
     */
    public SchemaPlus getRootSchema() {
        final SchemaPlus rootSchema = CalciteSchema.createRootSchema(false, true).plus();
        for (final Schema schema : schemas.values()) {
            final SchemaPlus subSchema =
                    rootSchema
                            .add(schema.getName(), new org.apache.calcite.schema.impl.AbstractSchema())
                            .unwrap(SchemaPlus.class);
            if (schema.getTables() != null && subSchema != null) {
                for (final Entry<String, Table> entry : schema.getTables().entrySet()) {
                    final Table table = entry.getValue();
                    subSchema.add(entry.getKey(), createCalciteTable(table));
                }
            }
        }
        return rootSchema;
    }

    /**
     * Initializes metadata from connectors.
     *
     * @param connectors the data source connectors with their configurations
     */
    public void initialize(final Map<DataSourceConnector, DataSourceConfig> connectors) {
        for (Entry<DataSourceConnector, DataSourceConfig> entry : connectors.entrySet()) {
            try {
                final com.intellisql.connector.model.Schema connectorSchema =
                        entry.getKey().discoverSchema(entry.getValue());
                if (connectorSchema != null) {
                    registerFromConnectorSchema(connectorSchema);
                }
                // CHECKSTYLE:OFF
            } catch (final Exception ex) {
                // CHECKSTYLE:ON
                ex.printStackTrace();
                // Log and continue with other connectors
            }
        }
    }

    /**
     * Registers schema from connector.
     *
     * @param connectorSchema the connector schema
     */
    private void registerFromConnectorSchema(
                                             final com.intellisql.connector.model.Schema connectorSchema) {
        final Map<String, Table> schemaTables = new ConcurrentHashMap<>();
        if (connectorSchema.getTables() != null) {
            for (final com.intellisql.connector.model.Table connectorTable : connectorSchema.getTables()) {
                final java.util.List<Column> columns = new java.util.ArrayList<>();
                if (connectorTable.getColumns() != null) {
                    for (final com.intellisql.connector.model.Column column : connectorTable.getColumns()) {
                        columns.add(
                                Column.builder().name(column.getName()).nullable(column.isNullable()).build());
                    }
                }
                final Table table = Table.builder().name(connectorTable.getName()).columns(columns).build();
                schemaTables.put(connectorTable.getName(), table);
                tables.put(connectorTable.getName(), table);
            }
        }
        final Schema schema =
                Schema.builder()
                        .name(connectorSchema.getName())
                        .dataSourceId(connectorSchema.getDataSourceName())
                        .tables(schemaTables)
                        .type(SchemaType.valueOf(connectorSchema.getType().name()))
                        .build();
        schemas.put(connectorSchema.getName(), schema);
    }

    /**
     * Creates a Calcite table from IntelliSql table metadata.
     *
     * @param table the table metadata
     * @return the Calcite table
     */
    private org.apache.calcite.schema.Table createCalciteTable(final Table table) {
        return new org.apache.calcite.schema.impl.AbstractTable() {

            @Override
            public org.apache.calcite.rel.type.RelDataType getRowType(
                                                                      final org.apache.calcite.rel.type.RelDataTypeFactory typeFactory) {
                final org.apache.calcite.rel.type.RelDataTypeFactory.Builder builder =
                        typeFactory.builder();
                if (table.getColumns() != null) {
                    for (final Column column : table.getColumns()) {
                        builder.add(
                                column.getName(),
                                typeFactory.createSqlType(org.apache.calcite.sql.type.SqlTypeName.VARCHAR));
                    }
                }
                return builder.build();
            }
        };
    }

    /** Closes the metadata manager and releases resources. */
    public void close() {
        clear();
    }
}
