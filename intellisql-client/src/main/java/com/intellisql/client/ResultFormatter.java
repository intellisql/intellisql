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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/** Formats query results for display in the CLI. */
@Slf4j
public class ResultFormatter {

    private static final int MAX_COLUMN_WIDTH = 50;

    private static final int MIN_COLUMN_WIDTH = 10;

    /**
     * Formats a ResultSet as a table for display.
     *
     * @param rs the ResultSet to format
     * @return formatted string representation
     * @throws SQLException if database access fails
     */
    public String formatTable(final ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<String[]> rows = new ArrayList<>();
        String[] headers = new String[columnCount];
        int[] widths = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            headers[i] = metaData.getColumnLabel(i + 1);
            widths[i] = Math.max(MIN_COLUMN_WIDTH, headers[i].length());
        }
        while (rs.next()) {
            String[] row = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                Object value = rs.getObject(i + 1);
                row[i] = value == null ? "NULL" : value.toString();
                widths[i] = Math.max(widths[i], Math.min(MAX_COLUMN_WIDTH, row[i].length()));
            }
            rows.add(row);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(formatSeparator(widths));
        sb.append(formatRow(headers, widths));
        sb.append(formatSeparator(widths));
        for (String[] row : rows) {
            sb.append(formatRow(row, widths));
        }
        sb.append(formatSeparator(widths));
        sb.append("(").append(rows.size()).append(" rows)\n");
        return sb.toString();
    }

    /**
     * Formats a separator line.
     *
     * @param widths the column widths
     * @return formatted separator string
     */
    private String formatSeparator(final int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int width : widths) {
            for (int i = 0; i < width + 2; i++) {
                sb.append("-");
            }
            sb.append("+");
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Formats a single row.
     *
     * @param values the row values
     * @param widths the column widths
     * @return formatted row string
     */
    private String formatRow(final String[] values, final int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < values.length; i++) {
            sb.append(" ");
            String value = values[i];
            if (value.length() > widths[i]) {
                value = value.substring(0, widths[i] - 3) + "...";
            }
            sb.append(String.format("%-" + widths[i] + "s", value));
            sb.append(" |");
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Formats an update count result.
     *
     * @param updateCount the number of rows affected
     * @return formatted string
     */
    public String formatUpdateCount(final int updateCount) {
        return "(" + updateCount + " row" + (updateCount != 1 ? "s" : "") + " affected)\n";
    }

    /**
     * Formats a translation result.
     *
     * @param original the original SQL
     * @param translated the translated SQL
     * @return formatted string
     */
    public String formatTranslation(final String original, final String translated) {
        StringBuilder sb = new StringBuilder();
        sb.append("Original SQL:\n");
        sb.append("  ").append(original).append("\n\n");
        sb.append("Translated SQL:\n");
        sb.append("  ").append(translated).append("\n");
        return sb.toString();
    }
}
