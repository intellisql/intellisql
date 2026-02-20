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

package com.intellisql.parser.dialect;

import org.apache.calcite.sql.dialect.AnsiSqlDialect;
import org.apache.calcite.sql.dialect.HiveSqlDialect;
import org.apache.calcite.sql.dialect.MssqlSqlDialect;
import org.apache.calcite.sql.dialect.MysqlSqlDialect;
import org.apache.calcite.sql.dialect.OracleSqlDialect;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;

/** Enumeration of supported SQL dialect types. */
public enum SqlDialect {

    MYSQL(MysqlSqlDialect.DEFAULT),
    POSTGRESQL(PostgresqlSqlDialect.DEFAULT),
    ORACLE(OracleSqlDialect.DEFAULT),
    SQLSERVER(MssqlSqlDialect.DEFAULT),
    HIVE(HiveSqlDialect.DEFAULT),
    STANDARD(AnsiSqlDialect.DEFAULT);

    private final org.apache.calcite.sql.SqlDialect calciteDialect;

    SqlDialect(final org.apache.calcite.sql.SqlDialect calciteDialect) {
        this.calciteDialect = calciteDialect;
    }

    /**
     * Converts to Calcite dialect.
     *
     * @return Calcite dialect
     */
    public org.apache.calcite.sql.SqlDialect toCalciteDialect() {
        return calciteDialect;
    }
}
