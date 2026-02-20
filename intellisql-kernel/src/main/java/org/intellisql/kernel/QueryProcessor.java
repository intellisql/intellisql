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

package org.intellisql.kernel;

import org.apache.calcite.sql.SqlNode;
import org.intellisql.connector.ConnectorRegistry;
import org.intellisql.connector.api.DataSourceConnector;
import org.intellisql.connector.model.QueryResult;
import org.intellisql.kernel.config.Props;
import org.intellisql.kernel.logger.QueryContext;
import org.intellisql.kernel.logger.QueryContextManager;
import org.intellisql.kernel.logger.StructuredLogger;
import org.intellisql.kernel.metadata.MetadataManager;
import org.intellisql.kernel.metadata.enums.DataSourceType;
import org.intellisql.kernel.retry.ExponentialBackoffRetry;
import org.intellisql.kernel.retry.RetryableOperation;
import org.intellisql.optimizer.Optimizer;
import org.intellisql.optimizer.plan.ExecutionPlan;
import org.intellisql.parser.SqlParserFactory;
import org.intellisql.parser.dialect.SqlDialect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query processor that orchestrates SQL parsing, optimization, and execution. Coordinates the
 * entire query processing pipeline.
 */
@Slf4j
@RequiredArgsConstructor
public class QueryProcessor {

    private final DataSourceManager dataSourceManager;

    private final MetadataManager metadataManager;

    private final Optimizer optimizer;

    private final Props props;

    private final StructuredLogger structuredLogger = StructuredLogger.getLogger(QueryProcessor.class);

    private final ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry();

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
            final org.apache.calcite.rel.RelNode logicalPlan = convertToRelational(parsedSql, context);
            final org.apache.calcite.rel.RelNode optimizedPlan = optimizer.optimize(logicalPlan);
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
     *
     * @param sqlNode the parsed SQL node
     * @param context the query context
     * @return the relational plan
     * @throws RuntimeException if conversion fails
     */
    private org.apache.calcite.rel.RelNode convertToRelational(
                                                               final SqlNode sqlNode, final QueryContext context) {
        structuredLogger.debug(context, "Converting SQL to relational plan");
        org.apache.calcite.tools.Planner planner = null;
        try {
            final org.apache.calcite.tools.FrameworkConfig frameworkConfig = buildFrameworkConfig();
            planner = org.apache.calcite.tools.Frameworks.getPlanner(frameworkConfig);
            return planner.rel(sqlNode).project();
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            structuredLogger.error(context, "Failed to convert to relational plan: {}", ex.getMessage());
            throw new RuntimeException("Failed to convert SQL to relational plan: " + ex.getMessage(), ex);
        } finally {
            if (planner != null) {
                try {
                    planner.close();
                    // CHECKSTYLE:OFF IllegalCatch
                } catch (final Exception ex) {
                    // CHECKSTYLE:ON
                    // Ignore close errors
                }
            }
        }
    }

