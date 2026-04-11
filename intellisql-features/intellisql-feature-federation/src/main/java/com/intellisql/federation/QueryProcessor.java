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

package com.intellisql.federation;

import com.intellisql.connector.ConnectorRegistry;
import com.intellisql.connector.api.DataSourceConnector;
import com.intellisql.connector.config.DataSourceConfigs;
import com.intellisql.connector.model.QueryResult;
import com.intellisql.common.config.Props;
import com.intellisql.federation.converter.RelConverter;
import com.intellisql.common.logger.QueryContext;
import com.intellisql.common.logger.QueryContextManager;
import com.intellisql.common.logger.StructuredLogger;
import com.intellisql.federation.metadata.MetadataManager;
import com.intellisql.common.metadata.enums.DataSourceType;
import com.intellisql.common.retry.ExponentialBackoffRetry;
import com.intellisql.common.retry.RetryableOperation;
import com.intellisql.optimizer.HybridOptimizer;
import com.intellisql.optimizer.plan.ExecutionPlan;
import com.intellisql.parser.SqlParserFactory;
import com.intellisql.parser.SqlNodeToStringConverter;

import lombok.extern.slf4j.Slf4j;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;

/**
 * Query processor that orchestrates SQL parsing, optimization, and execution. Coordinates the
 * entire query processing pipeline.
 */
@Slf4j
public class QueryProcessor {

    private final DataSourceManager dataSourceManager;

    private final MetadataManager metadataManager;

    private final HybridOptimizer optimizer;

    private final Props props;

    private final StructuredLogger structuredLogger = StructuredLogger.getLogger(QueryProcessor.class);

    private final ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry();

    /** Lazily initialized RelConverter - created on first use after metadata is initialized. */
    private volatile RelConverter relConverter;

    /**
     * Creates a new QueryProcessor.
     *
     * @param dataSourceManager the data source manager
     * @param metadataManager the metadata manager
     * @param optimizer the hybrid optimizer
     * @param props the configuration properties
     */
    public QueryProcessor(
                          final DataSourceManager dataSourceManager,
                          final MetadataManager metadataManager,
                          final HybridOptimizer optimizer,
                          final Props props) {
        this.dataSourceManager = dataSourceManager;
        this.metadataManager = metadataManager;
        this.optimizer = optimizer;
        this.props = props;
        // RelConverter is lazily created on first use to ensure metadata is initialized
    }

    /**
     * Gets the RelConverter, creating it lazily if needed.
     * The RelConverter creates its own VolcanoPlanner, ensuring RelNodes
     * are associated with the correct planner for optimization.
     *
     * @return the RelConverter instance
     */
    private RelConverter getRelConverter() {
        if (relConverter == null) {
            synchronized (this) {
                if (relConverter == null) {
                    log.info("Creating RelConverter with {} tables in schema",
                            metadataManager.getAllTables().size());
                    // RelConverter creates its own planner/cluster
                    // The optimizer will get the planner from the RelNode's cluster
                    relConverter = new RelConverter(metadataManager.getRootSchema());
                }
            }
        }
        return relConverter;
    }

    /**
     * Processes a SQL query and returns the result.
     *
     * @param sql the SQL query to process
     * @param context the query context
     * @return the query result
     */
    public QueryResult process(final String sql, final QueryContext context) {
        final long startTime = System.currentTimeMillis();
        QueryContextManager.setContext(context);
        try {
            structuredLogger.info(context, "Processing query: {}", sql);
            final SqlNode parsedSql = parseSQL(sql, context);
            final RelNode logicalPlan = convertToRelational(parsedSql, context);
            final RelNode optimizedPlan = optimizer.optimize(logicalPlan);
            final ExecutionPlan executionPlan =
                    optimizer.generateExecutionPlan(optimizedPlan, context.getQueryId());
            final QueryResult result = executeWithRetry(executionPlan, context);
            final long duration = System.currentTimeMillis() - startTime;
            structuredLogger.info(
                    context, "Query completed in {}ms, rows={}", duration, result.getRowCount());
            return result;
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            final long duration = System.currentTimeMillis() - startTime;
            ex.printStackTrace();
            structuredLogger.error(context, "Query failed after {}ms: {}", duration, ex.getMessage());
            return QueryResult.failure("Query execution failed: " + ex.getMessage());
        } finally {
            QueryContextManager.clearContext();
        }
    }

    /**
     * Parses SQL into a SqlNode AST.
     *
     * @param sql the SQL to parse
     * @param context the query context
     * @return the parsed SqlNode
     * @throws RuntimeException if SQL parsing fails
     */
    private SqlNode parseSQL(final String sql, final QueryContext context) {
        structuredLogger.debug(context, "Parsing SQL");
        try {
            return SqlParserFactory.parseWithBabel(sql);
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            structuredLogger.error(context, "SQL parsing failed: {}", ex.getMessage());
            throw new RuntimeException("Failed to parse SQL: " + ex.getMessage(), ex);
        }
    }

    /**
     * Converts a SqlNode to a relational plan.
     * Uses direct SqlValidator and SqlToRelConverter instead of Frameworks.
     * Reference: ShardingSphere SQLFederationRelConverter.
     *
     * @param sqlNode the parsed SQL node
     * @param context the query context
     * @return the relational plan
     * @throws RuntimeException if conversion fails
     */
    private RelNode convertToRelational(
                                        final SqlNode sqlNode, final QueryContext context) {
        structuredLogger.debug(context, "Converting SQL to relational plan using RelConverter");
        try {
            // Convert SQL to RelNode using the direct approach (no Frameworks)
            // This follows ShardingSphere's SQLFederationRelConverter pattern
            // Use getRelConverter() to ensure lazy initialization after metadata is loaded
            final RelRoot relRoot = getRelConverter().convertQuery(sqlNode, true, true);
            return relRoot.rel;
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            structuredLogger.error(context, "Failed to convert to relational plan: {}", ex.getMessage());
            throw new RuntimeException("Failed to convert SQL to relational plan: " + ex.getMessage(), ex);
        }
    }

