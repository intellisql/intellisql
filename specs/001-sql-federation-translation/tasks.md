# Tasks: IntelliSql SQL 联邦和翻译

**Input**: 设计文档来自 `/specs/001-sql-federation-translation/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: 本项目采用 TDD 方式，每个功能模块需编写单元测试

**Organization**: 任务按用户故事组织，支持独立实现和测试

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 任务所属用户故事（US1, US2, US3, US4, US5）
- 描述中包含精确的文件路径

## Path Conventions

```
intellisql/                        # 仓库根目录
├── intellisql-parser/             # SQL 解析模块
├── intellisql-optimizer/          # SQL 优化模块
├── intellisql-executor/           # SQL 执行模块
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

- [ ] T001 创建父 POM 文件 pom.xml，定义依赖版本、插件配置（Spotless 2.43.0、Checkstyle 3.3.1、Lombok 1.18.30）
- [ ] T002 [P] 创建 Maven Wrapper 配置 .mvn/wrapper/maven-wrapper.properties
- [ ] T003 [P] 创建 mvnw 和 mvnw.cmd 脚本
- [ ] T004 [P] 创建 Checkstyle 配置文件 src/resources/checkstyle/checkstyle.xml（参考 ShardingSphere 风格）
- [ ] T005 [P] 创建 Spotless 配置在 pom.xml 中（Google Java Format，无空行规则）
- [ ] T006 [P] 创建 logback.xml 配置 conf/logback.xml（JSON 格式日志，包含 Query ID、线程 ID）
- [ ] T007 创建 intellisql-parser 模块 pom.xml 和目录结构
- [ ] T008 [P] 创建 intellisql-optimizer 模块 pom.xml 和目录结构
- [ ] T009 [P] 创建 intellisql-executor 模块 pom.xml 和目录结构
- [ ] T010 [P] 创建 intellisql-connector 模块 pom.xml 和目录结构
- [ ] T011 [P] 创建 intellisql-kernel 模块 pom.xml 和目录结构
- [ ] T012 [P] 创建 intellisql-jdbc 模块 pom.xml 和目录结构
- [ ] T013 [P] 创建 intellisql-server 模块 pom.xml 和目录结构
- [ ] T014 [P] 创建 intellisql-client 模块 pom.xml 和目录结构
- [ ] T015 [P] 创建 intellisql-distribution 父模块及子模块 pom.xml
- [ ] T016 [P] 创建 intellisql-test 父模块及子模块 pom.xml
- [ ] T017 创建示例配置文件 conf/model.yaml
- [ ] T018 验证构建 ./mvnw clean install -DskipTests

**Checkpoint**: 项目结构完整，可成功构建

---

## Phase 2: Foundational（阻塞性前置条件）

**Purpose**: 所有用户故事依赖的核心基础设施

**⚠️ CRITICAL**: 此阶段完成前，任何用户故事工作都不能开始

### 2.1 枚举定义（基础类型）

- [ ] T019 [P] 创建 DataSourceType 枚举 in intellisql-kernel/src/main/java/org/intellisql/kernel/metadata/enums/DataSourceType.java
- [ ] T020 [P] 创建 DataType 枚举 in intellisql-kernel/src/main/java/org/intellisql/kernel/metadata/enums/DataType.java
- [ ] T021 [P] 创建 SchemaType 枚举 in intellisql-kernel/src/main/java/org/intellisql/kernel/metadata/enums/SchemaType.java
- [ ] T022 [P] 创建 TableType 枚举 in intellisql-kernel/src/main/java/org/intellisql/kernel/metadata/enums/TableType.java
- [ ] T023 [P] 创建 QueryStatus 枚举 in intellisql-kernel/src/main/java/org/intellisql/kernel/executor/enums/QueryStatus.java
- [ ] T024 [P] 创建 SqlDialect 枚举 in intellisql-parser/src/main/java/org/intellisql/parser/dialect/SqlDialect.java
- [ ] T025 [P] 创建 TranslationMode 枚举 in intellisql-parser/src/main/java/org/intellisql/parser/TranslationMode.java
- [ ] T026 [P] 创建 ConnectionStatus 枚举 in intellisql-server/src/main/java/org/intellisql/server/ConnectionStatus.java

### 2.2 配置加载（YAML 解析）

