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

package com.intellisql.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.calcite.avatica.ColumnMetaData;
import org.apache.calcite.avatica.Meta;
import org.apache.calcite.avatica.remote.Service;
import org.apache.calcite.avatica.remote.TypedValue;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** JDBC PreparedStatement implementation for IntelliSql. */
@Slf4j
@Getter
public class IntelliSqlPreparedStatement extends IntelliSqlStatement implements PreparedStatement {

    private final String sql;

    private final List<ParameterValue> parameters;

    private final List<List<ParameterValue>> batchParameters;

    /**
     * Creates a new prepared statement.
     *
     * @param connection the parent connection
     * @param statementHandle the statement handle
     * @param sql the SQL statement
     */
    public IntelliSqlPreparedStatement(
                                       final IntelliSqlConnection connection, final Meta.StatementHandle statementHandle,
                                       final String sql) {
        super(connection, statementHandle.id);
        this.statementHandle = statementHandle;
        this.sql = sql;
        this.parameters = new ArrayList<>();
        this.batchParameters = new ArrayList<>();
        log.debug("Created prepared statement for SQL: {}", sql);
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        checkClosed();
        closeCurrentResultSet();
        Service.ExecuteResponse executeResponse =
                connection.getClient().execute(statementHandle, createParameterValues(), fetchSize);
        currentResultSet = createResultSet(statementHandle, executeResponse);
        updateCount = -1;
        return currentResultSet;
    }

    @Override
    public ResultSet executeQuery(final String sql) throws SQLException {
        throw new SQLException("Cannot execute query on PreparedStatement with SQL parameter");
    }

    @Override
    public int executeUpdate() throws SQLException {
        checkClosed();
        closeCurrentResultSet();
        Service.ExecuteResponse executeResponse =
                connection.getClient().execute(statementHandle, createParameterValues(), fetchSize);
        updateCount = getResponseUpdateCount(executeResponse);
        currentResultSet = null;
        return (int) updateCount;
    }

    @Override
    public int executeUpdate(final String sql) throws SQLException {
        throw new SQLException("Cannot execute update on PreparedStatement with SQL parameter");
    }

    @Override
    public boolean execute() throws SQLException {
        checkClosed();
        closeCurrentResultSet();
        Service.ExecuteResponse executeResponse =
                connection.getClient().execute(statementHandle, createParameterValues(), fetchSize);
        updateCount = getResponseUpdateCount(executeResponse);
        if (updateCount >= 0) {
            currentResultSet = null;
            return false;
        }
        currentResultSet = createResultSet(statementHandle, executeResponse);
        return true;
    }

    @Override
    public boolean execute(final String sql) throws SQLException {
        throw new SQLException("Cannot execute on PreparedStatement with SQL parameter");
    }

