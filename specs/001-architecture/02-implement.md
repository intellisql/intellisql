# IntelliSQL 架构实现说明

更新时间：2026-04-19

本文件根据 [`01-research.md`](./01-research.md) 的架构方案与当前代码库真实实现整理，重点描述 IntelliSQL 当前的整体架构设计、模块边界、运行时装配、扩展机制、部署形态与架构缺口。本文尽量少展开联邦查询、翻译器等单点功能细节，重点放在“系统如何被组织成一个可运行的产品”。

## 1. 架构总览

从代码实现看，IntelliSQL 当前采用的是“多模块单进程内核 + HTTP 协议服务端 + 自定义 JDBC 驱动 + CLI 前端 + SPI 插件扩展”的架构。

研究文档中的理想分层大致是：

- 解析层
- 计算层
- 存储层
- 协议层
- 客户端层

当前仓库里的实际落地方式更细，已经演化为 Maven 多模块拆分：

- `intellisql-spi`
- `intellisql-common`
- `intellisql-parser`
- `intellisql-features`
- `intellisql-connector`
- `intellisql-plugins`
- `intellisql-jdbc`
- `intellisql-server`
- `intellisql-client`
- `intellisql-distributions`
- `intellisql-tests`

也就是说，研究文档里的“一个 intellisql-core 承载核心能力”的思路，在当前实现里被拆成了多个内聚模块：

- `common` 承载共享配置、元数据模型、日志和重试
- `parser` 承载 SQL 解析器与扩展 AST
- `features` 承载内核能力
- `connector` 承载连接器抽象
- `plugins` 承载具体数据库与方言扩展
- `server/jdbc/client` 分别承载对外协议入口

这个拆分比研究方案更工程化。它不是一个“大核心包”，而是一组围绕运行时装配协作的中等粒度模块。

## 2. 根工程与模块编排

根 `pom.xml` 是整个系统的编排中心。

### 2.1 聚合关系

根工程声明了全部一级模块：

- `intellisql-spi`
- `intellisql-common`
- `intellisql-parser`
- `intellisql-features`
- `intellisql-connector`
- `intellisql-plugins`
- `intellisql-jdbc`
- `intellisql-server`
- `intellisql-client`
- `intellisql-distributions`
- `intellisql-tests`

这表示 IntelliSQL 当前是标准的多模块聚合构建，而不是分散仓库。

### 2.2 技术基线

根 `pom.xml` 明确了架构级技术约束：

- Java 基线是 JDK 8
- Apache Calcite 版本是 `1.41.0`
- Avatica 版本是 `1.27.0`
- 连接池使用 HikariCP
- 配置读取使用 SnakeYAML
- 日志使用 SLF4J + Logback
- 测试使用 JUnit 5、Mockito、AssertJ、Testcontainers

这意味着 IntelliSQL 的架构目标很明确：

- 保持 Java 8 兼容
- 以 Calcite 作为语法、关系代数和优化基础设施
- 以 Avatica 作为远程 JDBC 协议层
- 以 Testcontainers 支撑集成测试

### 2.3 功能模块的二级聚合

`intellisql-features/pom.xml` 本身只是一个聚合层，继续拆成：

- `intellisql-feature-federation`
- `intellisql-feature-translator`
- `intellisql-feature-optimizer`

这说明 IntelliSQL 当前的“能力内核”不是按部署拆，而是按业务能力域拆。

从架构设计上看，这种拆法有两个效果：

- 上层应用可以按能力选择依赖
- 内核能力之间可以通过 Java 依赖直接协作，而不是走进程间协议

## 3. 当前实际分层

研究文档强调解析层、计算层、存储层。当前代码进一步细化为七层。

### 3.1 基础模型层

由 `intellisql-common` 提供。

它承载：

- 配置模型
- 元数据模型
- 查询上下文
- 重试机制
- 结构化日志基础设施

关键类：

