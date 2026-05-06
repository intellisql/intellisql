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
import java.util.List;

import com.intellisql.test.e2e.framework.casefile.E2ETestCase;
import com.intellisql.test.e2e.framework.environment.E2EEnvironment;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

/** JDBC DML E2E tests using state assertions against PostgreSQL baseline. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class JdbcDmlE2ETest {

    private E2EEnvironment environment;

    private JdbcE2ECaseRunner caseRunner;

    private List<E2ETestCase> testCases;

    @BeforeAll
    void setUp(@TempDir final Path tempDirectory) {
        environment = new E2EEnvironment();
        environment.start("basic", tempDirectory);
        caseRunner = new JdbcE2ECaseRunner(environment);
        testCases = caseRunner.scan("e2e/cases/dml");
    }

    @AfterAll
    void tearDown() {
        if (environment != null) {
            environment.close();
        }
    }

    @Test
    void assertExecuteCustomerUpdate() throws Exception {
        caseRunner.assertCase(caseRunner.find(testCases, "dml-customer-update"));
    }
}
