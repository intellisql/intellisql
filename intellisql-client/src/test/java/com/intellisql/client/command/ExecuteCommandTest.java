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
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for ExecuteCommand.
 */
class ExecuteCommandTest {

    @Test
    void testExecuteQuery() throws Exception {
        ConsoleReader console = mock(ConsoleReader.class);
        TerminalPrinter printer = mock(TerminalPrinter.class);
        when(console.getPrinter()).thenReturn(printer);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);
        when(statement.getResultSet()).thenReturn(rs);
        when(rs.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnDisplaySize(1)).thenReturn(10);
        when(metaData.getColumnLabel(1)).thenReturn("Test");
        when(rs.next()).thenReturn(false);
        ExecuteCommand cmd = new ExecuteCommand();
        cmd.execute(console, connection, new String[]{"SELECT", "*", "FROM", "table"});
        verify(statement).execute("SELECT * FROM table");
        verify(printer, atLeastOnce()).println(anyString());
    }

    @Test
    void testExecuteUpdate() throws Exception {
        ConsoleReader console = mock(ConsoleReader.class);
        TerminalPrinter printer = mock(TerminalPrinter.class);
        when(console.getPrinter()).thenReturn(printer);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        Mockito.when(connection.createStatement()).thenReturn(statement);
        Mockito.when(statement.execute(anyString())).thenReturn(false);
        Mockito.when(statement.getUpdateCount()).thenReturn(5);
        ExecuteCommand cmd = new ExecuteCommand();
        cmd.execute(console, connection, new String[]{"UPDATE", "table"});
        verify(printer).println("Update count: 5");
    }

    @Test
    void testExecuteException() throws Exception {
        ConsoleReader console = mock(ConsoleReader.class);
        TerminalPrinter printer = mock(TerminalPrinter.class);
        when(console.getPrinter()).thenReturn(printer);
        Connection connection = mock(Connection.class);
        Mockito.when(connection.createStatement()).thenThrow(new SQLException("Error"));
        ExecuteCommand cmd = new ExecuteCommand();
        cmd.execute(console, connection, new String[]{"SELECT"});
        verify(printer).println("Error executing SQL: Error");
    }

    @Test
    void testNotConnected() throws Exception {
        ConsoleReader console = mock(ConsoleReader.class);
        TerminalPrinter printer = mock(TerminalPrinter.class);
        when(console.getPrinter()).thenReturn(printer);
        ExecuteCommand cmd = new ExecuteCommand();
        cmd.execute(console, null, new String[]{"SELECT"});
        verify(printer).println(contains("Not connected"));
    }

    @Test
    void testEmptyArgs() throws Exception {
        ConsoleReader console = mock(ConsoleReader.class);
        Connection connection = mock(Connection.class);
        ExecuteCommand cmd = new ExecuteCommand();
        cmd.execute(console, connection, new String[]{});
        verifyNoInteractions(connection);
    }

    @Test
    void testGetInfo() {
        ExecuteCommand cmd = new ExecuteCommand();
        Assertions.assertEquals("execute", cmd.getName());
        Assertions.assertNotNull(cmd.getDescription());
    }
}
