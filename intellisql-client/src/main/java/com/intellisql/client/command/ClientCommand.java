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

import java.sql.Connection;

/**
 * Interface for isql commands.
 */
public interface ClientCommand {

    /**
     * Executes the command.
     *
     * @param console    The console reader for input/output
     * @param connection Current active connection (can be null)
     * @param args       Command arguments
     * @return New or existing connection after command execution
     */
    Connection execute(ConsoleReader console, Connection connection, String[] args);

    /**
     * Returns the command name (e.g., "\\connect").
     *
     * @return the command name
     */
    String getName();

    /**
     * Returns a brief description of the command.
     *
     * @return the command description
     */
    String getDescription();
}
