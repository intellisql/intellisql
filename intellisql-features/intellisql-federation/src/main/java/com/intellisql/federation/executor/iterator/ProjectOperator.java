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
import java.util.List;
import java.util.function.Function;

import com.intellisql.federation.executor.Row;

import lombok.extern.slf4j.Slf4j;

/**
 * Project operator that transforms rows by selecting/renaming columns.
 * Follows the Volcano iterator model for lazy evaluation.
 * Reference: ShardingSphere project operator pattern.
 */
@Slf4j
public class ProjectOperator extends AbstractOperator<Row> {

    /** The child operator to read rows from. */
    private final QueryIterator<Row> child;

    /** The projection expressions (column selectors). */
    private final List<Function<Row, Object>> projections;

    /** The output column names. */
    private final List<String> outputColumnNames;

    /**
     * Creates a new ProjectOperator.
     *
     * @param child             the child operator
     * @param projections       the projection expressions
     * @param outputColumnNames the output column names
     */
    public ProjectOperator(
                           final QueryIterator<Row> child,
                           final List<Function<Row, Object>> projections,
                           final List<String> outputColumnNames) {
        super("Project");
        this.child = child;
        this.projections = projections;
        this.outputColumnNames = outputColumnNames;
    }

    @Override
    protected void doOpen() throws Exception {
        log.debug("Opening project operator with {} columns", outputColumnNames.size());
        child.open();
    }

    @Override
    protected void doClose() throws Exception {
        log.debug("Closing project operator");
        child.close();
    }

    @Override
    protected boolean doHasNext() throws Exception {
        return child.hasNext();
    }

    @Override
    protected Row doNext() throws Exception {
        final Row inputRow = child.next();
        // Apply projections
        final List<Object> projectedValues = new ArrayList<>(projections.size());
        for (final Function<Row, Object> projection : projections) {
            projectedValues.add(projection.apply(inputRow));
        }
        return new Row(projectedValues, outputColumnNames);
    }

    /**
     * Creates a simple projection that selects columns by index.
     *
     * @param child           the child operator
     * @param columnIndices   the column indices to select
     * @param columnNames     the column names from the child
     * @return the project operator
     */
    public static ProjectOperator selectColumns(
                                                final QueryIterator<Row> child,
                                                final int[] columnIndices,
                                                final List<String> columnNames) {
        final List<Function<Row, Object>> projections = new ArrayList<>(columnIndices.length);
        final List<String> outputNames = new ArrayList<>(columnIndices.length);
        for (final int index : columnIndices) {
            projections.add(row -> row.getValue(index));
            outputNames.add(columnNames.get(index));
        }
        return new ProjectOperator(child, projections, outputNames);
    }

    /**
     * Creates a projection that selects columns by name.
     *
     * @param child         the child operator
     * @param columnNames   the column names to select
     * @return the project operator
     */
    public static ProjectOperator selectColumnsByName(
                                                      final QueryIterator<Row> child,
                                                      final List<String> columnNames) {
        final List<Function<Row, Object>> projections = new ArrayList<>(columnNames.size());
        for (final String name : columnNames) {
            projections.add(row -> row.getValue(name));
        }
        return new ProjectOperator(child, projections, columnNames);
    }
}
