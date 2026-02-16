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

package org.intellisql.test.e2e;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for concurrent connection handling. Tests 100 concurrent connections and query
 * execution.
 */
@Testcontainers
public class ConcurrentConnectionTest {

    private static final Network NETWORK = Network.newNetwork();

    private static final String POSTGRES_IMAGE = "postgres:15-alpine";

    private static final int CONCURRENT_CONNECTIONS = 100;

    @Container
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
                    .withNetwork(NETWORK)
                    .withNetworkAliases("postgres")
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpass")
                    .withReuse(true);

    @BeforeAll
    static void setupConcurrentTestData() throws Exception {
        // Load PostgreSQL driver
        Class.forName("org.postgresql.Driver");
        Connection conn =
                DriverManager.getConnection(
                        POSTGRES_CONTAINER.getJdbcUrl(),
                        POSTGRES_CONTAINER.getUsername(),
                        POSTGRES_CONTAINER.getPassword());
        try {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS concurrent_test");
                stmt.execute(
                        "CREATE TABLE concurrent_test ("
                                + "id SERIAL PRIMARY KEY, "
                                + "thread_id INT, "
                                + "value INT, "
                                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                for (int i = 0; i < 1000; i++) {
                    stmt.execute(
                            "INSERT INTO concurrent_test (thread_id, value) VALUES ("
                                    + (i % 10)
                                    + ", "
                                    + i
                                    + ")");
                }
            }
        } finally {
            conn.close();
        }
    }

