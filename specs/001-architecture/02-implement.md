# IntelliSQL 架构实现说明

更新时间：2026-04-19

本文件从代码级别描述 IntelliSQL 当前整体实现，不写概念图，只写仓库里真实存在的模块、主类、关键方法和实际调用关系。

## 1. 模块结构

### `intellisql-common`

核心职责：

- 配置模型
- 元数据模型
- 重试与日志基础设施

主要类：

- `com.intellisql.common.config.ConfigLoader`
  - `load(Path)`：读取 YAML、执行环境变量替换、解析成 `ModelConfig`
  - `load(InputStream)`：从资源流读取配置
  - `parseModelConfig(...)`：支持 `dataSources` 的 list/map 两种写法
  - `buildJdbcUrl(...)`：当配置未显式提供 URL 时根据 `host/port/database` 组装 JDBC URL
- `com.intellisql.common.config.ModelConfig`
  - 聚合 `Map<String, DataSourceConfig>` 和 `Props`
- `com.intellisql.common.config.EnvironmentVariableSubstitutor`
  - 负责 `${VAR}` 样式环境变量替换
- `com.intellisql.common.metadata.*`
  - `DataSource`、`Schema`、`Table`、`Column`、`Index` 等运行时元数据模型
- `com.intellisql.common.retry.ExponentialBackoffRetry`
  - 提供执行重试
- `com.intellisql.common.logger.QueryContext`
  - 给查询链路提供 queryId、user、connectionId

### `intellisql-spi`

核心职责：

- 定义数据库方言抽象
- 统一 SPI 装载机制

主要类：

- `com.intellisql.spi.database.DatabaseDialect`
  - 方言扩展接口，定义 `getType()`、`getCalciteDialect()`、`getLex()`、`getConformance()`
- `com.intellisql.spi.database.DatabaseDialectRegistry`
  - `getDialect(type)`：按类型获取方言实现
  - `getRegisteredTypes()`：返回所有已注册方言类型
- `com.intellisql.spi.loader.IntelliSqlServiceLoader`
  - 对 `ServiceLoader` 做统一封装

### `intellisql-parser`

核心职责：

- SQL parser 工厂
- 宽松解析
- 扩展 AST

主要类：

- `com.intellisql.parser.SqlParserFactory`
  - `createParser(sql, dialect)`：构建方言解析器
  - `parse(sql, dialect)`：解析查询
  - `parseExpression(sql, dialect)`：解析表达式
  - `createBabelParser(sql)`：创建宽松 parser
  - `parseWithBabel(sql)`：宽松解析入口
- `com.intellisql.parser.BabelParserConfiguration`
  - 封装 lenient config
- `com.intellisql.parser.ast.SqlShowTables`
- `com.intellisql.parser.ast.SqlShowSchemas`
- `com.intellisql.parser.ast.SqlUseSchema`

### `intellisql-connector`

核心职责：

- 连接器抽象
- 连接池和查询执行适配
- 健康检查

主要类：

- `com.intellisql.connector.api.DataSourceConnector`
  - 核心连接器接口，包含 `connect`、`testConnection`、`discoverSchema`
- `com.intellisql.connector.ConnectorRegistry`
  - 构造时自动 `loadConnectors()`
  - `getConnector(type)`：按 `DataSourceType` 返回具体连接器
- `com.intellisql.connector.jdbc.AbstractJdbcConnector`
  - JDBC 连接器公共基类
- `com.intellisql.connector.jdbc.JdbcConnectionPool`
  - JDBC 连接池包装
- `com.intellisql.connector.health.*`
  - 健康检查结果与调度支持

### `intellisql-plugins`

核心职责：

- 数据源连接器插件
- SQL 方言插件

数据源插件：

- `MySQLConnector`
- `PostgreSQLConnector`
- `ElasticsearchConnector`

方言插件：

- `MySQLDatabaseDialect`
- `PostgreSQLDatabaseDialect`
- `OracleDatabaseDialect`
- `SQLServerDatabaseDialect`
- `HiveDatabaseDialect`

### `intellisql-feature-federation`

核心职责：

- 联邦内核
- 数据源管理
- Calcite schema / rel 转换
- 查询主处理链

主要类：

