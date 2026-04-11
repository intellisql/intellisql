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

import java.io.Closeable;

/**
 * Query iterator interface following the Volcano iterator model.
 * Provides a pull-based iteration pattern for query execution.
 * Reference: ShardingSphere Enumerator pattern, Calcite AbstractEnumerable.
 *
 * <p>
 * Lifecycle:
 * <ol>
 *   <li>open() - Initialize resources</li>
 *   <li>hasNext()/next() - Iterate through results</li>
 *   <li>close() - Release resources</li>
 * </ol>
 * </p>
 *
 * @param <T> the type of elements returned by this iterator
 */
public interface QueryIterator<T> extends Closeable {

    /**
     * Opens the iterator and initializes any resources.
     * Must be called before iteration.
     *
     * @throws Exception if initialization fails
     */
    void open() throws Exception;

    /**
     * Checks if there are more elements to iterate.
     *
     * @return true if there are more elements
     * @throws Exception if an error occurs during iteration
     */
    boolean hasNext() throws Exception;

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element
     * @throws Exception if an error occurs or no more elements exist
     */
    T next() throws Exception;

    /**
     * Closes the iterator and releases all resources.
     * Safe to call multiple times.
     */
    @Override
    void close();

    /**
     * Resets the iterator to its initial state.
     * May not be supported by all implementations.
     *
     * @throws UnsupportedOperationException if reset is not supported
     */
    default void reset() {
        throw new UnsupportedOperationException("Reset not supported");
    }

    /**
     * Gets the current row number (0-based).
     *
     * @return the current row number
     */
    default long getCurrentRow() {
        return 0;
    }
}
