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

package com.intellisql.connector.elasticsearch;

import java.sql.Connection;

import com.intellisql.connector.model.QueryResult;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch implementation of Connection interface. Wraps an Elasticsearch Java client for
 * query execution.
 */
@Slf4j
public class ElasticsearchConnection implements com.intellisql.connector.api.Connection {

    @Getter
    private final ElasticsearchClient client;

    private final ElasticsearchQueryExecutor queryExecutor;

    private volatile boolean closed;

    /**
     * Creates a new Elasticsearch connection.
     *
     * @param client the Elasticsearch client
     */
    public ElasticsearchConnection(final ElasticsearchClient client) {
        this.client = client;
        this.queryExecutor = new ElasticsearchQueryExecutor();
    }

    @Override
    public QueryResult executeQuery(final String sql) throws Exception {
        checkNotClosed();
        return queryExecutor.executeQuery(client, sql);
    }

    @Override
    public int executeUpdate(final String sql) throws Exception {
        throw new UnsupportedOperationException(
                "Elasticsearch does not support direct updates via SQL. Use index API instead.");
    }

    @Override
    public boolean isValid() {
        if (closed) {
            return false;
        }
        try {
            HealthResponse health = client.cluster().health();
            return health != null && !"red".equals(health.status().jsonValue());
            // CHECKSTYLE:OFF: IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON: IllegalCatch
            log.error("Elasticsearch connection validity check failed", ex);
            return false;
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        try {
            client._transport().close();
            closed = true;
            log.debug("Elasticsearch connection closed");
            // CHECKSTYLE:OFF: IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON: IllegalCatch
            log.error("Error closing Elasticsearch connection", ex);
        }
    }

    @Override
    public Connection getJdbcConnection() {
        return null;
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Elasticsearch connection is already closed");
        }
    }
}
