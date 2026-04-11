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

package com.intellisql.client.console;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

import org.jline.builtins.SyntaxHighlighter;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;

/**
 * SQL syntax highlighter using jline's built-in syntax highlighter.
 */
public class IntelliSqlSyntaxHighlighter implements Highlighter {

    private final SyntaxHighlighter delegate;

    /**
     * Creates a new IntelliSqlSyntaxHighlighter instance.
     */
    public IntelliSqlSyntaxHighlighter() {
        this.delegate = createDelegate();
    }

    /**
     * Creates the delegate highlighter from the nanorc file.
     *
     * @return the delegate highlighter or null if creation fails
     */
    private SyntaxHighlighter createDelegate() {
        try {
            Path tempNanorc = Files.createTempFile("sql", ".nanorc");
            tempNanorc.toFile().deleteOnExit();
            return loadNanorcHighlighter(tempNanorc);
        } catch (final IOException ex) {
            System.err.println("[ERROR] IntelliSqlSyntaxHighlighter initialization failed: " + ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Loads the nanorc highlighter from the classpath resource.
     *
     * @param tempNanorc the temporary nanorc file path
     * @return the highlighter or null if loading fails
     * @throws IOException if an I/O error occurs
     */
    private SyntaxHighlighter loadNanorcHighlighter(final Path tempNanorc) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/sql.nanorc")) {
            if (is != null) {
                Files.copy(is, tempNanorc, StandardCopyOption.REPLACE_EXISTING);
                return SyntaxHighlighter.build(tempNanorc, "SQL");
            } else {
                System.err.println("[WARN] IntelliSqlSyntaxHighlighter: /sql.nanorc not found in classpath");
            }
        }
        return null;
    }

    @Override
    public AttributedString highlight(final LineReader reader, final String buffer) {
        if (delegate != null) {
            return delegate.highlight(buffer);
        }
        return new AttributedString(buffer);
    }

    @Override
    public void setErrorPattern(final Pattern errorPattern) {
        // delegate may not support this or not expose it
    }

    @Override
    public void setErrorIndex(final int errorIndex) {
        // delegate may not support this or not expose it
    }
}
