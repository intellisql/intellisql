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

package com.intellisql.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.avatica.AvaticaParameter;
import org.apache.calcite.avatica.ColumnMetaData;
import org.apache.calcite.avatica.Meta;

/** JDBC DatabaseMetaData implementation for IntelliSql. */
@Slf4j
@Getter
public class IntelliSqlDatabaseMetaData implements DatabaseMetaData {

    private static final String PRODUCT_NAME = "IntelliSql";

    private static final String PRODUCT_VERSION = "1.0.0";

    private static final int DRIVER_MAJOR_VERSION = 1;

    private static final int DRIVER_MINOR_VERSION = 0;

    private final IntelliSqlConnection connection;

    /**
     * Creates a new metadata object.
     *
     * @param connection the parent connection
     */
    public IntelliSqlDatabaseMetaData(final IntelliSqlConnection connection) {
        this.connection = connection;
    }

    @Override
    public boolean allProceduresAreCallable() throws SQLException {
        return false;
    }

    @Override
    public boolean allTablesAreSelectable() throws SQLException {
        return true;
    }

    @Override
    public String getURL() throws SQLException {
        return "jdbc:intellisql://"
                + connection.getUrlParser().getEndpoint()
                + "/"
                + connection.getUrlParser().getDatabase();
    }

    @Override
    public String getUserName() throws SQLException {
        return connection.getProperties().getProperty("user", "anonymous");
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return false;
    }

    @Override
    public boolean nullsAreSortedHigh() throws SQLException {
        return false;
    }

    @Override
    public boolean nullsAreSortedLow() throws SQLException {
        return true;
    }

    @Override
    public boolean nullsAreSortedAtStart() throws SQLException {
        return false;
    }

    @Override
    public boolean nullsAreSortedAtEnd() throws SQLException {
        return true;
    }

    @Override
    public String getDatabaseProductName() throws SQLException {
        return PRODUCT_NAME;
    }

    @Override
    public String getDatabaseProductVersion() throws SQLException {
        return PRODUCT_VERSION;
    }

    @Override
    public String getDriverName() throws SQLException {
        return "IntelliSql JDBC Driver";
    }

    @Override
    public String getDriverVersion() throws SQLException {
        return DRIVER_MAJOR_VERSION + "." + DRIVER_MINOR_VERSION;
    }

    @Override
    public int getDriverMajorVersion() {
        return DRIVER_MAJOR_VERSION;
    }

    @Override
    public int getDriverMinorVersion() {
        return DRIVER_MINOR_VERSION;
    }

    @Override
    public boolean usesLocalFiles() throws SQLException {
        return false;
    }

