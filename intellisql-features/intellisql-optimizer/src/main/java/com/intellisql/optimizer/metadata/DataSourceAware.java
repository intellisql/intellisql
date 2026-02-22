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

package com.intellisql.optimizer.metadata;

/**
 * Interface for tables that are associated with a data source.
 * Implemented by Calcite table wrappers to provide data source information
 * during query optimization and execution planning.
 *
 * <p>
 * This interface follows the Strategy pattern, allowing the optimizer module
 * to access data source information without depending on the kernel module.
 * </p>
 */
public interface DataSourceAware {

    /**
     * Returns the identifier of the data source this table belongs to.
     *
     * @return the data source identifier, or null if not associated with any data source
     */
    String getDataSourceId();

    /**
     * Returns the name of this table.
     *
     * @return the table name
     */
    String getTableName();
}
