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

package com.intellisql.client.console;

import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

/**
 * Console reader for interactive input with syntax highlighting and completion.
 */
public class ConsoleReader implements AutoCloseable {

    private final Terminal terminal;

    private final LineReader lineReader;

    private final TerminalPrinter printer;

    /**
     * Creates a new ConsoleReader without a completer.
     *
     * @throws IOException if terminal creation fails
     */
    public ConsoleReader() throws IOException {
        this(null);
    }

    /**
     * Creates a new ConsoleReader with the specified completer.
     *
     * @param completer the completer for auto-completion
     * @throws IOException if terminal creation fails
     */
    public ConsoleReader(final Completer completer) throws IOException {
        this.terminal = TerminalBuilder.builder().system(true).build();
        this.printer = new TerminalPrinter(terminal);
        DefaultParser parser = new DefaultParser() {

            @Override
            public boolean isEscapeChar(final CharSequence line, final int pos) {
                return false;
            }
        };
        parser.setEscapeChars(null);
        this.lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                .completer(completer)
                .highlighter(new SyntaxHighlighter())
                .variable(LineReader.HISTORY_FILE, System.getProperty("user.home") + "/.isql_history")
                .variable("DISABLE_EVENT_EXPANSION", true)
                .build();
        new SignalHandler(terminal).handleInterrupt();
    }

    /**
     * Reads a line of input with the specified prompt.
     *
     * @param prompt the prompt to display
     * @return the line read from the user
     */
    public String readLine(final String prompt) {
        return lineReader.readLine(prompt);
    }

    /**
     * Returns the terminal printer for output.
     *
     * @return the terminal printer
     */
    public TerminalPrinter getPrinter() {
        return printer;
    }

    @Override
    public void close() throws Exception {
        terminal.close();
    }
}
