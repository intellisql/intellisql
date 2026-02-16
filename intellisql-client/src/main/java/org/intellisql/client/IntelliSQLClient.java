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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.intellisql.client.command.QueryCommand;
import org.intellisql.client.command.ScriptCommand;
import org.intellisql.client.command.TranslateCommand;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOError;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Main entry point for the IntelliSql command-line client (isql). Provides interactive REPL and
 * script execution modes.
 */
@Slf4j
@Getter
public class IntelliSqlClient {

    private static final String VERSION = "1.0.0-SNAPSHOT";

    private static final String PROMPT = "isql> ";

    private final String url;

    private final String username;

    private final String password;

    private Connection connection;

    private final CommandParser commandParser;

    private final ResultFormatter resultFormatter;

    private final ReplHandler replHandler;

    private boolean running;

    /**
     * Creates a new IntelliSqlClient with the given connection parameters.
     *
     * @param url the JDBC URL to connect to
     * @param username the database username
     * @param password the database password
     */
    public IntelliSqlClient(final String url, final String username, final String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.commandParser = new CommandParser();
        this.resultFormatter = new ResultFormatter();
        this.replHandler = new ReplHandler(this);
    }

    /**
     * Main entry point for the IntelliSql client.
     *
     * @param args command line arguments
     */
    // allow main method for CLI entry point
    // CHECKSTYLE:OFF:UncommentedMain
    public static void main(final String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        String url = args[0];
        String username = args.length > 1 ? args[1] : "";
        String password = args.length > 2 ? args[2] : "";
        IntelliSqlClient client = new IntelliSqlClient(url, username, password);
        if (args.length > 3 && "-f".equals(args[3])) {
            String scriptFile = args.length > 4 ? args[4] : null;
            client.executeScript(scriptFile);
        } else {
            client.start();
        }
    }
    // CHECKSTYLE:ON:UncommentedMain

    private static void printUsage() {
        System.out.println("IntelliSql Client (isql) v" + VERSION);
        System.out.println();
        System.out.println("Usage: isql <url> [username] [password] [-f script.sql]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  <url>       JDBC URL to connect to IntelliSql Server");
        System.out.println("  <username>  Database username (optional)");
        System.out.println("  <password>  Database password (optional)");
        System.out.println("  -f <file>   Execute SQL script and exit");
        System.out.println();
        System.out.println("Interactive Commands:");
        System.out.println("  \\q          Quit the client");
        System.out.println("  \\h          Show help");
        System.out.println("  \\d          Show data sources");
        System.out.println("  \\t <sql>    Translate SQL to target dialect");
        System.out.println("  \\s <file>   Execute script file");
    }

    /**
     * Starts the interactive REPL session.
     */
    public void start() {
        System.out.println("IntelliSql Client v" + VERSION);
        System.out.println("Connecting to " + url + "...");
        try {
            connect();
            System.out.println("Connected successfully.");
            System.out.println("Type \\h for help, \\q to quit.");
            running = true;
            runRepl();
        } catch (final SQLException ex) {
            System.err.println("Failed to connect: " + ex.getMessage());
            log.error("Connection failed", ex);
            System.exit(1);
        }
    }

    /**
     * Executes a SQL script file and exits.
     *
     * @param scriptFile the path to the script file
     */
    public void executeScript(final String scriptFile) {
        try {
            connect();
            ScriptCommand command = new ScriptCommand(this, scriptFile);
            command.execute();
        } catch (final SQLException ex) {
            System.err.println("Failed to connect: " + ex.getMessage());
            log.error("Connection failed", ex);
            System.exit(1);
        } catch (final ClientException ex) {
            System.err.println("Script execution failed: " + ex.getMessage());
            log.error("Script execution failed", ex);
            System.exit(1);
        } finally {
            disconnect();
        }
    }

    private void connect() throws SQLException {
        log.debug("Connecting to {}", url);
        try {
            Class.forName("org.intellisql.jdbc.IntelliSqlDriver");
            // CHECKSTYLE:OFF
        } catch (final ClassNotFoundException ex) {
            // CHECKSTYLE:ON
            throw new SQLException("IntelliSql JDBC driver not found", ex);
        }
        connection = DriverManager.getConnection(url, username, password);
        log.info("Connected to {}", url);
    }