- [ ] T027 [P] 创建 Props 配置类 in intellisql-kernel/src/main/java/org/intellisql/kernel/config/Props.java
- [ ] T028 [P] 创建 HealthCheckConfig 值对象 in intellisql-kernel/src/main/java/org/intellisql/kernel/config/HealthCheckConfig.java
- [ ] T029 [P] 创建 ConnectionPoolConfig 值对象 in intellisql-kernel/src/main/java/org/intellisql/kernel/config/ConnectionPoolConfig.java
- [ ] T030 创建 DataSourceConfig 配置类 in intellisql-kernel/src/main/java/org/intellisql/kernel/config/DataSourceConfig.java
- [ ] T031 创建 ModelConfig 根配置类 in intellisql-kernel/src/main/java/org/intellisql/kernel/config/ModelConfig.java
- [ ] T032 实现 ConfigLoader YAML 解析 in intellisql-kernel/src/main/java/org/intellisql/kernel/config/ConfigLoader.java（使用 SnakeYAML 2.2）
- [ ] T033 实现环境变量替换逻辑 in intellisql-kernel/src/main/java/org/intellisql/kernel/config/EnvironmentVariableSubstitutor.java
- [ ] T034 创建 ConfigLoader 单元测试 in intellisql-kernel/src/test/java/org/intellisql/kernel/config/ConfigLoaderTest.java

### 2.3 日志基础设施（NFR-001 ~ NFR-005）

- [ ] T035 [P] 创建 QueryContext 查询上下文 in intellisql-kernel/src/main/java/org/intellisql/kernel/logging/QueryContext.java（包含 Query ID）
- [ ] T036 [P] 创建 StructuredLogger 结构化日志 in intellisql-kernel/src/main/java/org/intellisql/kernel/logging/StructuredLogger.java
- [ ] T037 实现 QueryContext MDC 管理器 in intellisql-kernel/src/main/java/org/intellisql/kernel/logging/QueryContextManager.java

### 2.4 重试机制（NFR-006 ~ NFR-009）

- [ ] T038 创建 RetryPolicy 重试策略 in intellisql-kernel/src/main/java/org/intellisql/kernel/retry/RetryPolicy.java
- [ ] T039 实现 ExponentialBackoffRetry 指数退避重试 in intellisql-kernel/src/main/java/org/intellisql/kernel/retry/ExponentialBackoffRetry.java
- [ ] T040 创建 TransientErrorDetector 瞬时错误检测 in intellisql-kernel/src/main/java/org/intellisql/kernel/retry/TransientErrorDetector.java
- [ ] T041 创建 RetryPolicy 单元测试 in intellisql-kernel/src/test/java/org/intellisql/kernel/retry/ExponentialBackoffRetryTest.java

**Checkpoint**: 基础设施就绪 - 用户故事实现可以并行开始

---

## Phase 3: User Story 1 - 跨数据源联邦查询 (Priority: P1) 🎯 MVP

**Goal**: 支持通过标准 SQL 执行跨异构数据源的 JOIN 查询

**Independent Test**:
- 配置 MySQL 和 Elasticsearch 两个数据源
- 执行跨源 JOIN 查询
- 验证结果正确合并两个数据源的数据

### 3.1 Tests for User Story 1

- [ ] T042 [P] [US1] 创建 Column 实体测试 in intellisql-kernel/src/test/java/org/intellisql/kernel/metadata/ColumnTest.java
- [ ] T043 [P] [US1] 创建 Table 实体测试 in intellisql-kernel/src/test/java/org/intellisql/kernel/metadata/TableTest.java
- [ ] T044 [P] [US1] 创建 Schema 实体测试 in intellisql-kernel/src/test/java/org/intellisql/kernel/metadata/SchemaTest.java
- [ ] T045 [P] [US1] 创建 DataSource 实体测试 in intellisql-kernel/src/test/java/org/intellisql/kernel/metadata/DataSourceTest.java
- [ ] T046 [P] [US1] 创建 QueryResult 集成测试 in intellisql-test/intellisql-test-it/src/test/java/org/intellisql/it/executor/QueryResultIT.java

### 3.2 Implementation for User Story 1

#### 元数据模型

- [ ] T047 [P] [US1] 创建 Column 实体 in intellisql-kernel/src/main/java/org/intellisql/kernel/metadata/Column.java
- [ ] T048 [P] [US1] 创建 Index 实体 in intellisql-kernel/src/main/java/org/intellisql/kernel/metadata/Index.java
- [ ] T049 [P] [US1] 创建 Table 实体 in intellisql-kernel/src/main/java/org/intellisql/kernel/metadata/Table.java
- [ ] T050 [US1] 创建 Schema 实体 in intellisql-kernel/src/main/java/org/intellisql/kernel/metadata/Schema.java
- [ ] T051 [US1] 创建 DataSource 实体 in intellisql-kernel/src/main/java/org/intellisql/kernel/metadata/DataSource.java
- [ ] T052 [US1] 创建 MetadataManager 元数据管理器 in intellisql-kernel/src/main/java/org/intellisql/kernel/metadata/MetadataManager.java

