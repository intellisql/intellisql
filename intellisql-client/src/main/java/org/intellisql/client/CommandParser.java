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

package org.intellisql.client;

import org.intellisql.client.command.Command;
import org.intellisql.client.command.QueryCommand;
import org.intellisql.client.command.ScriptCommand;
import org.intellisql.client.command.TranslateCommand;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** Parses user input into executable commands. */
@Slf4j
@Getter
public class CommandParser {

    private static final String META_PREFIX = "\\";

    /**
     * Parses a line of input into a Command object.
     *
     * @param input the user input
     * @param client the IntelliSqlClient instance
     * @return the parsed Command, or null if input should be accumulated
     */
    public Command parse(final String input, final IntelliSqlClient client) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.startsWith(META_PREFIX)) {
            return parseMetaCommand(trimmed, client);
        }
        return new QueryCommand(client, trimmed);
    }

    /**
     * Parses a meta command (starts with backslash).
     *
     * @param input the user input
     * @param client the IntelliSqlClient instance
     * @return the parsed Command, or null if unknown command
     */
    private Command parseMetaCommand(final String input, final IntelliSqlClient client) {
        String command = input.substring(1).toLowerCase();
        if (command.startsWith("t ") || command.startsWith("translate ")) {
            String sql = input.substring(input.indexOf(' ') + 1);
            return new TranslateCommand(client, sql, null, null);
        }
        if (command.startsWith("s ") || command.startsWith("script ")) {
            String file = input.substring(input.indexOf(' ') + 1);
            return new ScriptCommand(client, file);
        }
        return null;
    }

    /**
     * Checks if the input line indicates a complete statement.
     *
     * @param line the input line
     * @return true if the statement is complete
     */
    public boolean isCompleteStatement(final String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        String trimmed = line.trim();
        if (trimmed.startsWith(META_PREFIX)) {
            return true;
        }
        return trimmed.endsWith(";");
    }

    /**
     * Splits a script into individual statements.
     *
     * @param script the SQL script
     * @return array of individual statements
     */
    public String[] splitScript(final String script) {
        if (script == null || script.trim().isEmpty()) {
            return new String[0];
        }
        return script.split(";(?=(?:[^']*'[^']*')*[^']*$)");
    }
}
