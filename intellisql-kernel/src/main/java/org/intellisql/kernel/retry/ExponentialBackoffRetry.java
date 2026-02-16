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

package org.intellisql.kernel.retry;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Retry policy with exponential backoff. Implements retry logic with exponentially increasing
 * delays between attempts. Default configuration: max 3 retries with delays of 1s, 2s, 4s.
 */
@Slf4j
@Getter
public class ExponentialBackoffRetry implements RetryPolicy {

    private static final int DEFAULT_MAX_RETRIES = 3;

    private static final long INITIAL_DELAY_MS = 1000L;

    private static final double BACKOFF_MULTIPLIER = 2.0;

    private final int maxRetries;

    private final long initialDelayMs;

    private final double backoffMultiplier;

    private final TransientErrorDetector errorDetector;

    /** Creates a new ExponentialBackoffRetry with default settings. */
    public ExponentialBackoffRetry() {
        this(DEFAULT_MAX_RETRIES, INITIAL_DELAY_MS, BACKOFF_MULTIPLIER, new TransientErrorDetector());
    }

    /**
     * Creates a new ExponentialBackoffRetry with custom settings.
     *
     * @param maxRetries maximum number of retry attempts
     * @param initialDelayMs initial delay in milliseconds
     * @param backoffMultiplier multiplier for each subsequent delay
     * @param errorDetector detector for transient errors
     */
    public ExponentialBackoffRetry(
                                   final int maxRetries,
                                   final long initialDelayMs,
                                   final double backoffMultiplier,
                                   final TransientErrorDetector errorDetector) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        this.errorDetector = errorDetector;
    }

    @Override
    // CHECKSTYLE:OFF
    public <T> T execute(final RetryableOperation<T> operation) throws Exception {
        // CHECKSTYLE:ON
        Exception lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.execute();
                // CHECKSTYLE:OFF
            } catch (final RuntimeException runtimeException) {
                // CHECKSTYLE:ON
                lastException = runtimeException;
                if (!errorDetector.isTransient(runtimeException)) {
                    throw runtimeException;
                }
                if (attempt < maxRetries) {
                    final long delay = getRetryDelay(attempt);
                    log.warn(
                            "Operation failed (attempt {}/{}), retrying in {} ms: {}",
                            attempt + 1,
                            maxRetries + 1,
                            delay,
                            runtimeException.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (final InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", interruptedException);
                    }
                }
                // CHECKSTYLE:OFF
            } catch (final Exception exception) {
                // CHECKSTYLE:ON
                lastException = exception;
                if (!errorDetector.isTransient(exception)) {
                    throw exception;
                }
                if (attempt < maxRetries) {
                    final long delay = getRetryDelay(attempt);
                    log.warn(
                            "Operation failed (attempt {}/{}), retrying in {} ms: {}",
                            attempt + 1,
                            maxRetries + 1,
                            delay,
                            exception.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (final InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", interruptedException);
                    }
                }
            }
        }
        throw lastException;
    }

    @Override
    public long getRetryDelay(final int attempt) {
        return (long) (initialDelayMs * Math.pow(backoffMultiplier, attempt));
    }
}
