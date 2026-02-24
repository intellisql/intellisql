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

package com.intellisql.federation.executor.iterator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.intellisql.federation.executor.Row;

import lombok.extern.slf4j.Slf4j;

/**
 * Sort operator that sorts rows according to specified sort keys.
 * Implements in-memory sorting with configurable comparator.
 * Reference: ShardingSphere sort operator pattern.
 */
@Slf4j
public class SortOperator extends AbstractOperator<Row> {

    /** The child operator to read rows from. */
    private final QueryIterator<Row> child;

    /** The comparator for sorting. */
    private final Comparator<Row> comparator;

    /** The sorted results. */
    private List<Row> sortedResults;

    /** The current result index. */
    private int currentResultIndex;

    /** The limit for the number of rows to return (-1 for no limit). */
    private final int limit;

    /** The offset for pagination (0 for no offset). */
    private final int offset;

    /**
     * Creates a new SortOperator.
     *
     * @param child      the child operator
     * @param comparator the comparator for sorting
     */
    public SortOperator(final QueryIterator<Row> child, final Comparator<Row> comparator) {
        this(child, comparator, -1, 0);
    }

    /**
     * Creates a new SortOperator with limit and offset.
     *
     * @param child      the child operator
     * @param comparator the comparator for sorting
     * @param limit      the maximum number of rows to return (-1 for no limit)
     * @param offset     the number of rows to skip
     */
    public SortOperator(
                        final QueryIterator<Row> child,
                        final Comparator<Row> comparator,
                        final int limit,
                        final int offset) {
        super("Sort");
        this.child = child;
        this.comparator = comparator;
        this.limit = limit;
        this.offset = offset;
    }

    @Override
    protected void doOpen() throws Exception {
        log.debug("Opening sort operator");
        child.open();
        // Read all rows and sort
        sortedResults = performSort();
        // Start after offset
        currentResultIndex = offset;
        log.debug("Sort completed with {} total rows, starting at offset {}", sortedResults.size(), offset);
    }

    @Override
    protected void doClose() throws Exception {
        log.debug("Closing sort operator");
        child.close();
        if (sortedResults != null) {
            sortedResults.clear();
            sortedResults = null;
        }
    }

    @Override
    protected boolean doHasNext() throws Exception {
        if (sortedResults == null) {
            return false;
        }
        // Check if within bounds
        if (currentResultIndex >= sortedResults.size()) {
            return false;
        }
        // Check limit
        if (limit > 0 && (currentResultIndex - offset) >= limit) {
            return false;
        }
        return true;
    }

    @Override
    protected Row doNext() throws Exception {
        if (sortedResults == null || currentResultIndex >= sortedResults.size()) {
            throw new IllegalStateException("No more rows available");
        }
        if (limit > 0 && (currentResultIndex - offset) >= limit) {
            throw new IllegalStateException("Limit reached");
        }
        return sortedResults.get(currentResultIndex++);
    }

    /**
     * Performs the sort by reading all rows and sorting in memory.
     *
     * @return the sorted list of rows
     * @throws Exception if iteration fails
     */
    private List<Row> performSort() throws Exception {
        final List<Row> rows = new ArrayList<>();
        // Read all rows
        while (child.hasNext()) {
            rows.add(child.next());
        }
        log.debug("Read {} rows for sorting", rows.size());
        // Sort
        rows.sort(comparator);
        return rows;
    }

    /**
     * Creates a comparator for sorting by column index.
     *
     * @param columnIndex the column index to sort by
     * @param ascending   whether to sort ascending
     * @return the comparator
     */
    public static Comparator<Row> comparatorByColumn(final int columnIndex, final boolean ascending) {
        final Comparator<Row> comparator = (row1, row2) -> {
            final Object val1 = row1.getValue(columnIndex);
            final Object val2 = row2.getValue(columnIndex);
            return compareValues(val1, val2);
        };
        return ascending ? comparator : comparator.reversed();
    }

    /**
     * Creates a comparator for sorting by column name.
     *
     * @param columnName the column name to sort by
     * @param ascending  whether to sort ascending
     * @return the comparator
     */
    public static Comparator<Row> comparatorByColumnName(final String columnName, final boolean ascending) {
        final Comparator<Row> comparator = (row1, row2) -> {
            final Object val1 = row1.getValue(columnName);
            final Object val2 = row2.getValue(columnName);
            return compareValues(val1, val2);
        };
        return ascending ? comparator : comparator.reversed();
    }

    /**
     * Creates a composite comparator for multi-column sorting.
     *
     * @param columnIndices the column indices to sort by
     * @param ascendings    the sort directions for each column
     * @return the composite comparator
     */
    public static Comparator<Row> compositeComparator(final int[] columnIndices, final boolean[] ascendings) {
        return (row1, row2) -> compareRows(row1, row2, columnIndices, ascendings);
    }

    /**
     * Compares two rows based on multiple columns.
     *
     * @param row1          the first row
     * @param row2          the second row
     * @param columnIndices the column indices to sort by
     * @param ascendings    the sort directions for each column
     * @return the comparison result
     */
    private static int compareRows(
                                   final Row row1, final Row row2, final int[] columnIndices, final boolean[] ascendings) {
        for (int i = 0; i < columnIndices.length; i++) {
            final Object val1 = row1.getValue(columnIndices[i]);
            final Object val2 = row2.getValue(columnIndices[i]);
            final int cmp = compareValues(val1, val2);
            if (cmp != 0) {
                return ascendings[i] ? cmp : -cmp;
            }
        }
        return 0;
    }

    /**
     * Compares two values for sorting.
     *
     * @param val1 the first value
     * @param val2 the second value
     * @return the comparison result
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compareValues(final Object val1, final Object val2) {
        if (val1 == null && val2 == null) {
            return 0;
        }
        if (val1 == null) {
            return -1;
        }
        if (val2 == null) {
            return 1;
        }
        if (val1 instanceof Comparable && val2 instanceof Comparable) {
            return ((Comparable) val1).compareTo(val2);
        }
        return val1.toString().compareTo(val2.toString());
    }
}
