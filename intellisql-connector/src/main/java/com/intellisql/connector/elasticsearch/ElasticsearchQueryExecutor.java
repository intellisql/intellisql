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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.intellisql.connector.enums.DataType;
import com.intellisql.connector.model.QueryResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch implementation of QueryExecutor. Converts SQL-like queries to Elasticsearch Query
 * DSL and executes them. Uses Elasticsearch 7.x API for JDK 8 compatibility.
 */
@Slf4j
public class ElasticsearchQueryExecutor {

    private static final Pattern SELECT_PATTERN =
            Pattern.compile(
                    "SELECT\\s+(.+?)\\s+FROM\\s+(\\w+)(?:\\s+WHERE\\s+(.+?))?(?:\\s+ORDER\\s+BY\\s+(.+?))?(?:\\s+LIMIT\\s+(\\d+))?",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern WHERE_PATTERN =
            Pattern.compile(
                    "(\\w+)\\s*(=|!=|<>|>|<|>=|<=|\\s+LIKE\\s+|\\s+IN\\s+)\\s*(.+?)(?:\\s+AND\\s+|\\s+OR\\s+|$)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern NESTED_FIELD_PATTERN = Pattern.compile("^(\\w+)\\.(\\w+)$");

    /**
     * Executes a SQL-like query against Elasticsearch.
     *
     * @param client the Elasticsearch RestHighLevelClient
     * @param sql the SQL string
     * @return the query result
     * @throws Exception if execution fails
     */
    public QueryResult executeQuery(final RestHighLevelClient client, final String sql) throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            SqlQuery parsedQuery = parseSql(sql);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            if (parsedQuery.getWhereClause() != null && !parsedQuery.getWhereClause().isEmpty()) {
                BoolQueryBuilder query = buildQuery(parsedQuery.getWhereClause());
                sourceBuilder.query(query);
            }

            if (parsedQuery.getOrderBy() != null && !parsedQuery.getOrderBy().isEmpty()) {
                String[] orderParts = parsedQuery.getOrderBy().trim().split("\\s+");
                String sortField = orderParts[0];
                SortOrder sortOrder =
                        orderParts.length > 1 && "DESC".equalsIgnoreCase(orderParts[1])
                                ? SortOrder.DESC
                                : SortOrder.ASC;
                sourceBuilder.sort(sortField, sortOrder);
            }

            int size = parsedQuery.getLimit() > 0 ? parsedQuery.getLimit() : 1000;
            sourceBuilder.size(size);

            if (!parsedQuery.getFields().isEmpty() && !parsedQuery.getFields().contains("*")) {
                sourceBuilder.fetchSource(parsedQuery.getFields().toArray(new String[0]), null);
            }

            final SearchRequest searchRequest = new SearchRequest(parsedQuery.getIndex());
            searchRequest.source(sourceBuilder);

            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

            List<String> columnNames = new ArrayList<>();
            List<DataType> columnTypes = new ArrayList<>();
            List<List<Object>> rows = new ArrayList<>();
            boolean columnsInitialized = false;

            for (SearchHit hit : response.getHits().getHits()) {
                Map<String, Object> source = hit.getSourceAsMap();
                if (source == null) {
                    source = new HashMap<>();
                }
                if (!columnsInitialized) {
                    for (Map.Entry<String, Object> entry : source.entrySet()) {
                        columnNames.add(entry.getKey());
                        columnTypes.add(inferDataType(entry.getValue()));
                    }
                    columnsInitialized = true;
                }
                List<Object> row = new ArrayList<>();
                for (String columnName : columnNames) {
                    row.add(source.get(columnName));
                }
                rows.add(row);
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.debug(
                    "Elasticsearch query executed in {}ms, returned {} rows", executionTime, rows.size());
            return QueryResult.builder()
                    .columnNames(columnNames)
                    .columnTypes(columnTypes)
                    .rows(rows)
                    .rowCount(rows.size())
                    .executionTimeMs(executionTime)
                    .success(true)
                    .build();
            // CHECKSTYLE:OFF: IllegalCatch
        } catch (final Exception ex) {
            // CHECKSTYLE:ON: IllegalCatch
            log.error("Elasticsearch query execution failed: {}", ex.getMessage(), ex);
            return QueryResult.failure(ex.getMessage());
        }
    }

    private SqlQuery parseSql(final String sql) {
        SqlQuery query = new SqlQuery();
        Matcher matcher = SELECT_PATTERN.matcher(sql.trim());
        if (matcher.find()) {
            String fieldsStr = matcher.group(1).trim();
            if (!"*".equals(fieldsStr)) {
                for (String field : fieldsStr.split(",")) {
                    query.getFields().add(field.trim());
                }
            }
            query.setIndex(matcher.group(2));
            query.setWhereClause(matcher.group(3));
            query.setOrderBy(matcher.group(4));
            if (matcher.group(5) != null) {
                query.setLimit(Integer.parseInt(matcher.group(5)));
            }
        }
        return query;
    }

    private BoolQueryBuilder buildQuery(final String whereClause) {
        BoolQueryBuilder boolBuilder = QueryBuilders.boolQuery();
        String[] conditions = whereClause.split("(?i)\\s+AND\\s+");
        for (String condition : conditions) {
            String trimmedCondition = condition.trim();
            addCondition(boolBuilder, trimmedCondition);
        }
        return boolBuilder;
    }

    private void addCondition(final BoolQueryBuilder boolBuilder, final String condition) {
        String[] parts = condition.split("(?i)\\s+(=|!=|<>|>|<|>=|<=|LIKE|IN)\\s+", 2);
        if (parts.length < 2) {
            return;
        }
        String field = parts[0].trim();
        String operator =
                condition
                        .replaceAll("(?i)^" + field + "\\s+", "")
                        .replaceAll("(?i)\\s+" + parts[1] + "$", "")
                        .trim();
        String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");

        switch (operator.toUpperCase()) {
            case "=":
                boolBuilder.must(new TermQueryBuilder(field, value));
                break;
            case "!=":
            case "<>":
                boolBuilder.mustNot(new TermQueryBuilder(field, value));
                break;
            case ">":
                boolBuilder.must(new RangeQueryBuilder(field).gt(value));
                break;
            case "<":
                boolBuilder.must(new RangeQueryBuilder(field).lt(value));
                break;
            case ">=":
                boolBuilder.must(new RangeQueryBuilder(field).gte(value));
                break;
            case "<=":
                boolBuilder.must(new RangeQueryBuilder(field).lte(value));
                break;
            case "LIKE":
                String wildcard = value.replace("%", "*").replace("_", "?");
                boolBuilder.must(new WildcardQueryBuilder(field, wildcard));
                break;
            case "IN":
                String[] values = value.split(",");
                BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
                for (String v : values) {
                    inQuery.should(new TermQueryBuilder(field, v.trim().replaceAll("^['\"]|['\"]$", "")));
                }
                boolBuilder.must(inQuery);
                break;
            default:
                boolBuilder.must(new TermQueryBuilder(field, value));
        }
    }

    private DataType inferDataType(final Object value) {
        if (value == null) {
            return DataType.STRING;
        }
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return DataType.INTEGER;
        }
        if (value instanceof Long) {
            return DataType.LONG;
        }
        if (value instanceof Double || value instanceof Float) {
            return DataType.DOUBLE;
        }
        if (value instanceof Boolean) {
            return DataType.BOOLEAN;
        }
        if (value instanceof java.util.Date || value instanceof java.time.temporal.Temporal) {
            return DataType.TIMESTAMP;
        }
        if (value instanceof List) {
            return DataType.ARRAY;
        }
        if (value instanceof Map) {
            return DataType.JSON;
        }
        if (value instanceof byte[]) {
            return DataType.BINARY;
        }
        return DataType.STRING;
    }

    private static class SqlQuery {

        private List<String> fields = new ArrayList<>();

        private String index;

        private String whereClause;

        private String orderBy;

        private int limit;

        public List<String> getFields() {
            return fields;
        }

        public void setFields(final List<String> fields) {
            this.fields = fields;
        }

        public String getIndex() {
            return index;
        }

        public void setIndex(final String index) {
            this.index = index;
        }

        public String getWhereClause() {
            return whereClause;
        }

        public void setWhereClause(final String whereClause) {
            this.whereClause = whereClause;
        }

        public String getOrderBy() {
            return orderBy;
        }

        public void setOrderBy(final String orderBy) {
            this.orderBy = orderBy;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(final int limit) {
            this.limit = limit;
        }
    }
}