- `com.intellisql.common.config.ConfigLoader`
- `com.intellisql.common.config.ModelConfig`
- `com.intellisql.common.config.DataSourceConfig`
- `com.intellisql.common.config.Props`
- `com.intellisql.common.metadata.*`
- `com.intellisql.common.logger.QueryContext`
- `com.intellisql.common.logger.QueryContextManager`
- `com.intellisql.common.retry.ExponentialBackoffRetry`

这一层的架构意义非常大。它把“核心领域对象”从功能实现中抽离出来，让上层模块围绕统一对象协作，而不是各自维护独立模型。

### 3.2 扩展契约层

由 `intellisql-spi` 提供。

它承载：

- 方言 SPI
- 类型枚举与标准方言抽象
- 通用 ServiceLoader 封装

关键类：

- `com.intellisql.spi.TypedSPI`
- `com.intellisql.spi.database.DatabaseDialect`
- `com.intellisql.spi.database.DatabaseDialectRegistry`
- `com.intellisql.spi.loader.IntelliSqlServiceLoader`

这一层定义的不是业务逻辑，而是扩展入口。它的职责是把插件接入方式稳定下来。

### 3.3 语言前端层

由 `intellisql-parser` 提供。

它承载：

- SQL parser 工厂
- Babel 宽松解析配置
- 自定义扩展 AST
- SQL 节点转字符串的通用封装

关键类：

- `SqlParserFactory`
- `BabelParserConfiguration`
- `SqlNodeToStringConverter`
- `SqlShowTables`
- `SqlShowSchemas`
- `SqlUseSchema`

这一层的定位接近编译器前端。

### 3.4 数据源接入层

由 `intellisql-connector` 提供。

它承载：

- 数据源连接器接口
- JDBC 抽象基类
- 查询执行抽象
- schema discovery 抽象
- 健康检查
- 连接池包装

关键类：

- `DataSourceConnector`
- `IntelliSQLConnection`
- `QueryExecutor`
- `SchemaDiscoverer`
- `AbstractJdbcConnector`
- `AbstractJdbcQueryExecutor`
- `JdbcConnectionPool`
- `ConnectorRegistry`

这一层将“接入一个外部系统”抽象成统一合同。

### 3.5 能力内核层

由 `intellisql-features` 下的各子模块提供。

当前真正承担“应用内核”角色的是 `intellisql-feature-federation`，因为：

- 服务端启动时实际初始化的是 `IntelliSqlKernel`
- `IntelliSqlKernel` 内部装配了 `DataSourceManager`、`MetadataManager`、`QueryProcessor`、`HybridOptimizer`
- 服务端的 `Meta` 实现最终持有的也是这个 kernel

换句话说，虽然仓库已经拆出 translator 和 optimizer 模块，但运行时“系统核心”仍然围绕 federation kernel 来组织。

### 3.6 协议与接入层

分成三个入口：

- `intellisql-server`
- `intellisql-jdbc`
- `intellisql-client`

这三个模块共同组成对外接口面。

### 3.7 交付与验证层

由：

- `intellisql-distributions`
- `intellisql-tests`

提供。

前者负责可安装包和脚本，后者负责 IT/E2E 验证。

## 4. 架构核心对象与边界

### 4.1 `ModelConfig` 是系统配置总根

`com.intellisql.common.config.ModelConfig` 是整个内核装配的配置根对象。

它只聚合两部分：

- `Map<String, DataSourceConfig> dataSources`
- `Props props`

这说明当前架构把系统配置明确分成两类：

- 数据源配置
- 全局行为配置

这个设计比较清晰。它避免了把所有配置平铺在一个巨大的配置类里。

### 4.2 `IntelliSqlKernel` 是当前运行时总装配点

`com.intellisql.federation.IntelliSqlKernel` 是现在最接近“应用核心”的类。

它在构造阶段装配：

- `DataSourceManager`
- `HybridOptimizer`
- `MetadataManager`
- `QueryProcessor`

它在初始化阶段负责：

- 数据源初始化
- 元数据初始化

