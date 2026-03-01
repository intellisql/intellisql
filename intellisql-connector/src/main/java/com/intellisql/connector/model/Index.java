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

import com.intellisql.connector.enums.IndexType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents an index on a database table. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Index {

    private String name;

    private String tableName;

    private String schemaName;

    private IndexType type;

    private boolean unique;

    @Builder.Default
    private List<String> columnNames = new ArrayList<>();

    /**
     * Adds a column name to this index.
     *
     * @param columnName the column name to add
     */
    public void addColumnName(final String columnName) {
        if (columnNames == null) {
            columnNames = new ArrayList<>();
        }
        columnNames.add(columnName);
    }
}
