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

package org.intellisql.kernel.config;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for substituting environment variables in configuration strings. Supports
 * ${VAR_NAME} and ${VAR_NAME:default_value} syntax.
 */
public final class EnvironmentVariableSubstitutor {

    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?\\}");

    private final Map<String, String> environment;

    /** Creates a substitutor using system environment variables. */
    public EnvironmentVariableSubstitutor() {
        this(System.getenv());
    }

    /**
     * Creates a substitutor with a custom environment map (for testing).
     *
     * @param environment the environment variable map
     */
    public EnvironmentVariableSubstitutor(final Map<String, String> environment) {
        this.environment = environment;
    }

    /**
     * Substitutes environment variables in the given text.
     *
     * @param text the text containing ${VAR_NAME} or ${VAR_NAME:default} patterns
     * @return the text with all variables substituted
     * @throws IllegalArgumentException if a variable is not found and no default is provided
     */
    public String substitute(final String text) {
        if (text == null) {
            return null;
        }
        final StringBuffer result = new StringBuffer();
        final Matcher matcher = ENV_PATTERN.matcher(text);
        while (matcher.find()) {
            final String varName = matcher.group(1);
            final String defaultValue = matcher.group(2);
            final String envValue = environment.get(varName);
            if (envValue != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(envValue));
            } else if (defaultValue != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(defaultValue));
            } else {
                throw new IllegalArgumentException(
                        "Environment variable '" + varName + "' not found and no default value provided");
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
