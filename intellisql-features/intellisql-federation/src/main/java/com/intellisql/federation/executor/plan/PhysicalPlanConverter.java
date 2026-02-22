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

package com.intellisql.federation.executor.plan;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.Sort;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.util.ImmutableBitSet;

import com.intellisql.federation.executor.Row;
import com.intellisql.federation.executor.iterator.AggregateOperator;
import com.intellisql.federation.executor.iterator.FilterOperator;
import com.intellisql.federation.executor.iterator.JoinOperator;
import com.intellisql.federation.executor.iterator.ProjectOperator;
import com.intellisql.federation.executor.iterator.QueryIterator;
import com.intellisql.federation.executor.iterator.SortOperator;
import com.intellisql.federation.executor.iterator.TableScanOperator;
import com.intellisql.connector.api.QueryExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * Converts a Calcite RelNode physical plan into a tree of QueryIterator operators.
 * Implements the Volcano iterator model for query execution.
 * Reference: ShardingSphere PhysicalPlanConverter pattern.
 */
@Slf4j
public class PhysicalPlanConverter {

    /** Map of data source IDs to their JDBC connections. */
    private final Map<String, Connection> connections;

    /** Map of data source IDs to their query executors. */
    private final Map<String, QueryExecutor> queryExecutors;

    /** Map of table names to data source IDs. */
    private final Map<String, String> tableToDataSourceMap;

    /**
     * Creates a new PhysicalPlanConverter.
     *
     * @param connections         map of data source IDs to connections
     * @param queryExecutors      map of data source IDs to query executors
     * @param tableToDataSourceMap map of table names to data source IDs
     */
    public PhysicalPlanConverter(
                                 final Map<String, Connection> connections,
                                 final Map<String, QueryExecutor> queryExecutors,
                                 final Map<String, String> tableToDataSourceMap) {
        this.connections = connections;
        this.queryExecutors = queryExecutors;
        this.tableToDataSourceMap = tableToDataSourceMap;
    }

    /**
     * Converts a RelNode plan to a QueryIterator operator tree.
     *
     * @param relNode the root of the RelNode plan
     * @return the root of the operator tree
     */
    public QueryIterator<Row> convert(final RelNode relNode) {
        log.debug("Converting RelNode plan to operator tree: {}", relNode.getRelTypeName());
        return convertNode(relNode, new HashMap<>());
    }

    /**
     * Recursively converts a RelNode to an operator.
     *
     * @param node         the RelNode to convert
     * @param columnNames  the column names from parent context
     * @return the corresponding operator
     */
    private QueryIterator<Row> convertNode(final RelNode node, final Map<String, List<String>> columnNames) {
        if (node instanceof TableScan) {
            return convertTableScan((TableScan) node);
        } else if (node instanceof Filter) {
            return convertFilter((Filter) node, columnNames);
        } else if (node instanceof Project) {
            return convertProject((Project) node, columnNames);
        } else if (node instanceof Join) {
            return convertJoin((Join) node, columnNames);
        } else if (node instanceof Aggregate) {
            return convertAggregate((Aggregate) node, columnNames);
        } else if (node instanceof Sort) {
            return convertSort((Sort) node, columnNames);
        } else {
            throw createUnsupportedException(node);
        }
    }

    private UnsupportedOperationException createUnsupportedException(final RelNode node) {
        return new UnsupportedOperationException("Unsupported RelNode type: " + node.getRelTypeName());
    }

    /**
     * Converts a TableScan to a TableScanOperator.
     *
     * @param tableScan the TableScan node
     * @return the TableScanOperator
     * @throws IllegalStateException if no connection or executor found for data source
     */
    private QueryIterator<Row> convertTableScan(final TableScan tableScan) {
        final List<String> qualifiedName = tableScan.getTable().getQualifiedName();
        final String tableName = qualifiedName.get(qualifiedName.size() - 1);
        final String dataSourceId = tableToDataSourceMap.getOrDefault(tableName, "default");

        log.debug("Converting TableScan for table: {}, dataSource: {}", tableName, dataSourceId);

        final Connection connection = connections.get(dataSourceId);
        final QueryExecutor queryExecutor = queryExecutors.get(dataSourceId);

        if (connection == null || queryExecutor == null) {
            throw new IllegalStateException("No connection or executor found for data source: " + dataSourceId);
        }

        // Generate simple SELECT * query
        final String sql = "SELECT * FROM " + String.join(".", qualifiedName);

        return new TableScanOperator(connection, queryExecutor, sql);
    }

