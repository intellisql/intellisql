# Feature Specification: IntelliSql SQL Federation and Translation

**Feature Branch**: `001-sql-federation-translation`
**Created**: 2026-02-16
**Status**: Draft
**Input**: User description: "IntelliSql 是一个聚焦于 SQL 能力的平台，结合 LLM 能力提供智能化的数据服务。本次优先设计 SQL 联邦、SQL 翻译的能力。通过 Avatica 提供标准的 JDBC 能力以及 ODBC 能力。"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 跨数据源联邦查询 (Priority: P1)

作为数据分析师或开发人员，我需要在一个统一的 SQL 接口中查询分布在多个异构数据源（MySQL、PostgreSQL、Elasticsearch 等）的数据，而无需手动编写应用层代码进行数据关联。

**Why this priority**: 这是 IntelliSql 的核心价值主张，解决了企业级数据架构中最痛的"数据孤岛"问题，使用户能够通过单一接口访问异构数据源。

**Independent Test**:
- 配置两个数据源（如 MySQL 和 Elasticsearch）
- 执行跨源 JOIN 查询
- 验证返回的结果集正确合并了两个数据源的数据

**Acceptance Scenarios**:

1. **Given** 系统已配置 MySQL 数据源（orders_db）和 Elasticsearch 数据源（access_logs），**When** 用户执行 `SELECT o.id, o.customer_name, l.access_time FROM mysql_db.orders o JOIN es_logs.access_logs l ON o.id = l.order_id WHERE o.status = 'completed'`，**Then** 系统返回正确合并的结果集，包含来自两个数据源的字段。

2. **Given** 系统已配置多个 MySQL 数据源，**When** 用户执行跨库 JOIN 查询，**Then** 系统正确执行查询并返回结果，无需用户手动连接多个数据库。

3. **Given** 系统配置了 Elasticsearch 数据源，**When** 用户使用标准 SQL 语法查询 ES 索引（包括嵌套字段访问），**Then** 系统正确解析并返回结果。

---

### User Story 2 - SQL 方言翻译 (Priority: P1)

作为数据库迁移工程师或开发人员，我需要将一种数据库的 SQL 语句翻译为另一种数据库兼容的 SQL 语句，以支持数据库迁移或跨系统开发。

**Why this priority**: SQL 翻译是调试和跨系统迁移的关键工具，与联邦查询同为本次迭代的核心功能。

**Independent Test**:
- 输入一种数据库方言的 SQL
- 指定目标数据库类型
- 验证输出的 SQL 符合目标数据库语法

**Acceptance Scenarios**:

1. **Given** 用户输入 MySQL 方言的 SQL `SELECT * FROM users LIMIT 10 OFFSET 5`，**When** 用户指定目标方言为 Oracle，**Then** 系统输出 Oracle 兼容的 SQL（如使用 ROWNUM 语法）。

2. **Given** 用户输入包含特定数据库函数的 SQL（如 MySQL 的 `IFNULL`），**When** 用户指定目标方言为 PostgreSQL，**Then** 系统输出使用 `COALESCE` 的等价 SQL。

3. **Given** 用户通过 isql 执行 `isql --translate --from mysql --to postgresql "SELECT * FROM users WHERE name LIKE '%test%'"`，**When** 指定目标为 PostgreSQL，**Then** 系统返回语法正确的 PostgreSQL SQL。

---

### User Story 3 - JDBC 标准接口访问 (Priority: P2)

作为应用开发人员，我需要通过标准 JDBC 接口连接 IntelliSql Server，使我的现有应用能够无缝使用联邦查询能力。

**Why this priority**: JDBC 是 Java 应用访问数据库的标准方式，提供此能力可使 IntelliSql 与现有生态集成。

**Independent Test**:
- 使用任意 JDBC 客户端工具
- 连接 IntelliSql Server
- 执行 SQL 查询并获取结果

**Acceptance Scenarios**:

