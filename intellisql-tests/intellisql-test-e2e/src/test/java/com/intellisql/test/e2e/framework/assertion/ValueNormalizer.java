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

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Base64;

/** Normalizes JDBC values into stable textual values for result comparison. */
public final class ValueNormalizer {

    private final String nullToken;

    /**
     * Creates a value normalizer.
     *
     * @param nullToken the token used for null values
     */
    public ValueNormalizer(final String nullToken) {
        this.nullToken = nullToken;
    }

    /**
     * Normalizes a JDBC value.
     *
     * @param value the JDBC value
     * @return normalized textual value
     */
    public String normalize(final Object value) {
        if (value == null) {
            return nullToken;
        }
        if (value instanceof BigDecimal) {
            return normalizeBigDecimal((BigDecimal) value);
        }
        if (value instanceof Number) {
            return normalizeBigDecimal(new BigDecimal(value.toString()));
        }
        if (value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Date || value instanceof Time || value instanceof Timestamp) {
            return value.toString();
        }
        if (value instanceof byte[]) {
            return Base64.getEncoder().encodeToString((byte[]) value);
        }
        return String.valueOf(value);
    }

    /**
     * Normalizes a JDBC value with column type context.
     *
     * @param value the JDBC value
     * @param jdbcType the JDBC column type
     * @return normalized textual value
     */
    public String normalize(final Object value, final int jdbcType) {
        if (value instanceof Number && Types.DATE == jdbcType) {
            return LocalDate.ofEpochDay(((Number) value).longValue()).toString();
        }
        return normalize(value);
    }

    private String normalizeBigDecimal(final BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
