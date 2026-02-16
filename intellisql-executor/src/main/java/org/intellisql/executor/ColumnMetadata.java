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

package org.intellisql.executor;

import org.intellisql.kernel.metadata.enums.DataType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata information for a result set column. Contains name, type, and constraint information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMetadata {

    /** The column name in the database. */
    private String name;

    /** The display label (alias) for the column. */
    private String label;

    /** The data type of the column. */
    private DataType dataType;

    /** Whether the column can contain null values. */
    private boolean nullable;

    /** The precision for numeric columns, or length for string columns. */
    private int precision;

    /** The scale for numeric columns. */
    private int scale;

    /**
     * Creates a ColumnMetadata with just a name and type.
     *
     * @param name the column name
     * @param dataType the data type
     * @return a new ColumnMetadata instance
     */
    public static ColumnMetadata of(final String name, final DataType dataType) {
        return ColumnMetadata.builder()
                .name(name)
                .label(name)
                .dataType(dataType)
                .nullable(true)
                .precision(0)
                .scale(0)
                .build();
    }

    /**
     * Creates a ColumnMetadata with name, label, and type.
     *
     * @param name the column name
     * @param label the display label
     * @param dataType the data type
     * @return a new ColumnMetadata instance
     */
    public static ColumnMetadata of(final String name, final String label, final DataType dataType) {
        return ColumnMetadata.builder()
                .name(name)
                .label(label)
                .dataType(dataType)
                .nullable(true)
                .precision(0)
                .scale(0)
                .build();
    }

    /**
     * Gets the display label, falling back to the column name if not set.
     *
     * @return the display label
     */
    public String getDisplayLabel() {
        return label != null && !label.isEmpty() ? label : name;
    }
}