1. **Given** IntelliSql Server 已启动并监听默认端口，**When** 用户使用 JDBC URL `jdbc:intellisql://localhost:8765` 连接，**Then** 连接成功建立。

2. **Given** 用户已通过 JDBC 连接 IntelliSql，**When** 用户执行 `SELECT * FROM mysql_db.orders LIMIT 10`，**Then** 通过标准 ResultSet API 正确获取查询结果。

3. **Given** 用户执行大结果集查询，**When** 结果集超过内存限制，**Then** 系统通过分批拉取（Batch Fetching）机制正确返回所有数据，不发生内存溢出。

---

### User Story 4 - 命令行工具 isql (Priority: P2)

作为数据工程师或运维人员，我需要一个专业的命令行工具来连接 IntelliSql Server，执行查询、翻译 SQL 和管理连接。

**Why this priority**: CLI 是数据库中间件的标配工具，提供专业级的交互体验。

**Independent Test**:
- 下载 isql 客户端包
- 执行 isql 命令连接 Server
- 执行 SQL 查询和翻译命令

**Acceptance Scenarios**:

1. **Given** 用户已下载 isql 客户端包，**When** 用户执行 `./bin/isql` 命令，**Then** 系统显示 `isql>` 提示符，表示成功连接本地 Server。

2. **Given** 用户在 isql 交互模式中，**When** 用户输入 SQL 语句并以分号结束，**Then** 系统执行查询并以表格形式返回结果。

3. **Given** 用户需要翻译 SQL，**When** 用户执行 `isql --translate --from mysql --to postgresql "SELECT * FROM users LIMIT 10"`，**Then** 系统显示翻译后的 PostgreSQL SQL。

4. **Given** 用户需要执行批量 SQL 脚本，**When** 用户执行 `./bin/isql -f query.sql`，**Then** 系统按顺序执行脚本中的所有 SQL 并输出结果。

---

### User Story 5 - 数据源元数据管理 (Priority: P3)

作为系统管理员，我需要通过配置文件定义和管理多个数据源的连接信息和元数据映射。

**Why this priority**: 元数据管理是联邦查询的基础，但可使用简化的配置方式满足初期需求。

**Independent Test**:
- 创建/修改配置文件
- 重启 Server 或热加载配置
- 验证新的数据源可被查询

**Acceptance Scenarios**:

1. **Given** 管理员创建 model.json 配置文件定义 MySQL 数据源，**When** IntelliSql Server 启动，**Then** 系统自动加载配置并建立到 MySQL 的连接池。

2. **Given** 配置文件包含 Elasticsearch 数据源定义，**When** 系统加载配置，**Then** 系统自动调用 ES 的 _mapping API 获取索引结构并映射为关系型表结构。

3. **Given** 配置文件定义了多个 Schema，**When** 用户查询 `SHOW SCHEMAS`，**Then** 系统返回所有已配置的 Schema 列表。

---

### Edge Cases

- 当数据源连接失败时，系统返回清晰的错误信息，不影响其他数据源的使用
- 当跨源 JOIN 的中间结果集超过配置的行数限制（默认 10 万行）时，系统提前终止查询，返回部分结果和明确的警告信息
- 当 SQL 语法在目标方言中不支持时（如特定函数），翻译功能返回明确的不支持提示
- 当 ES 索引包含深层嵌套字段时，系统支持通过路径表达式（如 `user['address']['city']`）访问
- 当类型系统不兼容时（如 MySQL DATETIME 与 ES Long），系统自动进行隐式类型转换
- 当用户请求的分页超出结果集范围时，系统返回空结果集而非错误

## Requirements *(mandatory)*

### Functional Requirements

**联邦查询能力：**