#### 连接器 SPI

- [ ] T053 [US1] 创建 DataSourceConnector 接口 in intellisql-connector/src/main/java/org/intellisql/connector/api/DataSourceConnector.java
- [ ] T054 [US1] 创建 Connection 接口 in intellisql-connector/src/main/java/org/intellisql/connector/api/Connection.java
- [ ] T055 [US1] 创建 SchemaDiscoverer 接口 in intellisql-connector/src/main/java/org/intellisql/connector/api/SchemaDiscoverer.java
- [ ] T056 [US1] 创建 QueryExecutor 接口 in intellisql-connector/src/main/java/org/intellisql/connector/api/QueryExecutor.java
- [ ] T057 [US1] 创建 ConnectorRegistry 注册中心 in intellisql-connector/src/main/java/org/intellisql/connector/ConnectorRegistry.java

#### MySQL 连接器

- [ ] T058 [US1] 创建 MySQLConnector 实现类 in intellisql-connector/src/main/java/org/intellisql/connector/mysql/MySQLConnector.java
- [ ] T059 [US1] 创建 MySQLSchemaDiscoverer in intellisql-connector/src/main/java/org/intellisql/connector/mysql/MySQLSchemaDiscoverer.java
- [ ] T060 [US1] 创建 MySQLQueryExecutor in intellisql-connector/src/main/java/org/intellisql/connector/mysql/MySQLQueryExecutor.java
- [ ] T061 [US1] 创建 MySQLConnectionPool in intellisql-connector/src/main/java/org/intellisql/connector/mysql/MySQLConnectionPool.java（使用 HikariCP 5.1.0）
- [ ] T062 [US1] 创建 MySQLConnector 单元测试 in intellisql-connector/src/test/java/org/intellisql/connector/mysql/MySQLConnectorTest.java

#### PostgreSQL 连接器

- [ ] T063 [P] [US1] 创建 PostgreSQLConnector in intellisql-connector/src/main/java/org/intellisql/connector/postgresql/PostgreSQLConnector.java
- [ ] T064 [P] [US1] 创建 PostgreSQLSchemaDiscoverer in intellisql-connector/src/main/java/org/intellisql/connector/postgresql/PostgreSQLSchemaDiscoverer.java
- [ ] T065 [P] [US1] 创建 PostgreSQLQueryExecutor in intellisql-connector/src/main/java/org/intellisql/connector/postgresql/PostgreSQLQueryExecutor.java
- [ ] T066 [P] [US1] 创建 PostgreSQLConnectionPool in intellisql-connector/src/main/java/org/intellisql/connector/postgresql/PostgreSQLConnectionPool.java
- [ ] T067 [P] [US1] 创建 PostgreSQLConnector 单元测试 in intellisql-connector/src/test/java/org/intellisql/connector/postgresql/PostgreSQLConnectorTest.java

#### Elasticsearch 连接器

- [ ] T068 [P] [US1] 创建 ElasticsearchConnector in intellisql-connector/src/main/java/org/intellisql/connector/elasticsearch/ElasticsearchConnector.java
- [ ] T069 [P] [US1] 创建 ElasticsearchSchemaDiscoverer in intellisql-connector/src/main/java/org/intellisql/connector/elasticsearch/ElasticsearchSchemaDiscoverer.java
- [ ] T070 [P] [US1] 创建 ElasticsearchQueryExecutor in intellisql-connector/src/main/java/org/intellisql/connector/elasticsearch/ElasticsearchQueryExecutor.java
- [ ] T071 [P] [US1] 创建 ElasticsearchTypeMapping in intellisql-connector/src/main/java/org/intellisql/connector/elasticsearch/ElasticsearchTypeMapping.java
- [ ] T072 [P] [US1] 创建 ElasticsearchConnector 单元测试 in intellisql-connector/src/test/java/org/intellisql/connector/elasticsearch/ElasticsearchConnectorTest.java

#### 健康检查

