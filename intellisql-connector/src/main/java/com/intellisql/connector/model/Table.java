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

package com.intellisql.connector.model;

import java.util.ArrayList;
import java.util.List;

import com.intellisql.connector.enums.TableType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents a database table or view. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Table {

    private String name;

    private String schema;

    private String catalog;

    private TableType type;

    private String remarks;

    private List<Column> columns;

    private List<Index> indexes;

    /**
     * Gets the columns list, initializing if null.
     *
     * @return the columns list
     */
    public List<Column> getColumns() {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        return columns;
    }

    /**
     * Gets the indexes list, initializing if null.
     *
     * @return the indexes list
     */
    public List<Index> getIndexes() {
        if (indexes == null) {
            indexes = new ArrayList<>();
        }
        return indexes;
    }

    /**
     * Adds a column to this table.
     *
     * @param column the column to add
     */
    public void addColumn(final Column column) {
        getColumns().add(column);
    }

    /**
     * Gets a column by name from this table.
     *
     * @param columnName the name of the column to find
     * @return the column, or null if not found
     */
    public Column getColumn(final String columnName) {
        return getColumns().stream()
                .filter(c -> c.getName().equalsIgnoreCase(columnName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds an index to this table.
     *
     * @param index the index to add
     */
    public void addIndex(final Index index) {
        getIndexes().add(index);
    }
}
