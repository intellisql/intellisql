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

package com.intellisql.kernel.logger;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Context object for query execution tracking. Contains query ID and other metadata for logging and
 * tracing.
 */
@Getter
@Builder
public class QueryContext {

    /** Unique identifier for this query execution. */
    private final String queryId;

    /** SQL statement being executed. */
    private final String sql;

    /** User who submitted the query. */
    private final String user;

    /** Client connection identifier. */
    private final String connectionId;

    /** Timestamp when the query was submitted. */
    private final long submitTime;

    /**
     * Creates a new QueryContext with a generated query ID.
     *
     * @param sql the SQL statement
     * @param user the user submitting the query
     * @param connectionId the connection identifier
     * @return a new QueryContext instance
     */
    public static QueryContext create(
                                      final String sql, final String user, final String connectionId) {
        return QueryContext.builder()
                .queryId(UUID.randomUUID().toString())
                .sql(sql)
                .user(user)
                .connectionId(connectionId)
                .submitTime(System.currentTimeMillis())
                .build();
    }
}
