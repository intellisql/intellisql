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

package org.intellisql.client.command;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.intellisql.client.ClientException;
import org.intellisql.client.IntelliSqlClient;
import org.intellisql.client.ResultFormatter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Command to execute a SQL query and display results. */
@Slf4j
@Getter
@RequiredArgsConstructor
public class QueryCommand implements Command {

    private final IntelliSqlClient client;

    private final String sql;

    @Override
    public void execute() throws ClientException {
        log.debug("Executing query: {}", sql);
        Connection connection = client.getConnection();
        if (connection == null) {
            throw new ClientException("Not connected to server.");
        }
        try {
            if (connection.isClosed()) {
                throw new ClientException("Connection is closed.");
            }
        } catch (final SQLException ex) {
            throw new ClientException("Failed to check connection status.", ex);
        }
        long startTime = System.currentTimeMillis();
        try (Statement stmt = connection.createStatement()) {
            boolean hasResultSet = stmt.execute(sql);
            long elapsed = System.currentTimeMillis() - startTime;
            if (hasResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultFormatter formatter = client.getResultFormatter();
                    String result = formatter.formatTable(rs);
                    System.out.print(result);
                }
            } else {
                int updateCount = stmt.getUpdateCount();
                ResultFormatter formatter = client.getResultFormatter();
                System.out.print(formatter.formatUpdateCount(updateCount));
            }
            System.out.println("Time: " + elapsed + " ms");
        } catch (final SQLException ex) {
            throw new ClientException("Query execution failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getDescription() {
        return "Execute SQL query: " + (sql.length() > 50 ? sql.substring(0, 50) + "..." : sql);
    }
}
