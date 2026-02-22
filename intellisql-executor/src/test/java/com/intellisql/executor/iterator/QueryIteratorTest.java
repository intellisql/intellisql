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

package com.intellisql.executor.iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.intellisql.executor.Row;

/**
 * Unit tests for QueryIterator implementations.
 */
class QueryIteratorTest {

    private List<Row> testRows;

    @BeforeEach
    void setUp() {
        testRows = new ArrayList<>();
        testRows.add(new Row(Arrays.asList(1, "Alice"), Arrays.asList("id", "name")));
        testRows.add(new Row(Arrays.asList(2, "Bob"), Arrays.asList("id", "name")));
        testRows.add(new Row(Arrays.asList(3, "Charlie"), Arrays.asList("id", "name")));
    }

    @Test
    void testFilterOperator() throws Exception {
        final QueryIterator<Row> source = new ListIterator(testRows);
        final QueryIterator<Row> filter = new FilterOperator(
                source, row -> (Integer) row.getValue(0) > 1);

        filter.open();
        final List<Row> results = new ArrayList<>();
        while (filter.hasNext()) {
            results.add(filter.next());
        }
        filter.close();

        Assertions.assertEquals(2, results.size());
    }

    @Test
    void testProjectOperator() throws Exception {
        final QueryIterator<Row> source = new ListIterator(testRows);
        final List<java.util.function.Function<Row, Object>> projections = Arrays.asList(
                row -> row.getValue(0),
                row -> row.getValue(1));
        final QueryIterator<Row> project = new ProjectOperator(source, projections, Arrays.asList("id", "name"));

        project.open();
        final List<Row> results = new ArrayList<>();
        while (project.hasNext()) {
            results.add(project.next());
        }
        project.close();

        Assertions.assertEquals(3, results.size());
    }

    @Test
    void testSortOperator() throws Exception {
        final QueryIterator<Row> source = new ListIterator(testRows);
        final java.util.Comparator<Row> comparator = (r1, r2) -> {
            final String name1 = (String) r1.getValue(1);
            final String name2 = (String) r2.getValue(1);
            // Descending
            return name2.compareTo(name1);
        };
        final QueryIterator<Row> sort = new SortOperator(source, comparator, -1, 0);

        sort.open();
        final List<Row> results = new ArrayList<>();
        while (sort.hasNext()) {
            results.add(sort.next());
        }
        sort.close();

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("Charlie", results.get(0).getValue(1));
        Assertions.assertEquals("Bob", results.get(1).getValue(1));
        Assertions.assertEquals("Alice", results.get(2).getValue(1));
    }

    @Test
    void testSortOperatorWithLimit() throws Exception {
        final QueryIterator<Row> source = new ListIterator(testRows);
        final java.util.Comparator<Row> comparator = (r1, r2) -> 0;
        final QueryIterator<Row> sort = new SortOperator(source, comparator, 2, 0);

        sort.open();
        final List<Row> results = new ArrayList<>();
        while (sort.hasNext()) {
            results.add(sort.next());
        }
        sort.close();

        Assertions.assertEquals(2, results.size());
    }

    /**
     * Simple iterator implementation for testing.
     */
    private static class ListIterator extends AbstractOperator<Row> {

        private final List<Row> rows;

        private int index;

        ListIterator(final List<Row> rows) {
            super("ListIterator");
            this.rows = rows;
        }

        @Override
        protected void doOpen() {
            index = 0;
        }

        @Override
        protected boolean doHasNext() {
            return index < rows.size();
        }

        @Override
        protected Row doNext() {
            return rows.get(index++);
        }

        @Override
        protected void doClose() {
            index = rows.size();
        }
    }
}
