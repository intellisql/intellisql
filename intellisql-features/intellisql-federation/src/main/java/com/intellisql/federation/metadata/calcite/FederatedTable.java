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
import lombok.RequiredArgsConstructor;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import com.intellisql.optimizer.metadata.DataSourceAware;

import java.util.List;

/**
 * Federated table implementation for Calcite.
 * Provides row type information for query validation and data source association.
 * Implements {@link DataSourceAware} to expose data source information to the optimizer.
 * Reference: ShardingSphere SQLFederationTable.
 */
@RequiredArgsConstructor
@Getter
public final class FederatedTable extends AbstractTable implements DataSourceAware {

    private final String tableName;

    private final String dataSourceId;

    private final List<String> columnNames;

    private final List<SqlTypeName> columnTypes;

    private RelDataType rowType;

    @Override
    public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
        if (rowType != null) {
            return rowType;
        }
        final RelDataTypeFactory.Builder builder = typeFactory.builder();
        if (columnNames != null && columnTypes != null) {
            for (int i = 0; i < columnNames.size(); i++) {
                final SqlTypeName typeName = i < columnTypes.size() ? columnTypes.get(i) : SqlTypeName.VARCHAR;
                builder.add(columnNames.get(i), typeFactory.createSqlType(typeName));
            }
        }
        rowType = builder.build();
        return rowType;
    }

    @Override
    public String toString() {
        return "FederatedTable{" + tableName + ", columns=" + columnNames + "}";
    }
}
