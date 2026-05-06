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

package com.intellisql.test.e2e.framework.assertion;

import java.util.Locale;

/** Analyzer for SQL order-sensitive assertion mode. */
public final class SqlOrderAnalyzer {

    /**
     * Resolves the effective order comparison mode.
     *
     * @param sql the SQL statement
     * @param configuredOrderMode the configured order mode
     * @return effective order mode
     */
    public OrderMode analyze(final String sql, final String configuredOrderMode) {
        String mode = configuredOrderMode == null ? "auto" : configuredOrderMode.toLowerCase(Locale.ROOT);
        if ("strict".equals(mode)) {
            return OrderMode.STRICT;
        }
        if ("any".equals(mode)) {
            return OrderMode.ANY;
        }
        return hasTopLevelOrderBy(sql) ? OrderMode.STRICT : OrderMode.ANY;
    }

    private boolean hasTopLevelOrderBy(final String sql) {
        String normalized = sql.toLowerCase(Locale.ROOT);
        int depth = 0;
        boolean quoted = false;
        for (int i = 0; i < normalized.length(); i++) {
            char each = normalized.charAt(i);
            if ('\'' == each) {
                quoted = !quoted;
            } else if (!quoted && '(' == each) {
                depth++;
            } else if (!quoted && ')' == each && depth > 0) {
                depth--;
            } else if (!quoted && depth == 0 && startsWithWord(normalized, i, "order")) {
                int next = skipWhitespace(normalized, i + "order".length());
                if (startsWithWord(normalized, next, "by")) {
                    return true;
                }
            }
        }
        return false;
    }

    private int skipWhitespace(final String value, final int start) {
        int result = start;
        while (result < value.length() && Character.isWhitespace(value.charAt(result))) {
            result++;
        }
        return result;
    }

    private boolean startsWithWord(final String value, final int start, final String word) {
        int end = start + word.length();
        if (start < 0 || end > value.length() || !value.startsWith(word, start)) {
            return false;
        }
        boolean leftBoundary = start == 0 || !Character.isLetterOrDigit(value.charAt(start - 1));
        boolean rightBoundary = end == value.length() || !Character.isLetterOrDigit(value.charAt(end));
        return leftBoundary && rightBoundary;
    }
}
