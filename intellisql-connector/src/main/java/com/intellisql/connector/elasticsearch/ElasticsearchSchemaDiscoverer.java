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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.MappingMetadata;

import com.intellisql.connector.enums.SchemaType;
import com.intellisql.connector.enums.TableType;
import com.intellisql.connector.model.Column;
import com.intellisql.connector.model.Schema;
import com.intellisql.connector.model.Table;

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
        List<Table> tables = new ArrayList<>();
        GetMappingsRequest request = new GetMappingsRequest();
        if (indexPattern != null && !indexPattern.isEmpty()) {
            request.indices(indexPattern);
        } else {
            request.indices("*");
        }
        GetMappingsResponse response = client.indices().getMapping(request, RequestOptions.DEFAULT);
        // ES 7.x: mappings() returns ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetadata>>
        // Iterate using iterator() instead of entrySet()
        for (final String indexName : response.mappings().keys().toArray(String.class)) {
            org.elasticsearch.common.collect.ImmutableOpenMap<String, MappingMetadata> typeMappings =
                    response.mappings().get(indexName);
            // In ES 7.x, usually there's only one type "_doc"
            MappingMetadata mapping = typeMappings.values().iterator().next();
            Table table = discoverIndexMapping(indexName, mapping);
            tables.add(table);
            log.debug("Discovered Elasticsearch index: {}", indexName);
        }
        return Schema.builder()
                .name("elasticsearch")
                .type(SchemaType.PHYSICAL)
                .dataSourceName(dataSourceName)
                .tables(tables)
                .build();
    }

    private Table discoverIndexMapping(final String indexName, final MappingMetadata mapping) {
        List<Column> columns = new ArrayList<>();
        Map<String, Object> sourceAsMap = mapping.sourceAsMap();
        Object properties = sourceAsMap.get("properties");
        if (properties instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> propertiesMap = (Map<String, Object>) properties;
            int position = 0;
            for (Map.Entry<String, Object> entry : propertiesMap.entrySet()) {
                String fieldName = entry.getKey();
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fieldProps = (Map<String, Object>) entry.getValue();
                    Column column = buildColumn(fieldName, fieldProps, ++position);
                    columns.add(column);
                }
            }
        }
        return Table.builder()
                .name(indexName)
                .type(TableType.TABLE)
                .remarks("Elasticsearch index")
                .columns(columns)
                .build();
    }

    private Column buildColumn(final String fieldName, final Map<String, Object> fieldProps, final int position) {
        String esType = (String) fieldProps.getOrDefault("type", "object");
        return Column.builder()
                .name(fieldName)
                .dataType(ElasticsearchTypeMapping.mapToDataType(esType))
                .nativeType(esType)
                .nullable(true)
                .primaryKey("_id".equals(fieldName) || "_source".equals(fieldName))
                .ordinalPosition(position)
                .build();
    }
}