它向外暴露：

- `query(...)`
- `translate(...)`
- `getMetadataManager()`
- `getQueryProcessor()`
- `getDataSourceManager()`

从架构角度看，`IntelliSqlKernel` 兼有两个角色：

- 运行时容器
- 应用服务门面

这是一种比较典型的早期内核设计。优点是简单、集中、好启动；缺点是内核聚合度偏高，未来如果 server、CLI、embedded mode 要走不同启动模型，kernel 可能需要继续拆分。

### 4.3 `MetadataManager` 是共享元数据中心

从整体架构看，`MetadataManager` 的价值不只在“元数据查询”，更在于它承担了跨层共享职责：

- 连接器把外部 schema discovery 结果汇入这里
- Calcite root schema 从这里构造
- 服务端 `Meta` 也从这里读取元数据

这意味着 `MetadataManager` 现在同时服务三类场景：

- 规划期
- 执行期
- 协议暴露期

它本质上是一个进程内元数据注册中心。

## 5. 编译时依赖关系

### 5.1 从底到上的依赖方向

当前代码基本遵循下面的依赖方向：

1. `intellisql-spi` 与 `intellisql-common` 处于底层
2. `intellisql-parser`、`intellisql-connector` 依赖底层
3. `intellisql-plugins` 依赖 SPI 和 connector
4. `intellisql-feature-translator` 依赖 SPI、common、parser
5. `intellisql-feature-optimizer` 依赖 Calcite
6. `intellisql-feature-federation` 依赖 SPI、common、parser、translator、optimizer、connector
7. `intellisql-server` 依赖 federation feature 和具体插件
8. `intellisql-client` 依赖 JDBC、parser、translator 和具体插件
9. `intellisql-jdbc` 依赖 Avatica 与具体插件，不依赖 features
10. `intellisql-distributions` 依赖 server/client/jdbc
11. `intellisql-tests` 横向验证多个模块

这个方向整体是健康的，因为上层入口模块没有把底层逻辑反向注入回去。需要特别注意的是，具体插件通过 classpath 和 SPI 被入口模块装配，federation feature 并不在编译期直接依赖具体数据库插件。

### 5.2 一个重要事实

`intellisql-server/pom.xml` 直接依赖：

- `intellisql-feature-federation`
- 所有数据库插件

这说明服务端不是纯“协议壳”。它在部署层面主动决定了插件组合。

架构含义是：

- server 包含完整运行时
- server 自身承担插件装配责任
- 插件不是运行期热发现 jar 目录模式，而是构建期依赖驱动的 classpath 装配

这是一个非常关键的设计选择。它决定了 IntelliSQL 当前更像“内置插件的单体服务”，而不是“运行时可插拔容器”。

### 5.3 client 也直接依赖插件

`intellisql-client/pom.xml` 也直接依赖：

- `intellisql-parser`
- `intellisql-feature-translator`
- 各数据库插件

这说明 CLI 不是一个完全瘦客户端，它内置了本地翻译与本地方言识别能力。

因此当前客户端架构实际上是“双模式”：

- 连接 server 时是远程 SQL 客户端
- 执行 translate 命令时又是本地能力宿主

这是一个较强的设计信号：CLI 并不只是 JDBC shell，而是产品级前端。

## 6. 运行时拓扑

从当前实现看，IntelliSQL 的典型部署拓扑分为三类角色。

### 6.1 Server 进程

入口类是：

- `com.intellisql.server.IntelliSqlServer`

职责是：

- 读取配置
- 初始化 kernel
- 将 kernel 的 `MetadataManager` 和 kernel 本身注入 `IntelliSqlMeta`
- 使用 Avatica `LocalService` 暴露 HTTP 协议
- 维护进程生命周期

服务端是整个系统真正的长期运行节点。

### 6.2 JDBC Driver

入口类是：

- `com.intellisql.jdbc.IntelliSqlDriver`

职责是：

