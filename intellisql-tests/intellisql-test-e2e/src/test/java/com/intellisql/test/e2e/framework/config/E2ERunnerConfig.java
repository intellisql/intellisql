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

package com.intellisql.test.e2e.framework.config;

import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/** Configuration model for the JDBC E2E runner. */
@Getter
@Builder
@ToString
public final class E2ERunnerConfig {

    @Builder.Default
    private final ExecutionConfig execution = ExecutionConfig.builder().build();

    @Builder.Default
    private final Map<String, ContainerConfig> containers = defaultContainers();

    @Builder.Default
    private final AssertionConfig assertion = AssertionConfig.builder().build();

    private static Map<String, ContainerConfig> defaultContainers() {
        Map<String, ContainerConfig> result = new HashMap<>(1);
        result.put("postgresql", ContainerConfig.builder().build());
        return result;
    }

    /** Execution configuration. */
    @Getter
    @Builder
    @ToString
    public static final class ExecutionConfig {

        @Builder.Default
        private final String mode = "docker";

        @Builder.Default
        private final int serverPort = 0;

        @Builder.Default
        private final String jdbcDatabase = "intellisql";

        @Builder.Default
        private final String caseRoot = "e2e/cases";

        @Builder.Default
        private final String defaultModel = "basic";

        @Builder.Default
        private final int defaultFetchSize = 1000;

        @Builder.Default
        private final int timeoutSeconds = 60;
    }

    /** Container configuration. */
    @Getter
    @Builder
    @ToString
    public static final class ContainerConfig {

        @Builder.Default
        private final boolean enabled = true;

        @Builder.Default
        private final String image = "postgres:15-alpine";

        @Builder.Default
        private final String database = "testdb";

        @Builder.Default
        private final String username = "testuser";

        @Builder.Default
        private final String password = "testpass";
    }

    /** Assertion configuration. */
    @Getter
    @Builder
    @ToString
    public static final class AssertionConfig {

        @Builder.Default
        private final String orderMode = "auto";

        @Builder.Default
        private final String nullToken = "<NULL>";
    }
}