- **FR-001**: 系统 MUST 支持同时连接多个异构数据源，包括 MySQL、PostgreSQL、Elasticsearch
- **FR-002**: 系统 MUST 支持通过标准 SQL 语法执行跨数据源的 JOIN 查询
- **FR-003**: 系统 MUST 支持将过滤条件（WHERE）下推到数据源执行，减少数据传输量
- **FR-004**: 系统 MUST 支持将投影（SELECT 列表）下推到数据源，只查询必要的列
- **FR-005**: 系统 MUST 统一异构数据源的类型系统，支持隐式类型转换

**SQL 翻译能力（基于 Calcite 规则引擎）：**

- **FR-006**: 系统 MUST 支持 SQL 方言翻译，将输入 SQL 转换为目标数据库兼容的语法（支持全部 DML 语句：SELECT/INSERT/UPDATE/DELETE，以及 DDL，不含存储过程/触发器/UDF）
- **FR-007**: 系统 MUST 支持 MySQL、PostgreSQL、Oracle、SQL Server、Hive 五种数据库方言的翻译
- **FR-008**: 系统 MUST 正确处理标识符引用符的差异（如 MySQL 的反引号、Oracle 的双引号）
- **FR-009**: 系统 MUST 通过 isql 的 `--translate` 参数提供 SQL 翻译功能（如 `isql --translate --from mysql --to postgresql "SELECT..."`）
- **FR-009a**: 系统 MUST 基于 Calcite Babel Parser 支持宽松的 SQL 方言解析
- **FR-009b**: 系统 MUST 支持两种翻译模式：在线模式（连接数据库获取元数据实现精准翻译）和离线模式（纯语法转换，无需数据库连接）

**JDBC 接口（本期）：**

- **FR-010**: 系统 MUST 通过标准协议提供 JDBC 服务端能力，本期不提供认证（内部/可信网络使用）
- **FR-011**: 系统 MUST 支持高效的二进制传输协议，默认使用 Protobuf 序列化格式
- **FR-012**: 系统 MUST 支持连接池管理，复用数据源连接
- **FR-013**: 系统 MUST 支持大结果集的分批拉取（Batch Fetching），避免内存溢出

**ODBC 接口（后续迭代）：**

- **FR-014**: 系统预留 ODBC 接口扩展能力，本期不实现

**命令行工具：**

- **FR-015**: 系统 MUST 提供 isql 命令行工具，支持交互式和脚本执行模式
- **FR-015a**: isql MUST 内置自定义的分页渲染引擎（类似 Unix `less`），支持对超大结果集的流式渲染、按页滚动及 CJK 字符的正确对齐，严禁一次性加载所有数据到内存
- **FR-016**: isql MUST 支持基于 nanorc 规则的 SQL 语法高亮（包括多行注释、字符串字面量识别）和自定义提示符
- **FR-016a**: isql MUST 支持基于上下文的智能补全（提示关键字、表名、列名），元数据需异步加载
- **FR-016b**: isql MUST 支持持久化的命令历史记录，并通过上下箭头键进行导航
- **FR-016c**: isql MUST 能够捕获 SIGINT 信号（Ctrl+C），并在用户触发时仅取消当前执行的 SQL 查询而不退出程序
- **FR-017**: isql MUST 提供两种分发形式：GraalVM Native Image 原生二进制包（主推，支持 Linux/macOS/Windows，启动时间 < 0.5s）和标准可执行 JAR 包（备选，依赖 JVM）

**元数据管理：**

- **FR-018**: 系统 MUST 通过 JSON 格式的配置文件管理数据源连接信息（model.json）
- **FR-018a**: 配置文件 MUST 支持环境变量替换语法（如 `${DB_PASSWORD}`）用于敏感信息
- **FR-019**: 系统 MUST 自动发现和映射数据源的表结构
- **FR-020**: 系统 MUST 支持 Elasticsearch 索引结构的自动映射

### Non-Functional Requirements

**代码质量（本期）：**

- **NFR-014**: Java 源码 MUST NOT 包含无用的空行，代码应连续编写
- **NFR-015**: 方法之间、逻辑块之间无需额外空行分隔

**可观测性（本期）：**

