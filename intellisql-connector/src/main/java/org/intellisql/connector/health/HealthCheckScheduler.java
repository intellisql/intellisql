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

package org.intellisql.connector.health;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.intellisql.connector.config.DataSourceConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler for periodic health checks on data sources. Provides configurable intervals and
 * callback support for health check results.
 */
@Slf4j
public class HealthCheckScheduler {

    private final ScheduledExecutorService scheduler;

    private final HealthChecker healthChecker;

    private final Map<String, java.util.concurrent.ScheduledFuture<?>> scheduledTasks =
            new ConcurrentHashMap<>();

    private final Map<String, HealthCheckResult> lastResults = new ConcurrentHashMap<>();

    private Consumer<HealthCheckResult> healthCheckListener;

    /** Creates a new HealthCheckScheduler with the default health checker. */
    public HealthCheckScheduler() {
        this(new DataSourceHealthChecker());
    }

    /**
     * Creates a new HealthCheckScheduler with the specified health checker.
     *
     * @param healthChecker the health checker to use
     */
    public HealthCheckScheduler(final HealthChecker healthChecker) {
        this.healthChecker = healthChecker;
        this.scheduler =
                Executors.newScheduledThreadPool(
                        2,
                        r -> {
                            Thread thread = new Thread(r, "health-check-scheduler");
                            thread.setDaemon(true);
                            return thread;
                        });
    }

    /**
     * Schedules a periodic health check for the given data source.
     *
     * @param config the data source configuration
     * @param intervalSeconds the interval between health checks in seconds
     */
    public void scheduleHealthCheck(final DataSourceConfig config, final long intervalSeconds) {
        String dataSourceName = config.getName();
        if (scheduledTasks.containsKey(dataSourceName)) {
            log.warn("Health check already scheduled for data source: {}", dataSourceName);
            return;
        }
        log.info("Scheduling health check for '{}' every {} seconds", dataSourceName, intervalSeconds);
        java.util.concurrent.ScheduledFuture<?> future =
                scheduler.scheduleAtFixedRate(
                        () -> performHealthCheck(config), 0, intervalSeconds, TimeUnit.SECONDS);
        scheduledTasks.put(dataSourceName, future);
    }

    /**
     * Schedules a periodic health check for the given data source with an initial delay.
     *
     * @param config the data source configuration
     * @param initialDelaySeconds the initial delay before the first health check in seconds
     * @param intervalSeconds the interval between health checks in seconds
     */
    public void scheduleHealthCheck(
                                    final DataSourceConfig config, final long initialDelaySeconds, final long intervalSeconds) {
        String dataSourceName = config.getName();
        if (scheduledTasks.containsKey(dataSourceName)) {
            log.warn("Health check already scheduled for data source: {}", dataSourceName);
            return;
        }
        log.info(
                "Scheduling health check for '{}' every {} seconds (initial delay: {}s)",
                dataSourceName,
                intervalSeconds,
                initialDelaySeconds);
        java.util.concurrent.ScheduledFuture<?> future =
                scheduler.scheduleAtFixedRate(
                        () -> performHealthCheck(config),
                        initialDelaySeconds,
                        intervalSeconds,
                        TimeUnit.SECONDS);
        scheduledTasks.put(dataSourceName, future);
    }

    /**
     * Cancels the scheduled health check for the given data source.
     *
     * @param dataSourceName the name of the data source
     */
    public void cancelHealthCheck(final String dataSourceName) {
        java.util.concurrent.ScheduledFuture<?> future = scheduledTasks.remove(dataSourceName);
        if (future != null) {
            future.cancel(false);
            log.info("Cancelled health check for data source: {}", dataSourceName);
        }
    }

    /**
     * Performs a health check on the given data source.
     *
     * @param config the data source configuration
     * @return the health check result
     */
    public HealthCheckResult performHealthCheck(final DataSourceConfig config) {
        String dataSourceName = config.getName();
        HealthCheckResult result = healthChecker.check(config);
        lastResults.put(dataSourceName, result);
        if (healthCheckListener != null) {
            healthCheckListener.accept(result);
        }
        log.debug(
                "Health check for '{}': {} ({}ms)",
                dataSourceName,
                result.getStatus(),
                result.getResponseTimeMs());
        return result;
    }

    /**
     * Gets the last health check result for the given data source.
     *
     * @param dataSourceName the name of the data source
     * @return the last health check result, or null if none exists
     */
    public HealthCheckResult getLastResult(final String dataSourceName) {
        return lastResults.get(dataSourceName);
    }

    /**
     * Gets all last health check results.
     *
     * @return a map of data source names to their last health check results
     */
    public Map<String, HealthCheckResult> getAllLastResults() {
        return new ConcurrentHashMap<>(lastResults);
    }

    /**
     * Sets the listener for health check results.
     *
     * @param listener the listener to receive health check results
     */
    public void setHealthCheckListener(final Consumer<HealthCheckResult> listener) {
        this.healthCheckListener = listener;
    }

    /** Shuts down the health check scheduler. */
    public void shutdown() {
        log.info("Shutting down health check scheduler");
        scheduledTasks.values().forEach(future -> future.cancel(false));
        scheduledTasks.clear();
        scheduler.shutdown();
        boolean terminated = false;
        try {
            terminated = scheduler.awaitTermination(10, TimeUnit.SECONDS);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        if (!terminated) {
            scheduler.shutdownNow();
        }
        if (healthChecker instanceof DataSourceHealthChecker) {
            ((DataSourceHealthChecker) healthChecker).clearCache();
        }
    }

    /**
     * Checks if a health check is scheduled for the given data source.
     *
     * @param dataSourceName the name of the data source
     * @return true if a health check is scheduled, false otherwise
     */
    public boolean isScheduled(final String dataSourceName) {
        java.util.concurrent.ScheduledFuture<?> future = scheduledTasks.get(dataSourceName);
        return future != null && !future.isCancelled() && !future.isDone();
    }

    /**
     * Gets the count of active scheduled health checks.
     *
     * @return the count of active scheduled health checks
     */
    public int getScheduledCount() {
        return (int) scheduledTasks.values().stream().filter(f -> !f.isCancelled() && !f.isDone()).count();
    }
}