- `com.intellisql.federation.IntelliSqlKernel`
  - `create(Path)`：加载配置并创建内核
  - `initialize()`：初始化数据源和元数据
  - `query(sql)` / `query(sql, user, connectionId)`：执行查询
  - `translate(sql, source, target)`：直接做基础翻译
- `com.intellisql.federation.DataSourceManager`
  - `initialize()`：初始化所有数据源
  - `initializeDataSource(name, config)`：逐个测试数据源
  - `startHealthCheckScheduler()`：定时健康检查
  - `performHealthCheck(...)`：执行单数据源探测
- `com.intellisql.federation.metadata.MetadataManager`
  - `initialize(connectors)`：统一做 schema discovery
  - `registerSchema(schema)` / `registerTable(schemaName, table)`：注册运行时元数据
  - `getRootSchema()`：构造 Calcite root schema
  - `createCalciteTable(table)`：将 IntelliSQL 表元数据包装成 Calcite table
- `com.intellisql.federation.converter.RelConverter`
  - 构造时创建独立 `VolcanoPlanner`
  - `convertQuery(sqlNode, needsValidation, top)`：将 `SqlNode` 转成 `RelRoot`
  - `validate(sqlNode)`：执行 Calcite validator
- `com.intellisql.federation.QueryProcessor`
  - `process(sql, context)`：查询主链入口
  - `parseSQL(...)`
  - `convertToRelational(...)`
  - `executeWithRetry(...)`
  - `executePlan(...)`
  - `executeStage(...)`
  - `generateTargetSQL(stage, dataSourceType)`：将阶段 `RelNode` 反向渲染成目标 SQL

备用执行路径：

- `com.intellisql.federation.executor.FederatedQueryExecutor`
- `com.intellisql.federation.executor.plan.PhysicalPlanConverter`
- `com.intellisql.federation.executor.iterator.*`

这套代码存在，但不在当前 `IntelliSqlKernel -> QueryProcessor` 主执行链上。

### `intellisql-feature-translator`

核心职责：

- SQL 翻译统一入口
- 在线/离线翻译模式

主要类：

- `com.intellisql.translator.SqlTranslator`
  - `translate(Translation)`：统一入口
  - `translateOffline(...)`
  - `translateOnline(...)`
  - `validateSyntax(...)`
  - `parse(...)`
  - `format(...)`
- `OfflineTranslationService`
  - 直接 Parse -> ToSql
- `OnlineTranslationService`
  - Parse -> Analyze -> ToSql
  - `analyzeAndValidate(...)` 目前只有占位分析
- `DialectConverterFactory`
  - `toSql(sqlNode, targetDialect)`：按目标方言输出 SQL

### `intellisql-jdbc`

核心职责：

- 自定义 JDBC Driver
- IntelliSQL URL 解析
- Avatica client 适配

主要类：

- `com.intellisql.jdbc.IntelliSqlDriver`
  - 静态块中向 `DriverManager` 注册驱动
  - `connect(url, info)`：解析 URL、构造 `AvaticaClient`、返回 `IntelliSqlConnection`
  - `acceptsURL(url)`：识别 `jdbc:intellisql:` 前缀
- `com.intellisql.jdbc.JdbcUrlParser`
  - `parse(url)`：解析 `jdbc:intellisql://host:port/database?props`
  - `getEndpoint()` / `getProtobufEndpoint()`：生成 HTTP 访问端点
- `com.intellisql.jdbc.AvaticaClient`
  - Avatica 远程访问封装
- `IntelliSqlConnection` / `IntelliSqlStatement` / `IntelliSqlPreparedStatement`
  - JDBC 连接与语句适配
- `IntelliSqlDatabaseMetaData`
  - JDBC metadata 封装

### `intellisql-server`

核心职责：

- 启动 Avatica HTTP 服务
- 暴露 `Meta` 实现

主要类：

- `com.intellisql.server.IntelliSqlServer`
  - `start()`：初始化 kernel、创建 `LocalService`、启动 `HttpServer`
  - `initializeKernel()`：加载默认配置 `conf/model.yaml`
  - `stop()`：停止服务并关闭 kernel
