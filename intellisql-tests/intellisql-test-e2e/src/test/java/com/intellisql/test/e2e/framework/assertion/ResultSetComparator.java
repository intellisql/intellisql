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

package com.intellisql.test.e2e.framework.assertion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Comparator for JDBC result set snapshots. */
public final class ResultSetComparator {

    private static final char VALUE_SEPARATOR = 31;

    /**
     * Compares two result set snapshots.
     *
     * @param actual the actual snapshot
     * @param expected the expected snapshot
     * @param orderMode the order comparison mode
     * @return comparison result
     */
    public ComparisonResult compare(final ResultSetSnapshot actual, final ResultSetSnapshot expected, final OrderMode orderMode) {
        ComparisonResult columnResult = compareColumns(actual, expected);
        if (!columnResult.isMatched()) {
            return columnResult;
        }
        if (actual.getRows().size() != expected.getRows().size()) {
            List<String> details = new ArrayList<>(2);
            details.add("Expected rows: " + expected.getRows().size());
            details.add("Actual rows: " + actual.getRows().size());
            return ComparisonResult.unmatched("Row count mismatch", details);
        }
        return OrderMode.STRICT == orderMode ? compareRowsStrict(actual, expected) : compareRowsAny(actual, expected);
    }

    private ComparisonResult compareColumns(final ResultSetSnapshot actual, final ResultSetSnapshot expected) {
        if (actual.getColumns().size() != expected.getColumns().size()) {
            List<String> details = new ArrayList<>(2);
            details.add("Expected columns: " + expected.getColumns().size());
            details.add("Actual columns: " + actual.getColumns().size());
            return ComparisonResult.unmatched("Column count mismatch", details);
        }
        for (int i = 0; i < expected.getColumns().size(); i++) {
            String expectedLabel = normalizeLabel(expected.getColumns().get(i).getLabel());
            String actualLabel = normalizeLabel(actual.getColumns().get(i).getLabel());
            if (!expectedLabel.equals(actualLabel)) {
                List<String> details = new ArrayList<>(1);
                details.add("Column " + (i + 1) + " expected '" + expectedLabel + "' but was '" + actualLabel + "'");
                return ComparisonResult.unmatched("Column label mismatch", details);
            }
        }
        return ComparisonResult.matched();
    }

    private ComparisonResult compareRowsStrict(final ResultSetSnapshot actual, final ResultSetSnapshot expected) {
        for (int i = 0; i < expected.getRows().size(); i++) {
            List<String> expectedValues = expected.getRows().get(i).getValues();
            List<String> actualValues = actual.getRows().get(i).getValues();
            if (!expectedValues.equals(actualValues)) {
                List<String> details = new ArrayList<>(2);
                details.add("Row " + (i + 1) + " expected " + expectedValues);
                details.add("Row " + (i + 1) + " actual " + actualValues);
                return ComparisonResult.unmatched("Row value mismatch", details);
            }
        }
        return ComparisonResult.matched();
    }

    private ComparisonResult compareRowsAny(final ResultSetSnapshot actual, final ResultSetSnapshot expected) {
        Map<String, Integer> expectedRows = countRows(expected);
        Map<String, Integer> actualRows = countRows(actual);
        if (expectedRows.equals(actualRows)) {
            return ComparisonResult.matched();
        }
        List<String> details = new ArrayList<>(2);
        appendMissingRows(details, expectedRows, actualRows);
        appendExtraRows(details, expectedRows, actualRows);
        return ComparisonResult.unmatched("Unordered row mismatch", details);
    }

    private Map<String, Integer> countRows(final ResultSetSnapshot snapshot) {
        Map<String, Integer> result = new HashMap<>(Math.max(snapshot.getRows().size(), 1));
        for (RowSnapshot each : snapshot.getRows()) {
            String signature = signature(each);
            Integer count = result.get(signature);
            result.put(signature, count == null ? 1 : count + 1);
        }
        return result;
    }

    private void appendMissingRows(final List<String> details, final Map<String, Integer> expectedRows, final Map<String, Integer> actualRows) {
        for (Map.Entry<String, Integer> entry : expectedRows.entrySet()) {
            int actualCount = actualRows.containsKey(entry.getKey()) ? actualRows.get(entry.getKey()) : 0;
            if (actualCount < entry.getValue()) {
                details.add("Missing row " + entry.getKey() + " count " + (entry.getValue() - actualCount));
            }
        }
    }

    private void appendExtraRows(final List<String> details, final Map<String, Integer> expectedRows, final Map<String, Integer> actualRows) {
        for (Map.Entry<String, Integer> entry : actualRows.entrySet()) {
            int expectedCount = expectedRows.containsKey(entry.getKey()) ? expectedRows.get(entry.getKey()) : 0;
            if (expectedCount < entry.getValue()) {
                details.add("Extra row " + entry.getKey() + " count " + (entry.getValue() - expectedCount));
            }
        }
    }

    private String signature(final RowSnapshot row) {
        StringBuilder result = new StringBuilder();
        for (String each : row.getValues()) {
            if (result.length() > 0) {
                result.append(VALUE_SEPARATOR);
            }
            result.append(each);
        }
        return result.toString();
    }

    private String normalizeLabel(final String label) {
        return label == null ? "" : label.toLowerCase(Locale.ROOT);
    }
}
