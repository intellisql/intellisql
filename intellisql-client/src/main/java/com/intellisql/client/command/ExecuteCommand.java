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

package com.intellisql.client.command;

import com.intellisql.client.console.ConsoleReader;
import com.intellisql.client.renderer.PagingRenderer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Command to execute SQL statements.
 */
public class ExecuteCommand implements ClientCommand {

    @Override
    public Connection execute(final ConsoleReader console, final Connection connection, final String[] args) {
        if (connection == null) {
            console.getPrinter().println("Error: Not connected. Use \\connect first.");
            return null;
        }
        if (args.length == 0) {
            return connection;
        }
        String sql = String.join(" ", args);
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }
        try (Statement stmt = connection.createStatement()) {
            boolean hasResultSet = stmt.execute(sql);
            if (hasResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    PagingRenderer.render(rs, console.getPrinter());
                }
            } else {
                int updateCount = stmt.getUpdateCount();
                console.getPrinter().println("Update count: " + updateCount);
            }
        } catch (final SQLException ex) {
            console.getPrinter().println("Error executing SQL: " + ex.getMessage());
        }
        return connection;
    }

    @Override
    public String getName() {
        return "execute";
    }

    @Override
    public String getDescription() {
        return "Execute a SQL statement";
    }
}
