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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.intellisql.connector.api.QueryExecutor;
import com.intellisql.connector.enums.DataType;
import com.intellisql.connector.model.QueryResult;

import lombok.extern.slf4j.Slf4j;

/**
 * PostgreSQL implementation of QueryExecutor. Executes SQL queries against PostgreSQL databases.
 */
@Slf4j
public class PostgreSQLQueryExecutor implements QueryExecutor {

    @Override
    public QueryResult executeQuery(final Connection connection, final String sql) throws Exception {
        long startTime = System.currentTimeMillis();
        try (
                PreparedStatement stmt = connection.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String> columnNames = new ArrayList<>();
            List<DataType> columnTypes = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnLabel(i));
                columnTypes.add(
                        mapPostgreSQLTypeToDataType(metaData.getColumnTypeName(i), metaData.getColumnType(i)));
            }
            QueryResult.QueryResultBuilder resultBuilder =
                    QueryResult.builder().columnNames(columnNames).columnTypes(columnTypes).success(true);
            List<List<Object>> rows = new ArrayList<>();
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }
            long executionTime = System.currentTimeMillis() - startTime;
            resultBuilder.rows(rows).rowCount(rows.size()).executionTimeMs(executionTime);
            log.debug(
                    "PostgreSQL query executed successfully in {}ms, returned {} rows",
                    executionTime,
                    rows.size());
            return resultBuilder.build();
        } catch (final SQLException ex) {
            log.error("PostgreSQL query execution failed: {}", ex.getMessage(), ex);
            return QueryResult.failure(ex.getMessage());
        }
    }

    @Override
    public int executeUpdate(final Connection connection, final String sql) throws Exception {
        long startTime = System.currentTimeMillis();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int affectedRows = stmt.executeUpdate();
            long executionTime = System.currentTimeMillis() - startTime;
            log.debug(
                    "PostgreSQL update executed successfully in {}ms, affected {} rows",
                    executionTime,
                    affectedRows);
            return affectedRows;
        } catch (final SQLException ex) {
            log.error("PostgreSQL update execution failed: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public boolean executeDdl(final Connection connection, final String sql) throws Exception {
        long startTime = System.currentTimeMillis();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("PostgreSQL DDL executed successfully in {}ms", executionTime);
            return true;
        } catch (final SQLException ex) {
            log.error("PostgreSQL DDL execution failed: {}", ex.getMessage(), ex);
            throw ex;
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
}
