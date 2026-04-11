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

package com.intellisql.optimizer.metadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler for providing table statistics to the Calcite optimizer.
 * Integrates with Calcite's RelMetadataQuery system.
 * Reference: Calcite RelMetadataHandler.
 */
@Slf4j
public class StatisticsHandler {

    /** Cache of table statistics by table name. */
    private final Map<String, TableStatistics> statisticsCache = new ConcurrentHashMap<>();

    /** The metadata provider for federated queries. */
    private final FederatedMetadataProvider metadataProvider;

    /**
     * Creates a new StatisticsHandler.
     *
     * @param metadataProvider the federated metadata provider
     */
    public StatisticsHandler(final FederatedMetadataProvider metadataProvider) {
        this.metadataProvider = metadataProvider;
    }

    /**
     * Gets the row count for a table.
     *
     * @param tableScan the table scan node
     * @param mq        the metadata query
     * @return the estimated row count
     */
    public Double getRowCount(final TableScan tableScan, final RelMetadataQuery mq) {
        final String tableName = getTableName(tableScan);
        final TableStatistics stats = getStatistics(tableName);
        if (stats != null) {
            return (double) stats.getRowCount();
        }
        // Default estimate
        return 1000.0;
    }

    /**
     * Gets the average row size for a table.
     *
     * @param tableScan the table scan node
     * @param mq        the metadata query
     * @return the average row size in bytes
     */
    public Double getAverageRowSize(final TableScan tableScan, final RelMetadataQuery mq) {
        final String tableName = getTableName(tableScan);
        final TableStatistics stats = getStatistics(tableName);
        if (stats != null) {
            return stats.getAverageRowSize();
        }
        return 100.0;
    }

    /**
     * Gets the distinct count for a column.
     *
     * @param relNode    the relational node
     * @param columnName the column name
     * @param mq         the metadata query
     * @return the distinct count
     */
    public Double getDistinctCount(final RelNode relNode, final String columnName, final RelMetadataQuery mq) {
        if (relNode instanceof TableScan) {
            final String tableName = getTableName((TableScan) relNode);
            final TableStatistics stats = getStatistics(tableName);
            if (stats != null) {
                return (double) stats.getDistinctCount(columnName);
            }
        }
        // Default estimate based on row count
        final Double rowCount = mq.getRowCount(relNode);
        if (rowCount != null && rowCount > 0) {
            return Math.min(rowCount, 100.0);
        }
        return 10.0;
    }

    /**
     * Gets the selectivity for a predicate.
     *
     * @param relNode the relational node
     * @param mq      the metadata query
     * @return the predicate selectivity
     */
    public Double getSelectivity(final RelNode relNode, final RelMetadataQuery mq) {
        // Default selectivity for unknown predicates
        return 0.15;
    }

    /**
     * Gets statistics for a table.
     *
     * @param tableName the table name
     * @return the table statistics, or null if not found
     */
    public TableStatistics getStatistics(final String tableName) {
        return statisticsCache.computeIfAbsent(tableName, name -> {
            if (metadataProvider != null) {
                return metadataProvider.getTableStatistics(name);
            }
            return null;
        });
    }

    /**
     * Updates statistics for a table.
     *
     * @param tableName  the table name
     * @param statistics the new statistics
     */
    public void updateStatistics(final String tableName, final TableStatistics statistics) {
        statisticsCache.put(tableName, statistics);
        log.debug("Updated statistics for table: {}", tableName);
    }

    /**
     * Clears all cached statistics.
     */
    public void clearCache() {
        statisticsCache.clear();
        log.debug("Cleared statistics cache");
    }

    /**
     * Extracts the table name from a TableScan node.
     *
     * @param tableScan the table scan
     * @return the qualified table name
     */
    private String getTableName(final TableScan tableScan) {
        if (tableScan.getTable() != null) {
            return String.join(".", tableScan.getTable().getQualifiedName());
        }
        return "unknown";
    }
}
