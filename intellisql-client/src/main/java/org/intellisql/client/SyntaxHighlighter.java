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

package org.intellisql.client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/** Provides syntax highlighting for SQL in the CLI. */
@Slf4j
public class SyntaxHighlighter {

    // ANSI escape sequences required for terminal syntax highlighting
    // CHECKSTYLE:OFF:AvoidEscapedUnicodeCharacters
    /** ANSI reset code. */
    private static final String RESET = "\u001B[0m";

    /** ANSI color for SQL keywords (bold blue). */
    private static final String KEYWORD_COLOR = "\u001B[1;34m";

    /** ANSI color for string literals (green). */
    private static final String STRING_COLOR = "\u001B[0;32m";

    /** ANSI color for numbers (yellow). */
    private static final String NUMBER_COLOR = "\u001B[0;33m";

    /** ANSI color for comments (dark gray). */
    private static final String COMMENT_COLOR = "\u001B[0;90m";

    /** ANSI color for function names (magenta). */
    private static final String FUNCTION_COLOR = "\u001B[0;35m";
    // CHECKSTYLE:ON:AvoidEscapedUnicodeCharacters

    private static final Set<String> KEYWORDS =
            new HashSet<>(
                    Arrays.asList(
                            "SELECT",
                            "FROM",
                            "WHERE",
                            "AND",
                            "OR",
                            "NOT",
                            "IN",
                            "IS",
                            "NULL",
                            "INSERT",
                            "UPDATE",
                            "DELETE",
                            "CREATE",
                            "DROP",
                            "ALTER",
                            "TABLE",
                            "INDEX",
                            "VIEW",
                            "JOIN",
                            "LEFT",
                            "RIGHT",
                            "INNER",
                            "OUTER",
                            "ON",
                            "GROUP",
                            "BY",
                            "ORDER",
                            "HAVING",
                            "LIMIT",
                            "OFFSET",
                            "UNION",
                            "DISTINCT",
                            "AS",
                            "ASC",
                            "DESC",
                            "BETWEEN",
                            "LIKE",
                            "EXISTS",
                            "CASE",
                            "WHEN",
                            "THEN",
                            "ELSE",
                            "END",
                            "CAST",
                            "CONVERT",
                            "PRIMARY",
                            "KEY",
                            "FOREIGN",
                            "REFERENCES",
                            "CONSTRAINT",
                            "DEFAULT",
                            "AUTO_INCREMENT",
                            "IDENTITY",
                            "VARCHAR",
                            "INT",
                            "INTEGER",
                            "BIGINT",
                            "SMALLINT",
                            "DECIMAL",
                            "NUMERIC",
                            "FLOAT",
                            "DOUBLE",
                            "CHAR",
                            "TEXT",
                            "DATE",
                            "TIME",
                            "TIMESTAMP",
                            "BOOLEAN",
                            "TRUE",
                            "FALSE",
                            "WITH",
                            "RECURSIVE",
                            "VALUES",
                            "INTO",
                            "SET"));

    private static final Pattern KEYWORD_PATTERN =
            Pattern.compile("\\b([A-Z]+)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern STRING_PATTERN = Pattern.compile("'[^']*'");

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");

    private static final Pattern COMMENT_PATTERN =
            Pattern.compile("--[^\n]*|/\\*.*?\\*/", Pattern.DOTALL);

    private static final Pattern FUNCTION_PATTERN =
            Pattern.compile("\\b([A-Z_]+)\\s*\\(", Pattern.CASE_INSENSITIVE);

    private final boolean enabled;

    /** Creates a SyntaxHighlighter with highlighting enabled. */
    public SyntaxHighlighter() {
        this(true);
    }

    /**
     * Creates a SyntaxHighlighter with the specified setting.
     *
     * @param enabled whether syntax highlighting is enabled
     */
    public SyntaxHighlighter(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Highlights SQL syntax with ANSI color codes.
     *
     * @param sql the SQL string to highlight
     * @return highlighted string
     */
    public String highlight(final String sql) {
        if (!enabled || sql == null || sql.isEmpty()) {
            return sql;
        }
        String result = sql;
        result = highlightComments(result);
        result = highlightStrings(result);
        result = highlightKeywords(result);
        result = highlightFunctions(result);
        result = highlightNumbers(result);
        return result;
    }

    /**
     * Highlights comments in the SQL string.
     *
     * @param sql the SQL string
     * @return SQL string with highlighted comments
     */
    private String highlightComments(final String sql) {
        Matcher matcher = COMMENT_PATTERN.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, COMMENT_COLOR + matcher.group() + RESET);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Highlights string literals in the SQL string.
     *
     * @param sql the SQL string
     * @return SQL string with highlighted strings
     */
    private String highlightStrings(final String sql) {
        Matcher matcher = STRING_PATTERN.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, STRING_COLOR + matcher.group() + RESET);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Highlights keywords in the SQL string.
     *
     * @param sql the SQL string
     * @return SQL string with highlighted keywords
     */
    private String highlightKeywords(final String sql) {
        Matcher matcher = KEYWORD_PATTERN.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String word = matcher.group(1).toUpperCase();
            if (KEYWORDS.contains(word)) {
                matcher.appendReplacement(sb, KEYWORD_COLOR + matcher.group() + RESET);
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Highlights function names in the SQL string.
     *
     * @param sql the SQL string
     * @return SQL string with highlighted functions
     */
    private String highlightFunctions(final String sql) {
        Matcher matcher = FUNCTION_PATTERN.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String func = matcher.group(1).toUpperCase();
            if (!KEYWORDS.contains(func)) {
                matcher.appendReplacement(sb, FUNCTION_COLOR + matcher.group() + RESET);
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Highlights numbers in the SQL string.
     *
     * @param sql the SQL string
     * @return SQL string with highlighted numbers
     */
    private String highlightNumbers(final String sql) {
        Matcher matcher = NUMBER_PATTERN.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, NUMBER_COLOR + matcher.group() + RESET);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Strips ANSI color codes from a string.
     *
     * @param text the text with ANSI codes
     * @return text without ANSI codes
     */
    public String stripAnsi(final String text) {
        if (text == null) {
            return null;
        }
        // ANSI escape sequence pattern
        // CHECKSTYLE:OFF:AvoidEscapedUnicodeCharacters
        return text.replaceAll("\u001B\\[[;\\d]*m", "");
        // CHECKSTYLE:ON:AvoidEscapedUnicodeCharacters
    }
}
