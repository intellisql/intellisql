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

package org.intellisql.parser;

import lombok.Getter;

/** Exception thrown when SQL translation fails. */
@Getter
public class TranslationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final TranslationError error;

    /**
     * Creates a new translation exception.
     *
     * @param message the error message
     */
    public TranslationException(final String message) {
        super(message);
        this.error = TranslationError.of("TRANSLATION_ERROR", message);
    }

    /**
     * Creates a new translation exception with cause.
     *
     * @param message the error message
     * @param cause the cause
     */
    public TranslationException(final String message, final Throwable cause) {
        super(message, cause);
        this.error = TranslationError.of("TRANSLATION_ERROR", message);
    }

    /**
     * Creates a new translation exception with error details.
     *
     * @param error the translation error
     */
    public TranslationException(final TranslationError error) {
        super(error.getMessage());
        this.error = error;
    }

    /**
     * Creates a new translation exception with error details and cause.
     *
     * @param error the translation error
     * @param cause the cause
     */
    public TranslationException(final TranslationError error, final Throwable cause) {
        super(error.getMessage(), cause);
        this.error = error;
    }

    /**
     * Creates a new syntax error exception.
     *
     * @param message the error message
     * @return the exception
     */
    public static TranslationException syntaxError(final String message) {
        return new TranslationException(TranslationError.syntaxError(message));
    }

    /**
     * Creates a new unsupported feature exception.
     *
     * @param feature the unsupported feature
     * @return the exception
     */
    public static TranslationException unsupportedFeature(final String feature) {
        return new TranslationException(TranslationError.unsupportedFeature(feature));
    }

    /**
     * Creates a new dialect mismatch exception.
     *
     * @param source the source dialect
     * @param target the target dialect
     * @return the exception
     */
    public static TranslationException dialectMismatch(final String source, final String target) {
        return new TranslationException(TranslationError.dialectMismatch(source, target));
    }
}