- **NFR-001**: 系统 MUST 提供结构化日志记录，使用 JSON 格式输出
- **NFR-002**: 系统 MUST 为每个查询分配唯一 Query ID，并在日志中记录
- **NFR-003**: 系统 MUST 记录查询执行时间、数据源访问信息、结果集大小等关键指标
- **NFR-004**: 系统 MUST 支持可配置的日志级别（DEBUG、INFO、WARN、ERROR）
- **NFR-005**: 日志输出 MUST 包含时间戳、线程 ID、日志级别、类名、消息内容

**可靠性（本期）：**

- **NFR-006**: 系统 MUST 对数据源连接的瞬时错误（网络超时、连接池耗尽）实施指数退避重试策略
- **NFR-007**: 重试策略 MUST 限制最多 3 次重试，延迟时间分别为 1 秒、2 秒、4 秒
- **NFR-008**: 系统在重试失败后 MUST 返回明确的错误信息，包含失败原因和已重试次数
- **NFR-009**: 系统 MUST 区分瞬时错误（可重试）和永久错误（如认证失败、SQL 语法错误，不可重试）

**资源管理（本期）：**

- **NFR-010**: 系统 MUST 为跨源 JOIN 查询的中间结果集设置可配置的行数限制（默认 100,000 行）
- **NFR-011**: 当中间结果集超过行数限制时，系统 MUST 提前终止查询并返回部分结果
- **NFR-012**: 当查询因资源限制被终止时，系统 MUST 在结果中包含明确的警告信息和建议的优化方案
- **NFR-013**: 行数限制 MUST 可通过配置文件调整，以适应不同规模的部署环境

### Key Entities

- **DataSource**: 数据源配置实体，包含连接信息（URL、用户名、密码）、类型（MySQL/PostgreSQL/ES）、Schema 映射
- **Schema**: 逻辑数据库模式，映射到一个数据源或虚拟联邦视图
- **Table**: 表/索引的元数据表示，包含列定义、类型、约束
- **Query**: 用户提交的 SQL 查询，包含原始 SQL、目标数据源、执行计划
- **Translation**: SQL 翻译任务，包含源 SQL、源方言、目标方言、翻译结果
- **Connection**: 客户端连接会话，包含连接 ID、状态、最后活动时间

### Module Architecture

系统采用多模块架构设计，参考 QuickSQL 的分层理念，按职责分为四层：

**公共基础层（Common Layer）**

| 模块 | 职责 |
|------|------|
| **intellisql-common** | 公共基础设施，包含配置加载（ConfigLoader）、日志（StructuredLogger）、重试机制（RetryPolicy）、元数据实体（Column, Table, Schema, DataSource）、方言枚举（SqlDialect）等 |

**功能特性层（Features Layer）**

| 模块 | 职责 |
|------|------|
| **intellisql-parser** | SQL 解析模块，基于 Calcite Parser.jj 模板（参考 Quicksql 实现，使用 JavaCC + FreeMarker），支持多方言解析和方言间转换，通过 parserImpls.ftl 扩展语法规则 |
| **intellisql-features** | 功能特性父模块，聚合核心功能子模块 |
| ├─ **intellisql-optimizer** | SQL 优化器，包含查询优化规则、逻辑计划转换、元数据管理。采用混合优化策略：HepPlanner 用于 RBO，VolcanoPlanner 用于 CBO |
| ├─ **intellisql-translator** | SQL 翻译器，支持多种数据库方言间的 SQL 转换，提供在线模式（连接数据库获取元数据）和离线模式（纯语法转换） |
| └─ **intellisql-federation** | 联邦查询核心，包含执行引擎（QueryExecutor, FederatedQueryExecutor）、迭代器模型（QueryIterator 系列算子）、元数据管理（MetadataManager）、核心编排（IntelliSqlKernel, QueryProcessor） |
| **intellisql-connector** | 数据源连接器，包含各数据源（MySQL/PostgreSQL/Elasticsearch）的 Schema 映射和查询下推规则 |

