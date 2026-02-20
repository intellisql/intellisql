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

package com.intellisql.kernel.metadata;

import com.intellisql.kernel.metadata.enums.DataType;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Column definition representing a column in a table with type information. */
@Getter
@RequiredArgsConstructor
@Builder
public final class Column {

    private final String name;

    private final DataType dataType;

    private final boolean nullable;

    private final String defaultValue;

    private final String comment;

    private final Integer size;

    private final Integer precision;

    private final Integer scale;
}
