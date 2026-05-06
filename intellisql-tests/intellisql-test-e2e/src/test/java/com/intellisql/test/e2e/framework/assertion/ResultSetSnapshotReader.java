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

package com.intellisql.test.e2e.framework.assertion;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Reads JDBC result sets into immutable snapshots. */
public final class ResultSetSnapshotReader {

    private final ValueNormalizer valueNormalizer;

    /**
     * Creates a result set snapshot reader.
     *
     * @param valueNormalizer the value normalizer
     */
    public ResultSetSnapshotReader(final ValueNormalizer valueNormalizer) {
        this.valueNormalizer = valueNormalizer;
    }

    /**
     * Reads a result set into a snapshot.
     *
     * @param resultSet the JDBC result set
     * @return result set snapshot
     * @throws IllegalStateException if the JDBC result set cannot be read
     */
    public ResultSetSnapshot read(final ResultSet resultSet) {
        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<ColumnSnapshot> columns = readColumns(metaData, columnCount);
            List<RowSnapshot> rows = readRows(resultSet, columnCount);
            return new ResultSetSnapshot(columns, rows);
        } catch (final SQLException ex) {
            throw new IllegalStateException("Failed to read result set", ex);
        }
    }

    private List<ColumnSnapshot> readColumns(final ResultSetMetaData metaData, final int columnCount) throws SQLException {
        List<ColumnSnapshot> result = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            result.add(ColumnSnapshot.builder()
                    .label(metaData.getColumnLabel(i))
                    .jdbcType(metaData.getColumnType(i))
                    .typeName(metaData.getColumnTypeName(i))
                    .build());
        }
        return result;
    }

    private List<RowSnapshot> readRows(final ResultSet resultSet, final int columnCount) throws SQLException {
        List<RowSnapshot> result = new ArrayList<>();
        while (resultSet.next()) {
            List<String> values = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                values.add(valueNormalizer.normalize(resultSet.getObject(i), resultSet.getMetaData().getColumnType(i)));
            }
            result.add(new RowSnapshot(values));
        }
        return result;
    }
}
