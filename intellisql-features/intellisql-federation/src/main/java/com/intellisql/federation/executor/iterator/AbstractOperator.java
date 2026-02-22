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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for query operators in the Volcano iterator model.
 * Provides common lifecycle management and resource handling.
 * Reference: ShardingSphere AbstractEnumerable, Calcite Enumerator pattern.
 *
 * @param <T> the type of elements returned by this operator
 */
@Slf4j
public abstract class AbstractOperator<T> implements QueryIterator<T> {

    /** Whether the operator has been opened. */
    // CHECKSTYLE:OFF
    protected final AtomicBoolean opened = new AtomicBoolean(false);

    /** Whether the operator has been closed. */
    protected final AtomicBoolean closed = new AtomicBoolean(false);

    /** The current row number. */
    protected final AtomicLong currentRow = new AtomicLong(0);

    /** The operator name for logging. */
    protected final String operatorName;
    // CHECKSTYLE:ON

    /**
     * Creates a new AbstractOperator.
     *
     * @param operatorName the name of the operator for logging
     */
    protected AbstractOperator(final String operatorName) {
        this.operatorName = operatorName;
    }

    @Override
    public void open() throws Exception {
        if (opened.compareAndSet(false, true)) {
            log.debug("Opening operator: {}", operatorName);
            doOpen();
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            log.debug("Closing operator: {} after {} rows", operatorName, currentRow.get());
            // CHECKSTYLE:OFF
            try {
                doClose();
            } catch (final Exception ex) {
                log.warn("Error closing operator: {}", operatorName, ex);
            }
            // CHECKSTYLE:ON
        }
    }

    @Override
    public boolean hasNext() throws Exception {
        ensureOpen();
        return doHasNext();
    }

    @Override
    public T next() throws Exception {
        ensureOpen();
        final T result = doNext();
        currentRow.incrementAndGet();
        return result;
    }

    @Override
    public long getCurrentRow() {
        return currentRow.get();
    }

    /**
     * Template method for subclass-specific open logic.
     *
     * @throws Exception if open fails
     */
    protected abstract void doOpen() throws Exception;

    /**
     * Template method for subclass-specific close logic.
     *
     * @throws Exception if close fails
     */
    protected abstract void doClose() throws Exception;

    /**
     * Template method for subclass-specific hasNext logic.
     *
     * @return true if there are more elements
     * @throws Exception if iteration fails
     */
    protected abstract boolean doHasNext() throws Exception;

    /**
     * Template method for subclass-specific next logic.
     *
     * @return the next element
     * @throws Exception if iteration fails
     */
    protected abstract T doNext() throws Exception;

    /**
     * Ensures the operator has been opened.
     *
     * @throws IllegalStateException if the operator is not open
     */
    protected void ensureOpen() {
        if (!opened.get()) {
            throw new IllegalStateException("Operator not opened: " + operatorName);
        }
        if (closed.get()) {
            throw new IllegalStateException("Operator already closed: " + operatorName);
        }
    }

    /**
     * Gets the operator name.
     *
     * @return the operator name
     */
    public String getOperatorName() {
        return operatorName;
    }
}
