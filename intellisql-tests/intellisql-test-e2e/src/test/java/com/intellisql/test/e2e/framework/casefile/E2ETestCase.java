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

package com.intellisql.test.e2e.framework.casefile;

import java.nio.file.Path;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/** Parsed JDBC E2E SQL test case. */
@Getter
@Builder
@ToString
public final class E2ETestCase {

    private final String id;

    private final String model;

    private final String source;

    private final AssertionSpec assertion;

    @ToString.Exclude
    private final StatementSpec statement;

    private final String sql;

    private final Path resourcePath;
}