    /**
     * Builds the Calcite framework configuration.
     *
     * @return the framework configuration
     */
    private org.apache.calcite.tools.FrameworkConfig buildFrameworkConfig() {
        return org.apache.calcite.tools.Frameworks.newConfigBuilder()
                .defaultSchema(metadataManager.getRootSchema())
                .build();
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
        for (final org.intellisql.optimizer.plan.ExecutionStage stage : executionPlan.getStages()) {
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
                                     final org.intellisql.optimizer.plan.ExecutionStage stage,
                                     final QueryContext context,
                                     final QueryResult previousResult) {
        structuredLogger.debug(context, "Executing stage: {}", stage.getId());
        final String dataSourceId = stage.getDataSourceId();
        if (dataSourceId == null || "default".equals(dataSourceId)) {
            return previousResult;
        }
        try {
            final DataSourceType dataSourceType = determineDataSourceType(dataSourceId);
            final org.intellisql.connector.enums.DataSourceType connectorType =
                    org.intellisql.connector.enums.DataSourceType.valueOf(dataSourceType.name());
            final DataSourceConnector connector =
                    ConnectorRegistry.getInstance().getConnector(connectorType);
            final org.intellisql.connector.api.Connection connection =
                    getConnection(connector, dataSourceId);
            final String targetSql = generateTargetSQL(stage, dataSourceType);
            return connection.executeQuery(targetSql);
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
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
    private org.intellisql.connector.api.Connection getConnection(
                                                                  final DataSourceConnector connector, final String dataSourceId) throws Exception {
        final String normalizedName = dataSourceId.replaceAll("[\\[\\]]", "").split(",")[0];
        final org.intellisql.kernel.config.DataSourceConfig config =
                dataSourceManager.getDataSourceConfig(normalizedName);
        final org.intellisql.connector.config.DataSourceConfig connectorConfig = convertConfig(config);
        return connector.connect(connectorConfig);
    }

    /**
     * Converts kernel DataSourceConfig to connector DataSourceConfig.
     *
     * @param kernelConfig the kernel configuration
     * @return the connector configuration
     */
    private org.intellisql.connector.config.DataSourceConfig convertConfig(
                                                                           final org.intellisql.kernel.config.DataSourceConfig kernelConfig) {
        return org.intellisql.connector.config.DataSourceConfig.builder()
                .type(org.intellisql.connector.enums.DataSourceType.valueOf(kernelConfig.getType().name()))
                .jdbcUrl(kernelConfig.getUrl())
                .username(kernelConfig.getUsername())
                .password(kernelConfig.getPassword())
                .maxPoolSize(kernelConfig.getConnectionPool().getMaximumPoolSize())
                .minIdle(kernelConfig.getConnectionPool().getMinimumIdle())
                .connectionTimeout(kernelConfig.getConnectionPool().getConnectionTimeout())
                .idleTimeout(kernelConfig.getConnectionPool().getIdleTimeout())
                .maxLifetime(kernelConfig.getConnectionPool().getMaxLifetime())
                .build();
    }

    /**
     * Generates target SQL for a specific data source type.
     *
     * @param stage the execution stage
     * @param dataSourceType the target data source type
     * @return the generated SQL
     */
    private String generateTargetSQL(
                                     final org.intellisql.optimizer.plan.ExecutionStage stage,
                                     final DataSourceType dataSourceType) {
        final SqlDialect targetDialect = toSqlDialect(dataSourceType);
        final org.apache.calcite.rel.RelNode operation = stage.getOperation();
        if (operation == null) {
            return "SELECT 1";
        }
        final org.apache.calcite.sql.SqlDialect calciteDialect = toCalciteDialect(targetDialect);
        final org.apache.calcite.rel.rel2sql.RelToSqlConverter converter =
                new org.apache.calcite.rel.rel2sql.RelToSqlConverter(calciteDialect);
        return converter.visitRoot(operation).asStatement().toSqlString(calciteDialect).getSql();
    }

    /**
     * Converts DataSourceType to SqlDialect.
     *
     * @param dataSourceType the data source type
     * @return the SQL dialect
     */
    private SqlDialect toSqlDialect(final DataSourceType dataSourceType) {
        switch (dataSourceType) {
            case MYSQL:
                return SqlDialect.MYSQL;
            case POSTGRESQL:
                return SqlDialect.POSTGRESQL;
            case ELASTICSEARCH:
                return SqlDialect.STANDARD;
            default:
                return SqlDialect.STANDARD;
        }
    }

    /**
     * Converts SqlDialect to Calcite's SqlDialect.
     *
     * @param dialect the SQL dialect
     * @return the Calcite SQL dialect
     */
    private org.apache.calcite.sql.SqlDialect toCalciteDialect(final SqlDialect dialect) {
        switch (dialect) {
            case MYSQL:
                return org.apache.calcite.sql.dialect.MysqlSqlDialect.DEFAULT;
            case POSTGRESQL:
                return org.apache.calcite.sql.dialect.PostgresqlSqlDialect.DEFAULT;
            case ORACLE:
                return org.apache.calcite.sql.dialect.OracleSqlDialect.DEFAULT;
            case SQLSERVER:
                return org.apache.calcite.sql.dialect.MssqlSqlDialect.DEFAULT;
            case HIVE:
                return org.apache.calcite.sql.dialect.HiveSqlDialect.DEFAULT;
            case STANDARD:
            default:
                return org.apache.calcite.sql.dialect.AnsiSqlDialect.DEFAULT;
        }
    }
}
