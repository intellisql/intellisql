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

package com.intellisql.plugin.hive;

import com.intellisql.spi.database.DatabaseDialect;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.HiveSqlDialect;
import org.apache.calcite.sql.validate.SqlConformance;
import org.apache.calcite.sql.validate.SqlConformanceEnum;

/**
 * Hive dialect plugin.
 */
public final class HiveDatabaseDialect implements DatabaseDialect {

    @Override
    public String getType() {
        return "HIVE";
    }

    @Override
    public SqlDialect getCalciteDialect() {
        return HiveSqlDialect.DEFAULT;
    }

    @Override
    public Lex getLex() {
        return Lex.JAVA;
    }

    @Override
    public SqlConformance getConformance() {
        return SqlConformanceEnum.DEFAULT;
    }
}