**协议适配层（Protocol Layer）**

| 模块 | 职责 |
|------|------|
| **intellisql-jdbc** | JDBC 驱动实现，供客户端应用连接 IntelliSql Server |
| **intellisql-odbc** | ODBC 驱动实现（后续迭代，不在 Maven 体系中管理） |
| **intellisql-server** | 服务端模块，基于 Avatica 协议提供 JDBC 服务，使用 `jdbc:intellisql` URL 前缀 |

**工具与支撑层（Tooling & Support Layer）**

| 模块 | 职责 |
|------|------|
| **intellisql-client** | 客户端工具，打包 isql 命令行工具 |
| **intellisql-distribution** | 打包分发父模块，负责不同组件的打包发布 |
| ├─ **intellisql-distribution-jdbc** | JDBC Driver 包打包分发 |
| └─ **intellisql-distribution-server** | Server 包打包分发 |
| **intellisql-test** | 测试模块父模块，包含各类测试 |
| ├─ **intellisql-test-it** | 集成测试，负责单元功能的集成测试 |
| └─ **intellisql-test-e2e** | 端到端测试，负责完整的 SQL 功能测试 |

**模块依赖关系**

```
┌─────────────────────────────────────────────────────────────┐
│                  Protocol Layer (jdbc/server)                │
│                 依赖 features，提供协议适配                    │
├─────────────────────────────────────────────────────────────┤
│                    Features Layer                            │
│         (parser/features/connector)                          │
│          核心功能：解析、优化、翻译、联邦查询、连接器            │
├─────────────────────────────────────────────────────────────┤
│                    Common Layer (common)                     │
│          公共基础设施：配置、日志、重试、元数据实体              │
└─────────────────────────────────────────────────────────────┘
```

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 用户可以在 5 分钟内完成两个数据源的配置并执行首次跨源查询
- **SC-002**: 单表查询响应时间与直接访问数据源相比，额外开销不超过 50ms
- **SC-003**: 跨源 JOIN 查询在单表数据量不超过 10 万行时，响应时间在 5 秒以内
- **SC-004**: SQL 翻译功能支持至少 5 种主流数据库方言
- **SC-005**: 90% 的标准 SQL 语法可以正确翻译到目标方言
- **SC-006**: JDBC 连接成功率在服务正常运行时达到 100%
- **SC-007**: isql 工具启动时间不超过 3 秒
- **SC-008**: 系统支持至少 100 个并发 JDBC 连接
- **SC-009**: 大结果集（100 万行）查询通过分批拉取正常完成，不发生内存溢出
- **SC-010**: 错误信息的可读性评分达到 80% 以上（用户能理解错误原因和解决方法）

## Clarifications

### Session 2026-02-16

