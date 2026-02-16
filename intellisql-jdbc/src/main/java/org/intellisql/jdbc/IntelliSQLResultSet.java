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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.calcite.avatica.ColumnMetaData;
import org.apache.calcite.avatica.Meta;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** JDBC ResultSet implementation for IntelliSql. */
@Slf4j
@Getter
public class IntelliSqlResultSet implements ResultSet {

    private static final String RS_CLOSED = "ResultSet is closed";

    private final Statement statement;

    private final Meta.StatementHandle statementHandle;

    private final Meta.Signature signature;

    private final IntelliSqlConnection connection;

    private final List<ColumnMetaData> columnMetaData;

    private final IntelliSqlResultSetMetaData resultSetMetaData;

    private List<Object> currentRow;

    private List<List<Object>> rows;

    private Iterator<List<Object>> rowIterator;

    private boolean closed;

    private boolean wasNull;

    private SQLWarning warningChain;

    private int fetchSize;

    private int currentRowIndex = -1;

    /**
     * Creates a new result set.
     *
     * @param statement the parent statement
     * @param statementHandle the statement handle
     * @param signature the result signature
     * @param firstFrame the first frame of data
     */
    public IntelliSqlResultSet(
                               final Statement statement,
                               final Meta.StatementHandle statementHandle,
                               final Meta.Signature signature,
                               final Meta.Frame firstFrame) {
        this.statement = statement;
        this.statementHandle = statementHandle;
        this.signature = signature;
        try {
            this.connection = (IntelliSqlConnection) statement.getConnection();
        } catch (final java.sql.SQLException ex) {
            throw new RuntimeException("Failed to get connection", ex);
        }
        this.columnMetaData = signature != null ? signature.columns : new ArrayList<>();
        this.resultSetMetaData = new IntelliSqlResultSetMetaData(this.columnMetaData);
        this.rows = new ArrayList<>();
        this.fetchSize = connection.getIntProperty("fetchSize", 1000);
        if (firstFrame != null && firstFrame.rows != null) {
            for (Object row : firstFrame.rows) {
                if (row instanceof List) {
                    rows.add((List<Object>) row);
                }
            }
        }
        this.rowIterator = rows.iterator();
        log.debug("Created result set with {} columns and {} rows", columnMetaData.size(), rows.size());
    }

    @Override
    public boolean next() throws SQLException {
        checkClosed();
        if (rowIterator != null && rowIterator.hasNext()) {
            currentRow = rowIterator.next();
            currentRowIndex++;
            return true;
        }
        return false;
    }

    @Override
    public void close() throws SQLException {
        if (closed) {
            return;
        }
        closed = true;
        currentRow = null;
        rows = null;
        rowIterator = null;
        if (resultSetMetaData != null) {
            resultSetMetaData.close();
        }
        log.debug("Closed result set");
    }

    @Override
    public boolean wasNull() throws SQLException {
        checkClosed();
        return wasNull;
    }

