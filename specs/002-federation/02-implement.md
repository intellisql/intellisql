# 联邦查询实现说明

更新时间：2026-04-19

本文件只描述联邦查询相关实现，重点写清主类、关键方法、主执行路径和备用实现之间的关系。

## 1. 配置到数据源初始化

### 配置装载

- `com.intellisql.common.config.ConfigLoader`
  - `load(Path)`：读取 YAML 文件内容
  - 对内容执行环境变量替换
  - 解析为 `ModelConfig`
  - `parseDataSourceConfig(...)`：为每个数据源构造 `DataSourceConfig`
  - `buildJdbcUrl(...)`：在未显式提供 URL 时按类型组装 JDBC URL

### 数据源管理

- `com.intellisql.federation.DataSourceManager`
  - 构造时持有 `ModelConfig` 和所有 `DataSourceConfig`
  - `initialize()`：遍历数据源并调用 `initializeDataSource(...)`
  - `initializeDataSource(name, config)`：
    - 根据 `DataSourceType` 从 `ConnectorRegistry` 取连接器
    - 调用 `connector.testConnection(...)`
    - 根据结果记录 `DataSourceStatus`
  - `startHealthCheckScheduler()`：
    - 从首个数据源读取 `HealthCheckConfig`
    - 用单线程调度器周期执行 `performHealthChecks()`
  - `performHealthCheck(name, config)`：
    - 再次调用 `testConnection(...)`
    - 更新状态变化

## 2. 连接器装配与 schema discovery

### 连接器注册

- `com.intellisql.connector.ConnectorRegistry`
  - 单例
  - 构造时 `loadConnectors()`
  - 通过 `ServiceLoader<DataSourceConnector>` 自动发现插件
  - `getConnector(type)`：按 `DataSourceType` 返回连接器

### MySQL 插件

- `com.intellisql.connector.mysql.MySQLConnector`
  - 继承 `AbstractJdbcConnector`
  - 使用 `MySQLSchemaDiscoverer`
  - 使用 `MySQLQueryExecutor`
- `MySQLSchemaDiscoverer`
  - `discoverSchema(...)`
  - `discoverTables(...)`
  - `discoverColumns(...)`
  - 基于 JDBC `DatabaseMetaData` 采集表、列、主键、索引

### PostgreSQL 插件

- `com.intellisql.connector.postgresql.PostgreSQLConnector`
  - 同样继承 `AbstractJdbcConnector`
  - 结构与 MySQL 插件对应

### Elasticsearch 插件

- `com.intellisql.connector.elasticsearch.ElasticsearchConnector`
  - 自维护 `RestHighLevelClient`
  - `connect(config)`：返回 `ElasticsearchConnection`
  - `testConnection(config)`：做 cluster health 检查
  - `discoverSchema(config)`：通过 `ElasticsearchSchemaDiscoverer` 读取索引结构

## 3. 元数据装配

### 联邦内核初始化

- `com.intellisql.federation.IntelliSqlKernel`
  - `initialize()`：
    - 调用 `dataSourceManager.initialize()`
    - 调用 `initializeMetadata()`
  - `initializeMetadata()`：
    - 遍历所有数据源名
    - 从 `ConnectorRegistry` 获取连接器
    - 将 `common` 配置转换为 `IntelliSQLDataSourceConfig`
    - 调用 `metadataManager.initialize(connectorMap)`

### 元数据管理器

- `com.intellisql.federation.metadata.MetadataManager`
  - 内部持有：
    - `Map<String, DataSource> dataSources`
    - `Map<String, Schema> schemas`
    - `Map<String, Table> tables`
  - `initialize(connectors)`：
    - 遍历连接器
    - 调用 `connector.discoverSchema(config)`
    - 若 discovery 成功，调用 `registerFromConnectorSchema(schema)`
  - `registerSchema(schema)`：
    - 把 schema 放入 `schemas`
    - 把 schema 内部 tables 一并注册到 `tables`
  - `getRootSchema()`：
    - 构造 `CalciteSchema.createRootSchema(...)`
    - 构造一个 `FederatedSchema("root")`
    - 把所有表直接挂到 root
    - 再为每个 schema 构造一个子 `FederatedSchema`
  - `createCalciteTable(table)`：
    - 将 IntelliSQL 表封装为 `FederatedTable`
    - 当前实现中把所有列都写成 `SqlTypeName.VARCHAR`

### Calcite table 包装

- `com.intellisql.federation.metadata.calcite.FederatedSchema`
  - 作为 Calcite schema 容器
