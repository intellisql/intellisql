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

package com.intellisql.jdbc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.calcite.avatica.ConnectionPropertiesImpl;
import org.apache.calcite.avatica.Meta;
import org.apache.calcite.avatica.remote.ProtobufTranslation;
import org.apache.calcite.avatica.remote.ProtobufTranslationImpl;
import org.apache.calcite.avatica.remote.Service;
import org.apache.calcite.avatica.remote.TypedValue;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** Client for communicating with Avatica server using Protobuf serialization. */
@Slf4j
@Getter
public class AvaticaClient {

    private static final int MAX_RETRIES = 3;

    private static final long[] RETRY_DELAYS = {1000, 2000, 4000};

    private final String endpoint;

    private final Properties properties;

    private final ProtobufTranslation protobufTranslation;

    private final int connectTimeout;

    private final int socketTimeout;

    /**
     * Creates a new client.
     *
     * @param endpoint the server endpoint
     * @param properties the connection properties
     */
    public AvaticaClient(final String endpoint, final Properties properties) {
        this.endpoint = endpoint;
        this.properties = properties;
        this.protobufTranslation = new ProtobufTranslationImpl();
        this.connectTimeout = getIntProperty(properties, "connectTimeout", 30) * 1000;
        this.socketTimeout = getIntProperty(properties, "socketTimeout", 60) * 1000;
    }

    /**
     * Opens a connection to the server.
     *
     * @param connectionId the connection ID
     * @param info the connection info map
     * @return the open connection response
     */
    public Service.OpenConnectionResponse openConnection(
                                                         final String connectionId, final Map<String, String> info) {
        Service.OpenConnectionRequest request =
                new Service.OpenConnectionRequest(
                        connectionId, info != null ? info : Collections.emptyMap());
        return executeRequest(request, Service.OpenConnectionResponse.class);
    }

    /**
     * Closes the connection.
     *
     * @param connectionId the connection ID
     * @return the close connection response
     */
    public Service.CloseConnectionResponse closeConnection(final String connectionId) {
        Service.CloseConnectionRequest request = new Service.CloseConnectionRequest(connectionId);
        return executeRequest(request, Service.CloseConnectionResponse.class);
    }

    /**
     * Creates a statement.
     *
     * @param connectionId the connection ID
     * @return the create statement response
     */
    public Service.CreateStatementResponse createStatement(final String connectionId) {
        Service.CreateStatementRequest request = new Service.CreateStatementRequest(connectionId);
        return executeRequest(request, Service.CreateStatementResponse.class);
    }

    /**
     * Closes a statement.
     *
     * @param connectionId the connection ID
     * @param statementId the statement ID
     * @return the close statement response
     */
    public Service.CloseStatementResponse closeStatement(final String connectionId, final int statementId) {
        Service.CloseStatementRequest request =
                new Service.CloseStatementRequest(connectionId, statementId);
        return executeRequest(request, Service.CloseStatementResponse.class);
    }

    /**
     * Prepares a SQL statement.
     *
     * @param connectionId the connection ID
     * @param sql the SQL statement
     * @param maxRowCount the maximum row count
     * @return the prepare response
     */
    public Service.PrepareResponse prepare(final String connectionId, final String sql,
                                           final long maxRowCount) {
        Service.PrepareRequest request = new Service.PrepareRequest(connectionId, sql, maxRowCount);
        return executeRequest(request, Service.PrepareResponse.class);
    }

    /**
     * Executes a prepared statement.
     *
     * @param statementHandle the statement handle
     * @param parameterValues the parameter values
     * @param firstFrameMaxSize the first frame max size
     * @return the execute response
     */
    public Service.ExecuteResponse execute(
                                           final Meta.StatementHandle statementHandle,
                                           final List<TypedValue> parameterValues,
                                           final int firstFrameMaxSize) {
        Service.ExecuteRequest request =
                new Service.ExecuteRequest(
                        statementHandle,
                        parameterValues != null ? parameterValues : Collections.emptyList(),
                        firstFrameMaxSize);
        return executeRequest(request, Service.ExecuteResponse.class);
    }

    /**
     * Fetches results from a statement.
     *
     * @param connectionId the connection ID
     * @param statementId the statement ID
     * @param offset the offset
     * @param fetchMaxRowCount the fetch max row count
     * @return the fetch response
     */
    public Service.FetchResponse fetch(
                                       final String connectionId, final int statementId, final long offset,
                                       final int fetchMaxRowCount) {
        Service.FetchRequest request =
                new Service.FetchRequest(connectionId, statementId, offset, fetchMaxRowCount);
        return executeRequest(request, Service.FetchResponse.class);
    }

    /**
     * Syncs connection properties.
     *
     * @param connectionId the connection ID
     * @param connProps the connection properties
     * @return the connection sync response
     */
    public Service.ConnectionSyncResponse connectionSync(
                                                         final String connectionId, final ConnectionPropertiesImpl connProps) {
        Service.ConnectionSyncRequest request =
                new Service.ConnectionSyncRequest(connectionId, connProps);
        return executeRequest(request, Service.ConnectionSyncResponse.class);
    }

    private <T extends Service.Response> T executeRequest(
                                                          final Service.Request request, final Class<T> responseClass) {
        Exception lastException = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                Service.Response response = doExecute(request);
                if (response instanceof Service.ErrorResponse) {
                    throw new RuntimeException(
                            "Server error: " + ((Service.ErrorResponse) response).errorMessage);
                }
                return responseClass.cast(response);
                // CHECKSTYLE:OFF: IllegalCatch
            } catch (final Exception ex) {
                // CHECKSTYLE:ON: IllegalCatch
                lastException = ex;
                if (!isRetryable(ex) || attempt == MAX_RETRIES - 1) {
                    break;
                }
                log.warn("Request failed (attempt {}/{}), retrying...", attempt + 1, MAX_RETRIES, ex);
                try {
                    Thread.sleep(RETRY_DELAYS[attempt]);
                } catch (final InterruptedException iex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", iex);
                }
            }
        }
        throw new RuntimeException("Request failed after " + MAX_RETRIES + " attempts", lastException);
    }

    private Service.Response doExecute(final Service.Request request) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-protobuf");
            connection.setDoOutput(true);
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(socketTimeout);
            byte[] requestBytes = protobufTranslation.serializeRequest(request);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBytes);
                os.flush();
            }
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("HTTP request failed with code: " + responseCode);
            }
            try (
                    InputStream is = connection.getInputStream();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                return protobufTranslation.parseResponse(bos.toByteArray());
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean isRetryable(final Exception ex) {
        if (ex.getMessage() != null) {
            String message = ex.getMessage().toLowerCase();
            return message.contains("08001")
                    || message.contains("08004")
                    || message.contains("08s01")
                    || message.contains("hyt00")
                    || message.contains("timeout")
                    || message.contains("connection");
        }
        return false;
    }

    private int getIntProperty(final Properties props, final String key, final int defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (final NumberFormatException ex) {
                log.warn("Invalid value for property {}: {}, using default {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }
}
