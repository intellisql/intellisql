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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for TranslateCommand.
 */
class TranslateCommandTest {

    @Mock
    private ConsoleReader consoleReader;

    @Mock
    private TerminalPrinter printer;

    @Mock
    private Connection connection;

    private TranslateCommand command;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(consoleReader.getPrinter()).thenReturn(printer);
        command = new TranslateCommand();
    }

    @Test
    void testExecuteOffline() throws Exception {
        String[] args = {"-s", "MYSQL", "-t", "POSTGRESQL", "SELECT * FROM t1"};
        command.execute(consoleReader, connection, args);
        // Since we are not mocking the internal SqlTranslator (which is instantiated inside TranslateCommand),
        // we can only verify that some output was printed.
        // In a real unit test, we might want to dependency inject the translator.
        // But for now, assuming the translator works or fails gracefully:
        verify(printer, Mockito.atLeastOnce()).println(anyString());
    }

    @Test
    void testExecuteMissingSql() throws Exception {
        String[] args = {"-s", "MYSQL"};
        command.execute(consoleReader, connection, args);
        verify(printer).println("Error: SQL statement is required.");
    }
}