    /**
     * Converts a Filter to a FilterOperator.
     *
     * @param filter      the Filter node
     * @param columnNames the column names context
     * @return the FilterOperator
     */
    private QueryIterator<Row> convertFilter(final Filter filter, final Map<String, List<String>> columnNames) {
        log.debug("Converting Filter: {}", filter.getCondition());

        final QueryIterator<Row> child = convertNode(filter.getInput(), columnNames);
        final Predicate<Row> rowPredicate = createRowPredicate(filter.getCondition(), columnNames);

        return new FilterOperator(child, rowPredicate);
    }

    /**
     * Converts a Project to a ProjectOperator.
     *
     * @param project     the Project node
     * @param columnNames the column names context
     * @return the ProjectOperator
     */
    private QueryIterator<Row> convertProject(final Project project, final Map<String, List<String>> columnNames) {
        log.debug("Converting Project with {} columns", project.getProjects().size());

        final QueryIterator<Row> child = convertNode(project.getInput(), columnNames);

        final List<java.util.function.Function<Row, Object>> projections = new ArrayList<>();
        final List<String> outputColumnNames = new ArrayList<>();

        for (int i = 0; i < project.getProjects().size(); i++) {
            final RexNode expr = project.getProjects().get(i);
            final String alias = project.getRowType().getFieldNames().get(i);

            projections.add(createProjectionFunction(expr));
            outputColumnNames.add(alias);
        }

        return new ProjectOperator(child, projections, outputColumnNames);
    }

    /**
     * Converts a Join to a JoinOperator.
     *
     * @param join        the Join node
     * @param columnNames the column names context
     * @return the JoinOperator
     */
    private QueryIterator<Row> convertJoin(final Join join, final Map<String, List<String>> columnNames) {
        log.debug("Converting Join: {}", join.getJoinType());

        final QueryIterator<Row> left = convertNode(join.getLeft(), columnNames);
        final QueryIterator<Row> right = convertNode(join.getRight(), columnNames);

        // Extract join keys from condition
        final JoinKeyExtractor extractor = new JoinKeyExtractor(join);
        final java.util.function.Function<Row, Object> leftKeyExtractor = extractor.getLeftKeyExtractor();
        final java.util.function.Function<Row, Object> rightKeyExtractor = extractor.getRightKeyExtractor();

        // Get column names for output
        final List<String> leftColumnNames = join.getLeft().getRowType().getFieldNames();
        final List<String> rightColumnNames = join.getRight().getRowType().getFieldNames();

        // No additional condition for now
        return new JoinOperator(
                left, right,
                leftKeyExtractor, rightKeyExtractor,
                null,
                leftColumnNames, rightColumnNames);
    }

    /**
     * Converts an Aggregate to an AggregateOperator.
     *
     * @param aggregate   the Aggregate node
     * @param columnNames the column names context
     * @return the AggregateOperator
     */
    private QueryIterator<Row> convertAggregate(final Aggregate aggregate, final Map<String, List<String>> columnNames) {
        log.debug("Converting Aggregate with {} group keys", aggregate.getGroupSet().cardinality());

        final QueryIterator<Row> child = convertNode(aggregate.getInput(), columnNames);

        // Create group by key extractors
        final List<java.util.function.Function<Row, Object>> groupByKeyExtractors = new ArrayList<>();
        final ImmutableBitSet groupSet = aggregate.getGroupSet();
        for (int i = 0; i < groupSet.cardinality(); i++) {
            final int groupIndex = groupSet.nth(i);
            groupByKeyExtractors.add(row -> row.getValue(groupIndex));
        }

        // Create aggregate functions (simplified - just COUNT for now)
        final List<AggregateOperator.AggregateFunction> aggFunctions = new ArrayList<>();
        for (org.apache.calcite.rel.core.AggregateCall aggCall : aggregate.getAggCallList()) {
            if (aggCall.getAggregation().getKind() == SqlKind.COUNT) {
                // COUNT(*) always returns 1 per row
                aggFunctions.add(new AggregateOperator.AggregateFunction(
                        AggregateOperator.AggregateType.COUNT,
                        row -> 1));
            } else if (aggCall.getAggregation().getKind() == SqlKind.SUM) {
                final int argIndex = aggCall.getArgList().get(0);
                aggFunctions.add(new AggregateOperator.AggregateFunction(
                        AggregateOperator.AggregateType.SUM,
                        row -> row.getValue(argIndex)));
            } else if (aggCall.getAggregation().getKind() == SqlKind.AVG) {
                final int argIndex = aggCall.getArgList().get(0);
                aggFunctions.add(new AggregateOperator.AggregateFunction(
                        AggregateOperator.AggregateType.AVG,
                        row -> row.getValue(argIndex)));
            } else if (aggCall.getAggregation().getKind() == SqlKind.MIN) {
                final int argIndex = aggCall.getArgList().get(0);
                aggFunctions.add(new AggregateOperator.AggregateFunction(
                        AggregateOperator.AggregateType.MIN,
                        row -> row.getValue(argIndex)));
            } else if (aggCall.getAggregation().getKind() == SqlKind.MAX) {
                final int argIndex = aggCall.getArgList().get(0);
                aggFunctions.add(new AggregateOperator.AggregateFunction(
                        AggregateOperator.AggregateType.MAX,
                        row -> row.getValue(argIndex)));
            }
        }

        // Build output column names
        final List<String> outputColumnNames = new ArrayList<>();
        for (int i = 0; i < groupSet.cardinality(); i++) {
            outputColumnNames.add(aggregate.getRowType().getFieldNames().get(i));
        }
        for (int i = 0; i < aggFunctions.size(); i++) {
            outputColumnNames.add(aggregate.getRowType().getFieldNames().get(groupSet.cardinality() + i));
        }

        return new AggregateOperator(child, groupByKeyExtractors, aggFunctions, outputColumnNames);
    }

