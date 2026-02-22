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

package com.intellisql.kernel.converter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.config.CalciteConnectionConfigImpl;
import org.apache.calcite.config.NullCollation;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.plan.ConventionTraitDef;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.volcano.VolcanoPlanner;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.util.SqlOperatorTables;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.calcite.sql2rel.SqlToRelConverter;
import org.apache.calcite.sql2rel.StandardConvertletTable;
import com.intellisql.kernel.metadata.calcite.FederatedCatalogReader;
import com.intellisql.optimizer.cost.FederatedCostFactory;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Relational expression converter.
 * Converts SQL AST (SqlNode) to relational algebra (RelNode).
 * Creates its own RelOptCluster with VolcanoPlanner (like ShardingSphere does).
 * Reference: ShardingSphere SQLFederationRelConverter.
 */
@Slf4j
@Getter
public final class RelConverter {

    private final SqlToRelConverter sqlToRelConverter;

    private final SqlValidator sqlValidator;

    private final RelOptCluster relOptCluster;

    private final FederatedCatalogReader catalogReader;

    private final RelDataTypeFactory typeFactory;

    /**
     * Creates a new RelConverter with its own planner.
     * This follows ShardingSphere's approach where the converter creates its own
     * RelOptCluster with VolcanoPlanner, ensuring RelNodes are created with
     * the planner that will be used for optimization.
     *
     * @param rootSchema the root schema
     */
    public RelConverter(final SchemaPlus rootSchema) {
        this.typeFactory = new JavaTypeFactoryImpl(RelDataTypeSystem.DEFAULT);
        final CalciteConnectionConfig connectionConfig = createConnectionConfig();
        this.catalogReader = createCatalogReader(rootSchema, typeFactory, connectionConfig);
        this.sqlValidator = createSqlValidator(catalogReader, typeFactory, connectionConfig);
        this.relOptCluster = createRelOptCluster(typeFactory);
        this.sqlToRelConverter = createSqlToRelConverter(catalogReader, sqlValidator, relOptCluster);
        log.info("RelConverter created with own VolcanoPlanner");
    }

    private CalciteConnectionConfig createConnectionConfig() {
        final Properties properties = new Properties();
        properties.setProperty("defaultNullCollation", NullCollation.HIGH.name());
        properties.setProperty("conformance", SqlConformanceEnum.DEFAULT.name());
        properties.setProperty("caseSensitive", "false");
        return new CalciteConnectionConfigImpl(properties);
    }

    private FederatedCatalogReader createCatalogReader(
                                                       final SchemaPlus rootSchema,
                                                       final RelDataTypeFactory typeFactory,
                                                       final CalciteConnectionConfig connectionConfig) {
        final List<String> defaultSchemaPath = Collections.emptyList();
        return new FederatedCatalogReader(
                CalciteSchema.from(rootSchema),
                defaultSchemaPath,
                typeFactory,
                connectionConfig);
    }

    private SqlValidator createSqlValidator(
                                            final FederatedCatalogReader catalogReader,
                                            final RelDataTypeFactory typeFactory,
                                            final CalciteConnectionConfig connectionConfig) {
        final SqlOperatorTable operatorTable = SqlOperatorTables.chain(
                SqlStdOperatorTable.instance(),
                catalogReader);
        final SqlValidator.Config validatorConfig = SqlValidator.Config.DEFAULT
                .withLenientOperatorLookup(connectionConfig.lenientOperatorLookup())
                .withConformance(connectionConfig.conformance())
                .withDefaultNullCollation(connectionConfig.defaultNullCollation())
                .withIdentifierExpansion(true);
        return SqlValidatorUtil.newValidator(operatorTable, catalogReader, typeFactory, validatorConfig);
    }

    /**
     * Creates RelOptCluster with VolcanoPlanner.
     * Registers CBO rules with the planner during creation.
     * Reference: ShardingSphere SQLFederationRelConverter.createRelOptCluster().
     *
     * @param typeFactory the type factory for creating the RexBuilder
     * @return the created RelOptCluster with VolcanoPlanner
     */
    private RelOptCluster createRelOptCluster(final RelDataTypeFactory typeFactory) {
        // Create VolcanoPlanner with federated cost factory
        final VolcanoPlanner planner = new VolcanoPlanner(FederatedCostFactory.INSTANCE, null);
        // Register ConventionTraitDef to avoid NPE when getting Convention trait
        planner.addRelTraitDef(ConventionTraitDef.INSTANCE);
        // Register CBO rules (like ShardingSphere does in buildVolcanoPlanner)
        registerCboRules(planner);
        return RelOptCluster.create(planner, new RexBuilder(typeFactory));
    }

