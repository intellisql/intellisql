# Tasks: IntelliSql SQL 联邦和翻译

**Input**: 设计文档来自 `/specs/001-sql-federation-translation/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: 本项目采用 TDD 方式，每个功能模块需编写单元测试

**Organization**: 任务按用户故事组织，支持独立实现和测试

**Reference**: `/Users/duanzhengqiang/IdeaProjects/shardingsphere/kernel/sql-federation` (参考实现)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 任务所属用户故事（US1, US2, US3, US4, US5）
- 描述中包含精确的文件路径

## Path Conventions

```
intellisql/                        # 仓库根目录
├── intellisql-parser/             # SQL 解析模块
├── intellisql-feature-optimizer/          # SQL 优化模块
│   ├── cost/                      # 代价模型
│   ├── metadata/                  # 元数据提供者
│   ├── plan/                      # 逻辑执行计划、物理计划转换
│   └── rule/                      # 优化规则
├── intellisql-executor/           # SQL 执行模块
│   └── iterator/                  # Volcano 迭代器模型 
├── intellisql-connector/          # 数据源连接器
├── intellisql-kernel/             # 核心编排层
├── intellisql-jdbc/               # JDBC 驱动
├── intellisql-server/             # 服务端
├── intellisql-client/             # CLI 工具
├── intellisql-distribution/       # 打包分发
├── intellisql-test/               # 测试模块
├── conf/                          # 配置文件
├── pom.xml                        # 父 POM
└── mvnw                           # Maven Wrapper
```

---

## Phase 1: Setup（项目初始化）

**Purpose**: Maven 多模块项目结构搭建和基础配置

- [x] T001 验证父 POM 文件 pom.xml，确认依赖版本正确（Calcite 1.41.0, Avatica 1.27.0, Lombok 1.18.30）
- [x] T002 [P] 验证 Maven Wrapper 配置 .mvn/wrapper/maven-wrapper.properties
- [x] T003 [P] 验证 Checkstyle 配置文件 src/resources/checkstyle/checkstyle.xml（参考 ShardingSphere 风格）
- [x] T004 [P] 验证 Spotless 配置在 pom.xml 中（Palantir Java Format，无空行规则）
- [x] T005 [P] 验证 logback.xml 配置 intellisql-server/src/main/resources/logback.xml（JSON 格式日志）
- [x] T006 验证所有模块 pom.xml 存在且依赖正确
- [x] T007 验证构建 ./mvnw clean install -DskipTests

**Checkpoint**: 项目结构完整，可成功构建

---

## Phase 2: Foundational（阻塞性前置条件）

**Purpose**: 所有用户故事依赖的核心基础设施

**⚠️ CRITICAL**: 此阶段完成前，任何用户故事工作都不能开始

### 2.1 枚举定义（基础类型）

- [x] T008 [P] 验证 DataSourceType 枚举 in intellisql-kernel/src/main/java/com/intellisql/kernel/metadata/enums/DataSourceType.java
- [x] T009 [P] 验证 DataType 枚举 in intellisql-kernel/src/main/java/com/intellisql/kernel/metadata/enums/DataType.java
- [x] T010 [P] 验证 SchemaType 枚举 in intellisql-kernel/src/main/java/com/intellisql/kernel/metadata/enums/SchemaType.java
- [x] T011 [P] 验证 TableType 枚举 in intellisql-kernel/src/main/java/com/intellisql/kernel/metadata/enums/TableType.java
- [x] T012 [P] 验证 QueryStatus 枚举 in intellisql-kernel/src/main/java/com/intellisql/kernel/executor/enums/QueryStatus.java
- [x] T013 [P] 验证 SqlDialect 枚举 in intellisql-parser/src/main/java/com/intellisql/parser/dialect/SqlDialect.java
- [x] T014 [P] 验证 TranslationMode 枚举 in intellisql-parser/src/main/java/com/intellisql/parser/TranslationMode.java
- [x] T015 [P] 验证 ConnectionStatus 枚举 in intellisql-server/src/main/java/com/intellisql/server/ConnectionStatus.java

### 2.2 配置加载（YAML 解析）

- [x] T016 [P] 验证 Props 配置类 in intellisql-kernel/src/main/java/com/intellisql/kernel/config/Props.java
- [x] T017 [P] 验证 HealthCheckConfig 值对象 in intellisql-kernel/src/main/java/com/intellisql/kernel/config/HealthCheckConfig.java
- [x] T018 [P] 验证 ConnectionPoolConfig 值对象 in intellisql-kernel/src/main/java/com/intellisql/kernel/config/ConnectionPoolConfig.java
- [x] T019 验证 DataSourceConfig 配置类 in intellisql-kernel/src/main/java/com/intellisql/kernel/config/DataSourceConfig.java
- [x] T020 验证 ModelConfig 根配置类 in intellisql-kernel/src/main/java/com/intellisql/kernel/config/ModelConfig.java
- [x] T021 验证 ConfigLoader YAML 解析 in intellisql-kernel/src/main/java/com/intellisql/kernel/config/ConfigLoader.java
- [x] T022 验证环境变量替换逻辑 in intellisql-kernel/src/main/java/com/intellisql/kernel/config/EnvironmentVariableSubstitutor.java

### 2.3 日志基础设施（NFR-001 ~ NFR-005）

- [x] T023 [P] 验证 QueryContext 查询上下文 in intellisql-kernel/src/main/java/com/intellisql/kernel/logger/QueryContext.java
- [x] T024 [P] 验证 StructuredLogger 结构化日志 in intellisql-kernel/src/main/java/com/intellisql/kernel/logger/StructuredLogger.java
- [x] T025 验证 QueryContext MDC 管理器 in intellisql-kernel/src/main/java/com/intellisql/kernel/logger/QueryContextManager.java

### 2.4 重试机制（NFR-006 ~ NFR-009）

- [x] T026 验证 RetryPolicy 重试策略 in intellisql-kernel/src/main/java/com/intellisql/kernel/retry/RetryPolicy.java
- [x] T027 验证 ExponentialBackoffRetry 指数退避重试 in intellisql-kernel/src/main/java/com/intellisql/kernel/retry/ExponentialBackoffRetry.java
- [x] T028 验证 TransientErrorDetector 瞬时错误检测 in intellisql-kernel/src/main/java/com/intellisql/kernel/retry/TransientErrorDetector.java

**Checkpoint**: 基础设施就绪 - 用户故事实现可以并行开始

---

## Phase 3: User Story 1 - 跨数据源联邦查询 (Priority: P1) 🎯 MVP

**Goal**: 支持通过标准 SQL 执行跨异构数据源的 JOIN 查询，采用混合优化器策略和 Volcano 迭代器执行模型

**Independent Test**:
- 配置 MySQL 和 Elasticsearch 两个数据源
- 执行跨源 JOIN 查询
- 验证结果正确合并两个数据源的数据

**Reference**: ShardingSphere sql-federation (HybridOptimizer, Volcano Iterator Model)

### 3.1 Tests for User Story 1

- [x] T029 [P] [US1] 创建 FederatedQueryExecutorTest in intellisql-executor/src/test/java/com/intellisql/executor/FederatedQueryExecutorTest.java
- [x] T030 [P] [US1] 创建 MySQLConnectorIT in intellisql-connector/src/test/java/com/intellisql/connector/mysql/MySQLConnectorIT.java
- [x] T031 [P] [US1] 创建 PostgreSQLConnectorIT in intellisql-connector/src/test/java/com/intellisql/connector/postgresql/PostgreSQLConnectorIT.java
- [x] T032 [P] [US1] 创建 ElasticsearchConnectorIT in intellisql-connector/src/test/java/com/intellisql/connector/elasticsearch/ElasticsearchConnectorIT.java
- [x] T033 [US1] 创建 CrossSourceJoinIT in intellisql-test/intellisql-test-it/src/test/java/com/intellisql/it/federation/CrossSourceJoinIT.java
- [x] T034 [P] [US1] 创建 HybridOptimizerTest in intellisql-feature-optimizer/src/test/java/com/intellisql/optimizer/HybridOptimizerTest.java
- [x] T035 [P] [US1] 创建 FederatedCostTest in intellisql-feature-optimizer/src/test/java/com/intellisql/optimizer/cost/FederatedCostTest.java
- [x] T036 [P] [US1] 创建 QueryIteratorTest in intellisql-executor/src/test/java/com/intellisql/executor/iterator/QueryIteratorTest.java
- [x] T037 [P] [US1] 创建 PhysicalPlanConverterTest in intellisql-feature-optimizer/src/test/java/com/intellisql/optimizer/plan/PhysicalPlanConverterTest.java

### 3.2 元数据模型

- [x] T038 [P] [US1] 验证 Column 实体 in intellisql-kernel/src/main/java/com/intellisql/kernel/metadata/Column.java
- [x] T039 [P] [US1] 验证 Index 实体 in intellisql-kernel/src/main/java/com/intellisql/kernel/metadata/Index.java
- [x] T040 [P] [US1] 验证 Table 实体 in intellisql-kernel/src/main/java/com/intellisql/kernel/metadata/Table.java
- [x] T041 [US1] 验证 Schema 实体 in intellisql-kernel/src/main/java/com/intellisql/kernel/metadata/Schema.java
- [x] T042 [US1] 验证 DataSource 实体 in intellisql-kernel/src/main/java/com/intellisql/kernel/metadata/DataSource.java
- [x] T043 [US1] 验证 MetadataManager 元数据管理器 in intellisql-kernel/src/main/java/com/intellisql/kernel/metadata/MetadataManager.java

### 3.3 连接器 SPI（已有实现）

- [x] T044 [US1] 验证 DataSourceConnector 接口 in intellisql-connector/src/main/java/com/intellisql/connector/api/DataSourceConnector.java
- [x] T045 [US1] 验证 Connection 接口 in intellisql-connector/src/main/java/com/intellisql/connector/api/Connection.java
- [x] T046 [US1] 验证 SchemaDiscoverer 接口 in intellisql-connector/src/main/java/com/intellisql/connector/api/SchemaDiscoverer.java
- [x] T047 [US1] 验证 QueryExecutor 接口 in intellisql-connector/src/main/java/com/intellisql/connector/api/QueryExecutor.java
- [x] T048 [US1] 验证 ConnectorRegistry 注册中心 in intellisql-connector/src/main/java/com/intellisql/connector/ConnectorRegistry.java

### 3.4 MySQL 连接器（已有实现）

- [x] T049 [US1] 验证 MySQLConnector in intellisql-connector/src/main/java/com/intellisql/connector/mysql/MySQLConnector.java
- [x] T050 [US1] 验证 MySQLSchemaDiscoverer in intellisql-connector/src/main/java/com/intellisql/connector/mysql/MySQLSchemaDiscoverer.java
- [x] T051 [US1] 验证 MySQLQueryExecutor in intellisql-connector/src/main/java/com/intellisql/connector/mysql/MySQLQueryExecutor.java
- [x] T052 [US1] 验证 MySQLConnectionPool in intellisql-connector/src/main/java/com/intellisql/connector/mysql/MySQLConnectionPool.java

### 3.5 PostgreSQL 连接器（已有实现）

- [x] T053 [P] [US1] 验证 PostgreSQLConnector in intellisql-connector/src/main/java/com/intellisql/connector/postgresql/PostgreSQLConnector.java
- [x] T054 [P] [US1] 验证 PostgreSQLSchemaDiscoverer in intellisql-connector/src/main/java/com/intellisql/connector/postgresql/PostgreSQLSchemaDiscoverer.java
- [x] T055 [P] [US1] 验证 PostgreSQLQueryExecutor in intellisql-connector/src/main/java/com/intellisql/connector/postgresql/PostgreSQLQueryExecutor.java
- [x] T056 [P] [US1] 验证 PostgreSQLConnectionPool in intellisql-connector/src/main/java/com/intellisql/connector/postgresql/PostgreSQLConnectionPool.java

### 3.6 Elasticsearch 连接器（已有实现）

- [x] T057 [P] [US1] 验证 ElasticsearchConnector in intellisql-connector/src/main/java/com/intellisql/connector/elasticsearch/ElasticsearchConnector.java
- [x] T058 [P] [US1] 验证 ElasticsearchSchemaDiscoverer in intellisql-connector/src/main/java/com/intellisql/connector/elasticsearch/ElasticsearchSchemaDiscoverer.java
- [x] T059 [P] [US1] 验证 ElasticsearchQueryExecutor in intellisql-connector/src/main/java/com/intellisql/connector/elasticsearch/ElasticsearchQueryExecutor.java
- [x] T060 [P] [US1] 验证 ElasticsearchTypeMapping in intellisql-connector/src/main/java/com/intellisql/connector/elasticsearch/ElasticsearchTypeMapping.java

### 3.7 健康检查（已有实现）

- [x] T061 [US1] 验证 HealthChecker 接口 in intellisql-connector/src/main/java/com/intellisql/connector/health/HealthChecker.java
- [x] T062 [US1] 验证 DataSourceHealthChecker in intellisql-connector/src/main/java/com/intellisql/connector/health/DataSourceHealthChecker.java
- [x] T063 [US1] 验证 HealthCheckScheduler in intellisql-connector/src/main/java/com/intellisql/connector/health/HealthCheckScheduler.java

### 3.8 Parser 模块（已有实现）

- [x] T064 [US1] 验证 SqlParserFactory in intellisql-parser/src/main/java/com/intellisql/parser/SqlParserFactory.java
- [x] T065 [US1] 验证 BabelParserConfiguration in intellisql-parser/src/main/java/com/intellisql/parser/BabelParserConfiguration.java

### 3.9 混合优化器策略（新增 - 参考 ShardingSphere）

#### 优化器核心

- [x] T066 [US1] 重构 Optimizer.java 为 RboOptimizer.java in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/RboOptimizer.java
- [x] T067 [US1] 实现 CboOptimizer (VolcanoPlanner) in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/CboOptimizer.java
- [x] T068 [US1] 实现 HybridOptimizer (RBO → CBO) in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/HybridOptimizer.java
- [x] T069 [US1] 更新 QueryProcessor 使用 HybridOptimizer in intellisql-kernel/src/main/java/com/intellisql/kernel/QueryProcessor.java

### 3.10 完整代价模型（新增 - 参考 ShardingSphere）

- [x] T070 [US1] 实现 CostFactor 枚举 (CPU/IO/NETWORK/MEMORY) in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/cost/CostFactor.java
- [x] T071 [US1] 实现 FederatedCost (RelOptCost 接口) in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/cost/FederatedCost.java
- [x] T072 [US1] 实现 FederatedCostFactory in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/cost/FederatedCostFactory.java
- [x] T073 [US1] 注册 FederatedCostFactory 到 VolcanoPlanner in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/CboOptimizer.java

### 3.11 扩展 RBO 规则集（新增 - 参考 ShardingSphere PushFilterIntoScanRule 等）

- [x] T074 [P] [US1] 实现 PredicatePushDownRule in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/rule/PredicatePushDownRule.java
- [x] T075 [P] [US1] 实现 ProjectionPushDownRule in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/rule/ProjectionPushDownRule.java
- [x] T076 [P] [US1] 实现 JoinReorderRule in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/rule/JoinReorderRule.java
- [x] T077 [P] [US1] 实现 SubqueryRewriteRule in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/rule/SubqueryRewriteRule.java
- [x] T078 [P] [US1] 实现 AggregateSplitRule in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/rule/AggregateSplitRule.java
- [x] T079 [P] [US1] 实现 LimitPushDownRule in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/rule/LimitPushDownRule.java
- [x] T080 [US1] 注册所有规则到 RboOptimizer in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/RboOptimizer.java

### 3.12 Volcano 迭代器执行模型（新增 - 参考 ShardingSphere Enumerator 模式）

#### 迭代器接口和基类

- [x] T081 [US1] 定义 QueryIterator 接口 (open/hasNext/next/close) in intellisql-executor/src/main/java/com/intellisql/executor/iterator/QueryIterator.java
- [x] T082 [US1] 实现 AbstractOperator 基类 in intellisql-executor/src/main/java/com/intellisql/executor/iterator/AbstractOperator.java

#### 算子实现（参考 ShardingSphere JDBCDataRowEnumerator 模式）

- [x] T083 [P] [US1] 实现 TableScanOperator in intellisql-executor/src/main/java/com/intellisql/executor/iterator/TableScanOperator.java
- [x] T084 [P] [US1] 实现 FilterOperator in intellisql-executor/src/main/java/com/intellisql/executor/iterator/FilterOperator.java
- [x] T085 [P] [US1] 实现 ProjectOperator in intellisql-executor/src/main/java/com/intellisql/executor/iterator/ProjectOperator.java
- [x] T086 [P] [US1] 实现 JoinOperator (Hash Join) in intellisql-executor/src/main/java/com/intellisql/executor/iterator/JoinOperator.java
- [x] T087 [P] [US1] 实现 AggregateOperator in intellisql-executor/src/main/java/com/intellisql/executor/iterator/AggregateOperator.java
- [x] T088 [P] [US1] 实现 SortOperator in intellisql-executor/src/main/java/com/intellisql/executor/iterator/SortOperator.java

#### 物理计划转换

- [x] T089 [US1] 实现 PhysicalPlanConverter (RelNode → Operator Tree) in intellisql-executor/src/main/java/com/intellisql/executor/plan/PhysicalPlanConverter.java

### 3.13 RelMetadataQuery 元数据支持（新增）

- [x] T090 [US1] 实现 TableStatistics 实体 in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/metadata/TableStatistics.java
- [x] T091 [US1] 实现 StatisticsHandler in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/metadata/StatisticsHandler.java
- [x] T092 [US1] 实现 FederatedMetadataProvider in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/metadata/FederatedMetadataProvider.java

### 3.14 执行计划和查询处理

- [x] T093 [P] [US1] 验证 ExecutionPlan 实体 in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/plan/ExecutionPlan.java
- [x] T094 [P] [US1] 验证 ExecutionStage 实体 in intellisql-feature-optimizer/src/main/java/com/intellisql/optimizer/plan/ExecutionStage.java

### 3.15 查询结果模型

- [x] T095 [P] [US1] 验证 Query 实体 in intellisql-executor/src/main/java/com/intellisql/executor/Query.java
- [x] T096 [P] [US1] 验证 QueryResult 实体 in intellisql-executor/src/main/java/com/intellisql/executor/QueryResult.java
- [x] T097 [P] [US1] 验证 QueryError 实体 in intellisql-executor/src/main/java/com/intellisql/executor/QueryError.java
- [x] T098 [P] [US1] 验证 Row 实体 in intellisql-executor/src/main/java/com/intellisql/executor/Row.java
- [x] T099 [P] [US1] 验证 ColumnMetadata 实体 in intellisql-executor/src/main/java/com/intellisql/executor/ColumnMetadata.java

### 3.16 联邦查询执行器集成

- [x] T100 [US1] 更新 FederatedQueryExecutor 集成 Volcano 迭代器 in intellisql-executor/src/main/java/com/intellisql/executor/FederatedQueryExecutor.java
- [x] T101 [US1] 验证 IntermediateResultLimiter (100k rows) in intellisql-executor/src/main/java/com/intellisql/executor/IntermediateResultLimiter.java

### 3.17 核心编排层

- [x] T102 [US1] 验证 IntelliSqlKernel 内核入口 in intellisql-kernel/src/main/java/com/intellisql/kernel/IntelliSqlKernel.java
- [x] T103 [US1] 验证 QueryProcessor 查询处理器 in intellisql-kernel/src/main/java/com/intellisql/kernel/QueryProcessor.java
- [x] T104 [US1] 验证 DataSourceManager 数据源管理器 in intellisql-kernel/src/main/java/com/intellisql/kernel/DataSourceManager.java

**Checkpoint**: US1 完成 - 跨数据源联邦查询功能可独立测试

---

## Phase 4: User Story 2 - SQL 方言翻译 (Priority: P1)

**Goal**: 将一种数据库的 SQL 翻译为另一种数据库兼容的 SQL

**Independent Test**:
- 输入 MySQL 方言的 SQL
- 指定目标方言为 PostgreSQL
- 验证输出的 SQL 符合 PostgreSQL 语法

### 4.1 Tests for User Story 2

- [x] T105 [P] [US2] 验证 SqlTranslatorTest in intellisql-parser/src/test/java/com/intellisql/parser/SqlTranslatorTest.java
- [x] T106 [P] [US2] 验证 MySQLDialectConverterTest in intellisql-parser/src/test/java/com/intellisql/parser/dialect/MySQLDialectConverterTest.java
- [x] T107 [P] [US2] 验证 PostgreSQLDialectConverterTest in intellisql-parser/src/test/java/com/intellisql/parser/dialect/PostgreSQLDialectConverterTest.java

### 4.2 Implementation for User Story 2

- [x] T108 [US2] 验证 Translation 实体 in intellisql-parser/src/main/java/com/intellisql/parser/Translation.java
- [x] T109 [US2] 验证 TranslationException in intellisql-parser/src/main/java/com/intellisql/parser/TranslationException.java
- [x] T110 [US2] 验证 SqlTranslator 核心翻译器 in intellisql-parser/src/main/java/com/intellisql/parser/SqlTranslator.java
- [x] T111 [US2] 验证 SqlNodeToStringConverter in intellisql-parser/src/main/java/com/intellisql/parser/SqlNodeToStringConverter.java

### 4.3 方言转换器

- [x] T112 [US2] 验证 SqlDialect 枚举 in intellisql-parser/src/main/java/com/intellisql/parser/dialect/SqlDialect.java
- [x] T113 [US2] 实现 DialectConverter 接口 in intellisql-parser/src/main/java/com/intellisql/parser/dialect/DialectConverter.java
- [x] T114 [US2] 实现 DialectConverterFactory in intellisql-parser/src/main/java/com/intellisql/parser/dialect/DialectConverterFactory.java
- [x] T115 [US2] 验证 MySQLDialectConverter in intellisql-parser/src/main/java/com/intellisql/parser/dialect/MySQLDialectConverter.java
- [x] T116 [P] [US2] 验证 PostgreSQLDialectConverter in intellisql-parser/src/main/java/com/intellisql/parser/dialect/PostgreSQLDialectConverter.java
- [x] T117 [P] [US2] 实现 OracleDialectConverter in intellisql-parser/src/main/java/com/intellisql/parser/dialect/OracleDialectConverter.java
- [x] T118 [P] [US2] 实现 SQLServerDialectConverter in intellisql-parser/src/main/java/com/intellisql/parser/dialect/SQLServerDialectConverter.java
- [x] T119 [P] [US2] 实现 HiveDialectConverter in intellisql-parser/src/main/java/com/intellisql/parser/dialect/HiveDialectConverter.java

### 4.4 Parser 扩展（参考 Quicksql 实现）

- [x] T120 [US2] 创建 config.fmpp 配置 in intellisql-parser/src/main/codegen/config.fmpp
- [x] T121 [US2] 创建 Parser.jj 模板 in intellisql-parser/src/main/codegen/templates/Parser.jj
- [x] T122 [US2] 创建 parserImpls.ftl 自定义语法 in intellisql-parser/src/main/codegen/includes/parserImpls.ftl

### 4.5 AST 扩展节点

- [x] T123 [US2] 实现 SqlShowTables AST 节点 in intellisql-parser/src/main/java/com/intellisql/parser/ast/SqlShowTables.java
- [x] T124 [P] [US2] 实现 SqlShowSchemas AST 节点 in intellisql-parser/src/main/java/com/intellisql/parser/ast/SqlShowSchemas.java
- [x] T125 [P] [US2] 实现 SqlUseSchema AST 节点 in intellisql-parser/src/main/java/com/intellisql/parser/ast/SqlUseSchema.java

### 4.6 错误处理

- [x] T126 [US2] 实现 TranslationError in intellisql-parser/src/main/java/com/intellisql/parser/TranslationError.java

**Checkpoint**: US2 完成 - SQL 方言翻译功能可独立测试

---

## Phase 5: User Story 3 - JDBC 标准接口访问 (Priority: P2)

**Goal**: 通过标准 JDBC 接口连接 IntelliSql Server

**Independent Test**:
- 使用 JDBC 客户端工具
- 连接 IntelliSql Server
- 执行 SQL 查询并获取结果

### 5.1 Tests for User Story 3

- [x] T127 [P] [US3] 验证 IntelliSqlDriverTest in intellisql-jdbc/src/test/java/com/intellisql/jdbc/IntelliSqlDriverTest.java
- [x] T128 [P] [US3] 验证 IntelliSqlConnectionTest in intellisql-jdbc/src/test/java/com/intellisql/jdbc/IntelliSqlConnectionTest.java
- [x] T129 [US3] 创建 JdbcProtocolIT in intellisql-test/intellisql-test-it/src/test/java/com/intellisql/it/jdbc/JdbcProtocolIT.java

### 5.2 JDBC 驱动实现（已有基础）

- [x] T130 [US3] 验证 IntelliSqlDriver in intellisql-jdbc/src/main/java/com/intellisql/jdbc/IntelliSqlDriver.java
- [x] T131 [US3] 验证 DriverRegistration in intellisql-jdbc/src/main/java/com/intellisql/jdbc/DriverRegistration.java
- [x] T132 [US3] 验证 JdbcUrlParser in intellisql-jdbc/src/main/java/com/intellisql/jdbc/JdbcUrlParser.java
- [x] T133 [US3] 验证 META-INF/services/java.sql.Driver in intellisql-jdbc/src/main/resources/META-INF/services/java.sql.Driver
- [x] T134 [US3] 验证 IntelliSqlConnection in intellisql-jdbc/src/main/java/com/intellisql/jdbc/IntelliSqlConnection.java
- [x] T135 [US3] 验证 IntelliSqlStatement in intellisql-jdbc/src/main/java/com/intellisql/jdbc/IntelliSqlStatement.java
- [x] T136 [US3] 验证 IntelliSqlPreparedStatement in intellisql-jdbc/src/main/java/com/intellisql/jdbc/IntelliSqlPreparedStatement.java
- [x] T137 [US3] 验证 IntelliSqlResultSet in intellisql-jdbc/src/main/java/com/intellisql/jdbc/IntelliSqlResultSet.java
- [x] T138 [US3] 验证 IntelliSqlResultSetMetaData in intellisql-jdbc/src/main/java/com/intellisql/jdbc/IntelliSqlResultSetMetaData.java
- [x] T139 [US3] 验证 IntelliSqlDatabaseMetaData in intellisql-jdbc/src/main/java/com/intellisql/jdbc/IntelliSqlDatabaseMetaData.java
- [x] T140 [US3] 验证 AvaticaClient in intellisql-jdbc/src/main/java/com/intellisql/jdbc/AvaticaClient.java

### 5.3 Server 端（Avatica 协议）

- [x] T141 [US3] 实现 ConnectionManager in intellisql-server/src/main/java/com/intellisql/server/ConnectionManager.java
- [x] T142 [US3] 实现 StatementManager in intellisql-server/src/main/java/com/intellisql/server/StatementManager.java
- [x] T143 [US3] 实现 IntelliSqlHandler (AvaticaHandler) in intellisql-server/src/main/java/com/intellisql/server/IntelliSqlHandler.java
- [x] T144 [US3] 实现 IntelliSqlServer in intellisql-server/src/main/java/com/intellisql/server/IntelliSqlServer.java
- [x] T145 [US3] 实现 ServerMain 入口 in intellisql-server/src/main/java/com/intellisql/server/ServerMain.java

### 5.4 E2E 测试

- [x] T146 [US3] 创建 JdbcE2ETest in intellisql-test/intellisql-test-e2e/src/test/java/com/intellisql/e2e/jdbc/JdbcE2ETest.java
- [x] T147 [US3] 创建 LargeResultSetE2ETest (100万行) in intellisql-test/intellisql-test-e2e/src/test/java/com/intellisql/e2e/jdbc/LargeResultSetE2ETest.java

**Checkpoint**: US3 完成 - JDBC 标准接口访问可独立测试

---

## Phase 6: User Story 4 - 命令行工具 isql (Priority: P2)

**Goal**: 提供专业的命令行工具连接 Server，执行查询和翻译 SQL

**Independent Test**:
- 下载 isql 客户端包
- 执行 isql 命令连接 Server
- 执行 SQL 查询和翻译命令

### 6.1 Tests for User Story 4

- [x] T148 [P] [US4] 验证 IntelliSqlClientTest in intellisql-client/src/test/java/com/intellisql/client/IntelliSqlClientTest.java
- [x] T149 [P] [US4] 验证 ReplHandlerTest in intellisql-client/src/test/java/com/intellisql/client/ReplHandlerTest.java

### 6.2 Implementation for User Story 4（已有基础）

- [x] T150 [US4] 验证 IntelliSqlClient 主入口 in intellisql-client/src/main/java/com/intellisql/client/IntelliSqlClient.java
- [x] T151 [US4] 验证 CommandParser in intellisql-client/src/main/java/com/intellisql/client/CommandParser.java
- [x] T152 [US4] 验证 ReplHandler in intellisql-client/src/main/java/com/intellisql/client/ReplHandler.java
- [x] T153 [US4] 验证 ClientException in intellisql-client/src/main/java/com/intellisql/client/ClientException.java

### 6.3 输出格式化

- [x] T154 [US4] 验证 ResultFormatter in intellisql-client/src/main/java/com/intellisql/client/ResultFormatter.java
- [x] T155 [US4] 验证 SyntaxHighlighter in intellisql-client/src/main/java/com/intellisql/client/SyntaxHighlighter.java
- [x] T156 [US4] 验证 PromptProvider in intellisql-client/src/main/java/com/intellisql/client/PromptProvider.java

### 6.4 命令实现

- [x] T157 [US4] 验证 Command 接口 in intellisql-client/src/main/java/com/intellisql/client/command/Command.java
- [x] T158 [P] [US4] 验证 QueryCommand in intellisql-client/src/main/java/com/intellisql/client/command/QueryCommand.java
- [x] T159 [P] [US4] 验证 TranslateCommand (--translate) in intellisql-client/src/main/java/com/intellisql/client/command/TranslateCommand.java
- [x] T160 [P] [US4] 验证 ScriptCommand (-f) in intellisql-client/src/main/java/com/intellisql/client/command/ScriptCommand.java

### 6.5 E2E 测试

- [x] T161 [US4] 创建 CommandLineIT in intellisql-test/intellisql-test-e2e/src/test/java/com/intellisql/e2e/CommandLineIT.java

**Checkpoint**: US4 完成 - 命令行工具 isql 可独立测试

---

## Phase 7: User Story 5 - 数据源元数据管理 (Priority: P3)

**Goal**: 通过配置文件定义和管理数据源连接信息

**Independent Test**:
- 创建/修改配置文件
- 重启 Server
- 验证新的数据源可被查询

### 7.1 Tests for User Story 5

- [x] T162 [P] [US5] 验证 SchemaMappingTest in intellisql-kernel/src/test/java/com/intellisql/kernel/metadata/SchemaMappingTest.java
- [x] T163 [US5] 创建 MetadataManagementIT in intellisql-test/intellisql-test-it/src/test/java/com/intellisql/it/metadata/MetadataManagementIT.java

### 7.2 Implementation for User Story 5

- [x] T164 [US5] 验证 SchemaMapping in intellisql-kernel/src/main/java/com/intellisql/kernel/metadata/SchemaMapping.java
- [x] T165 [US5] 验证 ColumnMapping in intellisql-kernel/src/main/java/com/intellisql/kernel/metadata/ColumnMapping.java
- [x] T166 [US5] 实现 SHOW SCHEMAS 支持 in QueryProcessor
- [x] T167 [US5] 实现 SHOW TABLES 支持 via SqlShowTables

**Checkpoint**: US5 完成 - 数据源元数据管理可独立测试

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: 跨用户故事的改进

### 分发包

- [x] T168 [P] 配置 intellisql-distribution-jdbc assembly in intellisql-distribution/intellisql-distribution-jdbc/pom.xml
- [x] T169 [P] 配置 intellisql-distribution-server assembly in intellisql-distribution/intellisql-distribution-server/pom.xml
- [x] T170 创建 bin/start.sh 启动脚本 in intellisql-distribution/intellisql-distribution-server/src/main/assembly/bin/start.sh
- [x] T171 [P] 创建 bin/stop.sh 停止脚本 in intellisql-distribution/intellisql-distribution-server/src/main/assembly/bin/stop.sh
- [x] T172 [P] 创建 bin/isql 客户端脚本 in intellisql-distribution/intellisql-distribution-jdbc/src/main/assembly/bin/isql

### 文档

- [x] T173 [P] 更新 README.md in README.md
- [x] T174 [P] 创建示例配置 conf/examples/dev-model.yaml

### E2E 测试

- [x] T175 创建 E2E 完整流程测试 in intellisql-test/intellisql-test-e2e/src/test/java/com/intellisql/e2e/EndToEndTest.java
- [x] T176 [P] 创建性能基准测试 in intellisql-test/intellisql-test-e2e/src/test/java/com/intellisql/e2e/PerformanceBenchmarkTest.java

### 质量检查

- [x] T177 运行 Spotless:apply on all modules
- [x] T178 运行 Checkstyle:check and fix violations
- [x] T179 验证所有测试通过 ./mvnw clean install

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖 - 可立即开始
- **Foundational (Phase 2)**: 依赖 Setup 完成 - **阻塞所有用户故事**
- **User Stories (Phase 3-7)**: 全部依赖 Foundational 完成
  - US1 和 US2 可以并行（P1 优先级）
  - US3 依赖 US1 的查询能力
  - US4 依赖 US2 的翻译能力和 US3 的 JDBC 连接
  - US5 可以在 Foundational 后开始
- **Polish (Phase 8)**: 依赖所有期望的用户故事完成

### User Story Dependencies

- **US1 (P1)**: Foundational 完成后可开始 - 核心联邦查询
- **US2 (P1)**: Foundational 完成后可开始 - SQL 翻译（独立）
- **US3 (P2)**: 依赖 US1 查询执行能力
- **US4 (P2)**: 依赖 US2 翻译能力和 US3 JDBC 连接
- **US5 (P3)**: Foundational 完成后可开始 - 配置管理

### Parallel Opportunities

- Setup 阶段所有标记 [P] 的任务可并行
- Foundational 阶段标记 [P] 的任务可并行
- US1 和 US2 可并行开始
- 连接器实现（MySQL/PostgreSQL/ES）可并行
- RBO 规则实现可并行
- 迭代器算子实现可并行

---

## Parallel Example: User Story 1 (联邦查询增强)

```bash
# 并行启动 RBO 规则实现:
Task T074: PredicatePushDownRule.java
Task T075: ProjectionPushDownRule.java
Task T076: JoinReorderRule.java
Task T077: SubqueryRewriteRule.java
Task T078: AggregateSplitRule.java
Task T079: LimitPushDownRule.java

