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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

import com.intellisql.federation.executor.Row;

import lombok.extern.slf4j.Slf4j;

/**
 * Join operator implementing hash join for cross-source queries.
 * Uses hash join algorithm: build phase on left (smaller) side, probe phase on right side.
 * Reference: ShardingSphere join operator pattern.
 */
@Slf4j
public class JoinOperator extends AbstractOperator<Row> {

    /** The left child operator (build side). */
    private final QueryIterator<Row> leftChild;

    /** The right child operator (probe side). */
    private final QueryIterator<Row> rightChild;

    /** The join condition (optional, for additional filtering after hash match). */
    private final BiPredicate<Row, Row> joinCondition;

    /** The left key extractor for hash join. */
    private final Function<Row, Object> leftKeyExtractor;

    /** The right key extractor for hash join. */
    private final Function<Row, Object> rightKeyExtractor;

    /** The hash table built from the left side. */
    private Map<Object, List<Row>> hashTable;

    /** The current right row being probed. */
    private Row currentRightRow;

    /** The matching left rows for the current right row. */
    private List<Row> currentLeftMatches;

    /** The current index in the matches list. */
    private int currentMatchIndex;

    /** The output column names. */
    private final List<String> outputColumnNames;

    /**
     * Creates a new JoinOperator.
     *
     * @param leftChild         the left child operator
     * @param rightChild        the right child operator
     * @param leftKeyExtractor  the left key extractor for hash join
     * @param rightKeyExtractor the right key extractor for hash join
     * @param joinCondition     the optional join condition
     * @param leftColumnNames   the left column names
     * @param rightColumnNames  the right column names
     */
    public JoinOperator(
                        final QueryIterator<Row> leftChild,
                        final QueryIterator<Row> rightChild,
                        final Function<Row, Object> leftKeyExtractor,
                        final Function<Row, Object> rightKeyExtractor,
                        final BiPredicate<Row, Row> joinCondition,
                        final List<String> leftColumnNames,
                        final List<String> rightColumnNames) {
        super("Join");
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.leftKeyExtractor = leftKeyExtractor;
        this.rightKeyExtractor = rightKeyExtractor;
        this.joinCondition = joinCondition;

        // Combine column names from both sides
        this.outputColumnNames = new ArrayList<>(leftColumnNames);
        this.outputColumnNames.addAll(rightColumnNames);
    }

    @Override
    protected void doOpen() throws Exception {
        log.debug("Opening join operator - building hash table");
        leftChild.open();
        rightChild.open();

        // Build phase: read all left rows into hash table
        hashTable = new HashMap<>();
        long leftRowCount = 0;
        while (leftChild.hasNext()) {
            final Row leftRow = leftChild.next();
            final Object key = leftKeyExtractor.apply(leftRow);
            hashTable.computeIfAbsent(key, k -> new ArrayList<>()).add(leftRow);
            leftRowCount++;
        }
        log.debug("Hash table built with {} rows from left side", leftRowCount);

        // Probe phase: start reading right rows
        fetchNextRightRow();
    }

    @Override
    protected void doClose() throws Exception {
        log.debug("Closing join operator");
        leftChild.close();
        rightChild.close();
        if (hashTable != null) {
            hashTable.clear();
            hashTable = null;
        }
    }

    @Override
    protected boolean doHasNext() throws Exception {
        return currentLeftMatches != null && currentMatchIndex < currentLeftMatches.size();
    }

    @Override
    protected Row doNext() throws Exception {
        if (currentLeftMatches == null || currentMatchIndex >= currentLeftMatches.size()) {
            throw new IllegalStateException("No more rows available");
        }

        final Row leftRow = currentLeftMatches.get(currentMatchIndex++);
        final Row result = mergeRows(leftRow, currentRightRow);

        // Check if we need to fetch more matches
        if (currentMatchIndex >= currentLeftMatches.size()) {
            fetchNextRightRow();
        }

        return result;
    }

    /**
     * Fetches the next right row and finds matching left rows.
     *
     * @throws Exception if iteration fails
     */
    private void fetchNextRightRow() throws Exception {
        currentLeftMatches = null;
        currentMatchIndex = 0;

        while (rightChild.hasNext()) {
            currentRightRow = rightChild.next();
            final Object key = rightKeyExtractor.apply(currentRightRow);

            final List<Row> matches = hashTable.get(key);
            if (matches != null && !matches.isEmpty()) {
                currentLeftMatches = filterMatchesIfNeeded(matches);
                if (currentLeftMatches != null) {
                    break;
                }
            }
        }
    }

    /**
     * Filters matches based on join condition if present.
     *
     * @param matches the potential matching rows
     * @return filtered matches, or original matches if no condition
     */
    private List<Row> filterMatchesIfNeeded(final List<Row> matches) {
        if (joinCondition == null) {
            return matches;
        }
        final List<Row> filteredMatches = new ArrayList<>();
        for (final Row leftRow : matches) {
            if (joinCondition.test(leftRow, currentRightRow)) {
                filteredMatches.add(leftRow);
            }
        }
        return filteredMatches.isEmpty() ? null : filteredMatches;
    }

    /**
     * Merges a left row and right row into a single output row.
     *
     * @param leftRow  the left row
     * @param rightRow the right row
     * @return the merged row
     */
    private Row mergeRows(final Row leftRow, final Row rightRow) {
        final List<Object> mergedValues = new ArrayList<>(leftRow.size() + rightRow.size());
        mergedValues.addAll(leftRow.getValues());
        mergedValues.addAll(rightRow.getValues());
        return new Row(mergedValues, outputColumnNames);
    }
}