    @Override
    public String getString(final int columnIndex) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        return wasNull ? null : value.toString();
    }

    @Override
    public String getString(final String columnLabel) throws SQLException {
        return getString(findColumn(columnLabel));
    }

    @Override
    public boolean getBoolean(final int columnIndex) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        if (wasNull) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    @Override
    public boolean getBoolean(final String columnLabel) throws SQLException {
        return getBoolean(findColumn(columnLabel));
    }

    @Override
    public byte getByte(final int columnIndex) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        if (wasNull) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).byteValue();
        }
        return Byte.parseByte(value.toString());
    }

    @Override
    public byte getByte(final String columnLabel) throws SQLException {
        return getByte(findColumn(columnLabel));
    }

    @Override
    public short getShort(final int columnIndex) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        if (wasNull) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).shortValue();
        }
        return Short.parseShort(value.toString());
    }

    @Override
    public short getShort(final String columnLabel) throws SQLException {
        return getShort(findColumn(columnLabel));
    }

    @Override
    public int getInt(final int columnIndex) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        if (wasNull) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    @Override
    public int getInt(final String columnLabel) throws SQLException {
        return getInt(findColumn(columnLabel));
    }

    @Override
    public long getLong(final int columnIndex) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        if (wasNull) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    @Override
    public long getLong(final String columnLabel) throws SQLException {
        return getLong(findColumn(columnLabel));
    }

    @Override
    public float getFloat(final int columnIndex) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        if (wasNull) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return Float.parseFloat(value.toString());
    }

    @Override
    public float getFloat(final String columnLabel) throws SQLException {
        return getFloat(findColumn(columnLabel));
    }

    @Override
    public double getDouble(final int columnIndex) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        if (wasNull) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    @Override
    public double getDouble(final String columnLabel) throws SQLException {
        return getDouble(findColumn(columnLabel));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BigDecimal getBigDecimal(final int columnIndex, final int scale) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        if (wasNull) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString()).setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public BigDecimal getBigDecimal(final String columnLabel, final int scale) throws SQLException {
        return getBigDecimal(findColumn(columnLabel), scale);
    }

    @Override
    public BigDecimal getBigDecimal(final int columnIndex) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        if (wasNull) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    @Override
    public BigDecimal getBigDecimal(final String columnLabel) throws SQLException {
        return getBigDecimal(findColumn(columnLabel));
    }

    @Override
    public byte[] getBytes(final int columnIndex) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        if (wasNull) {
            return null;
        }
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        return value.toString().getBytes();
    }

    @Override
    public byte[] getBytes(final String columnLabel) throws SQLException {
        return getBytes(findColumn(columnLabel));
    }

    @Override
    public Date getDate(final int columnIndex) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        if (wasNull) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }
        return Date.valueOf(value.toString());
    }

    @Override
    public Date getDate(final int columnIndex, final Calendar cal) throws SQLException {
        return getDate(columnIndex);
    }

    @Override
    public Date getDate(final String columnLabel) throws SQLException {
        return getDate(findColumn(columnLabel));
    }

    @Override
    public Date getDate(final String columnLabel, final Calendar cal) throws SQLException {
        return getDate(findColumn(columnLabel), cal);
    }

    @Override
    public Time getTime(final int columnIndex) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        if (wasNull) {
            return null;
        }
        if (value instanceof Time) {
            return (Time) value;
        }
        if (value instanceof Number) {
            return new Time(((Number) value).longValue());
        }
        return Time.valueOf(value.toString());
    }

    @Override
    public Time getTime(final int columnIndex, final Calendar cal) throws SQLException {
        return getTime(columnIndex);
    }

    @Override
    public Time getTime(final String columnLabel) throws SQLException {
        return getTime(findColumn(columnLabel));
    }

    @Override
    public Time getTime(final String columnLabel, final Calendar cal) throws SQLException {
        return getTime(findColumn(columnLabel), cal);
    }

    @Override
    public Timestamp getTimestamp(final int columnIndex) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        if (wasNull) {
            return null;
        }
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        if (value instanceof Number) {
            return new Timestamp(((Number) value).longValue());
        }
        return Timestamp.valueOf(value.toString());
    }

    @Override
    public Timestamp getTimestamp(final int columnIndex, final Calendar cal) throws SQLException {
        return getTimestamp(columnIndex);
    }

    @Override
    public Timestamp getTimestamp(final String columnLabel) throws SQLException {
        return getTimestamp(findColumn(columnLabel));
    }

    @Override
    public Timestamp getTimestamp(final String columnLabel, final Calendar cal) throws SQLException {
        return getTimestamp(findColumn(columnLabel), cal);
    }

    @Override
    public InputStream getAsciiStream(final int columnIndex) throws SQLException {
        checkClosed();
        throw new SQLException("AsciiStream not supported");
    }

    @Override
    public InputStream getAsciiStream(final String columnLabel) throws SQLException {
        return getAsciiStream(findColumn(columnLabel));
    }

    @Override
    @Deprecated
    public InputStream getUnicodeStream(final int columnIndex) throws SQLException {
        checkClosed();
        throw new SQLException("UnicodeStream not supported");
    }

    @Override
    @Deprecated
    public InputStream getUnicodeStream(final String columnLabel) throws SQLException {
        return getUnicodeStream(findColumn(columnLabel));
    }

    @Override
    public InputStream getBinaryStream(final int columnIndex) throws SQLException {
        checkClosed();
        throw new SQLException("BinaryStream not supported");
    }

    @Override
    public InputStream getBinaryStream(final String columnLabel) throws SQLException {
        return getBinaryStream(findColumn(columnLabel));
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        checkClosed();
        return warningChain;
    }

    @Override
    public void clearWarnings() throws SQLException {
        checkClosed();
        warningChain = null;
    }

    @Override
    public String getCursorName() throws SQLException {
        checkClosed();
        return "";
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        checkClosed();
        return resultSetMetaData;
    }

    @Override
    public Object getObject(final int columnIndex) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        return value;
    }

    @Override
    public Object getObject(final int columnIndex, final Map<String, Class<?>> map) throws SQLException {
        return getObject(columnIndex);
    }

    @Override
    public <T> T getObject(final int columnIndex, final Class<T> type) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        if (wasNull) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw new SQLException("Cannot convert " + value.getClass() + " to " + type);
    }

    @Override
    public Object getObject(final String columnLabel) throws SQLException {
        return getObject(findColumn(columnLabel));
    }

    @Override
    public Object getObject(final String columnLabel, final Map<String, Class<?>> map) throws SQLException {
        return getObject(findColumn(columnLabel), map);
    }

    @Override
    public <T> T getObject(final String columnLabel, final Class<T> type) throws SQLException {
        return getObject(findColumn(columnLabel), type);
    }

    @Override
    public int findColumn(final String columnLabel) throws SQLException {
        checkClosed();
        for (int i = 0; i < columnMetaData.size(); i++) {
            if (columnMetaData.get(i).label.equalsIgnoreCase(columnLabel)) {
                return i + 1;
            }
        }
        throw new SQLException("Column not found: " + columnLabel);
    }

    @Override
    public Reader getCharacterStream(final int columnIndex) throws SQLException {
        checkClosed();
        throw new SQLException("CharacterStream not supported");
    }

    @Override
    public Reader getCharacterStream(final String columnLabel) throws SQLException {
        return getCharacterStream(findColumn(columnLabel));
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        checkClosed();
        return currentRow == null;
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        checkClosed();
        return !rowIterator.hasNext() && currentRow != null;
    }

    @Override
    public boolean isFirst() throws SQLException {
        checkClosed();
        return currentRowIndex == 0;
    }

    @Override
    public boolean isLast() throws SQLException {
        checkClosed();
        return !rowIterator.hasNext();
    }

    @Override
    public void beforeFirst() throws SQLException {
        checkClosed();
        throw new SQLException("Result set is TYPE_FORWARD_ONLY");
    }

    @Override
    public void afterLast() throws SQLException {
        checkClosed();
        throw new SQLException("Result set is TYPE_FORWARD_ONLY");
    }

    @Override
    public boolean first() throws SQLException {
        checkClosed();
        throw new SQLException("Result set is TYPE_FORWARD_ONLY");
    }

    @Override
    public boolean last() throws SQLException {
        checkClosed();
        throw new SQLException("Result set is TYPE_FORWARD_ONLY");
    }

    @Override
    public int getRow() throws SQLException {
        checkClosed();
        return currentRowIndex;
    }

    @Override
    public boolean absolute(final int row) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is TYPE_FORWARD_ONLY");
    }

    @Override
    public boolean relative(final int rows) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is TYPE_FORWARD_ONLY");
    }

    @Override
    public boolean previous() throws SQLException {
        checkClosed();
        throw new SQLException("Result set is TYPE_FORWARD_ONLY");
    }

    @Override
    public void setFetchDirection(final int direction) throws SQLException {
        checkClosed();
        if (direction != FETCH_FORWARD) {
            throw new SQLException("Only FETCH_FORWARD is supported");
        }
    }

    @Override
    public int getFetchDirection() throws SQLException {
        checkClosed();
        return FETCH_FORWARD;
    }

    @Override
    public void setFetchSize(final int rows) throws SQLException {
        checkClosed();
        if (rows < 0) {
            throw new SQLException("Fetch size must be >= 0");
        }
        this.fetchSize = rows > 0 ? rows : fetchSize;
    }

    @Override
    public int getFetchSize() throws SQLException {
        checkClosed();
        return fetchSize;
    }

    @Override
    public int getType() throws SQLException {
        checkClosed();
        return TYPE_FORWARD_ONLY;
    }

    @Override
    public int getConcurrency() throws SQLException {
        checkClosed();
        return CONCUR_READ_ONLY;
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        checkClosed();
        return false;
    }

    @Override
    public boolean rowInserted() throws SQLException {
        checkClosed();
        return false;
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        checkClosed();
        return false;
    }

    @Override
    public void updateNull(final int columnIndex) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateNull(final String columnLabel) throws SQLException {
        updateNull(findColumn(columnLabel));
    }

    @Override
    public void updateBoolean(final int columnIndex, final boolean x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateBoolean(final String columnLabel, final boolean x) throws SQLException {
        updateBoolean(findColumn(columnLabel), x);
    }

    @Override
    public void updateByte(final int columnIndex, final byte x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateByte(final String columnLabel, final byte x) throws SQLException {
        updateByte(findColumn(columnLabel), x);
    }

    @Override
    public void updateShort(final int columnIndex, final short x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateShort(final String columnLabel, final short x) throws SQLException {
        updateShort(findColumn(columnLabel), x);
    }

    @Override
    public void updateInt(final int columnIndex, final int x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateInt(final String columnLabel, final int x) throws SQLException {
        updateInt(findColumn(columnLabel), x);
    }

    @Override
    public void updateLong(final int columnIndex, final long x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateLong(final String columnLabel, final long x) throws SQLException {
        updateLong(findColumn(columnLabel), x);
    }

    @Override
    public void updateFloat(final int columnIndex, final float x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateFloat(final String columnLabel, final float x) throws SQLException {
        updateFloat(findColumn(columnLabel), x);
    }

    @Override
    public void updateDouble(final int columnIndex, final double x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateDouble(final String columnLabel, final double x) throws SQLException {
        updateDouble(findColumn(columnLabel), x);
    }

    @Override
    public void updateBigDecimal(final int columnIndex, final BigDecimal x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateBigDecimal(final String columnLabel, final BigDecimal x) throws SQLException {
        updateBigDecimal(findColumn(columnLabel), x);
    }

    @Override
    public void updateString(final int columnIndex, final String x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateString(final String columnLabel, final String x) throws SQLException {
        updateString(findColumn(columnLabel), x);
    }

    @Override
    public void updateBytes(final int columnIndex, final byte[] x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateBytes(final String columnLabel, final byte[] x) throws SQLException {
        updateBytes(findColumn(columnLabel), x);
    }

    @Override
    public void updateDate(final int columnIndex, final Date x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateDate(final String columnLabel, final Date x) throws SQLException {
        updateDate(findColumn(columnLabel), x);
    }

    @Override
    public void updateTime(final int columnIndex, final Time x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateTime(final String columnLabel, final Time x) throws SQLException {
        updateTime(findColumn(columnLabel), x);
    }

    @Override
    public void updateTimestamp(final int columnIndex, final Timestamp x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateTimestamp(final String columnLabel, final Timestamp x) throws SQLException {
        updateTimestamp(findColumn(columnLabel), x);
    }

    @Override
    public void updateAsciiStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateAsciiStream(final int columnIndex, final InputStream x, final long length) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateAsciiStream(final int columnIndex, final InputStream x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateAsciiStream(final String columnLabel, final InputStream x, final int length) throws SQLException {
        updateAsciiStream(findColumn(columnLabel), x, length);
    }

    @Override
    public void updateAsciiStream(final String columnLabel, final InputStream x, final long length) throws SQLException {
        updateAsciiStream(findColumn(columnLabel), x, length);
    }

    @Override
    public void updateAsciiStream(final String columnLabel, final InputStream x) throws SQLException {
        updateAsciiStream(findColumn(columnLabel), x);
    }

    @Override
    public void updateBinaryStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateBinaryStream(final int columnIndex, final InputStream x, final long length) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateBinaryStream(final int columnIndex, final InputStream x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateBinaryStream(final String columnLabel, final InputStream x, final int length) throws SQLException {
        updateBinaryStream(findColumn(columnLabel), x, length);
    }

    @Override
    public void updateBinaryStream(final String columnLabel, final InputStream x, final long length) throws SQLException {
        updateBinaryStream(findColumn(columnLabel), x, length);
    }

    @Override
    public void updateBinaryStream(final String columnLabel, final InputStream x) throws SQLException {
        updateBinaryStream(findColumn(columnLabel), x);
    }

    @Override
    public void updateCharacterStream(final int columnIndex, final Reader x, final int length) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateCharacterStream(final int columnIndex, final Reader x, final long length) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateCharacterStream(final int columnIndex, final Reader x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateCharacterStream(final String columnLabel, final Reader x, final int length) throws SQLException {
        updateCharacterStream(findColumn(columnLabel), x, length);
    }

    @Override
    public void updateCharacterStream(final String columnLabel, final Reader reader, final long length) throws SQLException {
        updateCharacterStream(findColumn(columnLabel), reader, length);
    }

    @Override
    public void updateCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
        updateCharacterStream(findColumn(columnLabel), reader);
    }

    @Override
    public void updateObject(final int columnIndex, final Object x, final int scaleOrLength) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateObject(final int columnIndex, final Object x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateObject(final String columnLabel, final Object x, final int scaleOrLength) throws SQLException {
        updateObject(findColumn(columnLabel), x, scaleOrLength);
    }

    @Override
    public void updateObject(final String columnLabel, final Object x) throws SQLException {
        updateObject(findColumn(columnLabel), x);
    }

    @Override
    public void insertRow() throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateRow() throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void deleteRow() throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void refreshRow() throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public Statement getStatement() throws SQLException {
        checkClosed();
        return statement;
    }

    @Override
    public Ref getRef(final int columnIndex) throws SQLException {
        checkClosed();
        throw new SQLException("Ref not supported");
    }

    @Override
    public Ref getRef(final String columnLabel) throws SQLException {
        return getRef(findColumn(columnLabel));
    }

    @Override
    public Blob getBlob(final int columnIndex) throws SQLException {
        checkClosed();
        throw new SQLException("Blob not supported");
    }

    @Override
    public Blob getBlob(final String columnLabel) throws SQLException {
        return getBlob(findColumn(columnLabel));
    }

    @Override
    public Clob getClob(final int columnIndex) throws SQLException {
        checkClosed();
        throw new SQLException("Clob not supported");
    }

    @Override
    public Clob getClob(final String columnLabel) throws SQLException {
        return getClob(findColumn(columnLabel));
    }

    @Override
    public Array getArray(final int columnIndex) throws SQLException {
        checkClosed();
        throw new SQLException("Array not supported");
    }

    @Override
    public Array getArray(final String columnLabel) throws SQLException {
        return getArray(findColumn(columnLabel));
    }

    @Override
    public URL getURL(final int columnIndex) throws SQLException {
        checkClosed();
        Object value = getColumnValue(columnIndex);
        wasNull = value == null;
        if (wasNull) {
            return null;
        }
        try {
            return new URL(value.toString());
            // CHECKSTYLE:OFF: IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON: IllegalCatch
            throw new SQLException("Invalid URL: " + value, ex);
        }
    }

    @Override
    public URL getURL(final String columnLabel) throws SQLException {
        return getURL(findColumn(columnLabel));
    }

    @Override
    public void updateRef(final int columnIndex, final Ref x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateRef(final String columnLabel, final Ref x) throws SQLException {
        updateRef(findColumn(columnLabel), x);
    }

    @Override
    public void updateBlob(final int columnIndex, final Blob x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateBlob(final int columnIndex, final InputStream inputStream, final long length) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateBlob(final int columnIndex, final InputStream inputStream) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateBlob(final String columnLabel, final Blob x) throws SQLException {
        updateBlob(findColumn(columnLabel), x);
    }

    @Override
    public void updateBlob(final String columnLabel, final InputStream inputStream, final long length) throws SQLException {
        updateBlob(findColumn(columnLabel), inputStream, length);
    }

    @Override
    public void updateBlob(final String columnLabel, final InputStream inputStream) throws SQLException {
        updateBlob(findColumn(columnLabel), inputStream);
    }

    @Override
    public void updateClob(final int columnIndex, final Clob x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateClob(final int columnIndex, final Reader reader) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateClob(final String columnLabel, final Clob x) throws SQLException {
        updateClob(findColumn(columnLabel), x);
    }

    @Override
    public void updateClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
        updateClob(findColumn(columnLabel), reader, length);
    }

    @Override
    public void updateClob(final String columnLabel, final Reader reader) throws SQLException {
        updateClob(findColumn(columnLabel), reader);
    }

    @Override
    public void updateArray(final int columnIndex, final Array x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateArray(final String columnLabel, final Array x) throws SQLException {
        updateArray(findColumn(columnLabel), x);
    }

    @Override
    public RowId getRowId(final int columnIndex) throws SQLException {
        checkClosed();
        throw new SQLException("RowId not supported");
    }

    @Override
    public RowId getRowId(final String columnLabel) throws SQLException {
        return getRowId(findColumn(columnLabel));
    }

    @Override
    public void updateRowId(final int columnIndex, final RowId x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateRowId(final String columnLabel, final RowId x) throws SQLException {
        updateRowId(findColumn(columnLabel), x);
    }

    @Override
    public int getHoldability() throws SQLException {
        checkClosed();
        return HOLD_CURSORS_OVER_COMMIT;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    @Override
    public void updateNString(final int columnIndex, final String nString) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateNString(final String columnLabel, final String nString) throws SQLException {
        updateNString(findColumn(columnLabel), nString);
    }

    @Override
    public void updateNClob(final int columnIndex, final NClob nClob) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateNClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateNClob(final int columnIndex, final Reader reader) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateNClob(final String columnLabel, final NClob nClob) throws SQLException {
        updateNClob(findColumn(columnLabel), nClob);
    }

    @Override
    public void updateNClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
        updateNClob(findColumn(columnLabel), reader, length);
    }

    @Override
    public void updateNClob(final String columnLabel, final Reader reader) throws SQLException {
        updateNClob(findColumn(columnLabel), reader);
    }

    @Override
    public NClob getNClob(final int columnIndex) throws SQLException {
        checkClosed();
        throw new SQLException("NClob not supported");
    }

    @Override
    public NClob getNClob(final String columnLabel) throws SQLException {
        return getNClob(findColumn(columnLabel));
    }

    @Override
    public SQLXML getSQLXML(final int columnIndex) throws SQLException {
        checkClosed();
        throw new SQLException("SQLXML not supported");
    }

    @Override
    public SQLXML getSQLXML(final String columnLabel) throws SQLException {
        return getSQLXML(findColumn(columnLabel));
    }

    @Override
    public void updateSQLXML(final int columnIndex, final SQLXML xmlObject) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateSQLXML(final String columnLabel, final SQLXML xmlObject) throws SQLException {
        updateSQLXML(findColumn(columnLabel), xmlObject);
    }

    @Override
    public String getNString(final int columnIndex) throws SQLException {
        return getString(columnIndex);
    }

    @Override
    public String getNString(final String columnLabel) throws SQLException {
        return getString(columnLabel);
    }

    @Override
    public Reader getNCharacterStream(final int columnIndex) throws SQLException {
        checkClosed();
        throw new SQLException("NCharacterStream not supported");
    }

    @Override
    public Reader getNCharacterStream(final String columnLabel) throws SQLException {
        return getNCharacterStream(findColumn(columnLabel));
    }

    @Override
    public void updateNCharacterStream(final int columnIndex, final Reader x, final long length) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateNCharacterStream(final int columnIndex, final Reader x) throws SQLException {
        checkClosed();
        throw new SQLException("Result set is CONCUR_READ_ONLY");
    }

    @Override
    public void updateNCharacterStream(final String columnLabel, final Reader reader, final long length) throws SQLException {
        updateNCharacterStream(findColumn(columnLabel), reader, length);
    }

    @Override
    public void updateNCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
        updateNCharacterStream(findColumn(columnLabel), reader);
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

    private Object getColumnValue(final int columnIndex) throws SQLException {
        if (currentRow == null) {
            throw new SQLException("No current row");
        }
        if (columnIndex < 1 || columnIndex > currentRow.size()) {
            throw new SQLException("Invalid column index: " + columnIndex);
        }
        return currentRow.get(columnIndex - 1);
    }

    private void checkClosed() throws SQLException {
        if (closed) {
            throw new SQLException(RS_CLOSED);
        }
    }
}