- [ ] T073 [US1] 创建 HealthChecker 接口 in intellisql-connector/src/main/java/org/intellisql/connector/health/HealthChecker.java
- [ ] T074 [US1] 创建 DataSourceHealthChecker 实现 in intellisql-connector/src/main/java/org/intellisql/connector/health/DataSourceHealthChecker.java
- [ ] T075 [US1] 创建 HealthCheckScheduler 调度器 in intellisql-connector/src/main/java/org/intellisql/connector/health/HealthCheckScheduler.java

#### SQL 解析（Calcite 集成）

- [ ] T076 [US1] 创建 SqlParserFactory in intellisql-parser/src/main/java/org/intellisql/parser/SqlParserFactory.java
- [ ] T077 [US1] 创建 IntelliSqlConventions 约定 in intellisql-parser/src/main/java/org/intellisql/parser/IntelliSqlConventions.java
- [ ] T078 [US1] 创建 SqlNodeToStringConverter in intellisql-parser/src/main/java/org/intellisql/parser/SqlNodeToStringConverter.java

#### SQL 优化器

- [ ] T079 [US1] 创建 ExecutionStage 实体 in intellisql-optimizer/src/main/java/org/intellisql/optimizer/plan/ExecutionStage.java
- [ ] T080 [US1] 创建 ExecutionPlan 实体 in intellisql-optimizer/src/main/java/org/intellisql/optimizer/plan/ExecutionPlan.java
- [ ] T081 [US1] 创建 Optimizer 核心类 in intellisql-optimizer/src/main/java/org/intellisql/optimizer/Optimizer.java
- [ ] T082 [US1] 创建 PredicatePushDownRule 谓词下推规则 in intellisql-optimizer/src/main/java/org/intellisql/optimizer/rule/PredicatePushDownRule.java
- [ ] T083 [US1] 创建 ProjectionPushDownRule 投影下推规则 in intellisql-optimizer/src/main/java/org/intellisql/optimizer/rule/ProjectionPushDownRule.java
- [ ] T084 [US1] 创建 Optimizer 单元测试 in intellisql-optimizer/src/test/java/org/intellisql/optimizer/OptimizerTest.java

#### SQL 执行器

- [ ] T085 [US1] 创建 Row 行数据 in intellisql-executor/src/main/java/org/intellisql/executor/Row.java
- [ ] T086 [US1] 创建 ColumnMetadata 列元数据 in intellisql-executor/src/main/java/org/intellisql/executor/ColumnMetadata.java
- [ ] T087 [US1] 创建 QueryError 错误信息 in intellisql-executor/src/main/java/org/intellisql/executor/QueryError.java
- [ ] T088 [US1] 创建 QueryResult 结果集 in intellisql-executor/src/main/java/org/intellisql/executor/QueryResult.java
- [ ] T089 [US1] 创建 Query 实体 in intellisql-executor/src/main/java/org/intellisql/executor/Query.java
- [ ] T090 [US1] 创建 QueryExecutor 核心执行器 in intellisql-executor/src/main/java/org/intellisql/executor/QueryExecutor.java
- [ ] T091 [US1] 创建 FederatedQueryExecutor 联邦查询执行器 in intellisql-executor/src/main/java/org/intellisql/executor/FederatedQueryExecutor.java
- [ ] T092 [US1] 创建 IntermediateResultLimiter 中间结果限制器 in intellisql-executor/src/main/java/org/intellisql/executor/IntermediateResultLimiter.java（NFR-010）
- [ ] T093 [US1] 创建 QueryExecutor 单元测试 in intellisql-executor/src/test/java/org/intellisql/executor/QueryExecutorTest.java

#### 核心编排层

- [ ] T094 [US1] 创建 IntelliSqlKernel 内核入口 in intellisql-kernel/src/main/java/org/intellisql/kernel/IntelliSqlKernel.java
- [ ] T095 [US1] 创建 QueryProcessor 查询处理器 in intellisql-kernel/src/main/java/org/intellisql/kernel/QueryProcessor.java
- [ ] T096 [US1] 创建 DataSourceManager 数据源管理器 in intellisql-kernel/src/main/java/org/intellisql/kernel/DataSourceManager.java

#### 集成测试

- [ ] T097 [US1] 创建 MySQL 容器测试 in intellisql-test/intellisql-test-it/src/test/java/org/intellisql/it/connector/MySQLConnectorIT.java（使用 TestContainers）
- [ ] T098 [US1] 创建 PostgreSQL 容器测试 in intellisql-test/intellisql-test-it/src/test/java/org/intellisql/it/connector/PostgreSQLConnectorIT.java
- [ ] T099 [US1] 创建 Elasticsearch 容器测试 in intellisql-test/intellisql-test-it/src/test/java/org/intellisql/it/connector/ElasticsearchConnectorIT.java
- [ ] T100 [US1] 创建跨源 JOIN 集成测试 in intellisql-test/intellisql-test-it/src/test/java/org/intellisql/it/federation/CrossSourceJoinIT.java

