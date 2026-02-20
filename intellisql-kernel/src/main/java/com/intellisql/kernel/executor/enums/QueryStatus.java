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

package com.intellisql.kernel.executor.enums;

/**
 * Enumeration of query execution status values. Used to track the lifecycle of a query from
 * submission to completion.
 */
public enum QueryStatus {
    /** Query is waiting to be executed. */
    PENDING,
    /** Query is currently executing. */
    RUNNING,
    /** Query completed successfully. */
    COMPLETED,
    /** Query failed with an error. */
    FAILED,
    /** Query was cancelled by user or system. */
    CANCELLED
}
