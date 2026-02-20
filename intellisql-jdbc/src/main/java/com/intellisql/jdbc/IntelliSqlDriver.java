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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import lombok.extern.slf4j.Slf4j;

/** JDBC Driver implementation for IntelliSql. URL format: jdbc:intellisql://host:port/database */
@Slf4j
public class IntelliSqlDriver implements Driver {

    private static final String JDBC_PREFIX = "jdbc:intellisql:";

    private static final int MAJOR_VERSION = 1;

    private static final int MINOR_VERSION = 0;

    static {
        try {
            DriverManager.registerDriver(new IntelliSqlDriver());
            log.info("IntelliSql JDBC Driver registered successfully");
        } catch (final SQLException ex) {
            log.error("Failed to register IntelliSql JDBC Driver", ex);
            throw new RuntimeException("Failed to register IntelliSqlDriver", ex);
        }
    }

    @Override
    public Connection connect(final String url, final Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        try {
            JdbcUrlParser parser = JdbcUrlParser.parse(url);
            Properties mergedProps = mergeProperties(parser.toProperties(), info);
            AvaticaClient client = new AvaticaClient(parser.getProtobufEndpoint(), mergedProps);
            return new IntelliSqlConnection(client, parser, mergedProps);
            // CHECKSTYLE:OFF: IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON: IllegalCatch
            log.error("Failed to create connection to {}", url, ex);
            throw new SQLException("Failed to create connection: " + ex.getMessage(), "08001", ex);
        }
    }

    @Override
    public boolean acceptsURL(final String url) throws SQLException {
        return url != null && url.startsWith(JDBC_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) throws SQLException {
        return new DriverPropertyInfo[]{
                createPropertyInfo("fetchSize", "1000", "Default fetch size for result sets"),
                createPropertyInfo("queryTimeout", "300", "Query timeout in seconds"),
                createPropertyInfo("serialization", "protobuf", "Serialization format (protobuf/json)"),
                createPropertyInfo("connectTimeout", "30", "Connection timeout in seconds"),
                createPropertyInfo("socketTimeout", "60", "Socket read timeout in seconds"),
                createPropertyInfo("maxRows", "0", "Maximum rows to return (0=unlimited)")
        };
    }

    private DriverPropertyInfo createPropertyInfo(final String name, final String value, final String description) {
        DriverPropertyInfo info = new DriverPropertyInfo(name, value);
        info.description = description;
        return info;
    }

    @Override
    public int getMajorVersion() {
        return MAJOR_VERSION;
    }

    @Override
    public int getMinorVersion() {
        return MINOR_VERSION;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Use SLF4J for logging");
    }

    private Properties mergeProperties(final Properties urlProps, final Properties userProps) {
        Properties merged = new Properties();
        merged.putAll(urlProps);
        if (userProps != null) {
            merged.putAll(userProps);
        }
        return merged;
    }
}
