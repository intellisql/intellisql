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

import java.util.function.Predicate;

import com.intellisql.federation.executor.Row;

import lombok.extern.slf4j.Slf4j;

/**
 * Filter operator that applies a predicate to filter rows.
 * Follows the Volcano iterator model for lazy evaluation.
 * Reference: ShardingSphere filter operator pattern.
 */
@Slf4j
public class FilterOperator extends AbstractOperator<Row> {

    /** The child operator to read rows from. */
    private final QueryIterator<Row> child;

    /** The predicate to filter rows. */
    private final Predicate<Row> predicate;

    /** The current row that passed the filter. */
    private Row currentRow;

    /**
     * Creates a new FilterOperator.
     *
     * @param child     the child operator
     * @param predicate the filter predicate
     */
    public FilterOperator(final QueryIterator<Row> child, final Predicate<Row> predicate) {
        super("Filter");
        this.child = child;
        this.predicate = predicate;
    }

    @Override
    protected void doOpen() throws Exception {
        log.debug("Opening filter operator");
        child.open();
        // Fetch the first matching row
        fetchNext();
    }

    @Override
    protected void doClose() throws Exception {
        log.debug("Closing filter operator");
        child.close();
    }

    @Override
    protected boolean doHasNext() throws Exception {
        return currentRow != null;
    }

    @Override
    protected Row doNext() throws Exception {
        if (currentRow == null) {
            throw new IllegalStateException("No more rows available");
        }
        final Row result = currentRow;
        // Fetch the next matching row
        fetchNext();
        return result;
    }

    /**
     * Fetches the next row that matches the predicate.
     *
     * @throws Exception if iteration fails
     */
    private void fetchNext() throws Exception {
        currentRow = null;
        while (child.hasNext()) {
            final Row row = child.next();
            if (predicate.test(row)) {
                currentRow = row;
                break;
            }
        }
    }
}
