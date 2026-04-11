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

package com.intellisql.connector.elasticsearch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.cluster.metadata.MappingMetadata;

import com.intellisql.common.metadata.Column;
import com.intellisql.common.metadata.Index;
import com.intellisql.common.metadata.Schema;
import com.intellisql.common.metadata.Table;
import com.intellisql.common.metadata.enums.SchemaType;
import com.intellisql.common.metadata.enums.TableType;

import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch implementation of SchemaDiscoverer. Uses _mapping API to discover index structure
 * and mappings. Uses Elasticsearch 7.x API for JDK 8 compatibility.
 */
@Slf4j
public class ElasticsearchSchemaDiscoverer {

    /**
     * Discovers the schema for the specified index pattern.
     *
     * @param client the Elasticsearch RestHighLevelClient
     * @param indexPattern the index pattern
     * @param dataSourceName the data source configuration name
     * @return the schema
     * @throws Exception if discovery fails
     */
    public Schema discoverSchema(final RestHighLevelClient client, final String indexPattern,
                                 final String dataSourceName) throws Exception {
        final Map<String, Table> tables = new LinkedHashMap<>();
        final GetMappingsRequest request = new GetMappingsRequest();
        if (indexPattern != null && !indexPattern.isEmpty()) {
            request.indices(indexPattern);
        } else {
            request.indices("*");
        }
        final GetMappingsResponse response = client.indices().getMapping(request, RequestOptions.DEFAULT);
        for (final String indexName : response.mappings().keys().toArray(String.class)) {
            final ImmutableOpenMap<String, MappingMetadata> typeMappings = response.mappings().get(indexName);
            final MappingMetadata mapping = typeMappings.values().iterator().next();
            final Table table = discoverIndexMapping(indexName, mapping, "elasticsearch", dataSourceName);
            tables.put(table.getName(), table);
            log.debug("Discovered Elasticsearch index: {}", indexName);
        }
        return Schema.builder()
                .name("elasticsearch")
                .type(SchemaType.PHYSICAL)
                .dataSourceId(dataSourceName)
                .tables(tables)
                .build();
    }

    private Table discoverIndexMapping(
                                       final String indexName,
                                       final MappingMetadata mapping,
                                       final String schemaName,
                                       final String dataSourceId) {
        final List<Column> columns = new ArrayList<>();
        final List<String> primaryKey = new ArrayList<>();
        final Map<String, Object> sourceAsMap = mapping.sourceAsMap();
        final Object properties = sourceAsMap.get("properties");
        if (properties instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> propertiesMap = (Map<String, Object>) properties;
            int position = 0;
            for (final Map.Entry<String, Object> entry : propertiesMap.entrySet()) {
                final String fieldName = entry.getKey();
                if (!(entry.getValue() instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                final Map<String, Object> fieldProps = (Map<String, Object>) entry.getValue();
                final Column column = buildColumn(indexName, schemaName, fieldName, fieldProps, ++position);
                columns.add(column);
                if (isPrimaryKeyField(fieldName)) {
                    primaryKey.add(fieldName);
                }
            }
        }
        return Table.builder()
                .name(indexName)
                .schemaName(schemaName)
                .dataSourceId(dataSourceId)
                .columns(columns)
                .primaryKey(primaryKey)
                .indexes(new ArrayList<Index>())
                .type(TableType.TABLE)
                .metadata(singleMetadata("remarks", "Elasticsearch index"))
                .build();
    }

    private Column buildColumn(
                               final String tableName,
                               final String schemaName,
                               final String fieldName,
                               final Map<String, Object> fieldProps,
                               final int position) {
        final String esType = (String) fieldProps.getOrDefault("type", "object");
        return Column.builder()
                .name(fieldName)
                .dataType(ElasticsearchTypeMapping.mapToDataType(esType))
                .nullable(true)
                .metadata(columnMetadata(tableName, schemaName, esType, isPrimaryKeyField(fieldName), position))
                .build();
    }

    private boolean isPrimaryKeyField(final String fieldName) {
        return "_id".equals(fieldName) || "_source".equals(fieldName);
    }

    private Map<String, String> columnMetadata(
                                               final String tableName,
                                               final String schemaName,
                                               final String nativeType,
                                               final boolean primaryKey,
                                               final int ordinalPosition) {
        final Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("tableName", tableName);
        metadata.put("schemaName", schemaName);
        metadata.put("nativeType", nativeType);
        metadata.put("primaryKey", String.valueOf(primaryKey));
        metadata.put("autoIncrement", "false");
        metadata.put("ordinalPosition", String.valueOf(ordinalPosition));
        return metadata;
    }

    private Map<String, String> singleMetadata(final String key, final String value) {
        final Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put(key, value);
        return metadata;
    }
}
