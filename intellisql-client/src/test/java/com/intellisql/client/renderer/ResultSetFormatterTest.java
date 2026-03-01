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

package com.intellisql.client.renderer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for ResultSetFormatter.
 * Uses stub implementations instead of Mockito to ensure compatibility with JDK 21+
 * where mocking java.sql interfaces is restricted.
 */
class ResultSetFormatterTest {

    @Test
    void testFormatHeader() throws SQLException {
        ResultSetMetaData metaData = new StubResultSetMetaData(
                2,
                Arrays.asList("ID", "Name"),
                new int[]{2, 10});
        int[] widths = new int[]{2, 10};
        List<String> header = ResultSetFormatter.formatHeader(metaData, widths);
        Assertions.assertEquals(3, header.size());
        Assertions.assertEquals("+----+------------+", header.get(0));
        Assertions.assertEquals("| ID | Name       |", header.get(1));
        Assertions.assertEquals("+----+------------+", header.get(2));
    }

    @Test
    void testCalculateColumnWidths() throws SQLException {
        ResultSetMetaData metaData = new StubResultSetMetaData(
                2,
                Arrays.asList("ID", "Name"),
                new int[]{5, 20});
        int[] widths = ResultSetFormatter.calculateColumnWidths(metaData);
        // Max(8, 2, 5) -> 8
        Assertions.assertEquals(8, widths[0]);
        // Max(8, 4, 20) -> 20
        Assertions.assertEquals(20, widths[1]);
    }

    @Test
    void testFormatRow() throws SQLException {
        ResultSetMetaData metaData = new StubResultSetMetaData(
                2,
                Arrays.asList("ID", "Name"),
                new int[]{2, 6});
        ResultSet rs = new StubResultSet(metaData, new Object[]{1, "Test"});
        int[] widths = new int[]{2, 6};
        String row = ResultSetFormatter.formatRow(rs, widths);
        Assertions.assertEquals("| 1  | Test   |", row);
    }

    @Test
    void testFormatRowWithNull() throws SQLException {
        ResultSetMetaData metaData = new StubResultSetMetaData(
                2,
                Arrays.asList("ID", "Name"),
                new int[]{4, 4});
        ResultSet rs = new StubResultSet(metaData, new Object[]{null, null});
        int[] widths = new int[]{4, 4};
        String row = ResultSetFormatter.formatRow(rs, widths);
        Assertions.assertEquals("| null | null |", row);
    }

    @Test
    void testFormatSeparator() {
        int[] widths = new int[]{2, 4};
        String separator = ResultSetFormatter.formatSeparator(widths);
        Assertions.assertEquals("+----+------+", separator);
    }

    // CHECKSTYLE:OFF - Stub implementation for testing, minimal code style required
    /**
     * Stub implementation of ResultSetMetaData for testing.
     */
    private static class StubResultSetMetaData implements ResultSetMetaData {

        private final int columnCount;
        private final List<String> columnLabels;
        private final int[] displaySizes;

        StubResultSetMetaData(int columnCount, List<String> columnLabels, int[] displaySizes) {
            this.columnCount = columnCount;
            this.columnLabels = columnLabels;
            this.displaySizes = displaySizes;
        }

        @Override
        public int getColumnCount() {
            return columnCount;
        }

        @Override
        public String getColumnLabel(int column) {
            return columnLabels.get(column - 1);
        }

        @Override
        public int getColumnDisplaySize(int column) {
            return displaySizes[column - 1];
        }

        // Required stub methods - return defaults
        @Override
        public int getColumnType(int column) {
            return 0;
        }

        @Override
        public String getColumnTypeName(int column) {
            return null;
        }

        @Override
        public String getColumnName(int column) {
            return columnLabels.get(column - 1);
        }

        @Override
        public boolean isAutoIncrement(int column) {
            return false;
        }

        @Override
        public boolean isCaseSensitive(int column) {
            return false;
        }

        @Override
        public boolean isSearchable(int column) {
            return false;
        }

        @Override
        public boolean isCurrency(int column) {
            return false;
        }

        @Override
        public int isNullable(int column) {
            return columnNullableUnknown;
        }

        @Override
        public boolean isSigned(int column) {
            return false;
        }

        @Override
        public int getPrecision(int column) {
            return 0;
        }