- Q: SQL 翻译引擎架构选择？ → A: 仅使用 Calcite 规则引擎（无 LLM），LLM 后续再实现
- Q: ODBC 能力是否本期提供？ → A: 本次不提供，未来迭代
- Q: 模块架构设计？ → A: 多模块设计（intellisql-parser, intellisql-core, intellisql-connector, intellisql-driver, intellisql-server, intellisql-client, intellisql-assembly）
- Q: 支持的数据库方言？ → A: MySQL, PostgreSQL, Oracle, SQL Server, Hive
- Q: SQL 翻译模式？ → A: 同时支持在线模式（连接数据库获取元数据）和离线模式（纯语法转换）
- Q: SQL 翻译 CLI 交互方式？ → A: 使用 isql 参数方式（如 `isql --translate`），统一交互方式
- Q: 模块命名调整？ → A: intellisql-storage 改为 intellisql-connector；新增 intellisql-distribution（打包分发模块，含子模块 intellisql-distribution-driver、intellisql-distribution-server）；新增 intellisql-driver（含子模块 intellisql-driver-jdbc，未来增加 intellisql-driver-odbc）
- Q: intellisql-server 模块结构？ → A: intellisql-server 作为父模块，本期实现 intellisql-server-avatica，未来支持 intellisql-server-mysql、intellisql-server-postgresql 等数据库协议
- Q: 测试模块结构？ → A: 新增 intellisql-test 父模块，包含 intellisql-test-it（集成测试，单元功能测试）和 intellisql-test-e2e（端到端完整 SQL 测试）
- Q: intellisql-core 模块拆分？ → A: 拆分为 intellisql-optimizer（SQL 优化，翻译和执行共用）和 intellisql-executor（SQL 执行），因为 SQL 翻译不需要执行能力
- Q: intellisql-kernel 模块？ → A: 新增 intellisql-kernel 作为核心处理层，driver 和 server 模块都依赖它，它依赖 parser、optimizer、executor、connector 等功能模块
- Q: intellisql-server 模块架构简化？ → A: intellisql-server 不再作为父模块，直接作为单一模块实现（基于 Avatica 协议），不实现 MySQL/PostgreSQL 协议
- Q: JDBC URL 格式？ → A: 使用自定义前缀 `jdbc:intellisql`（而非 `jdbc:avatica:remote:url=...`），简化连接配置
- Q: 认证授权模型？ → A: MVP 阶段不提供认证（内部/可信网络使用），未来迭代增加用户名/密码认证
- Q: JDBC 传输协议？ → A: 默认使用 Protobuf 格式传输（而非 JSON），保证性能
- Q: 可观测性能力？ → A: 本期实现结构化日志记录（JSON 格式，包含 query ID、执行时间、数据源访问），未来迭代增加 Agent 和 Tracing
- Q: 数据源连接失败处理策略？ → A: 使用指数退避重试机制（最多 3 次重试，延迟 1s/2s/4s），仅针对瞬时错误（网络超时、连接池耗尽等）
- Q: 数据源配置文件格式与敏感信息管理？ → A: 使用 JSON 格式配置文件，支持环境变量替换（如 `${DB_PASSWORD}`）处理敏感信息
- Q: 跨源 JOIN 内存溢出处理策略？ → A: 强制执行可配置的行数限制（默认 10 万行），超过限制时提前终止查询并返回部分结果和警告信息

### Session 2026-02-17

- Q: JDBC/ODBC 驱动模块命名规范？ → A: intellisql-driver 改为 intellisql-jdbc；intellisql-driver-odbc 改为 intellisql-odbc（后续实现，不在 Maven 体系中管理）；删除 intellisql-driver 父模块；intellisql-distribution-driver 改为 intellisql-distribution-jdbc
- Q: Java 源码空行规范？ → A: Java 源码中不允许有无用空行，所有代码连续编写，方法之间、逻辑块之间无需额外空行分隔
- Q: Apache Calcite 和 Avatica 版本？ → A: Apache Calcite 使用 1.41.0，Avatica 使用 1.27.0
- Q: 数据源配置格式与 schemaMappings？ → A: 数据源配置使用扁平化结构，MVP 阶段不支持 schemaMappings（后续根据情况再支持），healthCheckIntervalSeconds > 0 时开启健康检查，否则关闭

### Session 2026-02-20

- Q: Calcite 优化器策略选择？ → A: 混合模式（HepPlanner 用于 RBO 阶段，VolcanoPlanner 用于 CBO 阶段），参考 ShardingSphere sql-federation 实现
- Q: CBO 代价模型包含哪些因素？ → A: 完整代价模型（CPU + I/O + 网络 + 内存），提供最准确的计划选择
- Q: RBO 规则集选择？ → A: 参考 ShardingSphere 的规则集实现（包括 filter pushdown、projection pushdown、join reorder、subquery rewrite、aggregate split 等）
- Q: 联邦查询执行模型？ → A: Volcano 迭代器模型（operator tree with open-next-close 协议）
- Q: 优化器元数据提供方式？ → A: 使用 Calcite RelMetadataQuery（标准 Calcite 集成，内置统计信息支持）

