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

package org.intellisql.parser.dialect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link HiveDialectConverter}.
 */
class HiveDialectConverterTest {

    private HiveDialectConverter converter;

    @BeforeEach
    void setUp() {
        converter = new HiveDialectConverter();
    }

    @Test
    void assertGetCalciteDialect() {
        assertNotNull(converter.getCalciteDialect());
    }

    @Test
    void assertConvertFromDialect() {
        String sql = "SELECT * FROM users";
        String result = converter.convertFromDialect(sql);
        assertNotNull(result);
    }
}
