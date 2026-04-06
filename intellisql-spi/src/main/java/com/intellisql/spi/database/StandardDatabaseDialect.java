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

package com.intellisql.spi.database;

import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.AnsiSqlDialect;
import org.apache.calcite.sql.validate.SqlConformance;
import org.apache.calcite.sql.validate.SqlConformanceEnum;

import java.util.Arrays;
import java.util.Collection;

/**
 * Built-in ANSI SQL dialect.
 */
public final class StandardDatabaseDialect implements DatabaseDialect {

    public static final String TYPE = "STANDARD";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Collection<String> getAliases() {
        return Arrays.asList("ANSI");
    }

    @Override
    public SqlDialect getCalciteDialect() {
        return AnsiSqlDialect.DEFAULT;
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
