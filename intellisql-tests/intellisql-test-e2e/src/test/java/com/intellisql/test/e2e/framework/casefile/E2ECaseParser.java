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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Parser for JDBC E2E SQL case files. */
public final class E2ECaseParser {

    private final String defaultModel;

    /**
     * Creates a parser with the default model name.
     *
     * @param defaultModel the default model name
     */
    public E2ECaseParser(final String defaultModel) {
        this.defaultModel = defaultModel;
    }

    /**
     * Parses a SQL case file.
     *
     * @param casePath the case file path
     * @return parsed test case
     * @throws IllegalStateException if the case file cannot be read
     */
    public E2ETestCase parse(final Path casePath) {
        try {
            return parseLines(casePath, Files.readAllLines(casePath, StandardCharsets.UTF_8));
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to read case file: " + casePath, ex);
        }
    }

    private E2ETestCase parseLines(final Path casePath, final List<String> lines) {
        Map<String, String> caseValues = new HashMap<>();
        Map<String, String> assertValues = new HashMap<>();
        Map<String, String> statementValues = new HashMap<>();
        String source = "intellisql";
        List<String> sqlLines = new ArrayList<>();
        boolean sqlStarted = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!sqlStarted && trimmed.startsWith("-- @")) {
                String directive = trimmed.substring(4).trim();
                String name = parseDirectiveName(directive);
                Map<String, String> values = parseDirectiveValues(directive);
                if ("case".equals(name)) {
                    caseValues.putAll(values);
                } else if ("source".equals(name)) {
                    source = getSource(values, source);
                } else if ("assert".equals(name)) {
                    assertValues.putAll(values);
                } else if ("statement".equals(name)) {
                    statementValues.putAll(values);
                } else if ("expected-sql".equals(name)) {
                    assertValues.put("expectedSql", directive.substring(name.length()).trim());
                }
            } else {
                sqlStarted = true;
                sqlLines.add(line);
            }
        }
        return buildTestCase(casePath, caseValues, assertValues, statementValues, source, sqlLines);
    }

    private String getSource(final Map<String, String> values, final String defaultSource) {
        if (values.containsKey("value")) {
            return values.get("value");
        }
        return values.containsKey("type") ? values.get("type") : defaultSource;
    }

    private E2ETestCase buildTestCase(
                                      final Path casePath,
                                      final Map<String, String> caseValues,
                                      final Map<String, String> assertValues,
                                      final Map<String, String> statementValues,
                                      final String source,
                                      final List<String> sqlLines) {
        String id = caseValues.get("id");
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Missing @case id in " + casePath);
        }
        if (assertValues.isEmpty()) {
            throw new IllegalArgumentException("Missing @assert in " + casePath);
        }
        String sql = joinSql(sqlLines);
        if (sql.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing SQL body in " + casePath);
        }
        return E2ETestCase.builder()
                .id(id)
                .model(caseValues.containsKey("model") ? caseValues.get("model") : defaultModel)
                .source(source)
                .assertion(buildAssertion(assertValues))
                .statement(buildStatement(statementValues))
                .sql(sql)
                .resourcePath(casePath)
                .build();
    }

    private AssertionSpec buildAssertion(final Map<String, String> values) {
        return AssertionSpec.builder()
                .type(values.get("type"))
                .target(values.get("target"))
                .expected(values.get("expected"))
                .expectedSql(getExpectedSql(values))
                .order(values.containsKey("order") ? values.get("order") : "auto")
                .build();
    }

    private String getExpectedSql(final Map<String, String> values) {
        return values.containsKey("expectedSql") ? values.get("expectedSql") : values.get("expected-sql");
    }

    private StatementSpec buildStatement(final Map<String, String> values) {
        return StatementSpec.builder()
                .mode(values.containsKey("mode") ? values.get("mode") : "statement")
                .build();
    }

    private String parseDirectiveName(final String directive) {
        int index = directive.indexOf(' ');
        return index < 0 ? directive : directive.substring(0, index);
    }

    private Map<String, String> parseDirectiveValues(final String directive) {
        List<String> tokens = tokenize(directive);
        Map<String, String> result = new HashMap<>(Math.max(tokens.size(), 1));
        for (int i = 1; i < tokens.size(); i++) {
            String token = tokens.get(i);
            int separator = token.indexOf('=');
            if (separator < 0 && i == 1) {
                result.put("type", token);
            } else if (separator < 0) {
                result.put("value", token);
            } else {
                result.put(token.substring(0, separator), stripQuotes(token.substring(separator + 1)));
            }
        }
        return result;
    }

    private List<String> tokenize(final String directive) {
        List<String> result = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < directive.length(); i++) {
            char each = directive.charAt(i);
            if ('"' == each) {
                quoted = !quoted;
                token.append(each);
            } else if (Character.isWhitespace(each) && !quoted) {
                addToken(result, token);
            } else {
                token.append(each);
            }
        }
        addToken(result, token);
        return result;
    }

    private void addToken(final List<String> tokens, final StringBuilder token) {
        if (token.length() > 0) {
            tokens.add(token.toString());
            token.setLength(0);
        }
    }

    private String stripQuotes(final String value) {
        return value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")
                ? value.substring(1, value.length() - 1)
                : value;
    }

    private String joinSql(final List<String> sqlLines) {
        StringBuilder result = new StringBuilder();
        for (String each : sqlLines) {
            result.append(each).append(System.lineSeparator());
        }
        return result.toString().trim();
    }
}