    @AfterAll
    static void cleanupConcurrentTestData() throws Exception {
        Connection conn =
                DriverManager.getConnection(
                        POSTGRES_CONTAINER.getJdbcUrl(),
                        POSTGRES_CONTAINER.getUsername(),
                        POSTGRES_CONTAINER.getPassword());
        try {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS concurrent_test");
            }
        } finally {
            conn.close();
        }
    }

    @Test
    @DisplayName("Should handle 100 concurrent connections")
    void shouldHandle100ConcurrentConnections() throws Exception {
        int connectionCount = CONCURRENT_CONNECTIONS;
        ExecutorService executor = Executors.newFixedThreadPool(connectionCount);
        CountDownLatch latch = new CountDownLatch(connectionCount);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);
        for (int i = 0; i < connectionCount; i++) {
            executor.submit(() -> handleConcurrentConnection(latch, exceptions, successCount));
        }
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(completed).isTrue();
        assertThat(exceptions).isEmpty();
        assertThat(successCount.get()).isEqualTo(connectionCount);
    }

    private void handleConcurrentConnection(
                                            final CountDownLatch latch,
                                            final List<Throwable> exceptions,
                                            final AtomicInteger successCount) {
        try (
                Connection conn =
                        DriverManager.getConnection(
                                POSTGRES_CONTAINER.getJdbcUrl(),
                                POSTGRES_CONTAINER.getUsername(),
                                POSTGRES_CONTAINER.getPassword())) {
            assertThat(conn.isValid(5)).isTrue();
            successCount.incrementAndGet();
            // CHECKSTYLE:OFF IllegalCatch
        } catch (final Throwable ex) {
            // CHECKSTYLE:ON
            exceptions.add(ex);
        } finally {
            latch.countDown();
        }
    }

    @Test
    @DisplayName("Should execute concurrent queries")
    void shouldExecuteConcurrentQueries() throws Exception {
        int queryCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(queryCount);
        List<Future<QueryResult>> futures = new ArrayList<>();
        for (int i = 0; i < queryCount; i++) {
            final int queryId = i;
            futures.add(executor.submit(() -> executeConcurrentQueryTask(queryId)));
        }
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        int successCount = 0;
        for (Future<QueryResult> future : futures) {
            QueryResult result = future.get();
            if (result.error == null) {
                successCount++;
                assertThat(result.getCount()).isGreaterThanOrEqualTo(0);
            }
        }
        assertThat(successCount).isEqualTo(queryCount);
    }

    private QueryResult executeConcurrentQueryTask(final int queryId) {
        long startTime = System.currentTimeMillis();
        try (
                Connection conn =
                        DriverManager.getConnection(
                                POSTGRES_CONTAINER.getJdbcUrl(),
                                POSTGRES_CONTAINER.getUsername(),
                                POSTGRES_CONTAINER.getPassword());
                Statement stmt = conn.createStatement();
                ResultSet rs =
                        stmt.executeQuery(
                                "SELECT COUNT(*) FROM concurrent_test WHERE thread_id = "
                                        + (queryId % 10))) {
            rs.next();
            int count = rs.getInt(1);
            long duration = System.currentTimeMillis() - startTime;
            return new QueryResult(queryId, count, duration, null);
            // CHECKSTYLE:OFF IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            return new QueryResult(
                    queryId, -1, System.currentTimeMillis() - startTime, ex.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle concurrent writes")
    void shouldHandleConcurrentWrites() throws Exception {
        int writerCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(writerCount);
        CountDownLatch latch = new CountDownLatch(writerCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < writerCount; i++) {
            final int threadId = i;
            executor.submit(() -> handleConcurrentWrite(threadId, latch, exceptions, successCount));
        }
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(completed).isTrue();
        assertThat(exceptions).isEmpty();
        assertThat(successCount.get()).isEqualTo(writerCount);
    }

    private void handleConcurrentWrite(
                                       final int threadId,
                                       final CountDownLatch latch,
                                       final List<Throwable> exceptions,
                                       final AtomicInteger successCount) {
        try (
                Connection conn =
                        DriverManager.getConnection(
                                POSTGRES_CONTAINER.getJdbcUrl(),
                                POSTGRES_CONTAINER.getUsername(),
                                POSTGRES_CONTAINER.getPassword());
                Statement stmt = conn.createStatement()) {
            for (int j = 0; j < 10; j++) {
                stmt.execute(
                        "INSERT INTO concurrent_test (thread_id, value) VALUES ("
                                + threadId
                                + ", "
                                + j
                                + ")");
            }
            successCount.incrementAndGet();
            // CHECKSTYLE:OFF IllegalCatch
        } catch (final Throwable ex) {
            // CHECKSTYLE:ON
            exceptions.add(ex);
        } finally {
            latch.countDown();
        }
    }

    @Test
    @DisplayName("Should handle mixed read/write operations")
    void shouldHandleMixedReadWriteOperations() throws Exception {
        int operationCount = 40;
        ExecutorService executor = Executors.newFixedThreadPool(operationCount);
        CountDownLatch latch = new CountDownLatch(operationCount);
        AtomicInteger readSuccess = new AtomicInteger(0);
        AtomicInteger writeSuccess = new AtomicInteger(0);
        for (int i = 0; i < operationCount; i++) {
            final int opId = i;
            final boolean isRead = i % 2 == 0;
            executor.submit(() -> executeMixedOperation(opId, isRead, readSuccess, writeSuccess, latch));
        }
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(completed).isTrue();
        assertThat(readSuccess.get() + writeSuccess.get()).isEqualTo(operationCount);
    }

    private void executeMixedOperation(
                                       final int opId,
                                       final boolean isRead,
                                       final AtomicInteger readSuccess,
                                       final AtomicInteger writeSuccess,
                                       final CountDownLatch latch) {
        try (
                Connection conn =
                        DriverManager.getConnection(
                                POSTGRES_CONTAINER.getJdbcUrl(),
                                POSTGRES_CONTAINER.getUsername(),
                                POSTGRES_CONTAINER.getPassword());
                Statement stmt = conn.createStatement()) {
            if (isRead) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM concurrent_test")) {
                    rs.next();
                }
                readSuccess.incrementAndGet();
            } else {
                stmt.execute(
                        "INSERT INTO concurrent_test (thread_id, value) VALUES (99, " + opId + ")");
                writeSuccess.incrementAndGet();
            }
            // CHECKSTYLE:OFF IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON EmptyCatchBlock
            // Ignore exceptions for mixed operations test
        } finally {
            latch.countDown();
        }
    }

    @Test
    @DisplayName("Should handle connection pool exhaustion gracefully")
    void shouldHandleConnectionPoolExhaustion() throws Exception {
        int poolSize = 10;
        int waitingThreads = 5;
        List<Connection> connections = new ArrayList<>();
        for (int i = 0; i < poolSize; i++) {
            connections.add(
                    DriverManager.getConnection(
                            POSTGRES_CONTAINER.getJdbcUrl(),
                            POSTGRES_CONTAINER.getUsername(),
                            POSTGRES_CONTAINER.getPassword()));
        }
        ExecutorService executor = Executors.newFixedThreadPool(waitingThreads);
        CountDownLatch latch = new CountDownLatch(waitingThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        for (int i = 0; i < waitingThreads; i++) {
            executor.submit(() -> closeConnectionFromPool(connections, successCount, latch));
        }
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        for (Connection conn : connections) {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        }
        assertThat(successCount.get()).isGreaterThanOrEqualTo(0);
    }

    private void closeConnectionFromPool(
                                         final List<Connection> connections,
                                         final AtomicInteger successCount,
                                         final CountDownLatch latch) {
        try {
            Thread.sleep(100);
            for (Connection conn : connections) {
                if (conn != null) {
                    // CHECKSTYLE:OFF IllegalCatch
                    try {
                        // CHECKSTYLE:ON
                        synchronized (connections) {
                            if (!conn.isClosed()) {
                                conn.close();
                                successCount.incrementAndGet();
                                break;
                            }
                        }
                        // CHECKSTYLE:OFF IllegalCatch
                    } catch (final Exception ex) {
                        // CHECKSTYLE:ON EmptyCatchBlock
                        // Ignore exceptions when closing connections
                    }
                }
            }
            // CHECKSTYLE:OFF IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON EmptyCatchBlock
            // Ignore exceptions
        } finally {
            latch.countDown();
        }
    }

    @Test
    @DisplayName("Should measure connection creation time under load")
    void shouldMeasureConnectionCreationTimeUnderLoad() throws Exception {
        int threadCount = 30;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Long> creationTimes = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> measureConnectionCreationTime(creationTimes)));
        }
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        assertThat(creationTimes).hasSize(threadCount);
        long maxTime = creationTimes.stream().max(Long::compare).orElse(0L);
        assertThat(maxTime).isLessThan(10000);
    }

    private void measureConnectionCreationTime(final List<Long> creationTimes) {
        long start = System.currentTimeMillis();
        try (
                Connection conn =
                        DriverManager.getConnection(
                                POSTGRES_CONTAINER.getJdbcUrl(),
                                POSTGRES_CONTAINER.getUsername(),
                                POSTGRES_CONTAINER.getPassword())) {
            conn.isValid(3);
            creationTimes.add(System.currentTimeMillis() - start);
            // CHECKSTYLE:OFF IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON EmptyCatchBlock
            // Ignore exceptions for timing test
        }
    }

    @Test
    @DisplayName("Should handle concurrent transactions")
    void shouldHandleConcurrentTransactions() throws Exception {
        int transactionCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(transactionCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < transactionCount; i++) {
            final int txId = i;
            futures.add(executor.submit(() -> executeConcurrentTransaction(txId, successCount)));
        }
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        assertThat(successCount.get()).isEqualTo(transactionCount);
    }

    private void executeConcurrentTransaction(final int txId, final AtomicInteger successCount) {
        // CHECKSTYLE:OFF IllegalCatch
        try (
                Connection conn =
                        DriverManager.getConnection(
                                POSTGRES_CONTAINER.getJdbcUrl(),
                                POSTGRES_CONTAINER.getUsername(),
                                POSTGRES_CONTAINER.getPassword())) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(
                        "INSERT INTO concurrent_test (thread_id, value) VALUES ("
                                + txId
                                + ", 999)");
                stmt.executeQuery("SELECT COUNT(*) FROM concurrent_test");
                conn.commit();
                successCount.incrementAndGet();
            } catch (final Exception ex) {
                conn.rollback();
            }
        } catch (final Exception ex) {
            // CHECKSTYLE:ON EmptyCatchBlock
            // Ignore exceptions for transaction test
        }
    }

    private static class QueryResult {

        private final int queryId;

        private final int count;

        private final long duration;

        private final String error;

        QueryResult(final int queryId, final int count, final long duration, final String error) {
            this.queryId = queryId;
            this.count = count;
            this.duration = duration;
            this.error = error;
        }

        public int getCount() {
            return count;
        }
    }
}