        @Override
        public int getScale(int column) {
            return 0;
        }

        @Override
        public String getSchemaName(int column) {
            return null;
        }

        @Override
        public String getTableName(int column) {
            return null;
        }

        @Override
        public String getCatalogName(int column) {
            return null;
        }

        @Override
        public boolean isReadOnly(int column) {
            return false;
        }

        @Override
        public boolean isWritable(int column) {
            return false;
        }

        @Override
        public boolean isDefinitelyWritable(int column) {
            return false;
        }

        @Override
        public String getColumnClassName(int column) {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }

    /**
     * Stub implementation of ResultSet for testing.
     */
    private static class StubResultSet implements ResultSet {

        private final ResultSetMetaData metaData;
        private final Object[] values;

        StubResultSet(ResultSetMetaData metaData, Object[] values) {
            this.metaData = metaData;
            this.values = values;
        }

        @Override
        public ResultSetMetaData getMetaData() {
            return metaData;
        }

        @Override
        public Object getObject(int columnIndex) {
            return values[columnIndex - 1];
        }

        // Required stub methods - return defaults or throw
        @Override
        public boolean next() {
            return false;
        }

        @Override
        public void close() {
        }

        @Override
        public boolean wasNull() {
            return false;
        }

        @Override
        public String getString(int columnIndex) {
            return null;
        }

        @Override
        public boolean getBoolean(int columnIndex) {
            return false;
        }

        @Override
        public byte getByte(int columnIndex) {
            return 0;
        }

        @Override
        public short getShort(int columnIndex) {
            return 0;
        }

        @Override
        public int getInt(int columnIndex) {
            return 0;
        }

        @Override
        public long getLong(int columnIndex) {
            return 0;
        }

        @Override
        public float getFloat(int columnIndex) {
            return 0;
        }

        @Override
        public double getDouble(int columnIndex) {
            return 0;
        }

        @Override
        public byte[] getBytes(int columnIndex) {
            return new byte[0];
        }

        @Override
        public java.sql.Date getDate(int columnIndex) {
            return null;
        }

        @Override
        public java.sql.Time getTime(int columnIndex) {
            return null;
        }

        @Override
        public java.sql.Timestamp getTimestamp(int columnIndex) {
            return null;
        }

        @Override
        public java.io.InputStream getAsciiStream(int columnIndex) {
            return null;
        }

        @Override
        public java.io.InputStream getUnicodeStream(int columnIndex) {
            return null;
        }

        @Override
        public java.io.InputStream getBinaryStream(int columnIndex) {
            return null;
        }

        @Override
        public String getString(String columnLabel) {
            return null;
        }

        @Override
        public boolean getBoolean(String columnLabel) {
            return false;
        }

        @Override
        public byte getByte(String columnLabel) {
            return 0;
        }

        @Override
        public short getShort(String columnLabel) {
            return 0;
        }

        @Override
        public int getInt(String columnLabel) {
            return 0;
        }

        @Override
        public long getLong(String columnLabel) {
            return 0;
        }

        @Override
        public float getFloat(String columnLabel) {
            return 0;
        }

        @Override
        public double getDouble(String columnLabel) {
            return 0;
        }

        @Override
        public byte[] getBytes(String columnLabel) {
            return new byte[0];
        }

        @Override
        public java.sql.Date getDate(String columnLabel) {
            return null;
        }

        @Override
        public java.sql.Time getTime(String columnLabel) {
            return null;
        }

        @Override
        public java.sql.Timestamp getTimestamp(String columnLabel) {
            return null;
        }

        @Override
        public java.io.InputStream getAsciiStream(String columnLabel) {
            return null;
        }

        @Override
        public java.io.InputStream getUnicodeStream(String columnLabel) {
            return null;
        }

        @Override
        public java.io.InputStream getBinaryStream(String columnLabel) {
            return null;
        }

        @Override
        public SQLWarning getWarnings() {
            return null;
        }

        @Override
        public void clearWarnings() {
        }

        @Override
        public String getCursorName() {
            return null;
        }

        @Override
        public int findColumn(String columnLabel) {
            return 0;
        }

        @Override
        public java.io.Reader getCharacterStream(int columnIndex) {
            return null;
        }

