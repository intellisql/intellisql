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

package org.intellisql.connector.elasticsearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import org.intellisql.connector.enums.SchemaType;
import org.intellisql.connector.enums.TableType;
import org.intellisql.connector.model.Column;
import org.intellisql.connector.model.Schema;
import org.intellisql.connector.model.Table;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch implementation of SchemaDiscoverer. Uses _mapping API to discover index structure
 * and mappings.
 */
@Slf4j
public class ElasticsearchSchemaDiscoverer {

    /**
     * Discovers the schema for the specified index pattern.
     *
     * @param client the Elasticsearch client
     * @param indexPattern the index pattern
     * @return the schema
     * @throws Exception if discovery fails
     */
    public Schema discoverSchema(final ElasticsearchClient client, final String indexPattern) throws Exception {
        List<Table> tables = new ArrayList<>();
        GetMappingResponse response;
        if (indexPattern != null && !indexPattern.isEmpty()) {
            response = client.indices().getMapping(m -> m.index(indexPattern));
        } else {
            response = client.indices().getMapping(m -> m.index("*"));
        }
        for (Map.Entry<String, IndexMappingRecord> entry : response.result().entrySet()) {
            String indexName = entry.getKey();
            IndexMappingRecord mapping = entry.getValue();
            Table table = discoverIndexMapping(indexName, mapping);
            tables.add(table);
            log.debug("Discovered Elasticsearch index: {}", indexName);
        }
        return Schema.builder().name("elasticsearch").type(SchemaType.PHYSICAL).tables(tables).build();
    }

    /**
     * Discovers all indexes in the cluster.
     *
     * @param client the Elasticsearch client
     * @return list of index names
     * @throws Exception if discovery fails
     */
    public List<String> discoverIndexes(final ElasticsearchClient client) throws Exception {
        List<String> indexes = new ArrayList<>();
        co.elastic.clients.elasticsearch.cat.IndicesResponse response = client.cat().indices();
        for (co.elastic.clients.elasticsearch.cat.indices.IndicesRecord record : response.valueBody()) {
            if (record.index() != null) {
                indexes.add(record.index());
            }
        }
        return indexes;
    }

    private Table discoverIndexMapping(final String indexName, final IndexMappingRecord mapping) {
        List<Column> columns = new ArrayList<>();
        if (mapping.mappings() != null && mapping.mappings().properties() != null) {
            int position = 0;
            for (Map.Entry<String, co.elastic.clients.elasticsearch._types.mapping.Property> entry : mapping.mappings().properties().entrySet()) {
                String fieldName = entry.getKey();
                co.elastic.clients.elasticsearch._types.mapping.Property property = entry.getValue();
                Column column = buildColumn(fieldName, property, ++position);
                columns.add(column);
            }
        }
        return Table.builder()
                .name(indexName)
                .type(TableType.TABLE)
                .remarks("Elasticsearch index")
                .columns(columns)
                .build();
    }

    private Column buildColumn(final String fieldName, final Property property, final int position) {
        String esType = getElasticsearchType(property);
        return Column.builder()
                .name(fieldName)
                .dataType(ElasticsearchTypeMapping.mapToDataType(esType))
                .nativeType(esType)
                .nullable(true)
                .primaryKey("_id".equals(fieldName) || "_source".equals(fieldName))
                .ordinalPosition(position)
                .build();
    }

    private String getElasticsearchType(final Property property) {
        if (property.isKeyword()) {
            return "keyword";
        } else if (property.isText()) {
            return "text";
        } else if (property.isLong()) {
            return "long";
        } else if (property.isInteger()) {
            return "integer";
        } else if (property.isShort()) {
            return "short";
        } else if (property.isByte()) {
            return "byte";
        } else if (property.isDouble()) {
            return "double";
        } else if (property.isFloat()) {
            return "float";
        } else if (property.isHalfFloat()) {
            return "half_float";
        } else if (property.isScaledFloat()) {
            return "scaled_float";
        } else if (property.isBoolean()) {
            return "boolean";
        } else if (property.isDate()) {
            return "date";
        } else if (property.isDateNanos()) {
            return "date_nanos";
        } else if (property.isNested()) {
            return "nested";
        } else if (property.isObject()) {
            return "object";
        } else if (property.isBinary()) {
            return "binary";
        } else if (property.isIp()) {
            return "ip";
        } else if (property.isGeoPoint()) {
            return "geo_point";
        } else if (property.isGeoShape()) {
            return "geo_shape";
        } else if (property.isFlattened()) {
            return "flattened";
        } else if (property.isJoin()) {
            return "join";
        } else if (property.isAlias()) {
            return "alias";
        } else if (property.isDenseVector()) {
            return "dense_vector";
        } else if (property.isRankFeature()) {
            return "rank_feature";
        } else if (property.isRankFeatures()) {
            return "rank_features";
        } else if (property.isHistogram()) {
            return "histogram";
        } else {
            return "object";
        }
    }
}