**Checkpoint**: US1 完成 - 跨数据源联邦查询功能可独立测试

---

## Phase 4: User Story 2 - SQL 方言翻译 (Priority: P1)

**Goal**: 将一种数据库的 SQL 翻译为另一种数据库兼容的 SQL

**Independent Test**:
- 输入 MySQL 方言的 SQL
- 指定目标方言为 PostgreSQL
- 验证输出的 SQL 符合 PostgreSQL 语法

### 4.1 Tests for User Story 2

- [ ] T101 [P] [US2] 创建 Translation 实体测试 in intellisql-parser/src/test/java/org/intellisql/parser/TranslationTest.java
- [ ] T102 [P] [US2] 创建 SqlTranslator 单元测试 in intellisql-parser/src/test/java/org/intellisql/parser/SqlTranslatorTest.java

### 4.2 Implementation for User Story 2

- [ ] T103 [US2] 创建 Translation 实体 in intellisql-parser/src/main/java/org/intellisql/parser/Translation.java
- [ ] T104 [US2] 创建 TranslationError 错误信息 in intellisql-parser/src/main/java/org/intellisql/parser/TranslationError.java
- [ ] T105 [US2] 创建 SqlTranslator 核心翻译器 in intellisql-parser/src/main/java/org/intellisql/parser/SqlTranslator.java
- [ ] T106 [US2] 创建 DialectConverter 方言转换器 in intellisql-parser/src/main/java/org/intellisql/parser/DialectConverter.java
- [ ] T107 [US2] 创建 BabelParserConfiguration Calcite Babel 配置 in intellisql-parser/src/main/java/org/intellisql/parser/BabelParserConfiguration.java
- [ ] T108 [US2] 创建 MySQL dialect 适配 in intellisql-parser/src/main/java/org/intellisql/parser/dialect/MySQLDialectConverter.java
- [ ] T109 [P] [US2] 创建 PostgreSQL dialect 适配 in intellisql-parser/src/main/java/org/intellisql/parser/dialect/PostgreSQLDialectConverter.java
- [ ] T110 [P] [US2] 创建 Oracle dialect 适配 in intellisql-parser/src/main/java/org/intellisql/parser/dialect/OracleDialectConverter.java
- [ ] T111 [P] [US2] 创建 SQLServer dialect 适配 in intellisql-parser/src/main/java/org/intellisql/parser/dialect/SQLServerDialectConverter.java
- [ ] T112 [P] [US2] 创建 Hive dialect 适配 in intellisql-parser/src/main/java/org/intellisql/parser/dialect/HiveDialectConverter.java
- [ ] T113 [US2] 创建 OnlineTranslationService 在线翻译服务 in intellisql-parser/src/main/java/org/intellisql/parser/OnlineTranslationService.java
- [ ] T114 [US2] 创建 OfflineTranslationService 离线翻译服务 in intellisql-parser/src/main/java/org/intellisql/parser/OfflineTranslationService.java

**Checkpoint**: US2 完成 - SQL 方言翻译功能可独立测试

---

## Phase 5: User Story 3 - JDBC 标准接口访问 (Priority: P2)

**Goal**: 通过标准 JDBC 接口连接 IntelliSql Server

**Independent Test**:
- 使用 JDBC 客户端工具
- 连接 IntelliSql Server
- 执行 SQL 查询并获取结果

### 5.1 Tests for User Story 3

- [ ] T115 [P] [US3] 创建 IntelliSqlDriver 单元测试 in intellisql-jdbc/src/test/java/org/intellisql/jdbc/IntelliSqlDriverTest.java
- [ ] T116 [P] [US3] 创建 IntelliSqlConnection 单元测试 in intellisql-jdbc/src/test/java/org/intellisql/jdbc/IntelliSqlConnectionTest.java
- [ ] T117 [P] [US3] 创建 JDBC 集成测试 in intellisql-test/intellisql-test-it/src/test/java/org/intellisql/it/jdbc/JdbcConnectionIT.java

### 5.2 Implementation for User Story 3

#### JDBC 驱动

