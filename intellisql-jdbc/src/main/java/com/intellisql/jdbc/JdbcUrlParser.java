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

package com.intellisql.jdbc;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;

/** Parser for IntelliSql JDBC URLs. URL format: jdbc:intellisql://host:port/database?properties. */
@Getter
public final class JdbcUrlParser {

    private static final String JDBC_PREFIX = "jdbc:intellisql://";

    private static final int DEFAULT_PORT = 8765;

    private static final String DEFAULT_DATABASE = "intellisql";

    private static final Pattern URL_PATTERN =
            Pattern.compile("jdbc:intellisql://([^:/]+)(?::(\\d+))?(?:/([^?]*))?(?:\\?(.+))?");

    private final String host;

    private final int port;

    private final String database;

    private final Map<String, String> properties;

    /**
     * Private constructor.
     *
     * @param host the host
     * @param port the port
     * @param database the database
     * @param properties the properties
     */
    private JdbcUrlParser(final String host, final int port, final String database,
                          final Map<String, String> properties) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.properties = properties;
    }

    /**
     * Parses the JDBC URL and returns a JdbcUrlParser instance.
     *
     * @param url the JDBC URL to parse
     * @return the JdbcUrlParser instance
     * @throws IllegalArgumentException if the URL is invalid
     */
    public static JdbcUrlParser parse(final String url) {
        if (url == null || !url.startsWith(JDBC_PREFIX)) {
            throw new IllegalArgumentException("Invalid IntelliSql JDBC URL: " + url);
        }
        Matcher matcher = URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid IntelliSql JDBC URL format: " + url);
        }
        String host = matcher.group(1);
        int port = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : DEFAULT_PORT;
        String database =
                matcher.group(3) != null && !matcher.group(3).isEmpty()
                        ? matcher.group(3)
                        : DEFAULT_DATABASE;
        Map<String, String> properties = new HashMap<>();
        if (matcher.group(4) != null) {
            parseQueryParams(matcher.group(4), properties);
        }
        return new JdbcUrlParser(host, port, database, properties);
    }

    private static void parseQueryParams(final String queryString, final Map<String, String> properties) {
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                String key =
                        idx > 0
                                ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name())
                                : pair;
                String value =
                        idx > 0 && pair.length() > idx + 1
                                ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name())
                                : "";
                properties.put(key, value);
            } catch (final UnsupportedEncodingException ex) {
                throw new RuntimeException("Failed to decode URL parameter", ex);
            }
        }
    }

    /**
     * Converts to Properties object.
     *
     * @return the properties
     */
    public Properties toProperties() {
        Properties props = new Properties();
        props.putAll(properties);
        return props;
    }

    /**
     * Gets the server endpoint.
     *
     * @return the endpoint
     */
    public String getEndpoint() {
        return "http://" + host + ":" + port;
    }

    /**
     * Gets the protobuf endpoint.
     *
     * @return the protobuf endpoint
     */
    public String getProtobufEndpoint() {
        return getEndpoint() + "/api/protobuf";
    }
}
