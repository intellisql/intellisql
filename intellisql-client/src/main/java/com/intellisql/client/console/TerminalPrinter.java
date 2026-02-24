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

package com.intellisql.client.console;

import org.jline.terminal.Terminal;

/**
 * Terminal printer for outputting messages to the console.
 */
public class TerminalPrinter {

    private final Terminal terminal;

    /**
     * Creates a new TerminalPrinter instance.
     *
     * @param terminal the terminal to print to
     */
    public TerminalPrinter(final Terminal terminal) {
        this.terminal = terminal;
    }

    /**
     * Prints a message to the terminal.
     *
     * @param message the message to print
     */
    public synchronized void print(final String message) {
        terminal.writer().print(message);
        terminal.flush();
    }

    /**
     * Prints a message followed by a newline to the terminal.
     *
     * @param message the message to print
     */
    public synchronized void println(final String message) {
        terminal.writer().println(message);
        terminal.flush();
    }
}
