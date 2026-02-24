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

package com.intellisql.client;

import com.intellisql.client.console.ConsoleReader;
import com.intellisql.client.console.TerminalPrinter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for IntelliSqlClient.
 */
class IntelliSqlClientTest {

    private static final String UNKNOWN_CMD = "\\unknown";

    private static final String SELECT_CMD = "SELECT 1";

    private static final String QUIT_CMD = "\\quit";

    @Test
    void testCallLoop() throws Exception {
        try (MockedConstruction<ConsoleReader> mocked = createMockedConsoleReader()) {
            IntelliSqlClient client = new IntelliSqlClient();
            int exitCode = client.call();
            Assertions.assertEquals(0, exitCode);
            verifyMockInteractions(mocked);
        }
    }

    /**
     * Creates a mocked ConsoleReader construction.
     *
     * @return the mocked construction
     */
    private MockedConstruction<ConsoleReader> createMockedConsoleReader() {
        return Mockito.mockConstruction(ConsoleReader.class,
                (mockReader, ctx) -> setupMockReaderBehavior(mockReader));
    }

    /**
     * Sets up mock reader behavior with predefined command sequence.
     *
     * @param mockReader the mock reader to configure
     */
    private void setupMockReaderBehavior(final ConsoleReader mockReader) {
        when(mockReader.readLine(anyString()))
                .thenReturn(UNKNOWN_CMD)
                .thenReturn(SELECT_CMD)
                .thenReturn(QUIT_CMD);
        TerminalPrinter printer = mock(TerminalPrinter.class);
        when(mockReader.getPrinter()).thenReturn(printer);
    }

    /**
     * Verifies mock interactions after test execution.
     *
     * @param mocked the mocked construction to verify
     */
    private void verifyMockInteractions(final MockedConstruction<ConsoleReader> mocked) {
        ConsoleReader mockReader = mocked.constructed().get(0);
        verify(mockReader, times(3)).readLine(anyString());
        TerminalPrinter printer = mockReader.getPrinter();
        verify(printer).println(contains("Unknown command"));
    }
}