# 并行启动迭代器算子:
Task T083: TableScanOperator.java
Task T084: FilterOperator.java
Task T085: ProjectOperator.java
Task T086: JoinOperator.java
Task T087: AggregateOperator.java
Task T088: SortOperator.java

# 并行启动连接器测试:
Task T030: MySQLConnectorIT.java
Task T031: PostgreSQLConnectorIT.java
Task T032: ElasticsearchConnectorIT.java
```

---

## Implementation Strategy

### MVP First (仅 User Story 1)

1. 完成 Phase 1: Setup
2. 完成 Phase 2: Foundational（阻塞关键路径）
3. 完成 Phase 3: User Story 1（含混合优化器和 Volcano 迭代器）
4. **停止并验证**: 独立测试 US1 跨源 JOIN
5. 如果就绪可部署/演示

### Incremental Delivery

1. 完成 Setup + Foundational → 基础就绪
2. 添加 US1 → 独立测试 → 部署/演示（**MVP!**）
3. 添加 US2 → 独立测试 → 部署/演示
4. 添加 US3 + US4 → 独立测试 → 部署/演示
5. 添加 US5 → 独立测试 → 部署/演示
6. 每个故事独立增加价值，不破坏之前的故事

### Parallel Team Strategy

多开发者协作:

1. 团队共同完成 Setup + Foundational
2. Foundational 完成后:
   - 开发者 A: User Story 1（联邦查询 + 优化器 + 迭代器）
   - 开发者 B: User Story 2（SQL 翻译）
3. US1 基础完成后:
   - 开发者 C: User Story 3（JDBC 接口）
4. US3 完成后:
   - 开发者 D: User Story 4（isql CLI）

---

## Summary

| 指标 | 数值 |
|------|------|
| 总任务数 | 179 |
| Phase 1 (Setup) | 7 |
| Phase 2 (Foundational) | 21 |
| US1 (联邦查询) | 76 |
| US2 (SQL翻译) | 22 |
| US3 (JDBC接口) | 21 |
| US4 (isql CLI) | 14 |
| US5 (元数据管理) | 6 |
| Phase 8 (Polish) | 12 |
| 可并行任务数 | 68 |

**MVP 范围**: Phase 1 + Phase 2 + Phase 3 (US1) = 104 任务

**新增增强功能（参考 ShardingSphere）**:
- 混合优化器策略 (RBO → CBO)
- 完整代价模型 (CPU + I/O + 网络 + 内存)
- 扩展 RBO 规则集 (6 条规则)
- Volcano 迭代器执行模型 (6 个算子)
- RelMetadataQuery 元数据支持

---

## Notes

- [P] 任务 = 不同文件，无依赖
- [Story] 标签映射任务到具体用户故事，便于追踪
- 每个用户故事应独立可完成和测试
- 参考 ShardingSphere sql-federation 实现优化器和迭代器模式
- 验证测试失败后再实现
- 每个任务或逻辑组完成后提交
- 在任何检查点停止以独立验证故事
