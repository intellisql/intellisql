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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * Tests for ResultSetFormatter.
 */
class ResultSetFormatterTest {

    @Test
    void testFormatHeader() throws SQLException {
        ResultSetMetaData metaData = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(metaData.getColumnCount()).thenReturn(2);
        Mockito.when(metaData.getColumnLabel(1)).thenReturn("ID");
        Mockito.when(metaData.getColumnLabel(2)).thenReturn("Name");
        int[] widths = new int[]{2, 10};
        List<String> header = ResultSetFormatter.formatHeader(metaData, widths);
        Assertions.assertEquals(3, header.size());
        Assertions.assertEquals("+----+------------+", header.get(0));
        Assertions.assertEquals("| ID | Name       |", header.get(1));
        Assertions.assertEquals("+----+------------+", header.get(2));
    }

    @Test
    void testCalculateColumnWidths() throws SQLException {
        ResultSetMetaData metaData = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(metaData.getColumnCount()).thenReturn(2);
        Mockito.when(metaData.getColumnLabel(1)).thenReturn("ID");
        Mockito.when(metaData.getColumnLabel(2)).thenReturn("Name");
        Mockito.when(metaData.getColumnDisplaySize(1)).thenReturn(5);
        Mockito.when(metaData.getColumnDisplaySize(2)).thenReturn(20);
        int[] widths = ResultSetFormatter.calculateColumnWidths(metaData);
        // Max(8, 2, 5) -> 8
        Assertions.assertEquals(8, widths[0]);
        // Max(8, 4, 20) -> 20
        Assertions.assertEquals(20, widths[1]);
    }

    @Test
    void testFormatRow() throws SQLException {
        ResultSet rs = Mockito.mock(ResultSet.class);
        ResultSetMetaData metaData = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(rs.getMetaData()).thenReturn(metaData);
        Mockito.when(metaData.getColumnCount()).thenReturn(2);
        Mockito.when(rs.getObject(1)).thenReturn(1);
        Mockito.when(rs.getObject(2)).thenReturn("Test");
        int[] widths = new int[]{2, 6};
        String row = ResultSetFormatter.formatRow(rs, widths);
        Assertions.assertEquals("| 1  | Test   |", row);
    }

    @Test
    void testFormatRowWithNull() throws SQLException {
        ResultSet rs = Mockito.mock(ResultSet.class);
        ResultSetMetaData metaData = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(rs.getMetaData()).thenReturn(metaData);
        Mockito.when(metaData.getColumnCount()).thenReturn(2);
        Mockito.when(rs.getObject(1)).thenReturn(null);
        Mockito.when(rs.getObject(2)).thenReturn(null);
        int[] widths = new int[]{4, 4};
        String row = ResultSetFormatter.formatRow(rs, widths);
        Assertions.assertEquals("| null | null |", row);
    }

    @Test
    void testFormatSeparator() {
        int[] widths = new int[]{2, 4};
        String separator = ResultSetFormatter.formatSeparator(widths);
        Assertions.assertEquals("+----+------+", separator);
    }
}
