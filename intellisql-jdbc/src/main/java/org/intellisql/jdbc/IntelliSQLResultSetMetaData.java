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

package org.intellisql.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.calcite.avatica.ColumnMetaData;

import lombok.extern.slf4j.Slf4j;

/** JDBC ResultSetMetaData implementation for IntelliSql. */
@Slf4j
public class IntelliSqlResultSetMetaData implements ResultSetMetaData {

    private static final String RSMD_CLOSED = "ResultSetMetaData is closed";

    private final List<ColumnMetaData> columnMetaData;

    private boolean closed;

    /**
     * Creates a new result set metadata.
     *
     * @param columnMetaData the column metadata list
     */
    public IntelliSqlResultSetMetaData(final List<ColumnMetaData> columnMetaData) {
        this.columnMetaData = columnMetaData != null ? columnMetaData : new java.util.ArrayList<>();
        this.closed = false;
    }

    @Override
    public int getColumnCount() throws SQLException {
        checkClosed();
        return columnMetaData.size();
    }

    @Override
    public boolean isAutoIncrement(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        return false;
    }

    @Override
    public boolean isCaseSensitive(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        return true;
    }

    @Override
    public boolean isSearchable(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        return true;
    }

    @Override
    public boolean isCurrency(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        return false;
    }

    @Override
    public int isNullable(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        return columnNullableUnknown;
    }

    @Override
    public boolean isSigned(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        ColumnMetaData meta = columnMetaData.get(column - 1);
        return meta.type.id == Types.NUMERIC || meta.type.id == Types.DECIMAL
                || meta.type.id == Types.INTEGER || meta.type.id == Types.BIGINT
                || meta.type.id == Types.SMALLINT || meta.type.id == Types.TINYINT
                || meta.type.id == Types.FLOAT || meta.type.id == Types.DOUBLE
                || meta.type.id == Types.REAL;
    }

    @Override
    public int getColumnDisplaySize(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        ColumnMetaData meta = columnMetaData.get(column - 1);
        return meta.displaySize > 0 ? meta.displaySize : 50;
    }

    @Override
    public String getColumnLabel(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        ColumnMetaData meta = columnMetaData.get(column - 1);
        return meta.label != null ? meta.label : "";
    }

    @Override
    public String getColumnName(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        ColumnMetaData meta = columnMetaData.get(column - 1);
        return meta.columnName != null ? meta.columnName : meta.label != null ? meta.label : "";
    }

    @Override
    public String getSchemaName(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        ColumnMetaData meta = columnMetaData.get(column - 1);
        return meta.schemaName != null ? meta.schemaName : "";
    }

    @Override
    public int getPrecision(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        ColumnMetaData meta = columnMetaData.get(column - 1);
        return meta.precision;
    }

    @Override
    public int getScale(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        ColumnMetaData meta = columnMetaData.get(column - 1);
        return meta.scale;
    }

    @Override
    public String getTableName(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        ColumnMetaData meta = columnMetaData.get(column - 1);
        return meta.tableName != null ? meta.tableName : "";
    }

    @Override
    public String getCatalogName(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        return "";
    }

    @Override
    public int getColumnType(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        ColumnMetaData meta = columnMetaData.get(column - 1);
        return meta.type.id;
    }

    @Override
    public String getColumnTypeName(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        ColumnMetaData meta = columnMetaData.get(column - 1);
        return meta.type.name != null ? meta.type.name : "UNKNOWN";
    }

    @Override
    public boolean isReadOnly(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        return true;
    }

    @Override
    public boolean isWritable(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        return false;
    }

    @Override
    public boolean isDefinitelyWritable(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        return false;
    }

    @Override
    public String getColumnClassName(final int column) throws SQLException {
        checkClosed();
        checkColumnIndex(column);
        ColumnMetaData meta = columnMetaData.get(column - 1);
        if (meta.type.id == Types.VARCHAR || meta.type.id == Types.CHAR
                || meta.type.id == Types.LONGVARCHAR || meta.type.id == Types.CLOB) {
            return String.class.getName();
        } else if (meta.type.id == Types.INTEGER) {
            return Integer.class.getName();
        } else if (meta.type.id == Types.BIGINT) {
            return Long.class.getName();
        } else if (meta.type.id == Types.SMALLINT) {
            return Short.class.getName();
        } else if (meta.type.id == Types.TINYINT) {
            return Byte.class.getName();
        } else if (meta.type.id == Types.FLOAT || meta.type.id == Types.REAL) {
            return Float.class.getName();
        } else if (meta.type.id == Types.DOUBLE) {
            return Double.class.getName();
        } else if (meta.type.id == Types.NUMERIC || meta.type.id == Types.DECIMAL) {
            return java.math.BigDecimal.class.getName();
        } else if (meta.type.id == Types.BOOLEAN || meta.type.id == Types.BIT) {
            return Boolean.class.getName();
        } else if (meta.type.id == Types.DATE) {
            return java.sql.Date.class.getName();
        } else if (meta.type.id == Types.TIME) {
            return java.sql.Time.class.getName();
        } else if (meta.type.id == Types.TIMESTAMP) {
            return java.sql.Timestamp.class.getName();
        } else if (meta.type.id == Types.BLOB || meta.type.id == Types.BINARY
                || meta.type.id == Types.VARBINARY || meta.type.id == Types.LONGVARBINARY) {
            return byte[].class.getName();
        }
        return Object.class.getName();
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Not a wrapper for " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    /**
     * Closes this metadata.
     */
    public void close() {
        closed = true;
    }

    /**
     * Checks if this metadata is closed.
     *
     * @return true if closed
     */
    public boolean isClosed() {
        return closed;
    }

    private void checkClosed() throws SQLException {
        if (closed) {
            throw new SQLException(RSMD_CLOSED);
        }
    }

    private void checkColumnIndex(final int column) throws SQLException {
        if (column < 1 || column > columnMetaData.size()) {
            throw new SQLException("Invalid column index: " + column);
        }
    }
}
