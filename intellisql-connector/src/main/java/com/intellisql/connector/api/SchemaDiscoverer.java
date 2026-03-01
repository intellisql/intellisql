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

package com.intellisql.connector.api;

import java.sql.Connection;

import com.intellisql.connector.model.Schema;

/**
 * Interface for discovering database schema metadata. Implementations use DatabaseMetaData to
 * discover tables, columns, and indexes.
 */
public interface SchemaDiscoverer {

    /**
     * Discovers the complete schema from the given connection.
     *
     * @param connection the JDBC connection
     * @param schemaName the schema name to discover (null for all schemas)
     * @param dataSourceName the data source configuration name (e.g., "mysql_source")
     * @return the discovered schema
     * @throws Exception if schema discovery fails
     */
    Schema discoverSchema(Connection connection, String schemaName, String dataSourceName) throws Exception;

    /**
     * Discovers tables from the given connection.
     *
     * @param connection the JDBC connection
     * @param schemaName the schema name
     * @param tableNamePattern the table name pattern (null for all tables)
     * @return the schema with discovered tables
     * @throws Exception if table discovery fails
     */
    Schema discoverTables(Connection connection, String schemaName, String tableNamePattern) throws Exception;

    /**
     * Discovers columns for a specific table.
     *
     * @param connection the JDBC connection
     * @param schemaName the schema name
     * @param tableName the table name
     * @return the schema with discovered columns
     * @throws Exception if column discovery fails
     */
    Schema discoverColumns(Connection connection, String schemaName, String tableName) throws Exception;
}