- [ ] T118 [US3] 创建 IntelliSqlDriver 驱动类 in intellisql-jdbc/src/main/java/org/intellisql/jdbc/IntelliSqlDriver.java
- [ ] T119 [US3] 创建 DriverRegistration 驱动注册 in intellisql-jdbc/src/main/java/org/intellisql/jdbc/DriverRegistration.java
- [ ] T120 [US3] 创建 JdbcUrlParser URL 解析器 in intellisql-jdbc/src/main/java/org/intellisql/jdbc/JdbcUrlParser.java
- [ ] T121 [US3] 创建 META-INF/services/java.sql.Driver in intellisql-jdbc/src/main/resources/META-INF/services/java.sql.Driver
- [ ] T122 [US3] 创建 IntelliSqlConnection 连接类 in intellisql-jdbc/src/main/java/org/intellisql/jdbc/IntelliSqlConnection.java
- [ ] T123 [US3] 创建 IntelliSqlStatement 语句类 in intellisql-jdbc/src/main/java/org/intellisql/jdbc/IntelliSqlStatement.java
- [ ] T124 [US3] 创建 IntelliSqlPreparedStatement 预编译语句 in intellisql-jdbc/src/main/java/org/intellisql/jdbc/IntelliSqlPreparedStatement.java
- [ ] T125 [US3] 创建 IntelliSqlResultSet 结果集 in intellisql-jdbc/src/main/java/org/intellisql/jdbc/IntelliSqlResultSet.java
- [ ] T126 [US3] 创建 IntelliSqlDatabaseMetaData 元数据 in intellisql-jdbc/src/main/java/org/intellisql/jdbc/IntelliSqlDatabaseMetaData.java
- [ ] T127 [US3] 创建 AvaticaClient Avatica 客户端 in intellisql-jdbc/src/main/java/org/intellisql/jdbc/AvaticaClient.java

#### Server 端（Avatica 协议）

- [ ] T128 [US3] 创建 Connection 实体 in intellisql-server/src/main/java/org/intellisql/server/Connection.java
- [ ] T129 [US3] 创建 ConnectionManager 连接管理器 in intellisql-server/src/main/java/org/intellisql/server/ConnectionManager.java
- [ ] T130 [US3] 创建 IntelliSqlServer 服务入口 in intellisql-server/src/main/java/org/intellisql/server/IntelliSqlServer.java
- [ ] T131 [US3] 创建 AvaticaHandler Avatica 处理器 in intellisql-server/src/main/java/org/intellisql/server/AvaticaHandler.java
- [ ] T132 [US3] 创建 IntelliSqlMeta 元数据服务 in intellisql-server/src/main/java/org/intellisql/server/IntelliSqlMeta.java
- [ ] T133 [US3] 创建 ServerConfig 服务器配置 in intellisql-server/src/main/java/org/intellisql/server/ServerConfig.java
- [ ] T134 [US3] 创建 Main 启动类 in intellisql-server/src/main/java/org/intellisql/server/Main.java
- [ ] T135 [US3] 创建启动脚本 bin/start.sh in bin/start.sh

#### E2E 测试

- [ ] T136 [US3] 创建 JDBC E2E 测试 in intellisql-test/intellisql-test-e2e/src/test/java/org/intellisql/e2e/jdbc/JdbcE2ETest.java
- [ ] T137 [US3] 创建大结果集 E2E 测试 in intellisql-test/intellisql-test-e2e/src/test/java/org/intellisql/e2e/jdbc/LargeResultSetE2ETest.java（100 万行）

**Checkpoint**: US3 完成 - JDBC 标准接口访问可独立测试

---

## Phase 6: User Story 4 - 命令行工具 isql (Priority: P2)

**Goal**: 提供专业的命令行工具连接 Server，执行查询和翻译 SQL

**Independent Test**:
- 下载 isql 客户端包
- 执行 isql 命令连接 Server
- 执行 SQL 查询和翻译命令

### 6.1 Tests for User Story 4

- [ ] T138 [P] [US4] 创建 ISqlClient 单元测试 in intellisql-client/src/test/java/org/intellisql/client/ISqlClientTest.java
- [ ] T139 [P] [US4] 创建 CommandParser 单元测试 in intellisql-client/src/test/java/org/intellisql/client/CommandParserTest.java

### 6.2 Implementation for User Story 4

