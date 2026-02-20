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

package com.intellisql.executor;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a single row of data in a result set. Provides type-safe accessors for column values.
 */
@Getter
@RequiredArgsConstructor
public class Row {

    /** The column values in this row. */
    private final Object[] values;

    /**
     * Gets the number of columns in this row.
     *
     * @return the column count
     */
    public int getColumnCount() {
        return values != null ? values.length : 0;
    }

    /**
     * Gets a column value by index as Object.
     *
     * @param index zero-based column index
     * @return the column value
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Object getObject(final int index) {
        checkIndex(index);
        return values[index];
    }

    /**
     * Gets a column value by index as String.
     *
     * @param index zero-based column index
     * @return the column value as String, or null if the value is null
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public String getString(final int index) {
        Object value = getObject(index);
        return value != null ? value.toString() : null;
    }

    /**
     * Gets a column value by index as Long.
     *
     * @param index zero-based column index
     * @return the column value as Long, or null if the value is null
     * @throws IndexOutOfBoundsException if index is out of range
     * @throws ClassCastException if the value cannot be converted to Long
     */
    public Long getLong(final int index) {
        Object value = getObject(index);
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        throw new ClassCastException("Cannot convert " + value.getClass() + " to Long");
    }

    /**
     * Gets a column value by index as Integer.
     *
     * @param index zero-based column index
     * @return the column value as Integer, or null if the value is null
     * @throws IndexOutOfBoundsException if index is out of range
     * @throws ClassCastException if the value cannot be converted to Integer
     */
    public Integer getInteger(final int index) {
        Object value = getObject(index);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        throw new ClassCastException("Cannot convert " + value.getClass() + " to Integer");
    }

    /**
     * Gets a column value by index as Double.
     *
     * @param index zero-based column index
     * @return the column value as Double, or null if the value is null
     * @throws IndexOutOfBoundsException if index is out of range
     * @throws ClassCastException if the value cannot be converted to Double
     */
    public Double getDouble(final int index) {
        Object value = getObject(index);
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        throw new ClassCastException("Cannot convert " + value.getClass() + " to Double");
    }

    /**
     * Gets a column value by index as BigDecimal.
     *
     * @param index zero-based column index
     * @return the column value as BigDecimal, or null if the value is null
     * @throws IndexOutOfBoundsException if index is out of range
     * @throws ClassCastException if the value cannot be converted to BigDecimal
     */
    public BigDecimal getBigDecimal(final int index) {
        Object value = getObject(index);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            return new BigDecimal((String) value);
        }
        throw new ClassCastException("Cannot convert " + value.getClass() + " to BigDecimal");
    }

    /**
     * Gets a column value by index as Boolean.
     *
     * @param index zero-based column index
     * @return the column value as Boolean, or null if the value is null
     * @throws IndexOutOfBoundsException if index is out of range
     * @throws ClassCastException if the value cannot be converted to Boolean
     */
    public Boolean getBoolean(final int index) {
        Object value = getObject(index);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        throw new ClassCastException("Cannot convert " + value.getClass() + " to Boolean");
    }

    /**
     * Gets a column value by index as Date.
     *
     * @param index zero-based column index
     * @return the column value as Date, or null if the value is null
     * @throws IndexOutOfBoundsException if index is out of range
     * @throws ClassCastException if the value cannot be converted to Date
     */
    public Date getDate(final int index) {
        Object value = getObject(index);
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Timestamp) {
            return new Date(((Timestamp) value).getTime());
        }
        if (value instanceof java.util.Date) {
            return new Date(((java.util.Date) value).getTime());
        }
        if (value instanceof Long) {
            return new Date((Long) value);
        }
        throw new ClassCastException("Cannot convert " + value.getClass() + " to Date");
    }

    /**
     * Gets a column value by index as Timestamp.
     *
     * @param index zero-based column index
     * @return the column value as Timestamp, or null if the value is null
     * @throws IndexOutOfBoundsException if index is out of range
     * @throws ClassCastException if the value cannot be converted to Timestamp
     */
    public Timestamp getTimestamp(final int index) {
        Object value = getObject(index);
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        if (value instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) value).getTime());
        }
        if (value instanceof Long) {
            return new Timestamp((Long) value);
        }
        throw new ClassCastException("Cannot convert " + value.getClass() + " to Timestamp");
    }

    /**
     * Gets a column value by index as byte array.
     *
     * @param index zero-based column index
     * @return the column value as byte[], or null if the value is null
     * @throws IndexOutOfBoundsException if index is out of range
     * @throws ClassCastException if the value cannot be converted to byte[]
     */
    public byte[] getBytes(final int index) {
        Object value = getObject(index);
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        throw new ClassCastException("Cannot convert " + value.getClass() + " to byte[]");
    }

    /**
     * Checks if the column value at the given index is null.
     *
     * @param index zero-based column index
     * @return true if the value is null, false otherwise
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public boolean isNull(final int index) {
        checkIndex(index);
        return values[index] == null;
    }

    /**
     * Validates that the index is within bounds.
     *
     * @param index the index to check
     * @throws IndexOutOfBoundsException if index is out of range
     */
    private void checkIndex(final int index) {
        if (values == null || index < 0 || index >= values.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + getColumnCount());
        }
    }

    /**
     * Creates a new Row with the given values.
     *
     * @param values the column values
     * @return a new Row instance
     */
    public static Row of(final Object... values) {
        return new Row(values);
    }
}