    /**
     * Converts a Sort to a SortOperator.
     *
     * @param sort        the Sort node
     * @param columnNames the column names context
     * @return the SortOperator
     */
    private QueryIterator<Row> convertSort(final Sort sort, final Map<String, List<String>> columnNames) {
        log.debug("Converting Sort with {} collations", sort.getCollation().getFieldCollations().size());

        final QueryIterator<Row> child = convertNode(sort.getInput(), columnNames);

        // Build comparator from sort specification
        final List<org.apache.calcite.rel.RelFieldCollation> collations = sort.getCollation().getFieldCollations();
        final int[] columnIndices = new int[collations.size()];
        final boolean[] ascendings = new boolean[collations.size()];

        for (int i = 0; i < collations.size(); i++) {
            final org.apache.calcite.rel.RelFieldCollation fc = collations.get(i);
            columnIndices[i] = fc.getFieldIndex();
            ascendings[i] = !fc.direction.isDescending();
        }

        final java.util.Comparator<Row> comparator = SortOperator.compositeComparator(columnIndices, ascendings);

        // Handle LIMIT and OFFSET
        final int limit = sort.fetch != null ? RexLiteral.intValue(sort.fetch) : -1;
        final int offset = sort.offset != null ? RexLiteral.intValue(sort.offset) : 0;

        return new SortOperator(child, comparator, limit, offset);
    }

    /**
     * Creates a row predicate from a RexNode condition.
     *
     * @param condition   the RexNode condition
     * @param columnNames the column names context
     * @return the row predicate
     */
    private Predicate<Row> createRowPredicate(final RexNode condition, final Map<String, List<String>> columnNames) {
        // Simplified predicate creation - handles basic comparisons
        return row -> {
            // For now, return true for all rows
            // Full implementation would evaluate the RexNode condition
            return true;
        };
    }

    /**
     * Creates a projection function from a RexNode expression.
     *
     * @param expr the RexNode expression
     * @return the projection function
     */
    private java.util.function.Function<Row, Object> createProjectionFunction(final RexNode expr) {
        if (expr instanceof RexInputRef) {
            final int index = ((RexInputRef) expr).getIndex();
            return row -> row.getValue(index);
        } else if (expr instanceof RexLiteral) {
            final Object value = ((RexLiteral) expr).getValue();
            return row -> value;
        }
        // Default: return null
        return row -> null;
    }

    /**
     * Helper class to extract join keys from a join condition.
     */
    private static class JoinKeyExtractor {

        private java.util.function.Function<Row, Object> leftKeyExtractor;

        private java.util.function.Function<Row, Object> rightKeyExtractor;

        JoinKeyExtractor(final Join join) {
            extractKeys(join);
        }

        private void extractKeys(final Join join) {
            final RexNode condition = join.getCondition();
            if (condition instanceof RexInputRef) {
                // Simple single column join
                final int index = ((RexInputRef) condition).getIndex();
                final int leftFieldCount = join.getLeft().getRowType().getFieldCount();
                if (index < leftFieldCount) {
                    leftKeyExtractor = row -> row.getValue(index);
                    // Default
                    rightKeyExtractor = row -> row.getValue(0);
                } else {
                    // Default
                    leftKeyExtractor = row -> row.getValue(0);
                    rightKeyExtractor = row -> row.getValue(index - leftFieldCount);
                }
            } else {
                // Default extractors for complex conditions
                leftKeyExtractor = row -> row.getValue(0);
                rightKeyExtractor = row -> row.getValue(0);
            }
        }

        java.util.function.Function<Row, Object> getLeftKeyExtractor() {
            return leftKeyExtractor;
        }

        java.util.function.Function<Row, Object> getRightKeyExtractor() {
            return rightKeyExtractor;
        }
    }
}