- 注册 JDBC 驱动
- 解析 `jdbc:intellisql://...` URL
- 构造 `AvaticaClient`
- 返回 `IntelliSqlConnection`

也就是说，自定义 JDBC 驱动只负责“把 IntelliSQL 服务伪装成 JDBC 数据库”。

### 6.3 CLI 进程

入口类是：

- `com.intellisql.client.IntelliSqlClient`

职责是：

- 管理终端循环
- 处理斜杠命令
- 通过 JDBC 与服务端通信
- 在本地执行部分命令和渲染逻辑

CLI 不是必须组件，但它是最直接的交互界面。

## 7. Server 启动装配链

### 7.1 入口与生命周期

`IntelliSqlServer.main(...)` 的流程是：

1. 解析端口参数
2. 构造 `ServerConfig`
3. 注册 shutdown hook
4. 调用 `start()`
5. 在主线程中保持运行

### 7.2 `start()` 的装配动作

`IntelliSqlServer.start()` 的架构动作依次是：

1. `initializeKernel()`
2. 基于 `IntelliSqlMeta` 创建 `LocalService`
3. 创建 `AvaticaProtobufHandler`
4. 创建并启动 `HttpServer`

这表明当前 server 结构非常直接：

- kernel 在进程内
- meta 在进程内
- Avatica service 在进程内
- HTTP server 只是最外一层包装

没有额外的应用容器、Spring、Guice 或外部依赖注入框架。

### 7.3 `initializeKernel()` 的真实行为

`initializeKernel()` 当前的行为非常重要：

- 固定从 `conf/model.yaml` 读取配置
- 若类路径没有该资源，再尝试读取工作目录里的同名文件
- 将配置复制到临时文件
- 调用 `IntelliSqlKernel.create(tempConfig)`
- 调用 `kernel.initialize()`
- 将 `MetadataManager` 和 `kernel` 注入 `IntelliSqlMeta`

这说明当前 server 配置模型仍然是“单配置文件驱动内核启动”，并没有独立的 server 配置与 kernel 配置分层。

### 7.4 容错策略

如果 `initializeKernel()` 失败，当前逻辑是：

- 记录 warning
- 服务继续启动
- 以空 metadata 状态对外提供服务

这是一种“协议层尽量可用”的设计，但也意味着：

- 服务可能已经监听端口
- 内核却没有形成完整能力

对架构来说，这是一种偏宽松的启动策略。

## 8. 协议层设计

### 8.1 Avatica 是协议中心

当前 IntelliSQL 的远程协议核心并不是自定义 REST API，而是 Avatica。

服务端使用：

- `LocalService`
- `AvaticaProtobufHandler`
- `HttpServer`

客户端使用：

- `AvaticaClient`
- `IntelliSqlConnection`
- `IntelliSqlStatement`
- `IntelliSqlPreparedStatement`

这意味着 IntelliSQL 从一开始就把“远程数据库协议兼容”放在高优先级上，而不是先定义自己的 HTTP JSON 协议。

### 8.2 `IntelliSqlMeta` 的角色

`com.intellisql.server.IntelliSqlMeta` 是协议层和内核之间的桥梁。

它负责：

- 管理 Avatica `ConnectionHandle`
- 管理 `StatementHandle`
- 构造 `Signature`
- 处理 `prepare`
- 处理 `prepareAndExecute`
- 返回 metadata 结果集

从架构设计上看，它是一个“适配器层”：

- 上游说 Avatica
- 下游说 IntelliSQL kernel 和 metadata manager

### 8.3 当前协议层是“薄实现”

`IntelliSqlMeta` 当前大多数 metadata API 都直接返回 `emptyMetaResultSet()`。

只有少量路径有真实逻辑：

- `openConnection`
- `createStatement`
- `prepare`
- `prepareAndExecute`
- `SHOW TABLES` 的最小支持

所以当前协议层架构已经搭起来，但实现深度仍偏浅。它更像“协议骨架已完成，业务映射仍在补齐”的阶段。

## 9. JDBC 驱动设计