    /**
     * Executes the query plan with retry logic.
     *
     * @param executionPlan the execution plan
     * @param context the query context
     * @return the query result
     */
    private QueryResult executeWithRetry(
                                         final ExecutionPlan executionPlan, final QueryContext context) {
        structuredLogger.debug(context, "Executing query plan with retry policy");
        try {
            return retryPolicy.execute(
                    (RetryableOperation<QueryResult>) () -> executePlan(executionPlan, context));
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            structuredLogger.error(context, "Query execution failed after retries: {}", ex.getMessage());
            return QueryResult.failure("Execution failed after retries: " + ex.getMessage());
        }
    }

    /**
     * Executes the query plan.
     *
     * @param executionPlan the execution plan
     * @param context the query context
     * @return the query result
     */
    private QueryResult executePlan(final ExecutionPlan executionPlan, final QueryContext context) {
        structuredLogger.debug(
                context, "Executing execution plan with {} stages", executionPlan.getStages().size());
        QueryResult result =
                QueryResult.success(java.util.Collections.emptyList(), java.util.Collections.emptyList());
        for (final com.intellisql.optimizer.plan.ExecutionStage stage : executionPlan.getStages()) {
            result = executeStage(stage, context, result);
            if (!result.isSuccess()) {
                return result;
            }
        }
        return result;
    }

    /**
     * Executes a single stage of the execution plan.
     *
     * @param stage the execution stage
     * @param context the query context
     * @param previousResult the result from the previous stage
     * @return the query result
     */
    private QueryResult executeStage(
                                     final com.intellisql.optimizer.plan.ExecutionStage stage,
                                     final QueryContext context,
                                     final QueryResult previousResult) {
        structuredLogger.debug(context, "Executing stage: {}", stage.getId());
        final String dataSourceId = stage.getDataSourceId();
        if (dataSourceId == null || "default".equals(dataSourceId)) {
            return previousResult;
        }
        try {
            final DataSourceType dataSourceType = determineDataSourceType(dataSourceId);
            final DataSourceConnector connector =
                    ConnectorRegistry.getInstance().getConnector(dataSourceType);
            final com.intellisql.connector.api.Connection connection =
                    getConnection(connector, dataSourceId);
            final String targetSql = generateTargetSQL(stage, dataSourceType);
            return connection.executeQuery(targetSql);
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            ex.printStackTrace();
            // CHECKSTYLE:ON
            structuredLogger.error(context, "Stage execution failed: {}", ex.getMessage());
            return QueryResult.failure("Stage execution failed: " + ex.getMessage());
        }
    }

    /**
     * Determines the data source type from a data source ID.
     *
     * @param dataSourceId the data source identifier
     * @return the data source type
     */
    private DataSourceType determineDataSourceType(final String dataSourceId) {
        final String normalizedName = dataSourceId.replaceAll("[\\[\\]]", "").split(",")[0];
        if (dataSourceManager.hasDataSource(normalizedName)) {
            return dataSourceManager.getDataSourceConfig(normalizedName).getType();
        }
        return DataSourceType.MYSQL;
    }

    /**
     * Gets a connection to the specified data source.
     *
     * @param connector the data source connector
     * @param dataSourceId the data source identifier
     * @return the connection
     * @throws Exception if connection fails
     */
    private com.intellisql.connector.api.Connection getConnection(
                                                                  final DataSourceConnector connector, final String dataSourceId) throws Exception {
        final String normalizedName = dataSourceId.replaceAll("[\\[\\]]", "").split(",")[0];
        final com.intellisql.common.config.DataSourceConfig config =
                dataSourceManager.getDataSourceConfig(normalizedName);
        final com.intellisql.connector.config.DataSourceConfig connectorConfig =
                DataSourceConfigs.fromCommonConfig(normalizedName, config);
        return connector.connect(connectorConfig);
    }

    /**
     * Generates target SQL for a specific data source type.
     *
     * @param stage the execution stage
     * @param dataSourceType the target data source type
     * @return the generated SQL
     */
    private String generateTargetSQL(
                                     final com.intellisql.optimizer.plan.ExecutionStage stage,
                                     final DataSourceType dataSourceType) {
        final String targetDialect = toSqlDialect(dataSourceType);
        final RelNode operation = stage.getOperation();
        if (operation == null) {
            return "SELECT 1";
        }
        final SqlDialect calciteDialect = SqlNodeToStringConverter.getCalciteDialect(targetDialect);
        final RelToSqlConverter converter = new RelToSqlConverter(calciteDialect);
        return converter.visitRoot(operation).asStatement().toSqlString(calciteDialect).getSql();
    }

    /**
     * Converts DataSourceType to SqlDialect.
     *
     * @param dataSourceType the data source type
     * @return the SQL dialect
     */
    private String toSqlDialect(final DataSourceType dataSourceType) {
        switch (dataSourceType) {
            case MYSQL:
                return "MYSQL";
            case POSTGRESQL:
                return "POSTGRESQL";
            case ELASTICSEARCH:
                return "STANDARD";
            default:
                return "STANDARD";
        }
    }
}
