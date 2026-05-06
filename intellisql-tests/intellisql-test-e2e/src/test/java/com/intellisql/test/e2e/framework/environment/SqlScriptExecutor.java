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

package com.intellisql.test.e2e.framework.environment;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.intellisql.test.e2e.framework.io.ResourceReader;

/** Executes SQL scripts for JDBC E2E fixtures. */
public final class SqlScriptExecutor {

    private final ResourceReader resourceReader = new ResourceReader();

    /**
     * Executes a SQL script resource.
     *
     * @param connection the JDBC connection
     * @param resourcePath the classpath resource path
     */
    public void execute(final Connection connection, final String resourcePath) {
        String script = resourceReader.read(resourcePath);
        String[] statements = script.split(";");
        for (String each : statements) {
            executeStatement(connection, each);
        }
    }

    private void executeStatement(final Connection connection, final String sql) {
        String statementSql = sql.trim();
        if (statementSql.isEmpty()) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(statementSql);
        } catch (final SQLException ex) {
            throw new IllegalStateException("Failed to execute SQL: " + statementSql, ex);
        }
    }
}
