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

package com.intellisql.client.command;

import com.intellisql.client.console.ConsoleReader;

import java.sql.Connection;

/**
 * Command to display help information.
 */
public class HelpCommand implements ClientCommand {

    @Override
    public Connection execute(final ConsoleReader console, final Connection connection, final String[] args) {
        console.getPrinter().println("Available commands:");
        console.getPrinter().println("  \\connect <url> [user] [pass]  Connect to database");
        console.getPrinter().println("  \\translate [options] <sql>    Translate SQL between dialects");
        console.getPrinter().println("  \\quit                         Exit isql");
        console.getPrinter().println("  \\help                         Show this help");
        return connection;
    }

    @Override
    public String getName() {
        return "\\help";
    }

    @Override
    public String getDescription() {
        return "Show help";
    }
}