- [ ] T140 [US4] 创建 ISqlClient 主入口 in intellisql-client/src/main/java/org/intellisql/client/ISqlClient.java
- [ ] T141 [US4] 创建 CommandParser 命令解析器 in intellisql-client/src/main/java/org/intellisql/client/CommandParser.java
- [ ] T142 [US4] 创建 ReplHandler 交互处理器 in intellisql-client/src/main/java/org/intellisql/client/ReplHandler.java
- [ ] T143 [US4] 创建 QueryCommand 查询命令 in intellisql-client/src/main/java/org/intellisql/client/command/QueryCommand.java
- [ ] T144 [US4] 创建 TranslateCommand 翻译命令 in intellisql-client/src/main/java/org/intellisql/client/command/TranslateCommand.java
- [ ] T145 [US4] 创建 ScriptCommand 脚本命令 in intellisql-client/src/main/java/org/intellisql/client/command/ScriptCommand.java
- [ ] T146 [US4] 创建 ResultFormatter 结果格式化 in intellisql-client/src/main/java/org/intellisql/client/ResultFormatter.java
- [ ] T147 [US4] 创建 SyntaxHighlighter 语法高亮 in intellisql-client/src/main/java/org/intellisql/client/SyntaxHighlighter.java
- [ ] T148 [US4] 创建 PromptProvider 提示符 in intellisql-client/src/main/java/org/intellisql/client/PromptProvider.java
- [ ] T149 [US4] 创建 ISqlClient 启动脚本 in bin/isql

**Checkpoint**: US4 完成 - 命令行工具 isql 可独立测试

---

## Phase 7: User Story 5 - 数据源元数据管理 (Priority: P3)

**Goal**: 通过配置文件定义和管理数据源连接信息

**Independent Test**:
- 创建/修改配置文件
- 重启 Server
- 验证新的数据源可被查询

### 7.1 Tests for User Story 5

- [ ] T150 [P] [US5] 创建 SchemaMapping 测试 in intellisql-kernel/src/test/java/org/intellisql/kernel/metadata/SchemaMappingTest.java
- [ ] T151 [P] [US5] 创建配置加载 E2E 测试 in intellisql-test/intellisql-test-e2e/src/test/java/org/intellisql/e2e/config/ConfigLoadingE2ETest.java

### 7.2 Implementation for User Story 5

- [ ] T152 [US5] 创建 SchemaMapping 模式映射 in intellisql-kernel/src/main/java/org/intellisql/kernel/metadata/SchemaMapping.java
- [ ] T153 [US5] 创建 ColumnMapping 列映射 in intellisql-kernel/src/main/java/org/intellisql/kernel/metadata/ColumnMapping.java
- [ ] T154 [US5] 创建 SchemaDiscovererService 自动发现服务 in intellisql-kernel/src/main/java/org/intellisql/kernel/metadata/SchemaDiscovererService.java
- [ ] T155 [US5] 创建 ConfigValidator 配置验证器 in intellisql-kernel/src/main/java/org/intellisql/kernel/config/ConfigValidator.java
- [ ] T156 [US5] 创建 ConfigHotReloader 热加载器 in intellisql-kernel/src/main/java/org/intellisql/kernel/config/ConfigHotReloader.java
- [ ] T157 [US5] 实现 SHOW SCHEMAS 支持 in intellisql-kernel/src/main/java/org/intellisql/kernel/metadata/ShowSchemasHandler.java
- [ ] T158 [US5] 实现 SHOW TABLES 支持 in intellisql-kernel/src/main/java/org/intellisql/kernel/metadata/ShowTablesHandler.java

**Checkpoint**: US5 完成 - 数据源元数据管理可独立测试

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: 跨用户故事的改进

### 文档

- [ ] T159 [P] 更新 README.md 项目说明
- [ ] T160 [P] 创建 CONTRIBUTING.md 贡献指南
- [ ] T161 [P] 验证 quickstart.md 场景

### 分发包

- [ ] T162 [P] 创建 JDBC Driver 分发包 in intellisql-distribution/intellisql-distribution-jdbc/pom.xml
- [ ] T163 [P] 创建 Server 分发包 in intellisql-distribution/intellisql-distribution-server/pom.xml
- [ ] T164 创建 Server 打包脚本 in intellisql-distribution/intellisql-distribution-server/src/main/assembly/server.xml

### 质量检查

- [ ] T165 运行 Spotless 检查 ./mvnw spotless:check
- [ ] T166 运行 Checkstyle 检查 ./mvnw checkstyle:check
- [ ] T167 运行完整测试 ./mvnw clean install -Pcheck

### 性能验证

