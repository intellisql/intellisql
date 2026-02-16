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

package org.intellisql.connector.model;

import java.util.ArrayList;
import java.util.List;

import org.intellisql.connector.enums.DataType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents the result of a query execution. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {

    private List<String> columnNames;

    private List<DataType> columnTypes;

    @Builder.Default
    private List<List<Object>> rows = new ArrayList<>();

    private int rowCount;

    private long executionTimeMs;

    private boolean success;

    private String errorMessage;

    /**
     * Adds a row to the result set.
     *
     * @param row the row data to add
     */
    public void addRow(final List<Object> row) {
        if (rows == null) {
            rows = new ArrayList<>();
        }
        rows.add(row);
        rowCount = rows.size();
    }

    /**
     * Creates a successful query result with the given column names and types.
     *
     * @param columnNames the list of column names
     * @param columnTypes the list of column data types
     * @return a successful query result
     */
    public static QueryResult success(final List<String> columnNames, final List<DataType> columnTypes) {
        return QueryResult.builder()
                .columnNames(columnNames)
                .columnTypes(columnTypes)
                .success(true)
                .build();
    }

    /**
     * Creates a failed query result with the given error message.
     *
     * @param errorMessage the error message describing the failure
     * @return a failed query result
     */
    public static QueryResult failure(final String errorMessage) {
        return QueryResult.builder().success(false).errorMessage(errorMessage).build();
    }
}
