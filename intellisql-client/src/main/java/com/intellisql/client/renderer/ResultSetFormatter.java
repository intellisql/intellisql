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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for formatting ResultSet data into table format.
 */
public class ResultSetFormatter {

    /**
     * Formats the header of a result set table.
     *
     * @param metaData    the result set meta data
     * @param columnWidths the width of each column
     * @return a list of formatted header lines
     * @throws SQLException if a database access error occurs
     */
    public static List<String> formatHeader(final ResultSetMetaData metaData, final int[] columnWidths) throws SQLException {
        int columnCount = metaData.getColumnCount();
        List<String> lines = new ArrayList<>();
        StringBuilder header = new StringBuilder("|");
        StringBuilder separator = new StringBuilder("+");
        for (int i = 1; i <= columnCount; i++) {
            String label = metaData.getColumnLabel(i);
            header.append(" ").append(WidthCalculator.pad(label, columnWidths[i - 1])).append(" |");
            separator.append("-");
            for (int j = 0; j < columnWidths[i - 1]; j++) {
                separator.append("-");
            }
            separator.append("-+");
        }
        lines.add(separator.toString());
        lines.add(header.toString());
        lines.add(separator.toString());
        return lines;
    }

    /**
     * Formats a single row from a ResultSet.
     *
     * @param rs           the result set positioned at the row to format
     * @param columnWidths the width of each column
     * @return the formatted row string
     * @throws SQLException if a database access error occurs
     */
    public static String formatRow(final ResultSet rs, final int[] columnWidths) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        StringBuilder row = new StringBuilder("|");
        for (int i = 1; i <= columnCount; i++) {
            Object val = rs.getObject(i);
            String s = val == null ? "null" : val.toString();
            row.append(" ").append(WidthCalculator.padOrTruncate(s, columnWidths[i - 1], true)).append(" |");
        }
        return row.toString();
    }

    /**
     * Formats a single row from a list of values.
     *
     * @param rowData      the list of values in the row
     * @param columnWidths the width of each column
     * @return the formatted row string
     */
    public static String formatRow(final List<Object> rowData, final int[] columnWidths) {
        StringBuilder row = new StringBuilder("|");
        for (int i = 0; i < columnWidths.length; i++) {
            Object val = rowData.get(i);
            String s = val == null ? "null" : val.toString();
            row.append(" ").append(WidthCalculator.padOrTruncate(s, columnWidths[i], true)).append(" |");
        }
        return row.toString();
    }

    /**
     * Formats a separator line for the table.
     *
     * @param columnWidths the width of each column
     * @return the formatted separator string
     */
    public static String formatSeparator(final int[] columnWidths) {
        StringBuilder separator = new StringBuilder("+");
        for (int width : columnWidths) {
            separator.append("-");
            for (int j = 0; j < width; j++) {
                separator.append("-");
            }
            separator.append("-+");
        }
        return separator.toString();
    }

    /**
     * Calculates optimal column widths based on metadata only.
     *
     * @param metaData the result set meta data
     * @return an array of column widths
     * @throws SQLException if a database access error occurs
     */
    public static int[] calculateColumnWidths(final ResultSetMetaData metaData) throws SQLException {
        return calculateColumnWidths(metaData, null);
    }

    /**
     * Calculates optimal column widths based on metadata and buffered data.
     *
     * @param metaData the result set meta data
     * @param buffer   a buffer of row data to consider for width calculation
     * @return an array of column widths
     * @throws SQLException if a database access error occurs
     */
    public static int[] calculateColumnWidths(final ResultSetMetaData metaData, final List<List<Object>> buffer) throws SQLException {
        int columnCount = metaData.getColumnCount();
        int[] widths = new int[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            widths[i - 1] = Math.max(8, WidthCalculator.getWidth(metaData.getColumnLabel(i)));
            int displaySize = metaData.getColumnDisplaySize(i);
            // Use display size if reasonable, otherwise rely on buffer
            if (displaySize > 0 && displaySize < 50) {
                widths[i - 1] = Math.max(widths[i - 1], displaySize);
            }
        }
        if (buffer != null) {
            for (List<Object> row : buffer) {
                for (int i = 0; i < widths.length; i++) {
                    Object val = row.get(i);
                    String s = val == null ? "null" : val.toString();
                    widths[i] = Math.max(widths[i], WidthCalculator.getWidth(s));
                }
            }
        }
        return widths;
    }
}