- `com.intellisql.federation.metadata.calcite.FederatedTable`
  - 保存 `tableName`、`dataSourceId`、列名、列类型

## 4. SQL 到执行计划

### 查询入口

- `com.intellisql.federation.QueryProcessor`
  - `process(sql, context)`：
    - `parseSQL(sql, context)`
    - `convertToRelational(parsedSql, context)`
    - `optimizer.optimize(logicalPlan)`
    - `optimizer.generateExecutionPlan(optimizedPlan, queryId)`
    - `executeWithRetry(executionPlan, context)`

### SQL 解析

- `parseSQL(...)`
  - 直接调用 `SqlParserFactory.parseWithBabel(sql)`

### Rel 转换

- `convertToRelational(...)`
  - 通过 `getRelConverter()` 懒加载 `RelConverter`
  - 调用 `RelConverter.convertQuery(sqlNode, true, true)`

### RelConverter

- `com.intellisql.federation.converter.RelConverter`
  - 构造时创建：
    - `JavaTypeFactoryImpl`
    - `FederatedCatalogReader`
    - `SqlValidator`
    - `RelOptCluster`
    - `VolcanoPlanner`
    - `SqlToRelConverter`
  - `createRelOptCluster(...)`
    - 使用 `FederatedCostFactory.INSTANCE`
    - 注册 `ConventionTraitDef`
    - 注册 12 条 Calcite CBO rules

## 5. 执行阶段

### 主执行路径

- `QueryProcessor.executeWithRetry(...)`
  - 使用 `ExponentialBackoffRetry.execute(...)`
- `executePlan(executionPlan, context)`
  - 顺序执行每个 `ExecutionStage`
- `executeStage(stage, context, previousResult)`
  - 取 `stage.getDataSourceId()`
  - 调用 `determineDataSourceType(...)`
  - 从 `ConnectorRegistry` 获取连接器
  - `getConnection(connector, dataSourceId)`
  - `generateTargetSQL(stage, dataSourceType)`
  - `connection.executeQuery(targetSql)`

### 阶段 SQL 生成

- `generateTargetSQL(stage, dataSourceType)`
  - `toSqlDialect(dataSourceType)` 将数据源类型映射到目标方言名
  - 使用 `SqlNodeToStringConverter.getCalciteDialect(targetDialect)` 获取 `SqlDialect`
  - 使用 `RelToSqlConverter` 把阶段 `RelNode` 渲染回 SQL

### 当前执行限制

- `determineDataSourceType(...)` 未命中时默认返回 `MYSQL`
- `stage.getDataSourceId()` 为 `null` 或 `"default"` 时直接返回上一阶段结果
- 当前主路径没有调用 JVM 内存 `FederatedQueryExecutor`

## 6. 备用联邦执行实现

### `FederatedQueryExecutor`

- `execute(query, executionPlan)`：执行多阶段联邦查询
- `executeStages(...)`：顺序执行阶段
- `executeStage(...)`：为单阶段取连接、构造子查询、收集行
- `executeParallel(...)`：并行执行多个子查询

### `PhysicalPlanConverter`

- `convert(relNode)`：把 `RelNode` 转成 `QueryIterator<Row>` 树
- 支持的 `RelNode` 包括：
  - `TableScan`
  - `Filter`
  - `Project`
  - `Join`
  - `Aggregate`
  - `Sort`

### 当前状态判断

- 这套代码说明项目已经尝试向“真正的 JVM 联邦执行器”演进
- 但当前 `QueryProcessor` 没有调用它
- 因此它是存在的实现，不是当前默认执行链

## 7. 测试与验证现状

- `intellisql-plugin-mysql` 有 `MySQLConnectorIT`
- `intellisql-plugin-postgresql` 有 `PostgreSQLConnectorIT`
- `intellisql-plugin-elasticsearch` 有 `ElasticsearchConnectorIT`

当前缺少：

- 基于主查询链的跨源集成验证
- 服务端元数据接口与联邦元数据的一致性验证
- 与 `006-e2e` 对应的完整联调验证

## 8. 代码级缺口

- `MetadataManager.createCalciteTable()` 没有真实类型映射
- `QueryProcessor.determineDataSourceType()` 有临时默认值
- `IntelliSqlMeta` 只对 `SHOW TABLES` 做了最小可见支持
- 备用联邦执行器未接入主链
- 复杂跨源 JOIN、聚合、排序能力仍未形成统一可用闭环
