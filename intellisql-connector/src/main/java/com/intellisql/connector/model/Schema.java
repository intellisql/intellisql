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

package com.intellisql.connector.model;

import java.util.ArrayList;
import java.util.List;

import com.intellisql.connector.enums.SchemaType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

/** Represents a database schema containing tables and views. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schema {

    private String name;

    private String catalog;

    private SchemaType type;

    private String dataSourceName;

    @Singular
    private List<Table> tables = new ArrayList<>();

    /**
     * Adds a table to this schema.
     *
     * @param table the table to add
     */
    public void addTable(final Table table) {
        if (tables == null) {
            tables = new ArrayList<>();
        }
        tables.add(table);
    }

    /**
     * Gets a table by name from this schema.
     *
     * @param tableName the name of the table to find
     * @return the table, or null if not found
     */
    public Table getTable(final String tableName) {
        if (tables == null) {
            return null;
        }
        return tables.stream()
                .filter(t -> t.getName().equalsIgnoreCase(tableName))
                .findFirst()
                .orElse(null);
    }
}