        @Override
        public java.io.Reader getCharacterStream(String columnLabel) {
            return null;
        }

        @Override
        public BigDecimal getBigDecimal(int columnIndex) {
            return null;
        }

        @Override
        public BigDecimal getBigDecimal(int columnIndex, int scale) {
            return null;
        }

        @Override
        public BigDecimal getBigDecimal(String columnLabel) {
            return null;
        }

        @Override
        public BigDecimal getBigDecimal(String columnLabel, int scale) {
            return null;
        }

        @Override
        public boolean isBeforeFirst() {
            return false;
        }

        @Override
        public boolean isAfterLast() {
            return false;
        }

        @Override
        public boolean isFirst() {
            return false;
        }

        @Override
        public boolean isLast() {
            return false;
        }

        @Override
        public void beforeFirst() {
        }

        @Override
        public void afterLast() {
        }

        @Override
        public boolean first() {
            return false;
        }

        @Override
        public boolean last() {
            return false;
        }

        @Override
        public int getRow() {
            return 0;
        }

        @Override
        public boolean absolute(int row) {
            return false;
        }

        @Override
        public boolean relative(int rows) {
            return false;
        }

        @Override
        public boolean previous() {
            return false;
        }

        @Override
        public void setFetchDirection(int direction) {
        }

        @Override
        public int getFetchDirection() {
            return FETCH_FORWARD;
        }

        @Override
        public void setFetchSize(int rows) {
        }

        @Override
        public int getFetchSize() {
            return 0;
        }

        @Override
        public int getType() {
            return TYPE_FORWARD_ONLY;
        }

        @Override
        public int getConcurrency() {
            return CONCUR_READ_ONLY;
        }

        @Override
        public boolean rowUpdated() {
            return false;
        }

        @Override
        public boolean rowInserted() {
            return false;
        }

        @Override
        public boolean rowDeleted() {
            return false;
        }

        @Override
        public void updateNull(int columnIndex) {
        }

        @Override
        public void updateBoolean(int columnIndex, boolean x) {
        }

        @Override
        public void updateByte(int columnIndex, byte x) {
        }

        @Override
        public void updateShort(int columnIndex, short x) {
        }

        @Override
        public void updateInt(int columnIndex, int x) {
        }

        @Override
        public void updateLong(int columnIndex, long x) {
        }

        @Override
        public void updateFloat(int columnIndex, float x) {
        }

        @Override
        public void updateDouble(int columnIndex, double x) {
        }

        @Override
        public void updateBigDecimal(int columnIndex, BigDecimal x) {
        }

        @Override
        public void updateString(int columnIndex, String x) {
        }

        @Override
        public void updateBytes(int columnIndex, byte[] x) {
        }

        @Override
        public void updateDate(int columnIndex, java.sql.Date x) {
        }

        @Override
        public void updateTime(int columnIndex, java.sql.Time x) {
        }

        @Override
        public void updateTimestamp(int columnIndex, java.sql.Timestamp x) {
        }

        @Override
        public void updateAsciiStream(int columnIndex, java.io.InputStream x, int length) {
        }

        @Override
        public void updateBinaryStream(int columnIndex, java.io.InputStream x, int length) {
        }

        @Override
        public void updateCharacterStream(int columnIndex, java.io.Reader x, int length) {
        }

        @Override
        public void updateObject(int columnIndex, Object x, int scaleOrLength) {
        }

        @Override
        public void updateObject(int columnIndex, Object x) {
        }

        @Override
        public void updateNull(String columnLabel) {
        }

        @Override
        public void updateBoolean(String columnLabel, boolean x) {
        }

        @Override
        public void updateByte(String columnLabel, byte x) {
        }

        @Override
        public void updateShort(String columnLabel, short x) {
        }

        @Override
        public void updateInt(String columnLabel, int x) {
        }

        @Override
        public void updateLong(String columnLabel, long x) {
        }

        @Override
        public void updateFloat(String columnLabel, float x) {
        }

        @Override
        public void updateDouble(String columnLabel, double x) {
        }

        @Override
        public void updateBigDecimal(String columnLabel, BigDecimal x) {
        }

        @Override
        public void updateString(String columnLabel, String x) {
        }

        @Override
        public void updateBytes(String columnLabel, byte[] x) {
        }

