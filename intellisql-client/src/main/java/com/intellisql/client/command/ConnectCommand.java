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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Command to connect to a database.
 */
public class ConnectCommand implements ClientCommand {

    @Override
    public Connection execute(final ConsoleReader console, final Connection connection, final String[] args) {
        if (args.length < 1) {
            console.getPrinter().println("Usage: \\connect <url> [<user>] [<password>]");
            return connection;
        }
        String url = args[0];
        final String user = args.length > 1 ? args[1] : null;
        final String password = args.length > 2 ? args[2] : null;
        if (connection != null) {
            try {
                connection.close();
            } catch (final SQLException ex) {
                // Ignore close errors
            }
        }
        console.getPrinter().println("Connecting to " + url + "...");
        Properties props = new Properties();
        if (user != null) {
            props.setProperty("user", user);
        }
        if (password != null) {
            props.setProperty("password", password);
        }
        try {
            Connection newConn = DriverManager.getConnection(url, props);
            console.getPrinter().println("Connected successfully.");
            return newConn;
        } catch (final SQLException ex) {
            console.getPrinter().println("Connection failed: " + ex.getMessage());
            return null;
        }
    }

    @Override
    public String getName() {
        return "\\connect";
    }

    @Override
    public String getDescription() {
        return "Connect to a database (e.g., \\connect jdbc:intellisql://localhost:8765 user password)";
    }
}
