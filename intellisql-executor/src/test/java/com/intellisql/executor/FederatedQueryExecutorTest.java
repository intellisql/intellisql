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

package com.intellisql.executor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.intellisql.executor.FederatedQueryExecutor.ConnectionProvider;
import com.intellisql.executor.FederatedQueryExecutor.JoinType;
import com.intellisql.executor.FederatedQueryExecutor.KeyExtractor;
import com.intellisql.optimizer.plan.ExecutionPlan;

/**
 * Unit tests for FederatedQueryExecutor.
 */
class FederatedQueryExecutorTest {

    private FederatedQueryExecutor executor;

    private ConnectionProvider mockConnectionProvider;

    @BeforeEach
    void setUp() {
        mockConnectionProvider = mock(ConnectionProvider.class);
        executor = new FederatedQueryExecutor(mockConnectionProvider);
    }

    @Test
    void testExecuteWithEmptyPlan() {
        final Query query = Query.builder()
                .id("test-query-1")
                .sql("SELECT 1")
                .build();
        final ExecutionPlan plan = ExecutionPlan.builder()
                .id("plan-1")
                .queryId("test-query-1")
                .build();

        final QueryResult result = executor.execute(query, plan);
        Assertions.assertNotNull(result);
    }

    @Test
    void testInnerJoin() {
        final List<Row> leftRows = createTestRows(
                Arrays.asList("id", "name"),
                Arrays.asList(Arrays.asList(1, "Alice"), Arrays.asList(2, "Bob")));
        final List<Row> rightRows = createTestRows(
                Arrays.asList("user_id", "score"),
                Arrays.asList(Arrays.asList(1, 100), Arrays.asList(1, 200)));

        final KeyExtractor leftKeyExtractor = row -> row.getValue(0);
        final KeyExtractor rightKeyExtractor = row -> row.getValue(0);

        final List<Row> result = executor.join(
                leftRows, rightRows, leftKeyExtractor, rightKeyExtractor, JoinType.INNER);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
    }

    @Test
    void testLeftJoin() {
        final List<Row> leftRows = createTestRows(
                Arrays.asList("id", "name"),
                Arrays.asList(Arrays.asList(1, "Alice"), Arrays.asList(3, "Charlie")));
        final List<Row> rightRows = createTestRows(
                Arrays.asList("user_id", "score"),
                Arrays.asList(Arrays.asList(1, 100)));

        final KeyExtractor leftKeyExtractor = row -> row.getValue(0);
        final KeyExtractor rightKeyExtractor = row -> row.getValue(0);

        final List<Row> result = executor.join(
                leftRows, rightRows, leftKeyExtractor, rightKeyExtractor, JoinType.LEFT);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
    }

    @Test
    void testJoinWithEmptyRightSide() {
        final List<Row> leftRows = createTestRows(
                Arrays.asList("id", "name"),
                Arrays.asList(Arrays.asList(1, "Alice")));
        final List<Row> rightRows = Collections.emptyList();

        final KeyExtractor leftKeyExtractor = row -> row.getValue(0);
        final KeyExtractor rightKeyExtractor = row -> row.getValue(0);

        final List<Row> result = executor.join(
                leftRows, rightRows, leftKeyExtractor, rightKeyExtractor, JoinType.INNER);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    private List<Row> createTestRows(final List<String> columns, final List<List<Object>> values) {
        final List<Row> rows = new ArrayList<>();
        for (final List<Object> valueList : values) {
            rows.add(new Row(valueList, columns));
        }
        return rows;
    }
}
