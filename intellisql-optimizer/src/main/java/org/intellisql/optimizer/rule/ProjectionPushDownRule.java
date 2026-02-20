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

package org.intellisql.optimizer.rule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexVisitorImpl;
import org.apache.calcite.tools.RelBuilderFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Optimization rule that pushes SELECT projections down to data sources. This reduces the amount of
 * data transferred by only fetching required columns.
 */
@Slf4j
public class ProjectionPushDownRule extends RelOptRule {

    /** Singleton instance of the projection pushdown rule. */
    public static final ProjectionPushDownRule INSTANCE = new ProjectionPushDownRule();

    /** Constructs a new ProjectionPushDownRule. */
    public ProjectionPushDownRule() {
        super(operand(Project.class, any()), "ProjectionPushDownRule");
    }

    /**
     * Constructs a new ProjectionPushDownRule with a custom RelBuilderFactory.
     *
     * @param factory the relational builder factory
     */
    public ProjectionPushDownRule(final RelBuilderFactory factory) {
        super(operand(Project.class, any()), factory, "ProjectionPushDownRule");
    }

    /**
     * Constructs a new ProjectionPushDownRule with a custom operand.
     *
     * @param operand the rule operand
     * @param description the rule description
     */
    protected ProjectionPushDownRule(final RelOptRuleOperand operand, final String description) {
        super(operand, description);
    }

    /**
     * Checks if this rule matches the given relational expressions.
     *
     * @param call the rule call containing the matched rels
     * @return true if the rule matches and should be applied
     */
    @Override
    public boolean matches(final RelOptRuleCall call) {
        final Project project = call.rel(0);
        if (project.getProjects() == null || project.getProjects().isEmpty()) {
            return false;
        }
        final RelNode input = project.getInput();
        return canPushDown(input);
    }

    /**
     * Determines if projections can be pushed down through the given node.
     *
     * @param relNode the relational node to check
     * @return true if projections can be pushed down
     */
    private boolean canPushDown(final RelNode relNode) {
        if (relNode instanceof TableScan) {
            return true;
        }
        if (relNode instanceof Project) {
            return canPushDown(((Project) relNode).getInput());
        }
        if (relNode instanceof Filter) {
            return canPushDown(((Filter) relNode).getInput());
        }
        return false;
    }

    /**
     * Transforms the matched relational expressions.
     *
     * @param call the rule call containing the matched rels
     */
    @Override
    public void onMatch(final RelOptRuleCall call) {
        final Project project = call.rel(0);
        final RelNode input = project.getInput();
        final Set<Integer> referencedColumns = extractReferencedColumns(project);
        log.debug(
                "Pushing down projection with {} columns to input: {}", referencedColumns.size(), input);
        final RelNode newInput = pushDownProjection(input, referencedColumns);
        final List<RexNode> newProjects = adjustProjectExpressions(project, referencedColumns);
        final Project newProject =
                project.copy(project.getTraitSet(), newInput, newProjects, project.getRowType());
        call.transformTo(newProject);
    }

    /**
     * Extracts the set of column indices referenced by a project.
     *
     * @param project the project to analyze
     * @return the set of referenced column indices
     */
    private Set<Integer> extractReferencedColumns(final Project project) {
        final Set<Integer> columns = new HashSet<>();
        for (final RexNode expr : project.getProjects()) {
            collectInputRefs(expr, columns);
        }
        return columns;
    }

    /**
     * Collects input references from a RexNode expression.
     *
     * @param node the RexNode to analyze
     * @param columns the set to collect column indices into
     */
    private void collectInputRefs(final RexNode node, final Set<Integer> columns) {
        node.accept(
                new RexVisitorImpl<Void>(true) {

                    @Override
                    public Void visitInputRef(final RexInputRef inputRef) {
                        columns.add(inputRef.getIndex());
                        return null;
                    }
                });
    }

