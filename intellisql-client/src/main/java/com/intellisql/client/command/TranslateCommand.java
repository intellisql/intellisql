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
import com.intellisql.common.dialect.SqlDialect;
import com.intellisql.translator.SqlTranslator;
import com.intellisql.translator.Translation;
import com.intellisql.translator.TranslationMode;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command to translate SQL between dialects.
 */
public class TranslateCommand implements ClientCommand {

    private static final String SOURCE_SHORT = "-s";

    private static final String SOURCE_LONG = "--source";

    private static final String TARGET_SHORT = "-t";

    private static final String TARGET_LONG = "--target";

    private static final String MODE_SHORT = "-m";

    private static final String MODE_LONG = "--mode";

    private final SqlTranslator translator;

    /**
     * Creates a new TranslateCommand instance.
     */
    public TranslateCommand() {
        this.translator = new SqlTranslator();
    }

    @Override
    public Connection execute(final ConsoleReader console, final Connection connection, final String[] args) {
        if (args.length < 1) {
            printUsage(console);
            return connection;
        }
        TranslateOptions options = parseOptions(args);
        String sql = String.join(" ", options.getSqlParts()).trim();
        if (sql.isEmpty()) {
            console.getPrinter().println("Error: SQL statement is required.");
            printUsage(console);
            return connection;
        }
        doTranslation(console, options, sql);
        return connection;
    }

    /**
     * Parses command line arguments into options.
     *
     * @param args the command arguments
     * @return the parsed options
     */
    private TranslateOptions parseOptions(final String[] args) {
        String sourceStr = "MYSQL";
        String targetStr = "POSTGRESQL";
        String modeStr = "OFFLINE";
        List<String> sqlParts = new ArrayList<>();
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                if (isFlag(arg, SOURCE_SHORT, SOURCE_LONG) && i + 1 < args.length) {
                    sourceStr = args[i + 1].toUpperCase();
                    i += 2;
                } else if (isFlag(arg, TARGET_SHORT, TARGET_LONG) && i + 1 < args.length) {
                    targetStr = args[i + 1].toUpperCase();
                    i += 2;
                } else if (isFlag(arg, MODE_SHORT, MODE_LONG) && i + 1 < args.length) {
                    modeStr = args[i + 1].toUpperCase();
                    i += 2;
                } else {
                    i++;
                }
            } else {
                sqlParts.add(arg);
                i++;
            }
        }
        return new TranslateOptions(sourceStr, targetStr, modeStr, sqlParts);
    }

    /**
     * Checks if the argument matches either of two flag strings.
     *
     * @param arg     the argument to check
     * @param shortFlag the short flag
     * @param longFlag  the long flag
     * @return true if the argument matches either flag
     */
    private boolean isFlag(final String arg, final String shortFlag, final String longFlag) {
        return shortFlag.equals(arg) || longFlag.equals(arg);
    }

    /**
     * Performs the translation and displays results.
     *
     * @param console the console reader
     * @param options the translation options
     * @param sql     the SQL to translate
     */
    private void doTranslation(final ConsoleReader console, final TranslateOptions options, final String sql) {
        try {
            SqlDialect sourceDialect = SqlDialect.valueOf(options.getSourceStr());
            SqlDialect targetDialect = SqlDialect.valueOf(options.getTargetStr());
            TranslationMode mode = TranslationMode.valueOf(options.getModeStr());
            console.getPrinter().println(String.format("Translating from %s to %s (%s)...",
                    sourceDialect, targetDialect, mode));
            Translation result = translate(sql, sourceDialect, targetDialect, mode);
            if (result.isSuccessful()) {
                printSuccessResult(console, result);
            } else {
                printFailureResult(console, result);
            }
        } catch (final IllegalArgumentException ex) {
            console.getPrinter().println("Error: Invalid dialect or mode. " + ex.getMessage());
            console.getPrinter().println("Supported dialects: " + Arrays.toString(SqlDialect.values()));
            console.getPrinter().println("Supported modes: " + Arrays.toString(TranslationMode.values()));
        }
    }

    /**
     * Translates the SQL based on the mode.
     *
     * @param sql            the SQL to translate
     * @param sourceDialect  the source dialect
     * @param targetDialect  the target dialect
     * @param mode           the translation mode
     * @return the translation result
     */
    private Translation translate(final String sql, final SqlDialect sourceDialect,
                                  final SqlDialect targetDialect, final TranslationMode mode) {
        if (mode == TranslationMode.ONLINE) {
            return translator.translateOnline(sql, sourceDialect, targetDialect);
        } else {
            return translator.translateOffline(sql, sourceDialect, targetDialect);
        }
    }

    /**
     * Prints a successful translation result.
     *
     * @param console the console reader
     * @param result  the translation result
     */
    private void printSuccessResult(final ConsoleReader console, final Translation result) {
        console.getPrinter().println("\n--- Translated SQL ---");
        console.getPrinter().println(result.getTargetSql());
        console.getPrinter().println("----------------------");
        if (result.hasUnsupportedFeatures()) {
            console.getPrinter().println("\nUnsupported features:");
            for (String feature : result.getUnsupportedFeatures()) {
                console.getPrinter().println("- " + feature);
            }
        }
    }

    /**
     * Prints a failed translation result.
     *
     * @param console the console reader
     * @param result  the translation result
     */
    private void printFailureResult(final ConsoleReader console, final Translation result) {
        console.getPrinter().println("\nTranslation failed:");
        if (result.getError() != null) {
            console.getPrinter().println(result.getError().getMessage());
        } else {
            console.getPrinter().println("Unknown error");
        }
    }

    /**
     * Prints usage information to the console.
     *
     * @param console the console reader
     */
    private void printUsage(final ConsoleReader console) {
        console.getPrinter().println("Usage: \\translate [options] <sql>");
        console.getPrinter().println("Options:");
        console.getPrinter().println("  -s, --source <dialect>  Source dialect (default: MYSQL)");
        console.getPrinter().println("  -t, --target <dialect>  Target dialect (default: POSTGRESQL)");
        console.getPrinter().println("  -m, --mode <mode>       Translation mode: ONLINE or OFFLINE (default: OFFLINE)");
    }

    @Override
    public String getName() {
        return "\\translate";
    }

    @Override
    public String getDescription() {
        return "Translate SQL between dialects";
    }

    /**
     * Holds translation options.
     */
    private static final class TranslateOptions {

        private final String sourceStr;

        private final String targetStr;

        private final String modeStr;

        private final List<String> sqlParts;

        private TranslateOptions(final String sourceStr, final String targetStr,
                                 final String modeStr, final List<String> sqlParts) {
            this.sourceStr = sourceStr;
            this.targetStr = targetStr;
            this.modeStr = modeStr;
            this.sqlParts = sqlParts;
        }

        public String getSourceStr() {
            return sourceStr;
        }

        public String getTargetStr() {
            return targetStr;
        }

        public String getModeStr() {
            return modeStr;
        }

        public List<String> getSqlParts() {
            return sqlParts;
        }
    }
}