### 9.1 驱动注册方式

`IntelliSqlDriver` 在静态块中直接执行：

- `DriverManager.registerDriver(new IntelliSqlDriver())`

配合 `META-INF/services/java.sql.Driver`，这属于典型 JDBC SPI 注册方案。

### 9.2 URL 设计

`JdbcUrlParser` 解析的 URL 形式是：

- `jdbc:intellisql://host:port/database?properties`

它负责输出：

- `getEndpoint()`
- `getProtobufEndpoint()`

当前约定的 Protobuf 端点是：

- `http://host:port/api/protobuf`

这说明 IntelliSQL 在 JDBC 侧显式隐藏了 Avatica 细节，给用户暴露的是自己的品牌 URL，而不是原始 Avatica URL。

### 9.3 驱动层职责边界

驱动层不做真正的 SQL 处理。它只负责：

- URL 解释
- 属性合并
- 远程 client 初始化
- JDBC API 包装

这是合理的架构切分。SQL 语义不应进入驱动层。

因此当前 `intellisql-jdbc` 是 remote-only thin driver。联邦查询、翻译、优化等 features 由 server 或 CLI 承载；嵌入式 JDBC、本地 kernel 执行等模式属于未来演进方向。

## 10. CLI 架构设计

### 10.1 命令模型

`IntelliSqlClient` 内部维护：

- `Map<String, ClientCommand> commands`

并注册：

- `ConnectCommand`
- `ExecuteCommand`
- `TranslateCommand`
- `HelpCommand`

这种设计表明 CLI 采用的是轻量命令分发架构，而不是把所有逻辑堆到主类里。

### 10.2 交互模型

CLI 主循环由 `runInteractiveLoop(...)` 驱动：

- 读取行
- 判断是否退出
- 识别是否斜杠命令
- 否则视为 SQL

再配合：

- `ConsoleReader`
- `CompleterFactory`
- `MetaDataLoader`
- `TerminalPrinter`
- `PagingRenderer`

形成终端交互层。

### 10.3 CLI 的架构定位

CLI 当前同时承担三件事：

- JDBC 客户端
- 交互式终端
- 部分本地工具入口

例如 `TranslateCommand` 并不依赖 server 才能完成全部工作，它可以直接调用本地方言和 translator 模块。

因此 CLI 在系统设计中不是“server 的附属工具”，而是独立一层产品交互面。

## 11. SPI 与插件扩展机制

### 11.1 `TypedSPI` 统一扩展模型

SPI 扩展不是按任意接口散落实现，而是围绕 `TypedSPI` 统一。

`IntelliSqlServiceLoader` 做的事情是：

- 按接口类型缓存实例
- 按 `getType()` 注册主类型
- 按 `getAliases()` 注册别名
- 对外提供 `getService(...)` 与 `getAllServices(...)`

这让插件扩展获得了统一装载方式。

### 11.2 方言扩展

`DatabaseDialectRegistry` 实际上只是 `IntelliSqlServiceLoader` 的一个 facade。

它让 parser、translator、SQL 字符串渲染等多个层都共享同一套方言注册中心。

这个设计很重要，因为它避免了：

- parser 一套方言注册
- translator 一套方言注册
- connector 又一套方言注册

当前做法是统一入口、统一缓存、统一类型命名。

### 11.3 连接器扩展

`ConnectorRegistry` 与方言注册中心类似，承担“外部数据源插件发现”的职责。

从整体架构看，IntelliSQL 当前有两类插件：

- 数据源连接器插件
- SQL 方言插件

这两套机制都采用 ServiceLoader 思路，但职责不同：

- 连接器解决“怎么接”
- 方言解决“怎么解释和怎么输出”

### 11.4 当前插件模型的性质

虽然架构上叫插件，但当前部署方式仍然是构建期组装进 classpath。

因此它更准确的说法是：

- 代码层可扩展
- 打包层半静态

距离真正“运行时动态插件容器”还有一步。

