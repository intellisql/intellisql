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
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TerminalPrinterTest {

    @Test
    void testPrint() {
        Terminal terminal = mock(Terminal.class);
        PrintWriter writer = mock(PrintWriter.class);
        when(terminal.writer()).thenReturn(writer);
        TerminalPrinter printer = new TerminalPrinter(terminal);
        printer.print("Hello");
        verify(writer).print("Hello");
        verify(terminal).flush();
    }

    @Test
    void testPrintln() {
        Terminal terminal = mock(Terminal.class);
        PrintWriter writer = mock(PrintWriter.class);
        when(terminal.writer()).thenReturn(writer);
        TerminalPrinter printer = new TerminalPrinter(terminal);
        printer.println("Hello");
        verify(writer).println("Hello");
        verify(terminal).flush();
    }
}
