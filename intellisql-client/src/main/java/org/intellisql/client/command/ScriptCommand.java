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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.intellisql.client.ClientException;
import org.intellisql.client.CommandParser;
import org.intellisql.client.IntelliSqlClient;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Command to execute a SQL script file. */
@Slf4j
@Getter
@RequiredArgsConstructor
public class ScriptCommand implements Command {

    private final IntelliSqlClient client;

    private final String scriptPath;

    @Override
    public void execute() throws ClientException {
        log.debug("Executing script: {}", scriptPath);
        if (scriptPath == null || scriptPath.isEmpty()) {
            throw new ClientException("No script file specified.");
        }
        String script = readScript();
        if (script == null || script.trim().isEmpty()) {
            throw new ClientException("Script is empty or could not be read: " + scriptPath);
        }
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
        CommandParser parser = new CommandParser();
        String[] statements = parser.splitScript(script);
        int successCount = 0;
        int errorCount = 0;
        long startTime = System.currentTimeMillis();
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(trimmed);
                successCount++;
                System.out.print(".");
            } catch (final SQLException ex) {
                errorCount++;
                log.error("Statement failed: {}", trimmed, ex);
                System.err.println(
                        "\nError at statement " + successCount + errorCount + ": " + ex.getMessage());
            }
        }
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println();
        System.out.println(
                "Script completed: " + successCount + " succeeded, " + errorCount + " failed");
        System.out.println("Time: " + elapsed + " ms");
    }

    /**
     * Reads the script file content.
     *
     * @return the script content, or null if reading failed
     */
    private String readScript() {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(scriptPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (final IOException ex) {
            log.error("Failed to read script file: {}", scriptPath, ex);
            return null;
        }
        return sb.toString();
    }

    @Override
    public String getDescription() {
        return "Execute script: " + scriptPath;
    }
}
