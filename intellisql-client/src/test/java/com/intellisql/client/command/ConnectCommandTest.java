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
import com.intellisql.client.console.TerminalPrinter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for ConnectCommand.
 */
class ConnectCommandTest {

    @Test
    void testExecuteSuccess() throws Exception {
        ConsoleReader console = mock(ConsoleReader.class);
        TerminalPrinter printer = mock(TerminalPrinter.class);
        when(console.getPrinter()).thenReturn(printer);
        Connection connection = mock(Connection.class);
        try (MockedStatic<DriverManager> mockedDriverManager = Mockito.mockStatic(DriverManager.class)) {
            mockedDriverManager.when(() -> DriverManager.getConnection(eq("jdbc:url"), any(Properties.class)))
                    .thenReturn(connection);
            ConnectCommand cmd = new ConnectCommand();
            Connection result = cmd.execute(console, null, new String[]{"jdbc:url", "user", "pass"});
            Assertions.assertNotNull(result);
            verify(printer).println("Connected successfully.");
        }
    }

    @Test
    void testExecuteFail() throws Exception {
        ConsoleReader console = mock(ConsoleReader.class);
        TerminalPrinter printer = mock(TerminalPrinter.class);
        when(console.getPrinter()).thenReturn(printer);
        try (MockedStatic<DriverManager> mockedDriverManager = Mockito.mockStatic(DriverManager.class)) {
            SQLException exception = new SQLException("Failed");
            mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), any()))
                    .thenThrow(exception);
            ConnectCommand cmd = new ConnectCommand();
            Connection result = cmd.execute(console, null, new String[]{"jdbc:url"});
            Assertions.assertNull(result);
            verify(printer).println(contains("Connection failed"));
        }
    }

    @Test
    void testExecuteClosePrevious() throws Exception {
        ConsoleReader console = mock(ConsoleReader.class);
        TerminalPrinter printer = mock(TerminalPrinter.class);
        when(console.getPrinter()).thenReturn(printer);
        Connection prevConn = mock(Connection.class);
        try (MockedStatic<DriverManager> mockedDriverManager = Mockito.mockStatic(DriverManager.class)) {
            mockedDriverManager.when(() -> DriverManager.getConnection(eq("jdbc:url"), any(Properties.class)))
                    .thenReturn(mock(Connection.class));
            ConnectCommand cmd = new ConnectCommand();
            cmd.execute(console, prevConn, new String[]{"jdbc:url"});
            verify(prevConn).close();
        }
    }

    @Test
    void testExecuteNoArgs() throws Exception {
        ConsoleReader console = mock(ConsoleReader.class);
        TerminalPrinter printer = mock(TerminalPrinter.class);
        when(console.getPrinter()).thenReturn(printer);
        ConnectCommand cmd = new ConnectCommand();
        cmd.execute(console, null, new String[]{});
        verify(printer).println(contains("Usage"));
    }

    @Test
    void testGetInfo() {
        ConnectCommand cmd = new ConnectCommand();
        Assertions.assertEquals("\\connect", cmd.getName());
        Assertions.assertNotNull(cmd.getDescription());
    }
}