- [ ] T168 验证单表查询开销 < 50ms in intellisql-test/intellisql-test-e2e/src/test/java/org/intellisql/e2e/performance/QueryPerformanceTest.java
- [ ] T169 验证跨源 JOIN（10万行）< 5s in intellisql-test/intellisql-test-e2e/src/test/java/org/intellisql/e2e/performance/FederatedQueryPerformanceTest.java
- [ ] T170 验证 100 并发连接 in intellisql-test/intellisql-test-e2e/src/test/java/org/intellisql/e2e/performance/ConcurrentConnectionTest.java

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖 - 可立即开始
- **Foundational (Phase 2)**: 依赖 Setup 完成 - **阻塞所有用户故事**
- **User Stories (Phase 3-7)**: 全部依赖 Foundational 完成
  - US1 和 US2 可以并行（P1 优先级）
  - US3 和 US4 可以并行（P2 优先级）
  - US5 最后实现（P3 优先级）
- **Polish (Phase 8)**: 依赖所有期望的用户故事完成

### User Story Dependencies

- **US1 (P1)**: Foundational 完成后可开始 - 无其他故事依赖
- **US2 (P1)**: Foundational 完成后可开始 - 无其他故事依赖
- **US3 (P2)**: 依赖 US1 的元数据和查询能力
- **US4 (P2)**: 依赖 US2 的翻译能力、US3 的 JDBC 连接
- **US5 (P3)**: 依赖 US1 的元数据管理能力

### Within Each User Story

- 测试先行（TDD）
- 枚举 → 实体 → 服务 → 接口
- 核心实现 → 集成
- 故事完成后才能进入下一优先级

### Parallel Opportunities

- Setup 阶段所有标记 [P] 的任务可并行
- Foundational 阶段标记 [P] 的任务可并行（Phase 2 内）
- Foundational 完成后，US1 和 US2 可并行
- 同一用户故事内标记 [P] 的任务可并行

---

## Parallel Example: User Story 1 (联邦查询)

```bash
# 并行启动 US1 所有测试任务:
Task T042: ColumnTest.java
Task T043: TableTest.java
Task T044: SchemaTest.java
Task T045: DataSourceTest.java
Task T046: QueryResultIT.java

# 并行启动元数据模型:
Task T047: Column.java
Task T048: Index.java

# 并行启动各连接器:
Task T058-T062: MySQL 连接器
Task T063-T067: PostgreSQL 连接器
Task T068-T072: Elasticsearch 连接器
```

---

## Implementation Strategy

### MVP First (仅 User Story 1)

1. 完成 Phase 1: Setup
2. 完成 Phase 2: Foundational（阻塞关键路径）
3. 完成 Phase 3: User Story 1
4. **停止并验证**: 独立测试 US1
5. 如果就绪可部署/演示

### Incremental Delivery

1. 完成 Setup + Foundational → 基础就绪
2. 添加 US1 → 独立测试 → 部署/演示（**MVP!**）
3. 添加 US2 → 独立测试 → 部署/演示
4. 添加 US3 → 独立测试 → 部署/演示
5. 添加 US4 → 独立测试 → 部署/演示
6. 添加 US5 → 独立测试 → 部署/演示
7. 每个故事独立增加价值，不破坏之前的故事

### Parallel Team Strategy

多开发者协作:

1. 团队共同完成 Setup + Foundational
2. Foundational 完成后:
   - 开发者 A: User Story 1（联邦查询）
   - 开发者 B: User Story 2（SQL 翻译）
3. US1 和 US2 完成后:
   - 开发者 A: User Story 3（JDBC 接口）
   - 开发者 B: User Story 4（isql CLI）
4. US3 和 US4 完成后:
   - 开发者 A: User Story 5（元数据管理）
   - 开发者 B: Polish & 性能测试

---

## Summary

| 指标 | 数值 |
|------|------|
| 总任务数 | 170 |
| Phase 1 (Setup) | 18 |
| Phase 2 (Foundational) | 23 |
| US1 (联邦查询) | 59 |
| US2 (SQL翻译) | 14 |
| US3 (JDBC接口) | 23 |
| US4 (isql CLI) | 12 |
| US5 (元数据管理) | 9 |
| Phase 8 (Polish) | 12 |
| 可并行任务数 | 65 |

**MVP 范围**: Phase 1 + Phase 2 + Phase 3 (US1) = 100 任务

---

## Notes

- [P] 任务 = 不同文件，无依赖
- [Story] 标签映射任务到具体用户故事，便于追踪
- 每个用户故事应独立可完成和测试
- 验证测试失败后再实现
- 每个任务或逻辑组完成后提交
- 在任何检查点停止以独立验证故事
- 避免：模糊任务、相同文件冲突、破坏独立性的跨故事依赖