### Session 2026-02-19

- Q: Parser DML 语句支持范围？ → A: 支持全部 DML 语句（SELECT, INSERT, UPDATE, DELETE），移除原来"写操作支持"的 Out of Scope 限制
- Q: Parser 扩展机制架构？ → A: 参考 Quicksql/parser 实现，采用模板化可扩展解析器架构（JavaCC + FreeMarker 模板），通过 parserImpls.ftl 扩展语法规则，方言抽象无需重复解析器
- Q: MySQL/PostgreSQL 方言语法覆盖范围？ → A: 全部语法扩展，完整覆盖两个方言的所有语法特性
- Q: Parser 错误处理策略？ → A: 详细错误信息，包含位置、上下文片段和修复建议
- Q: Parser AST 节点结构？ → A: 继承 Calcite 基础 SqlNode 类，扩展方言特定的子类
- Q: isql 命令行工具的发布形式？ → A: 同时发布 GraalVM Native Image 原生二进制包（主推，无 JVM 依赖）和标准可执行 JAR 包（兼容性备选）
- Q: isql 智能补全策略？ → A: 实现基于上下文的元数据驱动补全（Context-aware），提示表名/列名；支持通过上下箭头键切换历史 SQL 记录
- Q: isql 信号处理策略（Ctrl+C）？ → A: 使用 JLine3 捕获 SIGINT 信号，仅取消当前正在执行的 SQL 查询（Statement.cancel），不退出 Shell 会话
- Q: isql 语法高亮实现方案？ → A: 使用 JLine3 内置的 nanorc 语法高亮引擎，通过配置 sql.nanorc 文件实现对关键字、字符串、注释的精确着色
- Q: isql 结果集渲染与分页策略？ → A: 采用自定义的原生分页渲染引擎（类似 Unix `less`），支持流式读取 ResultSet，自动计算列宽（处理 CJK 字符对齐），避免全量加载导致的 OOM

### Session 2026-02-22

- Q: 最终模块结构确认？ → A: 采用四层架构：(1) Common Layer - intellisql-common（配置、日志、重试、元数据实体）；(2) Features Layer - intellisql-features 父模块，包含 intellisql-optimizer、intellisql-translator、intellisql-federation；(3) Protocol Layer - intellisql-jdbc、intellisql-server；(4) Tooling Layer - intellisql-client、intellisql-distribution、intellisql-test
- Q: intellisql-kernel 和 intellisql-executor 的最终归属？ → A: 合并到 intellisql-features/intellisql-federation 模块中，federation 模块现在包含核心编排（IntelliSqlKernel, QueryProcessor）、执行引擎（FederatedQueryExecutor, QueryIterator 系列算子）、元数据管理（MetadataManager）
- Q: 翻译器模块位置？ → A: 独立为 intellisql-features/intellisql-translator，与 optimizer 和 federation 平级

## Assumptions

- 用户具备基本的 SQL 知识
- 数据源网络可达，防火墙规则已配置
- 初期版本不支持分布式计算，跨源 JOIN 在单机内存中执行
- ES 适配器支持基本的查询场景，复杂聚合可能有限制
- ODBC 功能作为后续迭代内容，本期仅预留接口
- SQL 翻译本期仅使用 Calcite 规则引擎，不支持存储过程/触发器/UDF 的翻译
- MVP 阶段不提供认证授权，仅限内部/可信网络使用，未来迭代增加用户名/密码认证
- JDBC 连接默认使用 Protobuf 序列化格式以保证性能

## Out of Scope

- SQL 优化功能（查询计划优化、索引推荐）
- SQL 缓存功能（结果集缓存、元数据缓存）
- LLM 智能功能（自然语言转 SQL、智能优化建议）
- 分布式计算引擎集成（Spark、Flink）
- 实时流查询
