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

package com.intellisql.common.retry;

/**
 * Interface for retry policies. Defines the contract for implementing retry strategies for
 * transient failures.
 */
public interface RetryPolicy {

    /**
     * Executes the given operation with retry logic.
     *
     * @param operation the operation to execute
     * @param <T> the return type of the operation
     * @return the result of the operation
     * @throws Exception if all retry attempts fail
     */
    <T> T execute(RetryableOperation<T> operation) throws Exception;

    /**
     * Gets the maximum number of retry attempts.
     *
     * @return the maximum retry attempts
     */
    int getMaxRetries();

    /**
     * Gets the delay before the next retry attempt in milliseconds.
     *
     * @param attempt the current attempt number (0-indexed)
     * @return the delay in milliseconds
     */
    long getRetryDelay(int attempt);
}
