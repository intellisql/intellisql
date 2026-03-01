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

package com.intellisql.federation.executor.iterator;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.intellisql.federation.executor.Row;
import com.intellisql.connector.api.QueryExecutor;
import com.intellisql.connector.model.QueryResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Table scan operator that reads data from a data source.
 * Supports JDBC-based data sources (MySQL, PostgreSQL) and converts results to Rows.
 * Reference: ShardingSphere JDBCDataRowEnumerator pattern.
 */
@Slf4j
public class TableScanOperator extends AbstractOperator<Row> {

    /** The connection to the data source. */
    private final Connection connection;

    /** The query executor for the data source. */
    private final QueryExecutor queryExecutor;

    /** The SQL query to execute. */
    private final String sql;

    /** The query result. */
    private QueryResult queryResult;

    /** Iterator over the result rows. */
    private Iterator<List<Object>> rowIterator;

    /** The column names. */
    private List<String> columnNames;

    /** The next row to return. */
    private Row nextRow;

    /**
     * Creates a new TableScanOperator.
     *
     * @param connection    the connection to the data source
     * @param queryExecutor the query executor
     * @param sql           the SQL query to execute
     */
    public TableScanOperator(final Connection connection, final QueryExecutor queryExecutor, final String sql) {
        super("TableScan");
        this.connection = connection;
        this.queryExecutor = queryExecutor;
        this.sql = sql;
    }

    @Override
    protected void doOpen() throws Exception {
        log.debug("Executing table scan query: {}", sql);
        // CHECKSTYLE:OFF
        try {
            queryResult = queryExecutor.executeQuery(connection, sql);
            columnNames = queryResult.getColumnNames();
            rowIterator = queryResult.getRows().iterator();
            log.debug("Table scan opened with {} columns, {} rows", columnNames.size(), queryResult.getRowCount());
        } catch (final Exception ex) {
            throw new RuntimeException("Failed to execute table scan: " + sql, ex);
        }
        // CHECKSTYLE:ON
    }

    @Override
    protected void doClose() throws Exception {
        if (queryResult != null) {
            queryResult.getRows().clear();
            queryResult = null;
        }
        rowIterator = null;
    }

    @Override
    protected boolean doHasNext() throws Exception {
        if (rowIterator != null && rowIterator.hasNext()) {
            final List<Object> values = rowIterator.next();
            nextRow = new Row(new ArrayList<>(values), columnNames);
            return true;
        }
        nextRow = null;
        return false;
    }

    @Override
    protected Row doNext() throws Exception {
        if (nextRow == null) {
            throw new IllegalStateException("No more rows available");
        }
        return nextRow;
    }

    /**
     * Gets the column names for this table scan.
     *
     * @return the column names
     */
    public List<String> getColumnNames() {
        return columnNames;
    }
}
