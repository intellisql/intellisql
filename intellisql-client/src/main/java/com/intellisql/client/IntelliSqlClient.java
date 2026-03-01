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

package com.intellisql.client;

import com.intellisql.client.command.ClientCommand;
import com.intellisql.client.command.ConnectCommand;
import com.intellisql.client.command.ExecuteCommand;
import com.intellisql.client.command.HelpCommand;
import com.intellisql.client.command.TranslateCommand;
import com.intellisql.client.console.CompleterFactory;
import com.intellisql.client.console.ConsoleReader;
import com.intellisql.client.console.MetaDataLoader;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * IntelliSQL Command Line Interface.
 */
@Command(name = "isql", mixinStandardHelpOptions = true, version = "isql 1.0.0",
        description = "IntelliSQL Command Line Interface")
public class IntelliSqlClient implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..1", description = "JDBC URL to connect to")
    private String url;

    @Parameters(index = "1", arity = "0..1", description = "Username")
    private String user;

    @Parameters(index = "2", arity = "0..1", description = "Password")
    private String password;

    @Option(names = {"-e", "--execute"}, description = "Execute command and quit")
    private String command;

    @Option(names = {"-f", "--file"}, description = "Execute commands from file")
    private String file;

    private final Map<String, ClientCommand> commands = new HashMap<>();

    private Connection connection;

    /**
     * Creates a new IntelliSqlClient instance.
     */
    public IntelliSqlClient() {
        registerCommand(new ConnectCommand());
        registerCommand(new ExecuteCommand());
        registerCommand(new TranslateCommand());
        registerCommand(new HelpCommand());
    }

    /**
     * Registers a command.
     *
     * @param cmd the command to register
     */
    private void registerCommand(final ClientCommand cmd) {
        commands.put(cmd.getName(), cmd);
    }

    @Override
    public Integer call() throws Exception {
        // Auto-connect if URL is provided
        if (url != null && !url.isEmpty()) {
            try {
                Properties props = new Properties();
                if (user != null && !user.isEmpty()) {
                    props.setProperty("user", user);
                }
                if (password != null && !password.isEmpty()) {
                    props.setProperty("password", password);
                }
                connection = DriverManager.getConnection(url, props);
            } catch (final SQLException ex) {
                System.err.println("Connection failed: " + ex.getMessage());
                System.err.println("Starting in disconnected mode.");
            }
        }
        if (command != null) {
            System.out.println("Executing command: " + command);
            return 0;
        }
        if (file != null) {
            System.out.println("Executing file: " + file);
            return 0;
        }
        System.out.println("Welcome to IntelliSQL CLI (isql)");
        System.out.println("Type \\help for help, \\quit to exit.");
        MetaDataLoader metaDataLoader = new MetaDataLoader();
        if (connection != null) {
            metaDataLoader.load(connection);
        }
        Completer completer = CompleterFactory.create(metaDataLoader);
        try (ConsoleReader console = new ConsoleReader(completer)) {
            runInteractiveLoop(console, metaDataLoader);
        } finally {
            closeConnection();
        }
        return 0;
    }

    /**
     * Runs the interactive command loop.
     *
     * @param console         the console reader
     * @param metaDataLoader  the metadata loader
     */
    private void runInteractiveLoop(final ConsoleReader console, final MetaDataLoader metaDataLoader) {
        while (true) {
            try {
                String line = console.readLine("isql> ");
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (isQuitCommand(line)) {
                    break;
                }
                if (line.startsWith("\\")) {
                    handleSlashCommand(console, line, metaDataLoader);
                } else {
                    handleSqlCommand(console, line, metaDataLoader);
                }
            } catch (final UserInterruptException ex) {
                // Ignore user interrupt
            } catch (final EndOfFileException ex) {
                // End of input, exit loop
                break;
            }
        }
    }

    /**
     * Checks if the line is a quit command.
     *
     * @param line the input line
     * @return true if it's a quit command
     */
    private boolean isQuitCommand(final String line) {
        return "\\q".equalsIgnoreCase(line)
                || "\\quit".equalsIgnoreCase(line)
                || "exit".equalsIgnoreCase(line);
    }

    /**
     * Handles a slash command.
     *
     * @param console         the console reader
     * @param line            the input line
     * @param metaDataLoader  the metadata loader
     */
    private void handleSlashCommand(final ConsoleReader console, final String line,
                                    final MetaDataLoader metaDataLoader) {
        String[] parts = line.split("\\s+");
        String cmdName = parts[0];
        ClientCommand cmd = commands.get(cmdName);
        if (cmd != null) {
            String[] cmdArgs = new String[parts.length - 1];
            System.arraycopy(parts, 1, cmdArgs, 0, parts.length - 1);
            Connection newConn = executeCommand(cmd, console, cmdArgs);
            updateConnection(newConn, metaDataLoader);
        } else {
            console.getPrinter().println("Unknown command: " + cmdName);
        }
    }

    /**
     * Handles an SQL command.
     *
     * @param console         the console reader
     * @param line            the input line
     * @param metaDataLoader  the metadata loader
     */
    private void handleSqlCommand(final ConsoleReader console, final String line,
                                  final MetaDataLoader metaDataLoader) {
        ClientCommand execCmd = commands.get("execute");
        if (execCmd != null) {
            Connection newConn = executeCommand(execCmd, console, line.split("\\s+"));
            updateConnection(newConn, metaDataLoader);
        }
    }

    /**
     * Executes a command and returns the resulting connection.
     *
     * @param cmd     the command to execute
     * @param console the console reader
     * @param args    the command arguments
     * @return the resulting connection or null
     */
    private Connection executeCommand(final ClientCommand cmd, final ConsoleReader console,
                                      final String[] args) {
        return cmd.execute(console, connection, args);
    }

    /**
     * Updates the connection and reloads metadata if changed.
     *
     * @param newConn         the new connection
     * @param metaDataLoader  the metadata loader
     */
    private void updateConnection(final Connection newConn, final MetaDataLoader metaDataLoader) {
        if (newConn != null && newConn != connection) {
            connection = newConn;
            metaDataLoader.clear();
            metaDataLoader.load(connection);
        }
    }

    /**
     * Closes the current connection if open.
     */
    private void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (final SQLException ex) {
                System.err.println("Error closing connection: " + ex.getMessage());
            }
        }
    }

    /**
     * Main entry point for the IntelliSQL CLI application.
     * This method initializes the command line interface and processes user input.
     *
     * @param args command line arguments for configuring the CLI connection and execution
     */
    public static void main(final String[] args) {
        int exitCode = new CommandLine(new IntelliSqlClient()).execute(args);
        System.exit(exitCode);
    }
}
