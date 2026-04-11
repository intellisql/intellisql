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

import com.intellisql.common.metadata.enums.DataSourceType;
import com.intellisql.connector.config.IntelliSQLDataSourceConfig;
import com.intellisql.connector.jdbc.AbstractJdbcConnector;

/**
 * PostgreSQL implementation of DataSourceConnector. Provides connection management and schema
 * discovery for PostgreSQL databases. Uses PostgreSQL JDBC Driver 42.7.1 with sslmode=require for
 * production.
 */
public class PostgreSQLConnector extends AbstractJdbcConnector<PostgreSQLConnectionPool> {

    /** Creates a new PostgreSQL connector. */
    public PostgreSQLConnector() {
        super(new PostgreSQLSchemaDiscoverer(), new PostgreSQLQueryExecutor());
    }

    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.POSTGRESQL;
    }

    @Override
    protected String getDatabaseName() {
        return "PostgreSQL";
    }

    @Override
    protected PostgreSQLConnectionPool createConnectionPool(final IntelliSQLDataSourceConfig config) {
        return new PostgreSQLConnectionPool(config);
    }
}
