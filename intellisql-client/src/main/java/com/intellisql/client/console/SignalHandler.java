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

import org.jline.terminal.Terminal;

/**
 * Handles terminal signals such as interrupt (Ctrl+C).
 */
public class SignalHandler {

    private final Terminal terminal;

    /**
     * Creates a new SignalHandler instance.
     *
     * @param terminal the terminal to handle signals for
     */
    public SignalHandler(final Terminal terminal) {
        this.terminal = terminal;
    }

    /**
     * Sets up handling for the interrupt signal.
     * The handler prints ^C and allows the LineReader to handle the interruption.
     */
    public void handleInterrupt() {
        terminal.handle(Terminal.Signal.INT, signal -> {
            // Just print ^C and do not exit
            // The LineReader will automatically handle the interruption of readLine
            // We can add custom logic here if needed
        });
    }
}
