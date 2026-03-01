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

import com.intellisql.connector.model.QueryResult;

/**
 * Connection interface representing a connection to a data source. Provides methods for query
 * execution and connection management.
 */
public interface Connection extends AutoCloseable {

    /**
     * Executes a SQL query and returns the result.
     *
     * @param sql the SQL query to execute
     * @return the query result containing rows and metadata
     * @throws Exception if query execution fails
     */
    QueryResult executeQuery(String sql) throws Exception;

    /**
     * Executes a SQL update/insert/delete statement.
     *
     * @param sql the SQL statement to execute
     * @return the number of rows affected
     * @throws Exception if statement execution fails
     */
    int executeUpdate(String sql) throws Exception;

    /**
     * Checks if the connection is still valid.
     *
     * @return true if the connection is valid, false otherwise
     */
    boolean isValid();

    /** Closes the connection and releases all resources. */
    @Override
    void close();

    /**
     * Gets the underlying JDBC connection if available.
     *
     * @return the JDBC connection or null if not applicable
     */
    java.sql.Connection getJdbcConnection();
}