## 12. 配置架构

### 12.1 统一配置入口

`ConfigLoader` 是统一配置入口，负责：

- 读取 YAML
- 环境变量替换
- 解析为 `ModelConfig`
- 兼容 `dataSources` 的 list/map 两种形式
- 补全 JDBC URL

从架构角度讲，这属于典型的“配置适配层”。

### 12.2 配置与运行时对象的转换

`IntelliSqlKernel.initializeMetadata()` 会把 common 层的 `DataSourceConfig` 转成 connector 层需要的：

- `IntelliSQLDataSourceConfig`

这个转换动作通过 `DataSourceConfigs.fromCommonConfig(...)` 完成。

这说明仓库并没有强行让所有层共享同一个配置对象，而是允许：

- 外部配置模型保持通用
- 接入层配置模型保留自己字段语义

这是健康的边界做法。

### 12.3 当前配置设计的一个限制

`ServerConfig` 只包含：

- port
- maxConnections
- idleTimeoutMs
- host

但 `IntelliSqlServer` 实际并没有从独立 `server.yml` 加载 `ServerConfig`，而是主要依赖 `conf/model.yaml` 初始化 kernel，端口只来自启动参数或默认值。

因此目前存在两个现实：

- 代码里已经有 server 配置对象
- 启动流程里还没有真正形成独立 server 配置文件装载链

这说明配置架构还在收束过程中。

## 13. 日志与上下文设计

### 13.1 查询上下文对象

`QueryContext` 封装：

- `queryId`
- `sql`
- `user`
- `connectionId`
- `submitTime`

它为查询执行提供统一上下文。

### 13.2 MDC 集成

`QueryContextManager` 通过：

- `ThreadLocal<QueryContext>`
- `MDC.put(...)`

把查询上下文带入日志体系。

这说明 IntelliSQL 当前已经在架构层考虑了：

- 可追踪性
- 请求级日志关联
- 进程内上下文传播

### 13.3 架构意义

虽然这部分不大，但它是从“功能 demo”走向“工程系统”的关键步骤。没有统一上下文，后续监控、审计、问题定位都会非常被动。

## 14. 分发与交付设计

### 14.1 distributions 模块

`intellisql-distributions` 再拆成：

- `intellisql-distribution-jdbc`
- `intellisql-distribution-server`
- `intellisql-distribution-client`

它体现的不是代码分层，而是交付分层。

### 14.2 Server Distribution

`intellisql-distribution-server` 使用 `maven-assembly-plugin` 生成：

- `tar.gz`
- `zip`

并打包：

- `lib/`
- `bin/`
- `conf/`
- `README.md`
- `LICENSE`
- `NOTICE`

这表示 IntelliSQL 不是只面向源码开发者，也在朝独立安装包方向推进。

### 14.3 Client Distribution

`intellisql-distribution-client` 单独打包 `isql` 客户端。

这和整体产品形态是一致的：

- server 可单独部署
- client 可单独分发
- JDBC 驱动也单独提供

这是典型的数据库中间件产品交付形态。

## 15. 测试架构

### 15.1 测试模块分层

`intellisql-tests` 是聚合层，下面有：

- `intellisql-test-it`
- `intellisql-test-e2e`

这说明测试设计已经明确分层：

- IT 验证模块级与跨模块集成
- E2E 验证完整运行路径

### 15.2 各模块本地单测

除 `intellisql-tests` 外，`client`、`parser`、`translator` 等模块自己也有单元测试。

因此当前测试体系是三层结构：

- 模块单测
- 集成测试
- 端到端测试

这比研究方案里抽象提到的“测试覆盖”更具体，也更接近真实工程结构。

## 16. 研究方案与当前实现的对齐关系

### 16.1 已经对齐的部分

以下设计已经较好落地：

- 使用 Calcite 作为解析与关系代数基础设施
- 使用 Avatica 作为远程 JDBC 协议层
- 使用多模块结构承载系统边界
- 使用插件机制扩展方言和连接器
- 提供 server、JDBC、CLI 三种入口
- 提供 distribution 与 tests 两类非功能性模块

