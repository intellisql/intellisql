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

package com.intellisql.federation.executor.iterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.intellisql.federation.executor.Row;

import lombok.extern.slf4j.Slf4j;

/**
 * Aggregate operator that groups rows and computes aggregate functions.
 * Supports: COUNT, SUM, AVG, MIN, MAX.
 * Reference: ShardingSphere aggregate operator pattern.
 */
@Slf4j
public class AggregateOperator extends AbstractOperator<Row> {

    /** The child operator to read rows from. */
    private final QueryIterator<Row> child;

    /** The group by key extractors. */
    private final List<Function<Row, Object>> groupByKeyExtractors;

    /** The aggregate function specifications. */
    private final List<AggregateFunction> aggregateFunctions;

    /** The output column names. */
    private final List<String> outputColumnNames;

    /** The aggregated results. */
    private List<Row> aggregatedResults;

    /** The current result index. */
    private int currentResultIndex;

    /**
     * Creates a new AggregateOperator.
     *
     * @param child               the child operator
     * @param groupByKeyExtractors the group by key extractors
     * @param aggregateFunctions   the aggregate functions
     * @param outputColumnNames    the output column names
     */
    public AggregateOperator(
                             final QueryIterator<Row> child,
                             final List<Function<Row, Object>> groupByKeyExtractors,
                             final List<AggregateFunction> aggregateFunctions,
                             final List<String> outputColumnNames) {
        super("Aggregate");
        this.child = child;
        this.groupByKeyExtractors = groupByKeyExtractors;
        this.aggregateFunctions = aggregateFunctions;
        this.outputColumnNames = outputColumnNames;
    }

    @Override
    protected void doOpen() throws Exception {
        log.debug("Opening aggregate operator");
        child.open();
        // Perform aggregation
        aggregatedResults = performAggregation();
        currentResultIndex = 0;
        log.debug("Aggregation completed with {} groups", aggregatedResults.size());
    }

    @Override
    protected void doClose() throws Exception {
        log.debug("Closing aggregate operator");
        child.close();
        if (aggregatedResults != null) {
            aggregatedResults.clear();
            aggregatedResults = null;
        }
    }

    @Override
    protected boolean doHasNext() throws Exception {
        return aggregatedResults != null && currentResultIndex < aggregatedResults.size();
    }

    @Override
    protected Row doNext() throws Exception {
        if (aggregatedResults == null || currentResultIndex >= aggregatedResults.size()) {
            throw new IllegalStateException("No more rows available");
        }
        return aggregatedResults.get(currentResultIndex++);
    }

    /**
     * Performs the aggregation by reading all rows and computing groups.
     *
     * @return the list of aggregated rows
     * @throws Exception if iteration fails
     */
    private List<Row> performAggregation() throws Exception {
        final Map<GroupKey, AggregateAccumulator[]> groups = new HashMap<>();
        // Read all rows and accumulate
        while (child.hasNext()) {
            final Row row = child.next();
            final GroupKey key = extractGroupKey(row);
            final AggregateAccumulator[] accumulators = groups.computeIfAbsent(key,
                    k -> createAccumulators());
            for (int i = 0; i < aggregateFunctions.size(); i++) {
                final Object value = aggregateFunctions.get(i).valueExtractor.apply(row);
                accumulators[i].accumulate(value);
            }
        }
        // Convert groups to result rows
        final List<Row> results = new ArrayList<>(groups.size());
        for (final Map.Entry<GroupKey, AggregateAccumulator[]> entry : groups.entrySet()) {
            final List<Object> values = new ArrayList<>();
            // Add group key values
            values.addAll(entry.getKey().values);
            // Add aggregate results
            for (final AggregateAccumulator accumulator : entry.getValue()) {
                values.add(accumulator.getResult());
            }
            results.add(new Row(values, outputColumnNames));
        }
        return results;
    }

    /**
     * Extracts the group key from a row.
     *
     * @param row the row
     * @return the group key
     */
    private GroupKey extractGroupKey(final Row row) {
        final List<Object> keyValues = new ArrayList<>(groupByKeyExtractors.size());
        for (final Function<Row, Object> extractor : groupByKeyExtractors) {
            keyValues.add(extractor.apply(row));
        }
        return new GroupKey(keyValues);
    }

