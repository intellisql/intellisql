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

package com.intellisql.connector.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.intellisql.common.metadata.enums.DataType;
import com.intellisql.connector.api.QueryExecutor;
import com.intellisql.connector.model.QueryResult;
import lombok.extern.slf4j.Slf4j;

/** Shared JDBC query execution flow for connector plugins. */
@Slf4j
public abstract class AbstractJdbcQueryExecutor implements QueryExecutor {

    @Override
    public QueryResult executeQuery(final Connection connection, final String sql) throws Exception {
        final long startTime = System.currentTimeMillis();
        try (
                PreparedStatement stmt = connection.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            final ResultSetMetaData metaData = rs.getMetaData();
            final int columnCount = metaData.getColumnCount();
            final List<String> columnNames = new ArrayList<>();
            final List<DataType> columnTypes = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnLabel(i));
                columnTypes.add(mapDataType(metaData, i));
            }

            final List<List<Object>> rows = new ArrayList<>();
            while (rs.next()) {
                final List<Object> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }

            final long executionTime = System.currentTimeMillis() - startTime;
            log.debug(
                    "{} query executed successfully in {}ms, returned {} rows",
                    getDatabaseName(),
                    executionTime,
                    rows.size());
            return QueryResult.builder()
                    .columnNames(columnNames)
                    .columnTypes(columnTypes)
                    .rows(rows)
                    .rowCount(rows.size())
                    .executionTimeMs(executionTime)
                    .success(true)
                    .build();
        } catch (final SQLException ex) {
            log.error("{} query execution failed: {}", getDatabaseName(), ex.getMessage(), ex);
            return QueryResult.failure(ex.getMessage());
        }
    }

    @Override
    public int executeUpdate(final Connection connection, final String sql) throws Exception {
        final long startTime = System.currentTimeMillis();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            final int affectedRows = stmt.executeUpdate();
            final long executionTime = System.currentTimeMillis() - startTime;
            log.debug(
                    "{} update executed successfully in {}ms, affected {} rows",
                    getDatabaseName(),
                    executionTime,
                    affectedRows);
            return affectedRows;
        } catch (final SQLException ex) {
            log.error("{} update execution failed: {}", getDatabaseName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public boolean executeDdl(final Connection connection, final String sql) throws Exception {
        final long startTime = System.currentTimeMillis();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
            final long executionTime = System.currentTimeMillis() - startTime;
            log.debug("{} DDL executed successfully in {}ms", getDatabaseName(), executionTime);
            return true;
        } catch (final SQLException ex) {
            log.error("{} DDL execution failed: {}", getDatabaseName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    protected DataType mapDataType(final ResultSetMetaData metaData, final int columnIndex) throws SQLException {
        return mapDataType(metaData.getColumnTypeName(columnIndex), metaData.getColumnType(columnIndex));
    }

    protected DataType mapDataType(final String nativeType, final int sqlType) {
        return mapSqlTypeToDataType(sqlType);
    }

    protected DataType mapSqlTypeToDataType(final int sqlType) {
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

    protected abstract String getDatabaseName();
}