        @Override
        public void updateDate(String columnLabel, java.sql.Date x) {
        }

        @Override
        public void updateTime(String columnLabel, java.sql.Time x) {
        }

        @Override
        public void updateTimestamp(String columnLabel, java.sql.Timestamp x) {
        }

        @Override
        public void updateAsciiStream(String columnLabel, java.io.InputStream x, int length) {
        }

        @Override
        public void updateBinaryStream(String columnLabel, java.io.InputStream x, int length) {
        }

        @Override
        public void updateCharacterStream(String columnLabel, java.io.Reader x, int length) {
        }

        @Override
        public void updateObject(String columnLabel, Object x, int scaleOrLength) {
        }

        @Override
        public void updateObject(String columnLabel, Object x) {
        }

        @Override
        public void insertRow() {
        }

        @Override
        public void updateRow() {
        }

        @Override
        public void deleteRow() {
        }

        @Override
        public void refreshRow() {
        }

        @Override
        public void cancelRowUpdates() {
        }

        @Override
        public void moveToInsertRow() {
        }

        @Override
        public void moveToCurrentRow() {
        }

        @Override
        public Statement getStatement() {
            return null;
        }

        @Override
        public Object getObject(String columnLabel) {
            return null;
        }

        @Override
        public Object getObject(int columnIndex, java.util.Map<String, Class<?>> map) {
            return null;
        }

        @Override
        public Object getObject(String columnLabel, java.util.Map<String, Class<?>> map) {
            return null;
        }

        @Override
        public java.sql.Ref getRef(int columnIndex) {
            return null;
        }

        @Override
        public java.sql.Ref getRef(String columnLabel) {
            return null;
        }

        @Override
        public java.sql.Blob getBlob(int columnIndex) {
            return null;
        }

        @Override
        public java.sql.Blob getBlob(String columnLabel) {
            return null;
        }

        @Override
        public java.sql.Clob getClob(int columnIndex) {
            return null;
        }

        @Override
        public java.sql.Clob getClob(String columnLabel) {
            return null;
        }

        @Override
        public java.sql.Array getArray(int columnIndex) {
            return null;
        }

        @Override
        public java.sql.Array getArray(String columnLabel) {
            return null;
        }

        @Override
        public java.sql.Date getDate(int columnIndex, java.util.Calendar cal) {
            return null;
        }

        @Override
        public java.sql.Date getDate(String columnLabel, java.util.Calendar cal) {
            return null;
        }

        @Override
        public java.sql.Time getTime(int columnIndex, java.util.Calendar cal) {
            return null;
        }

        @Override
        public java.sql.Time getTime(String columnLabel, java.util.Calendar cal) {
            return null;
        }

        @Override
        public java.sql.Timestamp getTimestamp(int columnIndex, java.util.Calendar cal) {
            return null;
        }

        @Override
        public java.sql.Timestamp getTimestamp(String columnLabel, java.util.Calendar cal) {
            return null;
        }

        @Override
        public java.net.URL getURL(int columnIndex) {
            return null;
        }

        @Override
        public java.net.URL getURL(String columnLabel) {
            return null;
        }

        @Override
        public void updateRef(int columnIndex, java.sql.Ref x) {
        }

        @Override
        public void updateRef(String columnLabel, java.sql.Ref x) {
        }

        @Override
        public void updateBlob(int columnIndex, java.sql.Blob x) {
        }

        @Override
        public void updateBlob(String columnLabel, java.sql.Blob x) {
        }

        @Override
        public void updateClob(int columnIndex, java.sql.Clob x) {
        }

        @Override
        public void updateClob(String columnLabel, java.sql.Clob x) {
        }

        @Override
        public void updateArray(int columnIndex, java.sql.Array x) {
        }

        @Override
        public void updateArray(String columnLabel, java.sql.Array x) {
        }

        @Override
        public java.sql.RowId getRowId(int columnIndex) {
            return null;
        }

        @Override
        public java.sql.RowId getRowId(String columnLabel) {
            return null;
        }

        @Override
        public void updateRowId(int columnIndex, java.sql.RowId x) {
        }

        @Override
        public void updateRowId(String columnLabel, java.sql.RowId x) {
        }

