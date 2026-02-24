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

import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.PrintWriter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for ConsoleReader.
 */
class ConsoleReaderTest {

    @Test
    void testReadLine() throws Exception {
        Terminal terminal = mock(Terminal.class);
        LineReader lineReader = mock(LineReader.class);
        LineReaderBuilder builder = mock(LineReaderBuilder.class);
        when(terminal.writer()).thenReturn(mock(PrintWriter.class));
        try (
                MockedStatic<TerminalBuilder> termBuilderMock = Mockito.mockStatic(TerminalBuilder.class);
                MockedStatic<LineReaderBuilder> lineBuilderMock = Mockito.mockStatic(LineReaderBuilder.class)) {
            TerminalBuilder tb = mock(TerminalBuilder.class);
            termBuilderMock.when(TerminalBuilder::builder).thenReturn(tb);
            when(tb.system(true)).thenReturn(tb);
            when(tb.build()).thenReturn(terminal);
            lineBuilderMock.when(LineReaderBuilder::builder).thenReturn(builder);
            when(builder.terminal(terminal)).thenReturn(builder);
            when(builder.parser(any())).thenReturn(builder);
            when(builder.highlighter(any())).thenReturn(builder);
            when(builder.completer(any())).thenReturn(builder);
            when(builder.variable(any(), any())).thenReturn(builder);
            when(builder.build()).thenReturn(lineReader);
            when(lineReader.readLine("prompt> ")).thenReturn("command");
            try (ConsoleReader reader = new ConsoleReader(mock(Completer.class))) {
                Assertions.assertEquals("command", reader.readLine("prompt> "));
            }
        }
    }
}