    /**
     * Pushes a projection down through a relational node.
     *
     * @param relNode the relational node
     * @param columns the set of column indices to project
     * @return the new relational node with the projection pushed down
     */
    private RelNode pushDownProjection(final RelNode relNode, final Set<Integer> columns) {
        if (relNode instanceof TableScan) {
            return pushDownToTableScan((TableScan) relNode, columns);
        }
        if (relNode instanceof Project) {
            return pushDownThroughProject((Project) relNode, columns);
        }
        if (relNode instanceof Filter) {
            return pushDownThroughFilter((Filter) relNode, columns);
        }
        return relNode;
    }

    /**
     * Pushes a projection down to a table scan.
     *
     * @param tableScan the table scan
     * @param columns the columns to project
     * @return the new table scan with projection
     */
    private RelNode pushDownToTableScan(final TableScan tableScan, final Set<Integer> columns) {
        log.debug(
                "Pushing projection to table scan: {} with {} columns",
                tableScan.getTable().getQualifiedName(),
                columns.size());
        final List<RexNode> projects = new ArrayList<>();
        final List<String> fieldNames = new ArrayList<>();
        final List<org.apache.calcite.rel.type.RelDataType> fieldTypes = new ArrayList<>();
        final List<RelDataTypeField> originalFields = tableScan.getRowType().getFieldList();
        for (final Integer colIdx : columns) {
            projects.add(RexInputRef.of(colIdx, tableScan.getRowType()));
            final RelDataTypeField field = originalFields.get(colIdx);
            fieldNames.add(field.getName());
            fieldTypes.add(field.getType());
        }
        final org.apache.calcite.rel.type.RelDataType newRowType =
                tableScan.getCluster().getTypeFactory().createStructType(fieldTypes, fieldNames);
        return new org.apache.calcite.rel.logical.LogicalProject(
                tableScan.getCluster(), tableScan.getTraitSet(), tableScan, projects, newRowType);
    }

    /**
     * Creates a projected row type based on the selected columns.
     *
     * @param relNode the source relational node
     * @param columns the set of column indices
     * @return the projected row type
     */
    private org.apache.calcite.rel.type.RelDataType createProjectedRowType(
                                                                           final RelNode relNode, final Set<Integer> columns) {
        final List<RelDataTypeField> fields = new ArrayList<>();
        final List<RelDataTypeField> originalFields = relNode.getRowType().getFieldList();
        for (final Integer colIdx : columns) {
            fields.add(originalFields.get(colIdx));
        }
        final List<String> names = new ArrayList<>();
        final List<org.apache.calcite.rel.type.RelDataType> types = new ArrayList<>();
        for (final RelDataTypeField field : fields) {
            names.add(field.getName());
            types.add(field.getType());
        }
        return relNode.getCluster().getTypeFactory().createStructType(types, names);
    }

    /**
     * Pushes a projection through an existing project.
     *
     * @param project the existing project
     * @param columns the columns to project
     * @return the new relational node
     */
    private RelNode pushDownThroughProject(final Project project, final Set<Integer> columns) {
        final Set<Integer> adjustedColumns = new HashSet<>();
        for (final Integer colIdx : columns) {
            final RexNode expr = project.getProjects().get(colIdx);
            collectInputRefs(expr, adjustedColumns);
        }
        final RelNode pushedInput = pushDownProjection(project.getInput(), adjustedColumns);
        return project.copy(
                project.getTraitSet(), pushedInput, project.getProjects(), project.getRowType());
    }

    /**
     * Pushes a projection through a filter.
     *
     * @param filter the filter
     * @param columns the columns to project
     * @return the new relational node
     */
    private RelNode pushDownThroughFilter(final Filter filter, final Set<Integer> columns) {
        final Set<Integer> filterColumns = new HashSet<>(columns);
        collectInputRefs(filter.getCondition(), filterColumns);
        final RelNode pushedInput = pushDownProjection(filter.getInput(), filterColumns);
        return filter.copy(filter.getTraitSet(), pushedInput, filter.getCondition());
    }

    /**
     * Adjusts project expressions after pushdown.
     *
     * @param project the original project
     * @param columns the pushed columns
     * @return the adjusted project expressions
     */
    private List<RexNode> adjustProjectExpressions(
                                                   final Project project, final Set<Integer> columns) {
        return new ArrayList<>(project.getProjects());
    }
}