    @Override
    public boolean usesLocalFilePerTable() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        return false;
    }

    @Override
    public boolean storesUpperCaseIdentifiers() throws SQLException {
        return false;
    }

    @Override
    public boolean storesLowerCaseIdentifiers() throws SQLException {
        return true;
    }

    @Override
    public boolean storesMixedCaseIdentifiers() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        return true;
    }

    @Override
    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        return false;
    }

    @Override
    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        return false;
    }

    @Override
    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        return true;
    }

    @Override
    public String getIdentifierQuoteString() throws SQLException {
        return "`";
    }

    @Override
    public String getSQLKeywords() throws SQLException {
        return "LIMIT,OFFSET";
    }

    @Override
    public String getNumericFunctions() throws SQLException {
        return "ABS,CEILING,FLOOR,ROUND,SIGN,SQRT,TRUNCATE";
    }

    @Override
    public String getStringFunctions() throws SQLException {
        return "CONCAT,LENGTH,LOWER,LTRIM,RTRIM,SUBSTRING,UPPER,TRIM";
    }

    @Override
    public String getSystemFunctions() throws SQLException {
        return "DATABASE,USER,VERSION";
    }

    @Override
    public String getTimeDateFunctions() throws SQLException {
        return "CURRENT_DATE,CURRENT_TIME,CURRENT_TIMESTAMP,EXTRACT";
    }

    @Override
    public String getSearchStringEscape() throws SQLException {
        return "\\";
    }

    @Override
    public String getExtraNameCharacters() throws SQLException {
        return "";
    }

    @Override
    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsColumnAliasing() throws SQLException {
        return true;
    }

    @Override
    public boolean nullPlusNonNullIsNull() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsConvert() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsConvert(final int fromType, final int toType) throws SQLException {
        return false;
    }

    @Override
    public boolean supportsTableCorrelationNames() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsDifferentTableCorrelationNames() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsExpressionsInOrderBy() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsOrderByUnrelated() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsGroupBy() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsGroupByUnrelated() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsGroupByBeyondSelect() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsLikeEscapeClause() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsMultipleResultSets() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsMultipleTransactions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsNonNullableColumns() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsMinimumSQLGrammar() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsCoreSQLGrammar() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsExtendedSQLGrammar() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsANSI92FullSQL() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsOuterJoins() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsFullOuterJoins() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsLimitedOuterJoins() throws SQLException {
        return true;
    }

    @Override
    public String getSchemaTerm() throws SQLException {
        return "schema";
    }

    @Override
    public String getProcedureTerm() throws SQLException {
        return "procedure";
    }

    @Override
    public String getCatalogTerm() throws SQLException {
        return "catalog";
    }

    @Override
    public boolean isCatalogAtStart() throws SQLException {
        return true;
    }

    @Override
    public String getCatalogSeparator() throws SQLException {
        return ".";
    }

    @Override
    public boolean supportsSchemasInDataManipulation() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSchemasInTableDefinitions() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCatalogsInDataManipulation() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsPositionedDelete() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsPositionedUpdate() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSelectForUpdate() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsStoredProcedures() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSubqueriesInComparisons() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsSubqueriesInExists() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsSubqueriesInIns() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsCorrelatedSubqueries() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsUnion() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsUnionAll() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        return true;
    }

    @Override
    public int getMaxBinaryLiteralLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxCharLiteralLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnsInGroupBy() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnsInIndex() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnsInOrderBy() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnsInSelect() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnsInTable() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxConnections() throws SQLException {
        return 100;
    }

    @Override
    public int getMaxCursorNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxIndexLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxSchemaNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxProcedureNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxCatalogNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxRowSize() throws SQLException {
        return 0;
    }

    @Override
    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        return true;
    }

    @Override
    public int getMaxStatementLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxStatements() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxTableNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxTablesInSelect() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxUserNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getDefaultTransactionIsolation() throws SQLException {
        return Connection.TRANSACTION_NONE;
    }

    @Override
    public boolean supportsTransactions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsTransactionIsolationLevel(final int level) throws SQLException {
        return level == Connection.TRANSACTION_NONE;
    }

    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
        return false;
    }

    @Override
    public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        return false;
    }

    @Override
    public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        return false;
    }

    @Override
    public ResultSet getProcedures(final String catalog, final String schemaPattern, final String procedureNamePattern) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getProcedureColumns(
                                         final String catalog, final String schemaPattern, final String procedureNamePattern, final String columnNamePattern) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getTables(
                               final String catalog, final String schemaPattern, final String tableNamePattern, final String[] types) throws SQLException {
        List<List<Object>> rows = new ArrayList<>();
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SHOW TABLES")) {
            while (resultSet.next()) {
                String tableName = resultSet.getString(1);
                if (matches(tableName, tableNamePattern) && matchesType(types, "TABLE")) {
                    rows.add(row(catalog, schemaPattern, tableName, "TABLE"));
                }
            }
        }
        return createResultSet(
                columns("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE"),
                rows);
    }

    @Override
    public ResultSet getSchemas() throws SQLException {
        return getSchemas(null, null);
    }

    @Override
    public ResultSet getSchemas(final String catalog, final String schemaPattern) throws SQLException {
        String schema = connection.getSchema();
        List<List<Object>> rows = matches(schema, schemaPattern)
                ? Arrays.asList(row(schema, catalog))
                : new ArrayList<List<Object>>(0);
        return createResultSet(columns("TABLE_SCHEM", "TABLE_CATALOG"), rows);
    }

    @Override
    public ResultSet getCatalogs() throws SQLException {
        return createResultSet(columns("TABLE_CAT"), Arrays.asList(row(connection.getCatalog())));
    }

    @Override
    public ResultSet getTableTypes() throws SQLException {
        return createResultSet(columns("TABLE_TYPE"), Arrays.asList(row("TABLE"), row("VIEW")));
    }

    @Override
    public ResultSet getColumns(
                                final String catalog, final String schemaPattern, final String tableNamePattern, final String columnNamePattern) throws SQLException {
        List<List<Object>> rows = new ArrayList<>();
        try (ResultSet tables = getTables(catalog, schemaPattern, tableNamePattern, new String[]{"TABLE"})) {
            while (tables.next()) {
                collectColumns(tables.getString("TABLE_NAME"), columnNamePattern, rows);
            }
        }
        return createResultSet(
                columns("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME", "ORDINAL_POSITION", "IS_NULLABLE"),
                rows);
    }

    private void collectColumns(final String tableName, final String columnNamePattern, final List<List<Object>> rows) throws SQLException {
        String sql = "SHOW COLUMNS FROM " + tableName;
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                if (matches(columnName, columnNamePattern)) {
                    String dataType = resultSet.getString("DATA_TYPE");
                    String ordinalPosition = resultSet.getString("ORDINAL_POSITION");
                    rows.add(row(
                            connection.getCatalog(),
                            connection.getSchema(),
                            tableName,
                            columnName,
                            parseIntegerMetadataValue(dataType, "DATA_TYPE", tableName, columnName),
                            resultSet.getString("TYPE_NAME"),
                            parseIntegerMetadataValue(ordinalPosition, "ORDINAL_POSITION", tableName, columnName),
                            resultSet.getString("IS_NULLABLE")));
                }
            }
        }
    }

    private Integer parseIntegerMetadataValue(final String rawValue, final String fieldName, final String tableName, final String columnName) throws SQLException {
        try {
            return Integer.valueOf(rawValue);
        } catch (final NumberFormatException ex) {
            throw new SQLException("Invalid numeric metadata value for " + tableName + "." + columnName + " field " + fieldName + ": " + rawValue, ex);
        }
    }

    @Override
    public ResultSet getColumnPrivileges(
                                         final String catalog, final String schema, final String table, final String columnNamePattern) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getTablePrivileges(final String catalog, final String schemaPattern, final String tableNamePattern) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getBestRowIdentifier(
                                          final String catalog, final String schema, final String table, final int scope, final boolean nullable) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getVersionColumns(final String catalog, final String schema, final String table) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getPrimaryKeys(final String catalog, final String schema, final String table) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getImportedKeys(final String catalog, final String schema, final String table) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getExportedKeys(final String catalog, final String schema, final String table) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getCrossReference(
                                       final String parentCatalog,
                                       final String parentSchema,
                                       final String parentTable,
                                       final String foreignCatalog,
                                       final String foreignSchema,
                                       final String foreignTable) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getTypeInfo() throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getIndexInfo(
                                  final String catalog, final String schema, final String table, final boolean unique, final boolean approximate) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public boolean supportsResultSetType(final int type) throws SQLException {
        return type == ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public boolean supportsResultSetConcurrency(final int type, final int concurrency) throws SQLException {
        return type == ResultSet.TYPE_FORWARD_ONLY && concurrency == ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public boolean ownUpdatesAreVisible(final int type) throws SQLException {
        return false;
    }

    @Override
    public boolean ownDeletesAreVisible(final int type) throws SQLException {
        return false;
    }

    @Override
    public boolean ownInsertsAreVisible(final int type) throws SQLException {
        return false;
    }

    @Override
    public boolean othersUpdatesAreVisible(final int type) throws SQLException {
        return false;
    }

    @Override
    public boolean othersDeletesAreVisible(final int type) throws SQLException {
        return false;
    }

    @Override
    public boolean othersInsertsAreVisible(final int type) throws SQLException {
        return false;
    }

    @Override
    public boolean updatesAreDetected(final int type) throws SQLException {
        return false;
    }

    @Override
    public boolean deletesAreDetected(final int type) throws SQLException {
        return false;
    }

    @Override
    public boolean insertsAreDetected(final int type) throws SQLException {
        return false;
    }

    @Override
    public boolean supportsBatchUpdates() throws SQLException {
        return false;
    }

    @Override
    public ResultSet getUDTs(
                             final String catalog, final String schemaPattern, final String typeNamePattern, final int[] types) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connection;
    }

    @Override
    public boolean supportsSavepoints() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsNamedParameters() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsMultipleOpenResults() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsGetGeneratedKeys() throws SQLException {
        return false;
    }

    @Override
    public ResultSet getSuperTypes(final String catalog, final String schemaPattern, final String typeNamePattern) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getSuperTables(final String catalog, final String schemaPattern, final String tableNamePattern) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getAttributes(
                                   final String catalog, final String schemaPattern, final String typeNamePattern, final String attributeNamePattern) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public boolean supportsResultSetHoldability(final int holdability) throws SQLException {
        return holdability == ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    @Override
    public int getDatabaseMajorVersion() throws SQLException {
        return 1;
    }

    @Override
    public int getDatabaseMinorVersion() throws SQLException {
        return 0;
    }

    @Override
    public int getJDBCMajorVersion() throws SQLException {
        return 4;
    }

    @Override
    public int getJDBCMinorVersion() throws SQLException {
        return 2;
    }

    @Override
    public int getSQLStateType() throws SQLException {
        return sqlStateXOpen;
    }

    @Override
    public boolean locatorsUpdateCopy() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsStatementPooling() throws SQLException {
        return false;
    }

    @Override
    public RowIdLifetime getRowIdLifetime() throws SQLException {
        return RowIdLifetime.ROWID_UNSUPPORTED;
    }

    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        return false;
    }

    @Override
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        return false;
    }

    @Override
    public ResultSet getClientInfoProperties() throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getFunctions(final String catalog, final String schemaPattern, final String functionNamePattern) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getFunctionColumns(
                                        final String catalog, final String schemaPattern, final String functionNamePattern, final String columnNamePattern) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public ResultSet getPseudoColumns(
                                      final String catalog, final String schemaPattern, final String tableNamePattern, final String columnNamePattern) throws SQLException {
        return createEmptyResultSet();
    }

    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        return false;
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Not a wrapper for " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    private ResultSet createEmptyResultSet() {
        return createResultSet(new ArrayList<ColumnMetaData>(0), new ArrayList<List<Object>>(0));
    }

    private ResultSet convertToResultSet(final List<?> data) {
        List<List<Object>> rows = new ArrayList<>(data.size());
        for (Object each : data) {
            rows.add(row(each));
        }
        return createResultSet(columns("VALUE"), rows);
    }

    private ResultSet createResultSet(final List<ColumnMetaData> columns, final List<List<Object>> rows) {
        List<Object> frameRows = new ArrayList<>(rows.size());
        frameRows.addAll(rows);
        Meta.Signature signature = new Meta.Signature(columns, "", new ArrayList<AvaticaParameter>(0), null, Meta.CursorFactory.ARRAY, Meta.StatementType.SELECT);
        Meta.Frame frame = new Meta.Frame(0L, true, frameRows);
        return new IntelliSqlResultSet(new IntelliSqlStatement(connection, 0), null, signature, frame);
    }

    private List<ColumnMetaData> columns(final String... labels) {
        List<ColumnMetaData> result = new ArrayList<>(labels.length);
        for (int i = 0; i < labels.length; i++) {
            result.add(column(i, labels[i]));
        }
        return result;
    }

    private ColumnMetaData column(final int ordinal, final String label) {
        ColumnMetaData.Rep rep = ColumnMetaData.Rep.of(String.class);
        ColumnMetaData.AvaticaType avaticaType = new ColumnMetaData.AvaticaType(Types.VARCHAR, "VARCHAR", rep);
        return new ColumnMetaData(ordinal, false, false, false, false, ResultSetMetaData.columnNullableUnknown, true, -1, label, label, "", 0, 0, "", "", avaticaType, true, false, false, "");
    }

    private List<Object> row(final Object... values) {
        return new ArrayList<Object>(Arrays.asList(values));
    }

    private boolean matches(final String value, final String pattern) {
        if (pattern == null || pattern.isEmpty() || "%".equals(pattern)) {
            return true;
        }
        if (value == null) {
            return false;
        }
        String regex = pattern.replace("_", ".").replace("%", ".*");
        return value.matches(regex);
    }

    private boolean matchesType(final String[] types, final String tableType) {
        if (types == null || types.length == 0) {
            return true;
        }
        for (String each : types) {
            if (tableType.equalsIgnoreCase(each)) {
                return true;
            }
        }
        return false;
    }
}