### 16.2 研究方案被重新拆分的部分

研究文档中的 `intellisql-core` 没有直接出现，实际被拆分为：

- `common`
- `parser`
- `features`
- `connector`
- `spi`

这个变化是正向的，因为它让职责更清楚。

### 16.3 当前尚未完全对齐的部分

以下地方能看出架构仍在演进：

- server 启动仍然围绕 `model.yaml`，独立的 server 配置链尚未完整落地
- `IntelliSqlMeta` 协议骨架已成，但 metadata API 大量未实现
- `intellisql-jdbc` 当前是远程薄驱动，不承载 federation、translator、optimizer 等 features；嵌入式 JDBC 模式尚未落地
- 分发 README 描述了 `server.yml`、`start.sh`、`stop.sh` 等结构，但代码和打包配置未完全与之闭合
- `intellisql-server/pom.xml` 的 shade main class 指向 `com.intellisql.server.Main`，而源码中的真实入口类是 `com.intellisql.server.IntelliSqlServer`
- 当前运行时核心仍高度围绕 federation kernel 组织，研究文档中的“更通用的核心容器”还没有完全独立出来

这些差异非常值得在架构文档里明确，因为它们决定了后续重构方向。

## 17. 当前架构的主要优点

### 17.1 模块边界已经比较清楚

当前仓库不是大杂烩式结构。模块边界基本合理：

- 共通模型
- 语言前端
- 接入层
- 内核能力
- 协议层
- 交付层
- 测试层

### 17.2 对外入口明确

当前有清晰的三种对外接口：

- 服务端 HTTP/Avatica
- JDBC Driver
- CLI

这让 IntelliSQL 已经具备“数据库产品”的基本外形。

### 17.3 扩展点集中

SPI 和 connector registry 让扩展入口比较统一，后续新增数据库类型不会把架构撕裂。

### 17.4 工程化基础已经具备

以下能力都已经存在：

- 多模块构建
- 可分发安装包
- 多层测试
- 查询上下文
- 结构化日志基础

这意味着系统已经越过“原型代码”阶段，进入“可持续迭代的工程化底座”阶段。

## 18. 当前架构的主要缺口

### 18.1 核心内核仍然偏单体

`IntelliSqlKernel` 当前聚合了较多职责：

- 配置驱动
- 生命周期管理
- 元数据初始化
- 查询入口
- 基础翻译入口

未来如果要支持更多运行模式，kernel 可能需要拆成：

- bootstrap
- metadata service
- query service
- translation service

### 18.2 协议层与业务层映射不完整

`IntelliSqlMeta` 还没有把 JDBC metadata 世界与 IntelliSQL 内部 metadata 世界完整连接起来。

### 18.3 打包与源码存在轻微漂移

当前在：

- main class
- server README
- 配置文件命名
- 启动脚本描述

这些地方能看到交付层和源码层的少量不一致。

### 18.4 插件是 classpath 插件，不是运行时插件

从代码扩展性看已经足够，但从运维和产品形态看，还没有做到真正动态增删插件。

## 19. 架构结论

如果只看研究方案，IntelliSQL 像一个“基于 Calcite 与 Avatica 的 SQL 中间件设想”；如果看当前代码，它已经是一个具有明确层次和交付外形的多模块数据库中间件雏形。

当前最准确的架构表述是：

- IntelliSQL 以 `IntelliSqlKernel` 为当前运行时核心
- 以 `common + spi + parser + connector + plugins + features` 组成内核底座
- 以 `server + jdbc + client` 组成对外入口
- 以 `distributions + tests` 补齐交付与验证

从“架构设计”的角度看，当前代码最有价值的不是某一个查询功能，而是已经形成了比较清楚的系统骨架。后续无论继续强化联邦查询、翻译器、优化器还是客户端体验，都会在这套骨架上演进，而不是推倒重来。