    /**
     * Registers CBO optimization rules with the planner.
     * Reference: ShardingSphere SQLFederationPlannerBuilder.
     *
     * @param planner the VolcanoPlanner to register rules with
     */
    private void registerCboRules(final VolcanoPlanner planner) {
        // Join optimization rules
        planner.addRule(org.apache.calcite.rel.rules.CoreRules.JOIN_TO_MULTI_JOIN);
        planner.addRule(org.apache.calcite.rel.rules.CoreRules.MULTI_JOIN_OPTIMIZE_BUSHY);
        planner.addRule(org.apache.calcite.rel.rules.CoreRules.FILTER_INTO_JOIN);
        planner.addRule(org.apache.calcite.rel.rules.CoreRules.JOIN_PUSH_EXPRESSIONS);
        // Project and filter rules
        planner.addRule(org.apache.calcite.rel.rules.CoreRules.PROJECT_MERGE);
        planner.addRule(org.apache.calcite.rel.rules.CoreRules.PROJECT_REMOVE);
        planner.addRule(org.apache.calcite.rel.rules.CoreRules.FILTER_MERGE);
        // Aggregate rules
        planner.addRule(org.apache.calcite.rel.rules.CoreRules.AGGREGATE_REDUCE_FUNCTIONS);
        planner.addRule(org.apache.calcite.rel.rules.CoreRules.AGGREGATE_PROJECT_MERGE);
        planner.addRule(org.apache.calcite.rel.rules.CoreRules.AGGREGATE_JOIN_TRANSPOSE);
        // Sort rules
        planner.addRule(org.apache.calcite.rel.rules.CoreRules.SORT_REMOVE);
        planner.addRule(org.apache.calcite.rel.rules.CoreRules.SORT_JOIN_TRANSPOSE);
        log.debug("Registered {} CBO rules", 12);
    }

    private SqlToRelConverter createSqlToRelConverter(
                                                      final FederatedCatalogReader catalogReader,
                                                      final SqlValidator validator,
                                                      final RelOptCluster cluster) {
        final RelOptTable.ViewExpander viewExpander = (rowType, queryString, schemaPath, viewPath) -> null;
        final SqlToRelConverter.Config converterConfig = SqlToRelConverter.config()
                .withTrimUnusedFields(true)
                .withRemoveSortInSubQuery(false)
                .withExpand(false);
        return new SqlToRelConverter(
                viewExpander,
                validator,
                catalogReader,
                cluster,
                StandardConvertletTable.INSTANCE,
                converterConfig);
    }

    /**
     * Converts a SQL query to a relational expression.
     *
     * @param sqlNode the parsed SQL node
     * @param needsValidation whether validation is needed
     * @param top whether this is a top-level query
     * @return the relational root
     */
    public RelRoot convertQuery(final SqlNode sqlNode, final boolean needsValidation, final boolean top) {
        return sqlToRelConverter.convertQuery(sqlNode, needsValidation, top);
    }

    /**
     * Gets the validated node type.
     *
     * @param sqlNode the SQL node
     * @return the validated node type
     */
    public RelDataType getValidatedNodeType(final SqlNode sqlNode) {
        return sqlValidator.getValidatedNodeType(sqlNode);
    }

    /**
     * Validates a SQL node.
     *
     * @param sqlNode the SQL node to validate
     * @return the validated SQL node
     */
    public SqlNode validate(final SqlNode sqlNode) {
        return sqlValidator.validate(sqlNode);
    }

    /**
     * Gets the RelOptCluster used by this converter.
     * The cluster contains the planner that will be used for optimization.
     *
     * @return the cluster
     */
    public RelOptCluster getCluster() {
        return relOptCluster;
    }

    /**
     * Gets the planner from the cluster.
     * This should be used for CBO optimization.
     *
     * @return the planner
     */
    public RelOptPlanner getPlanner() {
        return relOptCluster.getPlanner();
    }
}
