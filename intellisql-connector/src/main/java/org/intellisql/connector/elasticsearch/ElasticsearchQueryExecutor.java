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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intellisql.connector.enums.DataType;
import org.intellisql.connector.model.QueryResult;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch implementation of QueryExecutor. Converts SQL-like queries to Elasticsearch Query
 * DSL and executes them.
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
     * @param client the Elasticsearch client
     * @param sql the SQL string
     * @return the query result
     * @throws Exception if execution fails
     */
    public QueryResult executeQuery(final ElasticsearchClient client, final String sql) throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            SqlQuery parsedQuery = parseSql(sql);
            SearchRequest.Builder searchBuilder = new SearchRequest.Builder().index(parsedQuery.getIndex());
            if (parsedQuery.getWhereClause() != null && !parsedQuery.getWhereClause().isEmpty()) {
                Query query = buildQuery(parsedQuery.getWhereClause());
                searchBuilder.query(query);
            }
            if (parsedQuery.getOrderBy() != null && !parsedQuery.getOrderBy().isEmpty()) {
                String[] orderParts = parsedQuery.getOrderBy().trim().split("\\s+");
                String sortField = orderParts[0];
                SortOrder sortOrder =
                        orderParts.length > 1 && "DESC".equalsIgnoreCase(orderParts[1])
                                ? SortOrder.Desc
                                : SortOrder.Asc;
                searchBuilder.sort(s -> s.field(f -> f.field(sortField).order(sortOrder)));
            }
            if (parsedQuery.getLimit() > 0) {
                searchBuilder.size(parsedQuery.getLimit());
            } else {
                searchBuilder.size(1000);
            }
            if (!parsedQuery.getFields().isEmpty() && !parsedQuery.getFields().contains("*")) {
                searchBuilder.source(s -> s.filter(f -> f.includes(parsedQuery.getFields())));
            }
            SearchResponse<Map> response = client.search(searchBuilder.build(), Map.class);
            List<String> columnNames = new ArrayList<>();
            List<DataType> columnTypes = new ArrayList<>();
            List<List<Object>> rows = new ArrayList<>();
            boolean columnsInitialized = false;
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
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

    private Query buildQuery(final String whereClause) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        String[] conditions = whereClause.split("(?i)\\s+AND\\s+");
        for (String condition : conditions) {
            String trimmedCondition = condition.trim();
            Query termQuery = parseCondition(trimmedCondition);
            if (termQuery != null) {
                boolBuilder.must(termQuery);
            }
        }
        return boolBuilder.build()._toQuery();
    }

    private Query parseCondition(final String condition) {
        String[] parts = condition.split("(?i)\\s+(=|!=|<>|>|<|>=|<=|LIKE|IN)\\s+", 2);
        if (parts.length < 2) {
            return null;
        }
        String field = parts[0].trim();
        String operator =
                condition
                        .replaceAll("(?i)^" + field + "\\s+", "")
                        .replaceAll("(?i)\\s+" + parts[1] + "$", "")
                        .trim();
        String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");
        if (isNestedField(field)) {
            return buildNestedQuery(field, operator, value);
        }
        switch (operator.toUpperCase()) {
            case "=":
                return Query.of(q -> q.term(t -> t.field(field).value(value)));
            case "!=":
            case "<>":
                return Query.of(q -> q.bool(b -> b.mustNot(m -> m.term(t -> t.field(field).value(value)))));
            case ">":
                return Query.of(q -> q.range(r -> r.field(field).gt(JsonData.of(value))));
            case "<":
                return Query.of(q -> q.range(r -> r.field(field).lt(JsonData.of(value))));
            case ">=":
                return Query.of(q -> q.range(r -> r.field(field).gte(JsonData.of(value))));
            case "<=":
                return Query.of(q -> q.range(r -> r.field(field).lte(JsonData.of(value))));
            case "LIKE":
                String wildcard = value.replace("%", "*").replace("_", "?");
                return Query.of(q -> q.wildcard(w -> w.field(field).value(wildcard)));
            case "IN":
                String[] values = value.split(",");
                List<FieldValue> fieldValues = new ArrayList<>();
                for (String v : values) {
                    fieldValues.add(FieldValue.of(v.trim().replaceAll("^['\"]|['\"]$", "")));
                }
                return Query.of(
                        q -> q.terms(
                                t -> t.field(field)
                                        .terms(TermsQueryField.of(f -> f.value(fieldValues)))));
            default:
                return Query.of(q -> q.term(t -> t.field(field).value(value)));
        }
    }

    private boolean isNestedField(final String field) {
        return NESTED_FIELD_PATTERN.matcher(field).matches();
    }

    private Query buildNestedQuery(final String field, final String operator, final String value) {
        Matcher matcher = NESTED_FIELD_PATTERN.matcher(field);
        if (matcher.find()) {
            String path = matcher.group(1);
            return Query.of(
                    q -> q.nested(
                            n -> n.path(path)
                                    .query(parseCondition(matcher.group(2) + " " + operator + " " + value))));
        }
        return null;
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