    @Override
    public void setNull(final int parameterIndex, final int sqlType) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, null, sqlType);
    }

    @Override
    public void setNull(final int parameterIndex, final int sqlType, final String typeName) throws SQLException {
        setNull(parameterIndex, sqlType);
    }

    @Override
    public void setBoolean(final int parameterIndex, final boolean x) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, x, Types.BOOLEAN);
    }

    @Override
    public void setByte(final int parameterIndex, final byte x) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, x, Types.TINYINT);
    }

    @Override
    public void setShort(final int parameterIndex, final short x) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, x, Types.SMALLINT);
    }

    @Override
    public void setInt(final int parameterIndex, final int x) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, x, Types.INTEGER);
    }

    @Override
    public void setLong(final int parameterIndex, final long x) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, x, Types.BIGINT);
    }

    @Override
    public void setFloat(final int parameterIndex, final float x) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, x, Types.FLOAT);
    }

    @Override
    public void setDouble(final int parameterIndex, final double x) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, x, Types.DOUBLE);
    }

    @Override
    public void setBigDecimal(final int parameterIndex, final BigDecimal x) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, x, Types.DECIMAL);
    }

    @Override
    public void setString(final int parameterIndex, final String x) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, x, Types.VARCHAR);
    }

    @Override
    public void setBytes(final int parameterIndex, final byte[] x) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, x, Types.VARBINARY);
    }

    @Override
    public void setDate(final int parameterIndex, final Date x) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, x, Types.DATE);
    }

    @Override
    public void setDate(final int parameterIndex, final Date x, final Calendar cal) throws SQLException {
        setDate(parameterIndex, x);
    }

    @Override
    public void setTime(final int parameterIndex, final Time x) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, x, Types.TIME);
    }

    @Override
    public void setTime(final int parameterIndex, final Time x, final Calendar cal) throws SQLException {
        setTime(parameterIndex, x);
    }

    @Override
    public void setTimestamp(final int parameterIndex, final Timestamp x) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, x, Types.TIMESTAMP);
    }

    @Override
    public void setTimestamp(final int parameterIndex, final Timestamp x, final Calendar cal) throws SQLException {
        setTimestamp(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
        checkClosed();
        throw new SQLException("AsciiStream not supported");
    }

    @Override
    public void setAsciiStream(final int parameterIndex, final InputStream x, final long length) throws SQLException {
        checkClosed();
        throw new SQLException("AsciiStream not supported");
    }

    @Override
    public void setAsciiStream(final int parameterIndex, final InputStream x) throws SQLException {
        checkClosed();
        throw new SQLException("AsciiStream not supported");
    }

    @Override
    @Deprecated
    public void setUnicodeStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
        checkClosed();
        throw new SQLException("UnicodeStream not supported");
    }

    @Override
    public void setBinaryStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
        checkClosed();
        throw new SQLException("BinaryStream not supported");
    }

    @Override
    public void setBinaryStream(final int parameterIndex, final InputStream x, final long length) throws SQLException {
        checkClosed();
        throw new SQLException("BinaryStream not supported");
    }

    @Override
    public void setBinaryStream(final int parameterIndex, final InputStream x) throws SQLException {
        checkClosed();
        throw new SQLException("BinaryStream not supported");
    }

    @Override
    public void clearParameters() throws SQLException {
        checkClosed();
        parameters.clear();
    }

    @Override
    public void setObject(final int parameterIndex, final Object x, final int targetSqlType) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, x, targetSqlType);
    }

    @Override
    public void setObject(final int parameterIndex, final Object x) throws SQLException {
        checkClosed();
        setParameter(parameterIndex, x, inferSqlType(x));
    }

    @Override
    public void setObject(final int parameterIndex, final Object x, final int targetSqlType, final int scaleOrLength) throws SQLException {
        setObject(parameterIndex, x, targetSqlType);
    }

    @Override
    public void addBatch() throws SQLException {
        checkClosed();
        batchParameters.add(copyParameterValues());
    }

    @Override
    public void addBatch(final String sql) throws SQLException {
        throw new SQLException("Cannot add batch on PreparedStatement with SQL parameter");
    }

    @Override
    public void clearBatch() throws SQLException {
        checkClosed();
        batchParameters.clear();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        checkClosed();
        closeCurrentResultSet();
        int[] result = new int[batchParameters.size()];
        for (int i = 0; i < batchParameters.size(); i++) {
            Service.ExecuteResponse executeResponse =
                    connection.getClient().execute(statementHandle, createParameterValues(batchParameters.get(i)), fetchSize);
            result[i] = (int) getResponseUpdateCount(executeResponse);
        }
        batchParameters.clear();
        currentResultSet = null;
        updateCount = result.length == 0 ? -1L : result[result.length - 1];
        return result;
    }

    @Override
    public void setCharacterStream(final int parameterIndex, final Reader reader, final int length) throws SQLException {
        checkClosed();
        throw new SQLException("CharacterStream not supported");
    }

    @Override
    public void setCharacterStream(final int parameterIndex, final Reader reader, final long length) throws SQLException {
        checkClosed();
        throw new SQLException("CharacterStream not supported");
    }

    @Override
    public void setCharacterStream(final int parameterIndex, final Reader reader) throws SQLException {
        checkClosed();
        throw new SQLException("CharacterStream not supported");
    }

    @Override
    public void setRef(final int parameterIndex, final Ref x) throws SQLException {
        checkClosed();
        throw new SQLException("Ref not supported");
    }

    @Override
    public void setBlob(final int parameterIndex, final Blob x) throws SQLException {
        checkClosed();
        throw new SQLException("Blob not supported");
    }

    @Override
    public void setBlob(final int parameterIndex, final InputStream inputStream, final long length) throws SQLException {
        checkClosed();
        throw new SQLException("Blob not supported");
    }

    @Override
    public void setBlob(final int parameterIndex, final InputStream inputStream) throws SQLException {
        checkClosed();
        throw new SQLException("Blob not supported");
    }

    @Override
    public void setClob(final int parameterIndex, final Clob x) throws SQLException {
        checkClosed();
        throw new SQLException("Clob not supported");
    }

    @Override
    public void setClob(final int parameterIndex, final Reader reader, final long length) throws SQLException {
        checkClosed();
        throw new SQLException("Clob not supported");
    }

    @Override
    public void setClob(final int parameterIndex, final Reader reader) throws SQLException {
        checkClosed();
        throw new SQLException("Clob not supported");
    }

    @Override
    public void setArray(final int parameterIndex, final Array x) throws SQLException {
        checkClosed();
        throw new SQLException("Array not supported");
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        checkClosed();
        return null;
    }

    @Override
    public void setURL(final int parameterIndex, final URL x) throws SQLException {
        checkClosed();
        setString(parameterIndex, x != null ? x.toString() : null);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        checkClosed();
        return null;
    }

    @Override
    public void setRowId(final int parameterIndex, final RowId x) throws SQLException {
        checkClosed();
        throw new SQLException("RowId not supported");
    }

    @Override
    public void setNString(final int parameterIndex, final String value) throws SQLException {
        setString(parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(final int parameterIndex, final Reader value, final long length) throws SQLException {
        checkClosed();
        throw new SQLException("NCharacterStream not supported");
    }

    @Override
    public void setNCharacterStream(final int parameterIndex, final Reader value) throws SQLException {
        checkClosed();
        throw new SQLException("NCharacterStream not supported");
    }

    @Override
    public void setNClob(final int parameterIndex, final NClob value) throws SQLException {
        checkClosed();
        throw new SQLException("NClob not supported");
    }

    @Override
    public void setNClob(final int parameterIndex, final Reader reader, final long length) throws SQLException {
        checkClosed();
        throw new SQLException("NClob not supported");
    }

    @Override
    public void setNClob(final int parameterIndex, final Reader reader) throws SQLException {
        checkClosed();
        throw new SQLException("NClob not supported");
    }

    @Override
    public void setSQLXML(final int parameterIndex, final SQLXML xmlObject) throws SQLException {
        checkClosed();
        throw new SQLException("SQLXML not supported");
    }

    private void setParameter(final int parameterIndex, final Object value, final int sqlType) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException("Parameter index must be >= 1");
        }
        int index = parameterIndex - 1;
        while (parameters.size() <= index) {
            parameters.add(null);
        }
        parameters.set(index, new ParameterValue(value, sqlType));
    }

    private List<TypedValue> createParameterValues() throws SQLException {
        return createParameterValues(parameters);
    }

    private List<TypedValue> createParameterValues(final List<ParameterValue> parameterValues) throws SQLException {
        List<TypedValue> result = new ArrayList<>(parameterValues.size());
        for (int i = 0; i < parameterValues.size(); i++) {
            ParameterValue each = parameterValues.get(i);
            if (each == null) {
                throw new SQLException("Parameter " + (i + 1) + " is not set");
            }
            result.add(each.toTypedValue());
        }
        return result;
    }

    private List<ParameterValue> copyParameterValues() throws SQLException {
        List<ParameterValue> result = new ArrayList<>(parameters.size());
        for (int i = 0; i < parameters.size(); i++) {
            ParameterValue each = parameters.get(i);
            if (each == null) {
                throw new SQLException("Parameter " + (i + 1) + " is not set");
            }
            result.add(each);
        }
        return result;
    }

    private int inferSqlType(final Object value) {
        if (value instanceof Boolean) {
            return Types.BOOLEAN;
        }
        if (value instanceof Byte) {
            return Types.TINYINT;
        }
        if (value instanceof Short) {
            return Types.SMALLINT;
        }
        if (value instanceof Integer) {
            return Types.INTEGER;
        }
        if (value instanceof Long) {
            return Types.BIGINT;
        }
        if (value instanceof Float) {
            return Types.FLOAT;
        }
        if (value instanceof Double) {
            return Types.DOUBLE;
        }
        if (value instanceof BigDecimal) {
            return Types.DECIMAL;
        }
        if (value instanceof Date) {
            return Types.DATE;
        }
        if (value instanceof Time) {
            return Types.TIME;
        }
        if (value instanceof Timestamp) {
            return Types.TIMESTAMP;
        }
        if (value instanceof byte[]) {
            return Types.VARBINARY;
        }
        return Types.VARCHAR;
    }

    private static final class ParameterValue {

        private final Object value;

        private final int sqlType;

        private ParameterValue(final Object value, final int sqlType) {
            this.value = value;
            this.sqlType = sqlType;
        }

        private TypedValue toTypedValue() {
            ColumnMetaData.Rep rep = toRep();
            return value == null ? TypedValue.ofSerial(rep, null) : TypedValue.ofJdbc(rep, value, Calendar.getInstance());
        }

        private ColumnMetaData.Rep toRep() {
            switch (sqlType) {
                case Types.BOOLEAN:
                    return ColumnMetaData.Rep.BOOLEAN;
                case Types.TINYINT:
                    return ColumnMetaData.Rep.BYTE;
                case Types.SMALLINT:
                    return ColumnMetaData.Rep.SHORT;
                case Types.INTEGER:
                    return ColumnMetaData.Rep.INTEGER;
                case Types.BIGINT:
                    return ColumnMetaData.Rep.LONG;
                case Types.FLOAT:
                    return ColumnMetaData.Rep.FLOAT;
                case Types.DOUBLE:
                case Types.REAL:
                    return ColumnMetaData.Rep.DOUBLE;
                case Types.DATE:
                    return ColumnMetaData.Rep.JAVA_SQL_DATE;
                case Types.TIME:
                    return ColumnMetaData.Rep.JAVA_SQL_TIME;
                case Types.TIMESTAMP:
                    return ColumnMetaData.Rep.JAVA_SQL_TIMESTAMP;
                case Types.BINARY:
                case Types.VARBINARY:
                case Types.LONGVARBINARY:
                    return ColumnMetaData.Rep.BYTE_STRING;
                case Types.NUMERIC:
                case Types.DECIMAL:
                    return ColumnMetaData.Rep.NUMBER;
                default:
                    return ColumnMetaData.Rep.STRING;
            }
        }
    }
}
