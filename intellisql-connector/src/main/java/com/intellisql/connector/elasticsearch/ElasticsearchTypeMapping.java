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

import java.util.HashMap;
import java.util.Map;

import com.intellisql.connector.enums.DataType;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for mapping Elasticsearch types to IntelliSql types. Handles type conversion for
 * schema discovery and query execution.
 */
@Slf4j
public final class ElasticsearchTypeMapping {

    private static final Map<String, DataType> ES_TYPE_TO_DATATYPE = new HashMap<>();

    static {
        ES_TYPE_TO_DATATYPE.put("keyword", DataType.STRING);
        ES_TYPE_TO_DATATYPE.put("text", DataType.STRING);
        ES_TYPE_TO_DATATYPE.put("long", DataType.LONG);
        ES_TYPE_TO_DATATYPE.put("integer", DataType.INTEGER);
        ES_TYPE_TO_DATATYPE.put("short", DataType.INTEGER);
        ES_TYPE_TO_DATATYPE.put("byte", DataType.INTEGER);
        ES_TYPE_TO_DATATYPE.put("double", DataType.DOUBLE);
        ES_TYPE_TO_DATATYPE.put("float", DataType.DOUBLE);
        ES_TYPE_TO_DATATYPE.put("half_float", DataType.DOUBLE);
        ES_TYPE_TO_DATATYPE.put("scaled_float", DataType.DOUBLE);
        ES_TYPE_TO_DATATYPE.put("boolean", DataType.BOOLEAN);
        ES_TYPE_TO_DATATYPE.put("date", DataType.TIMESTAMP);
        ES_TYPE_TO_DATATYPE.put("date_nanos", DataType.TIMESTAMP);
        ES_TYPE_TO_DATATYPE.put("nested", DataType.JSON);
        ES_TYPE_TO_DATATYPE.put("object", DataType.JSON);
        ES_TYPE_TO_DATATYPE.put("flattened", DataType.JSON);
        ES_TYPE_TO_DATATYPE.put("binary", DataType.BINARY);
        ES_TYPE_TO_DATATYPE.put("ip", DataType.STRING);
        ES_TYPE_TO_DATATYPE.put("version", DataType.STRING);
        ES_TYPE_TO_DATATYPE.put("completion", DataType.STRING);
        ES_TYPE_TO_DATATYPE.put("search_as_you_type", DataType.STRING);
        ES_TYPE_TO_DATATYPE.put("token_count", DataType.INTEGER);
        ES_TYPE_TO_DATATYPE.put("murmur3", DataType.LONG);
        ES_TYPE_TO_DATATYPE.put("alias", DataType.STRING);
        ES_TYPE_TO_DATATYPE.put("join", DataType.JSON);
        ES_TYPE_TO_DATATYPE.put("rank_feature", DataType.LONG);
        ES_TYPE_TO_DATATYPE.put("rank_features", DataType.JSON);
        ES_TYPE_TO_DATATYPE.put("dense_vector", DataType.ARRAY);
        ES_TYPE_TO_DATATYPE.put("sparse_vector", DataType.ARRAY);
        ES_TYPE_TO_DATATYPE.put("histogram", DataType.JSON);
        ES_TYPE_TO_DATATYPE.put("aggregate_metric_double", DataType.JSON);
        ES_TYPE_TO_DATATYPE.put("geo_point", DataType.STRING);
        ES_TYPE_TO_DATATYPE.put("geo_shape", DataType.JSON);
        ES_TYPE_TO_DATATYPE.put("point", DataType.JSON);
        ES_TYPE_TO_DATATYPE.put("shape", DataType.JSON);
    }

    private ElasticsearchTypeMapping() {
    }

    /**
     * Map Elasticsearch type to IntelliSql DataType. Defaults to STRING for unknown types.
     *
     * @param esType es type
     * @return mapped DataType, or STRING if unknown
     */
    public static DataType mapToDataType(final String esType) {
        if (esType == null) {
            return DataType.STRING;
        }
        String normalizedType = esType.toLowerCase();
        DataType dataType = ES_TYPE_TO_DATATYPE.get(normalizedType);
        if (dataType != null) {
            return dataType;
        }
        log.debug("Unknown Elasticsearch type '{}', defaulting to STRING", esType);
        return DataType.STRING;
    }

    /**
     * Check if the given Elasticsearch type is a nested type.
     *
     * @param esType es type
     * @return true if the type is "nested", false otherwise
     */
    public static boolean isNestedType(final String esType) {
        return "nested".equalsIgnoreCase(esType);
    }

    /**
     * Check if the given Elasticsearch type is an object type (either "object" or "nested").
     *
     * @param esType es type
     * @return true if the type is "object" or "nested", false otherwise
     */
    public static boolean isObjectType(final String esType) {
        return "object".equalsIgnoreCase(esType) || "nested".equalsIgnoreCase(esType);
    }

    /**
     * Get es type.
     *
     * @param dataType data type
     * @return es type
     */
    public static String getEsType(final DataType dataType) {
        switch (dataType) {
            case INTEGER:
                return "integer";
            case LONG:
                return "long";
            case DOUBLE:
                return "double";
            case BOOLEAN:
                return "boolean";
            case DATE:
            case TIMESTAMP:
                return "date";
            case BINARY:
                return "binary";
            case JSON:
                return "object";
            case ARRAY:
                return "nested";
            case STRING:
            default:
                return "keyword";
        }
    }
}
