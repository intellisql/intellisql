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

package com.intellisql.client;

import com.intellisql.client.command.Command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Handles the Read-Eval-Print Loop (REPL) for interactive mode. */
@Slf4j
@Getter
@RequiredArgsConstructor
public class ReplHandler {

    private final IntelliSqlClient client;

    private final StringBuilder buffer = new StringBuilder();

    /**
     * Handles a single line of input in the REPL.
     *
     * @param line the input line
     */
    public void handleLine(final String line) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }
        if (isMetaCommand(line)) {
            handleMetaCommand(line);
            return;
        }
        buffer.append(line).append("\n");
        if (line.trim().endsWith(";")) {
            flushBuffer();
        }
    }

    /**
     * Checks if the line is a meta command.
     *
     * @param line the input line
     * @return true if the line is a meta command
     */
    private boolean isMetaCommand(final String line) {
        return line.trim().startsWith("\\");
    }

    /**
     * Handles a meta command.
     *
     * @param line the input line
     */
    private void handleMetaCommand(final String line) {
        String cmd = line.trim().substring(1).toLowerCase();
        switch (cmd) {
            case "q":
            case "quit":
                client.stop();
                break;
            case "h":
            case "help":
                printHelp();
                break;
            case "c":
            case "clear":
                clearBuffer();
                break;
            default:
                log.warn("Unknown meta command: {}", cmd);
                System.err.println("Unknown command. Type \\h for help.");
        }
    }

    /** Flushes the buffer and executes the accumulated query. */
    private void flushBuffer() {
        String sql = buffer.toString().trim();
        buffer.setLength(0);
        if (!sql.isEmpty()) {
            executeSql(sql);
        }
    }

    /** Clears the input buffer. */
    private void clearBuffer() {
        buffer.setLength(0);
        System.out.println("Buffer cleared.");
    }

    /**
     * Executes an SQL statement.
     *
     * @param sql the SQL statement to execute
     */
    private void executeSql(final String sql) {
        log.debug("Executing SQL: {}", sql);
        try {
            CommandParser parser = client.getCommandParser();
            Command command = parser.parse(sql, client);
            if (command != null) {
                command.execute();
            }
        } catch (final ClientException ex) {
            log.error("Execution failed", ex);
            System.err.println("Error: " + ex.getMessage());
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            log.error("Unexpected error during execution", ex);
            System.err.println("Error: " + ex.getMessage());
        }
    }

    /** Prints help text. */
    private void printHelp() {
        System.out.println("IntelliSql Client Commands:");
        System.out.println("  \\h, \\help     Show this help");
        System.out.println("  \\q, \\quit     Exit the client");
        System.out.println("  \\c, \\clear    Clear the input buffer");
        System.out.println("  \\t <sql>      Translate SQL between dialects");
        System.out.println("  \\s <file>     Execute a script file");
        System.out.println("  \\d            List data sources");
        System.out.println();
        System.out.println("Type SQL statements ending with ; to execute.");
    }

    /**
     * Returns the current buffer contents.
     *
     * @return the buffer contents
     */
    public String getBufferContents() {
        return buffer.toString();
    }

    /**
     * Checks if the buffer is empty.
     *
     * @return true if the buffer is empty
     */
    public boolean isBufferEmpty() {
        return buffer.length() == 0;
    }
}
