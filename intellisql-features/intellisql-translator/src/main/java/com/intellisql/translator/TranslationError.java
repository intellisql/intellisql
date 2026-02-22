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

package com.intellisql.translator;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Represents an error that occurred during SQL translation. */
@Getter
@Builder
@RequiredArgsConstructor
public class TranslationError {

    private final String code;

    private final String message;

    private final int line;

    private final int column;

    private final int position;

    private final String context;

    private final List<String> suggestions;

    /**
     * Creates a new translation error.
     *
     * @param code the error code
     * @param message the error message
     * @return the translation error
     */
    public static TranslationError of(final String code, final String message) {
        return TranslationError.builder()
                .code(code)
                .message(message)
                .line(-1)
                .column(-1)
                .position(-1)
                .context(null)
                .suggestions(Collections.emptyList())
                .build();
    }

    /**
     * Creates a new translation error with position information.
     *
     * @param code the error code
     * @param message the error message
     * @param position the position in the source SQL
     * @return the translation error
     */
    public static TranslationError of(final String code, final String message, final int position) {
        return TranslationError.builder()
                .code(code)
                .message(message)
                .line(-1)
                .column(-1)
                .position(position)
                .context(null)
                .suggestions(Collections.emptyList())
                .build();
    }

    /**
     * Creates a new translation error with full context information.
     *
     * @param code the error code
     * @param message the error message
     * @param line the line number (1-based)
     * @param column the column number (1-based)
     * @param position the character position in the source SQL
     * @param context the context snippet around the error
     * @param suggestions the list of fix suggestions
     * @return the translation error
     */
    public static TranslationError of(final String code, final String message, final int line,
                                      final int column, final int position, final String context, final List<String> suggestions) {
        return TranslationError.builder()
                .code(code)
                .message(message)
                .line(line)
                .column(column)
                .position(position)
                .context(context)
                .suggestions(suggestions != null ? suggestions : Collections.emptyList())
                .build();
    }

    /**
     * Creates a new syntax error.
     *
     * @param message the error message
     * @return the translation error
     */
    public static TranslationError syntaxError(final String message) {
        return of("SYNTAX_ERROR", message);
    }

    /**
     * Creates a new syntax error with position and context.
     *
     * @param message the error message
     * @param line the line number
     * @param column the column number
     * @param sourceSql the source SQL for context extraction
     * @return the translation error
     */
    public static TranslationError syntaxError(final String message, final int line, final int column,
                                               final String sourceSql) {
        String context = extractContext(sourceSql, line, column);
        return of("SYNTAX_ERROR", message, line, column, -1, context, Collections.emptyList());
    }

    /**
     * Creates a new unsupported feature error.
     *
     * @param feature the unsupported feature
     * @return the translation error
     */
    public static TranslationError unsupportedFeature(final String feature) {
        return of("UNSUPPORTED_FEATURE", "Feature not supported: " + feature);
    }

    /**
     * Creates a new unsupported feature error with suggestion.
     *
     * @param feature the unsupported feature
     * @param suggestion the suggested alternative
     * @return the translation error
     */
    public static TranslationError unsupportedFeature(final String feature, final String suggestion) {
        return TranslationError.builder()
                .code("UNSUPPORTED_FEATURE")
                .message("Feature not supported: " + feature)
                .line(-1)
                .column(-1)
                .position(-1)
                .context(null)
                .suggestions(Collections.singletonList(suggestion))
                .build();
    }

    /**
     * Creates a new dialect mismatch error.
     *
     * @param sourceDialect the source dialect
     * @param targetDialect the target dialect
     * @return the translation error
     */
    public static TranslationError dialectMismatch(final String sourceDialect, final String targetDialect) {
        return of(
                "DIALECT_MISMATCH", "Cannot translate from " + sourceDialect + " to " + targetDialect);
    }

    /**
     * Checks if the error has position information.
     *
     * @return true if the error has position information
     */
    public boolean hasPosition() {
        return position >= 0 || line >= 0 && column >= 0;
    }

    /**
     * Checks if the error has suggestions.
     *
     * @return true if the error has suggestions
     */
    public boolean hasSuggestions() {
        return suggestions != null && !suggestions.isEmpty();
    }

    /**
     * Checks if the error has context information.
     *
     * @return true if the error has context information
     */
    public boolean hasContext() {
        return context != null && !context.isEmpty();
    }

    /**
     * Generates a formatted error message with position and context highlighting.
     *
     * @return the formatted error message
     */
    public String toFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(code).append("] ").append(message);
        if (hasPosition()) {
            sb.append(" at ");
            if (line >= 0 && column >= 0) {
                sb.append("line ").append(line).append(", column ").append(column);
            } else if (position >= 0) {
                sb.append("position ").append(position);
            }
        }
        if (hasContext()) {
            sb.append("\n").append(context);
        }
        if (hasSuggestions()) {
            sb.append("\nSuggestions:");
            for (String suggestion : suggestions) {
                sb.append("\n  - ").append(suggestion);
            }
        }
        return sb.toString();
    }

    /**
     * Extracts context from source SQL with error position highlighting.
     *
     * @param sourceSql the source SQL
     * @param line the line number (1-based)
     * @param column the column number (1-based)
     * @return the context string with error highlighting
     */
    private static String extractContext(final String sourceSql, final int line, final int column) {
        if (sourceSql == null || line < 1 || column < 1) {
            return null;
        }
        String[] lines = sourceSql.split("\n");
        if (line > lines.length) {
            return null;
        }
        String errorLine = lines[line - 1];
        StringBuilder context = new StringBuilder();
        context.append(errorLine).append("\n");
        // Add caret (^) marker at the error position
        for (int i = 0; i < column - 1; i++) {
            context.append(" ");
        }
        context.append("^");
        return context.toString();
    }
}
