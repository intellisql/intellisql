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

package com.intellisql.client.renderer;

import com.intellisql.client.console.TerminalPrinter;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders ResultSet data with paging support.
 */
public class PagingRenderer {

    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Renders a ResultSet to the terminal with paging.
     *
     * @param rs      the result set to render
     * @param printer the terminal printer for output
     * @throws SQLException if a database access error occurs
     */
    public static void render(final ResultSet rs, final TerminalPrinter printer) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        // Buffer first N rows to calculate optimal column widths
        List<List<Object>> buffer = new ArrayList<>();
        int maxBuffer = 1000;
        while (buffer.size() < maxBuffer && rs.next()) {
            List<Object> row = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getObject(i));
            }
            buffer.add(row);
        }
        int[] columnWidths = ResultSetFormatter.calculateColumnWidths(metaData, buffer);
        List<String> headerLines = ResultSetFormatter.formatHeader(metaData, columnWidths);
        for (String line : headerLines) {
            printer.println(line);
        }
        int rowCount = 0;
        // Print buffered rows
        for (List<Object> rowData : buffer) {
            printer.println(ResultSetFormatter.formatRow(rowData, columnWidths));
            rowCount++;
            handlePageBoundary(rowCount);
        }
        // Print remaining rows (if any) - they will be truncated if exceeding calculated widths
        while (rs.next()) {
            printer.println(ResultSetFormatter.formatRow(rs, columnWidths));
            rowCount++;
            handlePageBoundary(rowCount);
        }
        printer.println(ResultSetFormatter.formatSeparator(columnWidths));
        printer.println(rowCount + " rows in set.");
    }

    /**
     * Handles page boundary for future paging implementation.
     * In a real less-like pager, this would wait for user input.
     *
     * @param rowCount the current row count
     */
    private static void handlePageBoundary(final int rowCount) {
        if (rowCount % DEFAULT_PAGE_SIZE == 0) {
            // Reserved for future paging implementation
            checkPageBreak();
        }
    }

    /**
     * Checks for page break - placeholder for future implementation.
     */
    private static void checkPageBreak() {
        // Future: implement user interaction for paging
    }
}
