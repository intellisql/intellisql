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

package com.intellisql.connector.postgresql;

import com.intellisql.common.metadata.enums.DataType;
import com.intellisql.connector.jdbc.AbstractJdbcQueryExecutor;

/**
 * PostgreSQL implementation of QueryExecutor. Executes SQL queries against PostgreSQL databases.
 */
public class PostgreSQLQueryExecutor extends AbstractJdbcQueryExecutor {

    @Override
    protected String getDatabaseName() {
        return "PostgreSQL";
    }

    @Override
    protected DataType mapDataType(final String nativeType, final int sqlType) {
        if (nativeType == null) {
            return mapSqlTypeToDataType(sqlType);
        }
        final String upperType = nativeType.toUpperCase();
        if (upperType.startsWith("_")) {
            return DataType.ARRAY;
        }
        switch (upperType) {
            case "INT4":
            case "INTEGER":
            case "SMALLINT":
            case "INT2":
                return DataType.INTEGER;
            case "INT8":
            case "BIGINT":
                return DataType.LONG;
            case "FLOAT4":
            case "REAL":
                return DataType.DOUBLE;
            case "FLOAT8":
            case "DOUBLE PRECISION":
            case "NUMERIC":
            case "DECIMAL":
                return DataType.DOUBLE;
            case "BOOL":
            case "BOOLEAN":
                return DataType.BOOLEAN;
            case "DATE":
                return DataType.DATE;
            case "TIMESTAMP":
            case "TIMESTAMPTZ":
            case "TIME":
            case "TIMETZ":
                return DataType.TIMESTAMP;
            case "BYTEA":
                return DataType.BINARY;
            case "JSON":
            case "JSONB":
                return DataType.JSON;
            case "VARCHAR":
            case "CHAR":
            case "TEXT":
            case "BPCHAR":
            case "NAME":
            default:
                return mapSqlTypeToDataType(sqlType);
        }
    }
}
