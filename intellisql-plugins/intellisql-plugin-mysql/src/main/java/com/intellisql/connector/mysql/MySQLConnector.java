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

package com.intellisql.connector.mysql;

import com.intellisql.common.metadata.enums.DataSourceType;
import com.intellisql.connector.config.IntelliSQLDataSourceConfig;
import com.intellisql.connector.jdbc.AbstractJdbcConnector;

/**
 * MySQL implementation of DataSourceConnector. Provides connection management and schema discovery
 * for MySQL databases.
 */
public class MySQLConnector extends AbstractJdbcConnector<MySQLConnectionPool> {

    /** Creates a new MySQL connector. */
    public MySQLConnector() {
        super(new MySQLSchemaDiscoverer(), new MySQLQueryExecutor());
    }

    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.MYSQL;
    }

    @Override
    protected String getDatabaseName() {
        return "MySQL";
    }

    @Override
    protected MySQLConnectionPool createConnectionPool(final IntelliSQLDataSourceConfig config) {
        return new MySQLConnectionPool(config);
    }
}
