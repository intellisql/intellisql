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

package com.intellisql.test.e2e.jdbc;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

import com.intellisql.test.e2e.framework.casefile.E2ETestCase;
import com.intellisql.test.e2e.framework.environment.E2EEnvironment;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** JDBC smoke E2E tests through IntelliSQL server and driver. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class JdbcSmokeE2ETest {

    private E2EEnvironment environment;

    private JdbcE2ECaseRunner caseRunner;

    @BeforeAll
    void setUp(@TempDir final Path tempDirectory) {
        environment = new E2EEnvironment();
        environment.start("basic", tempDirectory);
        caseRunner = new JdbcE2ECaseRunner(environment);
    }

    @AfterAll
    void tearDown() {
        if (environment != null) {
            environment.close();
        }
    }

    @Test
    void assertConnectToIntelliSqlServer() throws Exception {
        try (Connection connection = environment.createIntelliSqlConnection()) {
            assertFalse(connection.isClosed());
            assertNotNull(connection.getMetaData());
        }
    }

    @Test
    void assertShowTables() throws Exception {
        List<E2ETestCase> testCases = caseRunner.scan("e2e/cases/smoke");
        caseRunner.assertCase(caseRunner.find(testCases, "smoke-show-tables"));
    }
}
