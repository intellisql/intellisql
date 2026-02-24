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

package com.intellisql.client.console;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for MetaDataLoader.
 */
class MetaDataLoaderTest {

    @Test
    void testLoad() throws SQLException, InterruptedException {
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet tablesRs = mock(ResultSet.class);
        ResultSet columnsRs = mock(ResultSet.class);
        ResultSet schemasRs = mock(ResultSet.class);
        when(connection.getMetaData()).thenReturn(metaData);
        Mockito.when(metaData.getTables(any(), any(), any(), any())).thenReturn(tablesRs);
        when(tablesRs.next()).thenReturn(true, false);
        when(tablesRs.getString("TABLE_NAME")).thenReturn("test_table");
        Mockito.when(metaData.getColumns(any(), any(), any(), any())).thenReturn(columnsRs);
        when(columnsRs.next()).thenReturn(true, false);
        when(columnsRs.getString("COLUMN_NAME")).thenReturn("test_column");
        Mockito.when(metaData.getSchemas()).thenReturn(schemasRs);
        when(schemasRs.next()).thenReturn(true, false);
        when(schemasRs.getString("TABLE_SCHEM")).thenReturn("test_schema");
        MetaDataLoader loader = new MetaDataLoader();
        loader.load(connection);
        // Wait for async task
        int retries = 0;
        while (loader.getTables().isEmpty() && retries < 20) {
            Thread.sleep(50);
            retries++;
        }
        Assertions.assertTrue(loader.getTables().contains("test_table"));
        Assertions.assertTrue(loader.getColumns().contains("test_column"));
        Assertions.assertTrue(loader.getSchemas().contains("test_schema"));
        loader.clear();
        Assertions.assertTrue(loader.getTables().isEmpty());
    }

    @Test
    void testLoadNullConnection() {
        MetaDataLoader loader = new MetaDataLoader();
        loader.load(null);
        Assertions.assertTrue(loader.getTables().isEmpty());
    }
}
