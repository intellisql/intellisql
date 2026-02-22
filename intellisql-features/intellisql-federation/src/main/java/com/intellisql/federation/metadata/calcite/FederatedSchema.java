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

package com.intellisql.federation.metadata.calcite;

import lombok.Getter;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

import java.util.HashMap;
import java.util.Map;

/**
 * Federated schema implementation for Calcite.
 * Extends AbstractSchema to provide table lookup.
 * Reference: ShardingSphere SQLFederationSchema.
 */
@Getter
public final class FederatedSchema extends AbstractSchema {

    private final String name;

    private final Map<String, Table> tableMap;

    /**
     * Creates a new FederatedSchema.
     *
     * @param name the schema name
     */
    public FederatedSchema(final String name) {
        this.name = name;
        this.tableMap = new HashMap<>();
    }

    /**
     * Adds a table to this schema.
     *
     * @param tableName the table name
     * @param table the table
     */
    public void addTable(final String tableName, final Table table) {
        tableMap.put(tableName, table);
    }

    @Override
    protected Map<String, Table> getTableMap() {
        return tableMap;
    }
}
