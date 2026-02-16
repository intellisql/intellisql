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

import lombok.Getter;
import lombok.Setter;

/** Provides customizable prompt strings for the REPL. */
@Getter
@Setter
public class PromptProvider {

    private static final String DEFAULT_PROMPT = "isql> ";

    private static final String CONTINUATION_PROMPT = "    > ";

    /** ANSI prompt with cyan color. */
    // ANSI escape sequences required for terminal colors
    // CHECKSTYLE:OFF:AvoidEscapedUnicodeCharacters
    private static final String ANSI_PROMPT = "\u001B[1;36misql\u001B[0m> ";
    // CHECKSTYLE:ON:AvoidEscapedUnicodeCharacters

    private String prompt;

    private String continuationPrompt;

    private boolean useColor;

    /** Creates a PromptProvider with color enabled. */
    public PromptProvider() {
        this(true);
    }

    /**
     * Creates a PromptProvider with the specified color setting.
     *
     * @param useColor whether to use ANSI color codes
     */
    public PromptProvider(final boolean useColor) {
        this.useColor = useColor;
        this.prompt = useColor ? ANSI_PROMPT : DEFAULT_PROMPT;
        this.continuationPrompt = CONTINUATION_PROMPT;
    }

    /**
     * Gets the standard prompt.
     *
     * @return the prompt string
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Gets the continuation prompt for multi-line input.
     *
     * @return the continuation prompt string
     */
    public String getContinuationPrompt() {
        return continuationPrompt;
    }

    /**
     * Sets a custom prompt.
     *
     * @param customPrompt the custom prompt string
     */
    public void setCustomPrompt(final String customPrompt) {
        this.prompt = customPrompt;
    }

    /**
     * Creates a prompt with context information.
     *
     * @param database the current database name
     * @return contextualized prompt
     */
    public String getPromptWithContext(final String database) {
        if (database == null || database.isEmpty()) {
            return prompt;
        }
        if (useColor) {
            // ANSI colors: cyan for isql, yellow for database
            // CHECKSTYLE:OFF:AvoidEscapedUnicodeCharacters
            return "\u001B[1;36misql\u001B[0m:\u001B[1;33m" + database + "\u001B[0m> ";
            // CHECKSTYLE:ON:AvoidEscapedUnicodeCharacters
        }
        return "isql:" + database + "> ";
    }
}