    /**
     * Creates accumulators for each aggregate function.
     *
     * @return the array of accumulators
     */
    private AggregateAccumulator[] createAccumulators() {
        final AggregateAccumulator[] accumulators = new AggregateAccumulator[aggregateFunctions.size()];
        for (int i = 0; i < aggregateFunctions.size(); i++) {
            accumulators[i] = aggregateFunctions.get(i).type.createAccumulator();
        }
        return accumulators;
    }

    /** Group key wrapper. */
    private static class GroupKey {

        private final List<Object> values;

        GroupKey(final List<Object> values) {
            this.values = values;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final GroupKey that = (GroupKey) o;
            return values.equals(that.values);
        }

        @Override
        public int hashCode() {
            return values.hashCode();
        }
    }

    /** Aggregate type enum. */
    public enum AggregateType {

        COUNT {

            @Override
            AggregateAccumulator createAccumulator() {
                return new CountAccumulator();
            }
        },
        SUM {

            @Override
            AggregateAccumulator createAccumulator() {
                return new SumAccumulator();
            }
        },
        AVG {

            @Override
            AggregateAccumulator createAccumulator() {
                return new AvgAccumulator();
            }
        },
        MIN {

            @Override
            AggregateAccumulator createAccumulator() {
                return new MinAccumulator();
            }
        },
        MAX {

            @Override
            AggregateAccumulator createAccumulator() {
                return new MaxAccumulator();
            }
        };

        abstract AggregateAccumulator createAccumulator();
    }

    /** Aggregate function specification. */
    public static class AggregateFunction {

        private final AggregateType type;

        private final Function<Row, Object> valueExtractor;

        public AggregateFunction(final AggregateType type, final Function<Row, Object> valueExtractor) {
            this.type = type;
            this.valueExtractor = valueExtractor;
        }
    }

    /** Base accumulator interface. */
    private interface AggregateAccumulator {

        void accumulate(Object value);

        Object getResult();
    }

    /** COUNT accumulator. */
    private static class CountAccumulator implements AggregateAccumulator {

        private long count;

        @Override
        public void accumulate(final Object value) {
            if (value != null) {
                count++;
            }
        }

        @Override
        public Object getResult() {
            return count;
        }
    }

    /** SUM accumulator. */
    private static class SumAccumulator implements AggregateAccumulator {

        private double sum;

        @Override
        public void accumulate(final Object value) {
            if (value instanceof Number) {
                sum += ((Number) value).doubleValue();
            }
        }

        @Override
        public Object getResult() {
            return sum;
        }
    }

    /** AVG accumulator. */
    private static class AvgAccumulator implements AggregateAccumulator {

        private double sum;

        private long count;

        @Override
        public void accumulate(final Object value) {
            if (value instanceof Number) {
                sum += ((Number) value).doubleValue();
                count++;
            }
        }

        @Override
        public Object getResult() {
            return count > 0 ? sum / count : null;
        }
    }

    /** MIN accumulator. */
    private static class MinAccumulator implements AggregateAccumulator {

        private Comparable<Object> min;

        @Override
        @SuppressWarnings("unchecked")
        public void accumulate(final Object value) {
            if (value instanceof Comparable) {
                final Comparable<Object> comparable = (Comparable<Object>) value;
                if (min == null || comparable.compareTo(min) < 0) {
                    min = comparable;
                }
            }
        }

        @Override
        public Object getResult() {
            return min;
        }
    }

    /** MAX accumulator. */
    private static class MaxAccumulator implements AggregateAccumulator {

        private Comparable<Object> max;

        @Override
        @SuppressWarnings("unchecked")
        public void accumulate(final Object value) {
            if (value instanceof Comparable) {
                final Comparable<Object> comparable = (Comparable<Object>) value;
                if (max == null || comparable.compareTo(max) > 0) {
                    max = comparable;
                }
            }
        }

        @Override
        public Object getResult() {
            return max;
        }
    }
}