        @Override
        public int getHoldability() {
            return HOLD_CURSORS_OVER_COMMIT;
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void updateNString(int columnIndex, String nString) {
        }

        @Override
        public void updateNString(String columnLabel, String nString) {
        }

        @Override
        public void updateNClob(int columnIndex, java.sql.NClob nClob) {
        }

        @Override
        public void updateNClob(String columnLabel, java.sql.NClob nClob) {
        }

        @Override
        public java.sql.NClob getNClob(int columnIndex) {
            return null;
        }

        @Override
        public java.sql.NClob getNClob(String columnLabel) {
            return null;
        }

        @Override
        public java.sql.SQLXML getSQLXML(int columnIndex) {
            return null;
        }

        @Override
        public java.sql.SQLXML getSQLXML(String columnLabel) {
            return null;
        }

        @Override
        public void updateSQLXML(int columnIndex, java.sql.SQLXML xmlObject) {
        }

        @Override
        public void updateSQLXML(String columnLabel, java.sql.SQLXML xmlObject) {
        }

        @Override
        public String getNString(int columnIndex) {
            return null;
        }

        @Override
        public String getNString(String columnLabel) {
            return null;
        }

        @Override
        public java.io.Reader getNCharacterStream(int columnIndex) {
            return null;
        }

        @Override
        public java.io.Reader getNCharacterStream(String columnLabel) {
            return null;
        }

        @Override
        public void updateNCharacterStream(int columnIndex, java.io.Reader x, long length) {
        }

        @Override
        public void updateNCharacterStream(String columnLabel, java.io.Reader x, long length) {
        }

        @Override
        public void updateAsciiStream(int columnIndex, java.io.InputStream x, long length) {
        }

        @Override
        public void updateAsciiStream(String columnLabel, java.io.InputStream x, long length) {
        }

        @Override
        public void updateBinaryStream(int columnIndex, java.io.InputStream x, long length) {
        }

        @Override
        public void updateBinaryStream(String columnLabel, java.io.InputStream x, long length) {
        }

        @Override
        public void updateCharacterStream(int columnIndex, java.io.Reader x, long length) {
        }

        @Override
        public void updateCharacterStream(String columnLabel, java.io.Reader x, long length) {
        }

        @Override
        public void updateBlob(int columnIndex, java.io.InputStream inputStream, long length) {
        }

        @Override
        public void updateBlob(String columnLabel, java.io.InputStream inputStream, long length) {
        }

        @Override
        public void updateClob(int columnIndex, java.io.Reader reader, long length) {
        }

        @Override
        public void updateClob(String columnLabel, java.io.Reader reader, long length) {
        }

        @Override
        public void updateNClob(int columnIndex, java.io.Reader reader, long length) {
        }

        @Override
        public void updateNClob(String columnLabel, java.io.Reader reader, long length) {
        }

        @Override
        public void updateNCharacterStream(int columnIndex, java.io.Reader x) {
        }

        @Override
        public void updateNCharacterStream(String columnLabel, java.io.Reader x) {
        }

        @Override
        public void updateAsciiStream(int columnIndex, java.io.InputStream x) {
        }

        @Override
        public void updateAsciiStream(String columnLabel, java.io.InputStream x) {
        }

        @Override
        public void updateBinaryStream(int columnIndex, java.io.InputStream x) {
        }

        @Override
        public void updateBinaryStream(String columnLabel, java.io.InputStream x) {
        }

        @Override
        public void updateCharacterStream(int columnIndex, java.io.Reader x) {
        }

        @Override
        public void updateCharacterStream(String columnLabel, java.io.Reader x) {
        }

        @Override
        public void updateBlob(int columnIndex, java.io.InputStream x) {
        }

        @Override
        public void updateBlob(String columnLabel, java.io.InputStream x) {
        }

        @Override
        public void updateClob(int columnIndex, java.io.Reader x) {
        }

        @Override
        public void updateClob(String columnLabel, java.io.Reader x) {
        }

        @Override
        public void updateNClob(int columnIndex, java.io.Reader x) {
        }

        @Override
        public void updateNClob(String columnLabel, java.io.Reader x) {
        }

        @Override
        public <T> T getObject(int columnIndex, Class<T> type) {
            return null;
        }

        @Override
        public <T> T getObject(String columnLabel, Class<T> type) {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
    // CHECKSTYLE:ON
}