- `com.intellisql.server.IntelliSqlMeta`
  - 实现 `org.apache.calcite.avatica.Meta`
  - `openConnection(...)`
  - `createStatement(...)`
  - `prepare(...)`
  - `prepareAndExecute(...)`
  - `handleShowTables(...)`
  - 大多数 metadata 查询方法当前返回 `emptyMetaResultSet()`
- `ConnectionManager` / `StatementManager`
  - 服务端连接和语句状态管理辅助类

### `intellisql-client`

核心职责：

- `isql` CLI
- 终端输入
- 命令执行
- 文本渲染

主要类：

- `com.intellisql.client.IntelliSqlClient`
  - `call()`：CLI 主入口
  - `runInteractiveLoop(...)`
  - `handleSlashCommand(...)`
  - `handleSqlCommand(...)`
  - `updateConnection(...)`
- `ConnectCommand`
  - `execute(...)`：用 `DriverManager.getConnection()` 建立连接
- `ExecuteCommand`
  - `execute(...)`：创建 `Statement` 执行 SQL，结果交给 `PagingRenderer`
- `TranslateCommand`
  - `parseOptions(...)`：解析 `-s/-t/-m`
  - `doTranslation(...)`：调用 `SqlTranslator`
- `ConsoleReader`
  - 封装 `Terminal`、`LineReader`、history、highlighter、signal handler
- `MetaDataLoader`
  - 后台线程加载 `schemas/tables/columns`
- `CompleterFactory`
  - 组合关键字、命令和 metadata 补全
- `PagingRenderer`
  - 结果渲染与分页钩子
- `ResultSetFormatter`
  - 文本表格拼装

### `intellisql-distributions`

核心职责：

- 生成客户端和服务端发布包

主要模块：

- `intellisql-distribution-client`
  - assembly 打包客户端
  - `bin/isql.sh` 通过 `java -cp` 启动 `com.intellisql.client.IntelliSqlClient`
- `intellisql-distribution-server`
  - assembly 打包服务端
  - `bin/start.sh` / `bin/stop.sh`

当前问题：

- `start.sh` 指向的主类名是 `org.intellisql.server.IntelliSQLServer`，与真实代码 `com.intellisql.server.IntelliSqlServer` 不一致

## 2. 端到端调用链

### 查询链

1. `IntelliSqlServer.start()`
2. `initializeKernel()`
3. `IntelliSqlKernel.initialize()`
4. `DataSourceManager.initialize()`
5. `MetadataManager.initialize(...)`
6. 客户端通过 `IntelliSqlDriver.connect(...)` 建立 JDBC 连接
7. Avatica 调用 `IntelliSqlMeta.prepareAndExecute(...)`
8. 内核内由 `QueryProcessor.process(...)` 完成 parse / rel / optimize / execute

### 翻译链

1. CLI `TranslateCommand.execute(...)`
2. `SqlTranslator.translateOffline(...)` 或 `translateOnline(...)`
3. `SqlParserFactory.parse(...)`
4. `DialectConverterFactory.toSql(...)`

### 补全链

1. `IntelliSqlClient.updateConnection(...)`
2. `MetaDataLoader.load(connection)`
3. `DatabaseMetaData.getSchemas()/getTables()/getColumns()`
4. `CompleterFactory.create(loader)` 动态完成补全

## 3. 当前架构级问题

- `MetadataManager.createCalciteTable()` 当前无真实列类型映射，统一写成 `VARCHAR`
- `QueryProcessor.determineDataSourceType()` 未命中默认回落到 `MYSQL`
- `IntelliSqlMeta` 的大部分 JDBC metadata API 尚未落地
- `IntelliSqlServer` 当前固定读取 `conf/model.yaml`，没有消费启动脚本传入的 `-Dconfig.file`
- 服务端发布脚本主类名错误
- CLI、服务端、联邦执行存在多条实现路线，但主路线与备用路线尚未彻底收敛

## 4. 当前实现判断

从代码级别看，IntelliSQL 已经具备：

- 清晰的模块边界
- Calcite/Avatica/SPI 为中心的主架构
- 联邦、翻译、JDBC、CLI 的可运行骨架

但还没有做到：

- 完整 JDBC 元数据服务
- 收敛后的单一联邦执行主路径
- 类型精确的 Calcite schema
- 完整一致的发布脚本和运行时配置机制
