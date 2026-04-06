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

package com.intellisql.common.config;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigLoaderTest {

    @Test
    void shouldPreserveConnectorSpecificFieldsWhenLoadingDataSourceConfig() {
        String yaml = "dataSources:\n"
                + "  - name: mysql_source\n"
                + "    type: mysql\n"
                + "    host: 127.0.0.1\n"
                + "    port: 3306\n"
                + "    database: demo_db\n"
                + "    schema: app\n"
                + "    username: root\n"
                + "    password: secret\n"
                + "    properties:\n"
                + "      useSSL: false\n"
                + "      serverTimezone: UTC\n";

        ModelConfig actual =
                ConfigLoader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

        DataSourceConfig dataSourceConfig = actual.getDataSources().get("mysql_source");
        assertThat(dataSourceConfig).isNotNull();
        assertThat(dataSourceConfig.getType()).isEqualTo("MYSQL");
        assertThat(dataSourceConfig.getHost()).isEqualTo("127.0.0.1");
        assertThat(dataSourceConfig.getPort()).isEqualTo(3306);
        assertThat(dataSourceConfig.getDatabase()).isEqualTo("demo_db");
        assertThat(dataSourceConfig.getSchema()).isEqualTo("app");
        assertThat(dataSourceConfig.getUrl()).isEqualTo("jdbc:mysql://127.0.0.1:3306/demo_db");
        assertThat(dataSourceConfig.getUsername()).isEqualTo("root");
        assertThat(dataSourceConfig.getPassword()).isEqualTo("secret");
        assertThat(dataSourceConfig.getProperties())
                .containsEntry("useSSL", "false")
                .containsEntry("serverTimezone", "UTC");
    }
}
