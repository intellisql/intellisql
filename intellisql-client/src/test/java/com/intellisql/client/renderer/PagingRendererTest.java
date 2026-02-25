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

package com.intellisql.client.renderer;

import com.intellisql.client.console.TerminalPrinter;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for PagingRenderer.
 */
class PagingRendererTest {

    @Test
    void testRender() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        final TerminalPrinter printer = mock(TerminalPrinter.class);
        when(rs.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("Col1");
        when(metaData.getColumnDisplaySize(1)).thenReturn(10);
        // One row
        when(rs.next()).thenReturn(true, false);
        when(rs.getObject(1)).thenReturn("Value");
        PagingRenderer.render(rs, printer);
        InOrder inOrder = inOrder(printer);
        // Separator
        inOrder.verify(printer).println(contains("+"));
        // Header
        inOrder.verify(printer).println(contains("Col1"));
        // Separator
        inOrder.verify(printer).println(contains("+"));
        // Row
        inOrder.verify(printer).println(contains("Value"));
        // End Separator
        inOrder.verify(printer).println(contains("+"));
        inOrder.verify(printer).println(contains("1 rows in set."));
    }
}
