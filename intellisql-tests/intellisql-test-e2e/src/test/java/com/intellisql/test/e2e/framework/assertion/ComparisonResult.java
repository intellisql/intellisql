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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/** Result of comparing two result set snapshots. */
@Getter
public final class ComparisonResult {

    private final boolean matched;

    private final String message;

    private final List<String> details;

    /**
     * Creates a comparison result.
     *
     * @param matched whether the snapshots match
     * @param message summary message
     * @param details detail messages
     */
    public ComparisonResult(final boolean matched, final String message, final List<String> details) {
        this.matched = matched;
        this.message = message;
        this.details = Collections.unmodifiableList(new ArrayList<>(details));
    }

    /**
     * Creates a matched comparison result.
     *
     * @return matched comparison result
     */
    public static ComparisonResult matched() {
        return new ComparisonResult(true, "Result sets matched", Collections.<String>emptyList());
    }

    /**
     * Creates an unmatched comparison result.
     *
     * @param message summary message
     * @param details detail messages
     * @return unmatched comparison result
     */
    public static ComparisonResult unmatched(final String message, final List<String> details) {
        return new ComparisonResult(false, message, details);
    }
}