    /** Disconnects from the server. */
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                log.info("Disconnected from {}", url);
            } catch (final SQLException ex) {
                log.warn("Error closing connection", ex);
            }
            connection = null;
        }
    }

    private void runRepl() {
        Terminal terminal = null;
        LineReader reader = null;
        try {
            terminal = TerminalBuilder.builder().system(true).build();
            reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .history(new DefaultHistory())
                    .build();
            reader.setVariable(LineReader.HISTORY_FILE, System.getProperty("user.home") + "/.isql_history");
        } catch (final IOException ex) {
            log.error("Failed to initialize terminal, falling back to simple input", ex);
            runSimpleRepl();
            return;
        }

        StringBuilder queryBuilder = new StringBuilder();
        while (running) {
            try {
                String prompt = queryBuilder.length() == 0 ? PROMPT : "    > ";
                String line = reader.readLine(prompt).trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("\\")) {
                    processMetaCommand(line);
                    continue;
                }
                queryBuilder.append(line).append(" ");
                if (line.endsWith(";")) {
                    String query = queryBuilder.toString().trim();
                    queryBuilder.setLength(0);
                    executeQuery(query);
                }
            } catch (final UserInterruptException ex) {
                // Ctrl+C - clear current input or exit
                if (queryBuilder.length() == 0) {
                    System.out.println("\nGoodbye!");
                    running = false;
                } else {
                    queryBuilder.setLength(0);
                    System.out.println("^C");
                }
            } catch (final EndOfFileException ex) {
                // Ctrl+D - exit
                System.out.println("\nGoodbye!");
                running = false;
            } catch (final IOError ex) {
                log.error("IO error", ex);
                running = false;
            }
        }
        try {
            reader.getHistory().save();
            terminal.close();
        } catch (final IOException ex) {
            log.warn("Error closing terminal", ex);
        }
        disconnect();
    }

    /** Fallback simple REPL without history support. */
    private void runSimpleRepl() {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        StringBuilder queryBuilder = new StringBuilder();
        while (running) {
            System.out.print(queryBuilder.length() == 0 ? PROMPT : "    > ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("\\")) {
                processMetaCommand(line);
                continue;
            }
            queryBuilder.append(line).append(" ");
            if (line.endsWith(";")) {
                String query = queryBuilder.toString().trim();
                queryBuilder.setLength(0);
                executeQuery(query);
            }
        }
        scanner.close();
        disconnect();
    }

    private void processMetaCommand(final String command) {
        String cmd = command.substring(1).toLowerCase();
        switch (cmd) {
            case "q":
            case "quit":
                System.out.println("Goodbye!");
                running = false;
                break;
            case "h":
            case "help":
                printUsage();
                break;
            case "d":
            case "datasources":
                showDataSources();
                break;
            default:
                if (cmd.startsWith("t ") || cmd.startsWith("translate ")) {
                    String sql = command.substring(command.indexOf(' ') + 1);
                    translateSql(sql);
                } else if (cmd.startsWith("s ") || cmd.startsWith("script ")) {
                    String file = command.substring(command.indexOf(' ') + 1);
                    executeScriptFile(file);
                } else {
                    System.err.println("Unknown command: " + command);
                    System.err.println("Type \\h for help.");
                }
        }
    }

    private void executeQuery(final String sql) {
        try {
            QueryCommand command = new QueryCommand(this, sql);
            command.execute();
        } catch (final ClientException ex) {
            System.err.println("Error: " + ex.getMessage());
            log.error("Query execution failed", ex);
        }
    }

    private void translateSql(final String sql) {
        try {
            TranslateCommand command = new TranslateCommand(this, sql, null, null);
            command.execute();
        } catch (final ClientException ex) {
            System.err.println("Error: " + ex.getMessage());
            log.error("Translation failed", ex);
        }
    }

    private void executeScriptFile(final String file) {
        try {
            ScriptCommand command = new ScriptCommand(this, file);
            command.execute();
        } catch (final ClientException ex) {
            System.err.println("Error: " + ex.getMessage());
            log.error("Script execution failed", ex);
        }
    }

    private void showDataSources() {
        System.out.println("Data sources: (not implemented)");
    }

    /** Stops the client. */
    public void stop() {
        running = false;
    }
}
