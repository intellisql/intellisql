# **IntelliSQL 架构设计与实现深度研究报告：基于 Apache Calcite 与 Avatica 的联邦查询与翻译中间件**

## **1\. 战略背景与架构愿景**

### **1.1 数据孤岛与联邦查询的必要性**

在现代企业级数据架构的演进过程中，多模态持久化（Polyglot Persistence）已成为主流范式。根据业务场景的特定需求，事务性数据驻留在 MySQL 或 PostgreSQL 等关系型数据库中，日志与文档数据存储于 Elasticsearch，而海量历史数据则归档于 Hive 或 HDFS 1。这种“存储层解耦”虽然优化了单一场景的读写性能，却在分析层制造了深刻的“数据孤岛”问题。

开发人员与数据分析师被迫在应用层手动处理跨源数据关联（Join），这不仅导致了业务逻辑的极度复杂化，还引入了大量的冗余代码和维护成本。Qihoo 360 开源的 QuickSQL (QSQL) 项目通过提出“解析层-计算层-存储层”三层架构，试图解决这一异构数据源的统一查询问题 1。

**IntelliSQL** 项目旨在继承 QSQL 的核心设计理念，即“SQL 即接口”，通过构建一个轻量级、无需依赖繁重计算集群（如 Spark/Flink）的 SQL 中间件，实现对异构数据源的透明访问。本项目优先聚焦于 **SQL 联邦（Federation）** 与 **SQL 翻译（Translation）** 两大核心能力，利用 **Apache Calcite** 作为查询优化核心，并采用 **Apache Avatica** 构建标准化的 JDBC 服务端与客户端协议，最终通过定制化的命令行工具 **isql** 提供统一的操作界面。

### **1.2 QSQL 架构范式的继承与改良**

QSQL 的架构设计具有极高的参考价值，其核心在于模块的解耦。IntelliSQL 将沿用 QSQL 的模块划分逻辑，但在计算层实现上进行轻量化改造：

| 架构分层 | QSQL 设计方案 | IntelliSQL 适配方案 (Calcite Native) |
| :---- | :---- | :---- |
| **解析层 (Parsing Layer)** | 负责 SQL 解析、校验与逻辑计划生成。支持 SQL 方言屏蔽。 | **Calcite Core**: 利用 SqlParser 进行语法分析，SqlValidator 进行元数据校验，VolcanoPlanner 进行逻辑优化 1。 |
| **计算层 (Computing Layer)** | 混合执行模式。对于复杂查询路由至 Spark/Flink 集群，简单查询直接执行。 | **Calcite Enumerable**: 放弃对 Spark 的强依赖，采用 Calcite 内置的 Enumerable 约定（基于 Java Iterator），在单机 JVM 内实现轻量级内存 Join 与聚合 2。 |
| **存储层 (Storage Layer)** | 数据源连接器与元数据映射。 | **Calcite Adapters**: 利用 JdbcAdapter、ElasticsearchAdapter 等现成组件，通过 SchemaFactory 实现元数据动态加载 4。 |

IntelliSQL 的战略定位是“单一服务进程的虚拟数据库”，它对外暴露标准的 JDBC 接口，对内屏蔽底层存储的异构性，并在未来迭代中通过缓存与高级优化规则提升性能。

## ---

**2\. 核心架构解构与模块划分**

为了实现 IntelliSQL 的设计目标，必须对系统进行严谨的模块化拆分。参考 QSQL 的包结构 com.qihoo.qsql，IntelliSQL 将采用类似的包结构以保证代码组织的逻辑性与可维护性。

### **2.1 模块依赖关系**

系统将划分为以下核心 Maven 模块：

1. **intellisql-core**: 核心逻辑模块，包含解析器、优化器、元数据管理以及 SQL 翻译引擎。此模块直接依赖 calcite-core 和 calcite-linq4j。  
2. **intellisql-server**: 服务端入口，负责启动 Avatica Server，实现 Meta 接口以处理 JDBC 请求，并管理连接生命周期。此模块依赖 calcite-avatica-server 和 intellisql-core 6。  
3. **intellisql-client**: 客户端模块，打包定制化的 Sqlline 命令行工具 (isql)，内置 Avatica Thin Driver。  
4. **intellisql-storage**: 包含各数据源的适配器配置与扩展实现（如自定义的 Elasticsearch 算子下推规则）。

### **2.2 核心类职责映射**

IntelliSQL 的类设计将严格对标 QSQL 的分层逻辑，但基于 Calcite 原生接口进行实现：

* **解析层 (Parsing Layer)**:  
  * **QSQL 对标**: com.qihoo.qsql.parser.SqlParserHelper  
  * **IntelliSQL 实现**: com.intellisql.parser.IntelliSqlParser。封装 org.apache.calcite.sql.parser.SqlParser，配置 SqlParser.Config 以支持宽松的 SQL 方言（如同时支持反引号与双引号标识符），并集成 SQL 翻译逻辑。  
* **计算层 (Computing Layer)**:  
  * **QSQL 对标**: com.qihoo.qsql.planner.ExecutionPipeline 8  
  * **IntelliSQL 实现**: com.intellisql.compute.QueryExecutor。该类负责将优化后的 RelNode（关系代数树）转换为可执行的物理计划（Bindable 或 Enumerable），并触发执行。对于异构数据源的 Join，它利用 Calcite 的 EnumerableJoin 算子在内存中完成数据合并。  
* **元数据层 (Metadata Layer)**:  
  * **QSQL 对标**: com.qihoo.qsql.metadata.MetadataClient  
  * **IntelliSQL 实现**: com.intellisql.meta.SchemaManager。负责读取 model.json 配置文件，利用反射机制调用各数据源的 SchemaFactory，动态构建 Calcite 的 rootSchema。

## ---

**3\. 存储层深度解析：SQL 联邦机制的实现**

SQL 联邦是 IntelliSQL 的基石，其核心挑战在于如何将外部数据源的元数据（Schema）映射为 Calcite 内部的表结构，并实现高效的数据扫描（TableScan）。

### **3.1 基于 SchemaFactory 的元数据发现机制**

Calcite 通过 SchemaFactory 接口实现元数据的解耦 4。IntelliSQL 必须为每种支持的数据源配置相应的工厂类。

#### **3.1.1 JDBC 数据源适配 (MySQL/PostgreSQL)**

对于关系型数据库，IntelliSQL 利用 JdbcSchema。当 isql 启动时，SchemaFactory 会连接目标数据库，查询 INFORMATION\_SCHEMA，并将远程表结构映射为 Calcite 的 Table 对象。

* **技术细节**: 在 model.json 中配置 jdbcUrl、jdbcUser 等参数。Calcite 内部的 JdbcUtils 会通过 DataSource 池化连接，以减少握手开销。  
* **类型映射**: 必须处理 SQL 标准类型与特定数据库类型的差异。例如，MySQL 的 TINYINT(1) 通常映射为 SQL 的 BOOLEAN，而 Oracle 的 NUMBER 可能映射为 DECIMAL 或 DOUBLE。IntelliSQL 需在 JdbcConvention 中配置方言（Dialect）以自适应这些差异 4。

#### **3.1.2 文档型数据源适配 (Elasticsearch)**

对于 Elasticsearch，Schema 的发现更为复杂，因为 ES 是无模式（Schema-less）或弱模式的。

* **实现机制**: IntelliSQL 将使用 ElasticsearchSchemaFactory。该工厂类通过调用 ES 的 \_mapping API 获取索引结构，将其转换为关系型结构。  
* **嵌套结构处理**: ES 中的 JSON 文档常包含嵌套对象（Nested Object）。IntelliSQL 需采用“打平”（Flattening）策略或通过 Calcite 的 ITEM 操作符来支持对深层字段的访问（如 user\['address'\]\['city'\]）4。

### **3.2 统一元数据配置：Model.json 设计**

IntelliSQL 将采用 JSON 文件作为联邦配置的单一真理来源（Source of Truth）。这与 QSQL 的元数据管理思路一致，但实现上直接通过 Calcite 的 Model Handler 加载。

**IntelliSQL model.json 规范示例**:

JSON

{  
  "version": "1.0",  
  "defaultSchema": "federation",  
  "schemas": \[  
    {  
      "name": "federation",  
      "type": "map",  
      "tables":   
    },  
    {  
      "name": "mysql\_db",  
      "type": "jdbc",  
      "jdbcDriver": "com.mysql.cj.jdbc.Driver",  
      "jdbcUrl": "jdbc:mysql://192.168.1.10:3306/orders\_db",  
      "jdbcUser": "admin",  
      "jdbcPassword": "password",  
      "jdbcCatalog": "orders\_db"  
    },  
    {  
      "name": "es\_logs",  
      "type": "custom",  
      "factory": "org.apache.calcite.adapter.elasticsearch.ElasticsearchSchemaFactory",  
      "operand": {  
        "coordinates": "{'192.168.1.20': 9200}",  
        "index": "access\_logs\_\*"  
      }  
    }  
  \]  
}

**深度解析**: 此配置定义了一个虚拟数据库，其中包含两个 Schema：mysql\_db 和 es\_logs。用户在 isql 中可以直接编写跨源 Join 查询，如 SELECT \* FROM mysql\_db.orders o JOIN es\_logs.access\_logs l ON o.id \= l.order\_id 9。

### **3.3 异构类型系统的统一 (Type System)**

联邦查询的最大痛点在于类型系统的异构性。MySQL 的 DATETIME、Elasticsearch 的 Epoch Long 以及 Hive 的 TIMESTAMP 必须在 Calcite 内部统一为 RelDataType。

* **IntelliSQL 策略**: 在 intellisql-core 中实现自定义的 RelDataTypeSystem。  
* **隐式转换**: 默认情况下，Calcite 对类型转换较为严格 11。IntelliSQL 需启用隐式类型转换（Implicit Coercion），允许字符串类型的数字与整型字段进行比较，以提升用户体验，这在处理清洗不彻底的日志数据时尤为重要。

## ---

**4\. 解析层深度解析：SQL 翻译与方言隔离**

SQL 翻译（SQL Translation）是用户明确要求的核心功能。在 IntelliSQL 中，这不仅是一个辅助功能，更是调试与跨系统迁移的关键工具。其本质是将 Calcite 的中间表示（RelNode）逆向生成为特定数据库的 SQL 方言。

### **4.1 SQL 翻译流水线**

IntelliSQL 的翻译功能通过 RelToSqlConverter 实现。整个流程可以描述为：SQL (Source) \-\> SqlNode (AST) \-\> RelNode (Logical Plan) \-\> SqlNode (Target Dialect) \-\> SQL (Target)。

#### **4.1.1 阶段一：解析与验证**

输入 SQL 首先经过 SqlParser 解析为抽象语法树（AST）。此时，SqlValidator 介入，绑定元数据。

* **关键点**: 在翻译模式下，验证器可以配置为“宽松模式”，即不进行严格的表存在性检查（如果仅做语法翻译），但在联邦查询场景下，通常需要强校验以确保生成的 SQL 逻辑正确。

#### **4.1.2 阶段二：逻辑计划生成 (RelNode)**

通过 SqlToRelConverter 将 AST 转换为关系代数树。这一步消除了 SQL 语法糖。例如，SELECT \* 会被展开为具体的字段列表；子查询可能会被改写为 Join 或 Semi-Join 12。

#### **4.1.3 阶段三：方言逆向生成 (RelToSql)**

这是翻译的核心。RelToSqlConverter 遍历 RelNode 树，并根据目标方言（SqlDialect）生成 SQL 14。

**代码实现逻辑（Java 伪代码）**:

Java

public String translate(String sourceSql, SqlDialect targetDialect) {  
    // 1\. 解析  
    SqlNode parseTree \= sqlParser.parseQuery(sourceSql);  
    // 2\. 验证  
    SqlNode validatedTree \= sqlValidator.validate(parseTree);  
    // 3\. 转换为 RelNode  
    RelRoot root \= sqlToRelConverter.convertQuery(validatedTree, false, true);  
    // 4\. 逆向转换为目标方言的 SQL  
    RelToSqlConverter converter \= new RelToSqlConverter(targetDialect);  
    SqlImplementor.Result result \= converter.visitRoot(root.rel);  
    return result.asStatement().toSqlString(targetDialect).getSql();  
}

### **4.2 方言定制化 (Custom SqlDialect)**

为了支持复杂的翻译需求，IntelliSQL 必须内置多种方言配置。虽然 Calcite 提供了 MySQL、Oracle 等标准方言，但实际生产环境中常需微调。

* **场景**: 某些老旧的 MySQL 版本不支持 OFFSET 语法，只能使用 LIMIT。  
* **实现**: 继承 MysqlSqlDialect 并覆盖 unparseOffsetFetch 方法。  
  Java  
  @Override  
  public void unparseOffsetFetch(SqlWriter writer, SqlNode offset, SqlNode fetch) {  
      // 强制使用 LIMIT offset, fetch 语法而非 ANSI 标准  
      unparseFetchUsingLimit(writer, offset, fetch);  
  }

* **标识符引用**: 翻译功能必须正确处理标识符的引用符（Quoting）。例如，翻译为 MySQL 时使用反引号（\`），翻译为 Oracle 时使用双引号（"）。这是由 SqlDialect.quoteIdentifier 方法控制的 16。

### **4.3 翻译功能的 CLI 暴露**

在 isql 命令行中，IntelliSQL 将提供特殊的命令来触发翻译功能，例如：

\!translate \<target\_engine\> \<sql\_statement\>

这需要扩展 Sqlline 的 CommandHandler，拦截以 \!translate 开头的指令，调用服务端的翻译 API，并将结果打印回控制台。

## ---

**5\. 计算层深度解析：执行模型与优化策略**

计算层决定了查询的性能。IntelliSQL 采用 **Enumerable** 约定，这是一种基于 JVM 内存的执行模型，虽不如 Spark 具备大规模分布式洗牌（Shuffle）能力，但对于中小规模数据的联邦查询具有极低的延迟优势。

### **5.1 Enumerable Convention 与动态代码生成**

Calcite 的 Enumerable 算子（如 EnumerableJoin, EnumerableFilter）并不直接解释执行，而是通过 **Janino** 编译器动态生成 Java 代码 2。

* **执行流程**:  
  1. 优化器选择物理计划（Physical Plan），例如将逻辑 Join 转换为 EnumerableHashJoin。  
  2. EnumerableRelImplementor 遍历计划树，生成一段 Java 代码。这段代码本质上是一个复杂的迭代器链。  
  3. 代码被编译并加载到 JVM 中执行。  
* **优势**: 相比解释执行，动态编译消除了虚函数调用的开销，极大提升了 CPU 密集型操作（如复杂表达式计算）的性能。

### **5.2 算子下推（Pushdown）优化**

为了避免“将所有数据拉取到内存处理”的性能灾难，IntelliSQL 必须实现激进的算子下推策略 2。

#### **5.2.1 规则驱动的优化 (Rule-Based Optimization)**

IntelliSQL 将配置 HepPlanner 注册一系列核心下推规则：

* **CoreRules.FILTER\_INTO\_JOIN**: 将过滤条件穿透 Join 算子，推向数据源。  
* **JdbcRules.JDBC\_PROJECT\_RULE**: 确保只从数据库查询需要的列（即 SELECT name FROM t 而非 SELECT \*）。  
* **JdbcRules.JDBC\_FILTER\_RULE**: 将 WHERE 子句下推至 JDBC SQL 中。

#### **5.2.2 异构源 Join 的执行策略**

当查询涉及 MySQL 和 Elasticsearch 的 Join 时：

* **左侧 (MySQL)**: 优化器生成一个 JdbcTableScan，并附带下推的 SQL (SELECT \* FROM users WHERE id \> 100)。  
* **右侧 (ES)**: 优化器生成一个 ElasticsearchTableScan，附带下推的 JSON Query DSL。  
* **顶层 (IntelliSQL)**: 生成一个 EnumerableHashJoin。它首先从右侧构建哈希表（假设右表较小），然后流式读取左侧数据进行探测（Probe）。

**性能瓶颈预警**: 如果两侧数据量均巨大且无法过滤，JVM 将面临 OOM 风险。这是单机联邦引擎的物理局限。在“未来优化”迭代中，IntelliSQL 需引入**Block Join**（分块 Join）或**Spill-to-Disk**（落盘）机制，或者像 QSQL 一样接入 Spark 引擎 18。

## ---

**6\. 服务层深度解析：Avatica 协议与 Meta 实现**

为了让 IntelliSQL 成为通用的中间件，必须实现标准的 JDBC 协议。Apache Avatica 是实现这一目标的标准框架。

### **6.1 Avatica 架构组件**

* **Wire Protocol**: IntelliSQL 将支持 **Protobuf over HTTP**。相比 JSON，Protobuf 在序列化大量结果集时具有显著的性能优势（体积更小，解析更快）6。  
* **Service**: 使用 Avatica 提供的 LocalService，它负责将 RPC 请求（如 PrepareRequest, FetchRequest）转发给本地的 Meta 实现。

### **6.2 核心实现：IntelliSqlMeta**

IntelliSqlMeta 类是连接 Avatica Server 与 Calcite 引擎的桥梁。它需要继承 org.apache.calcite.avatica.jdbc.JdbcMeta。

* **连接管理**: 当客户端发起 openConnection 请求时，IntelliSqlMeta 会创建一个 Calcite Connection 对象。这个对象是通过 JDBC URL（jdbc:calcite:model=model.json）初始化的，从而加载了所有联邦数据源 21。  
* **语句执行**:  
  * prepareAndExecute(StatementHandle h, String sql,...): 拦截 SQL，调用 Calcite 的 prepareStatement。在此处，IntelliSQL 可以注入自定义的审计日志或查询改写逻辑。  
* **游标管理 (Fetch)**: Avatica 采用分批拉取（Batch Fetching）机制。IntelliSqlMeta 必须维护结果集的游标（Iterator），并根据 fetchSize 返回 Frame（数据帧）。这对于处理大结果集至关重要，避免了一次性内存溢出 23。

## ---

**7\. 客户端层深度解析：isql 命令行工具**

isql 是 IntelliSQL 的门面。我们将基于 **Sqlline** 进行二次开发与封装，打造专业级的 CLI 体验。

### **7.1 Sqlline 的定制化配置**

Sqlline 是一个纯 Java 的 JDBC 命令行外壳 25。为了将其转化为 isql，我们需要进行以下定制：

1. **依赖打包 (Shading)**: 使用 Maven Shade Plugin 将 sqlline, avatica-client, jackson, protobuf 以及日志库打包为一个单一的 intellisql-client.jar。这解决了“类路径地狱”问题，用户只需下载一个 Jar 包即可运行。  
2. **属性配置 (sqlline.properties)**:  
   * sqlline.prompt=isql\> : 将默认提示符修改为 isql\>，增强品牌识别度 26。  
   * sqlline.color=true: 启用语法高亮，提升 SQL 编写体验。  
   * sqlline.headerInterval=20: 每 20 行重复打印表头，便于阅读长结果集。

### **7.2 启动脚本设计 (isql script)**

为了提供类似 MySQL 客户端的体验，我们需要编写 Shell 脚本（Linux/Mac）和 Batch 脚本（Windows）来隐藏 Java 启动参数。

**bin/isql 脚本示例**:

Bash

\#\!/bin/bash  
\# IntelliSQL Command Line Interface

\# 定位安装目录  
BASE\_DIR=$(dirname $0)/..  
LIB\_DIR=$BASE\_DIR/lib

\# 设置 Classpath  
CP="$LIB\_DIR/intellisql-client.jar"

\# 默认连接本地 IntelliSQL Server  
DEFAULT\_URL="jdbc:avatica:remote:url=http://localhost:8765;serialization=protobuf"

\# 启动 Sqlline，预置连接参数  
java \-cp "$CP" sqlline.SqlLine \-u "$DEFAULT\_URL" \-n "admin" \-p "admin" \--color=true "$@"

此脚本允许用户直接输入 isql 进入交互模式，或通过 isql \-f query.sql 执行脚本文件 25。

## ---

**8\. 未来演进：优化与缓存架构预览**

虽然本次迭代优先实现联邦与翻译，但架构设计必须为未来的 **SQL 优化** 与 **SQL 缓存** 预留接口。

### **8.1 SQL 缓存架构 (Query Caching)**

IntelliSQL 的缓存将分为两层：

1. **元数据缓存 (Metadata Cache)**: 缓存数据源的表结构统计信息（行数、分布）。这对于优化器的代价估算（Cost-Based Optimization）至关重要。Calcite 的 CachingCalciteSchema 提供了基础支持 9。  
2. **结果集缓存 (Result Cache)**: 对于低频更新的报表类查询，可以在 IntelliSqlMeta 层通过 SQL 哈希值拦截请求。如果缓存命中，直接返回序列化好的 Protobuf 数据帧，完全绕过解析与执行层。

### **8.2 高级优化策略 (Advanced Optimization)**

* **物化视图 (Materialized Views)**: 利用 Calcite 的 MaterializationService。系统可以自动识别频繁查询的模式，并在后台将异构 Join 的结果“物化”到一张本地表或高速缓存（如 Redis）中。后续查询将通过 MaterializedViewSubstitutionRule 自动重写，指向物化表 28。  
* **Lattice (晶格) 优化**: 定义星型模型（Star Schema）的 Lattice，让 Calcite 自动管理聚合表（Aggregates），从而极速响应 OLAP 查询。

## ---

**9\. 结论**

IntelliSQL 的架构设计方案紧密围绕“联邦”与“翻译”两大核心诉求，通过对标 QSQL 并深度利用 Calcite 与 Avatica 的原生能力，构建了一个轻量级、高扩展的 SQL 中间件。

1. **存储层**通过 SchemaFactory 与 model.json 实现了对异构数据源的标准化接入与元数据统一。  
2. **解析层**利用 RelToSqlConverter 提供了强大的方言翻译能力，解决了跨库迁移与调试的痛点。  
3. **计算层**基于 Enumerable 约定实现了无依赖的内存计算，为轻量级部署提供了可能，同时保留了未来接入 Spark 的扩展性。  
4. **服务与客户端**通过 Avatica 与 Sqlline 的定制，提供了标准化的 JDBC 接口与专业级的 CLI 体验。

本报告提供的技术蓝图，不仅满足了当前的功能需求，更通过模块化的分层设计，为 IntelliSQL 向高性能、智能化的数据虚拟化平台演进奠定了坚实基础。

---

**数据引用索引**: 4 \- Calcite SchemaFactory, Model.json, JDBC Adapter 1 \- QSQL Architecture, Layering 12 \- RelToSqlConverter, SqlToRelConverter, Logical Planning 6 \- Avatica Meta, Service, Architecture 25 \- Sqlline, CLI Customization, Properties 2 \- Enumerable Convention, Pushdown Rules 10 \- Cross-source Join, Elasticsearch Adapter Details

#### **Works cited**

1. Qihoo360/Quicksql: A Flexible, Fast, Federated(3F) SQL Analysis Middleware for Multiple Data Sources \- GitHub, accessed February 16, 2026, [https://github.com/Qihoo360/Quicksql](https://github.com/Qihoo360/Quicksql)  
2. Adapters \- Apache Calcite, accessed February 16, 2026, [https://calcite.apache.org/docs/adapter.html](https://calcite.apache.org/docs/adapter.html)  
3. Apache Calcite: A Foundational Framework for Optimized Query Processing Over Heterogeneous Data Sources, accessed February 16, 2026, [https://r-libre.teluq.ca/1401/1/apache-calcite.pdf](https://r-libre.teluq.ca/1401/1/apache-calcite.pdf)  
4. Introduction to Apache Calcite | Baeldung, accessed February 16, 2026, [https://www.baeldung.com/apache-calcite](https://www.baeldung.com/apache-calcite)  
5. SchemaFactory (Apache Calcite API), accessed February 16, 2026, [https://calcite.apache.org/javadocAggregate/org/apache/calcite/schema/SchemaFactory.html](https://calcite.apache.org/javadocAggregate/org/apache/calcite/schema/SchemaFactory.html)  
6. Background \- Apache Calcite, accessed February 16, 2026, [https://calcite.apache.org/avatica/docs/](https://calcite.apache.org/avatica/docs/)  
7. Avatica HOWTO \- Apache Calcite, accessed February 16, 2026, [https://calcite.apache.org/avatica/docs/howto.html](https://calcite.apache.org/avatica/docs/howto.html)  
8. accessed January 1, 1970, [https://github.com/Qihoo360/Quicksql/blob/master/core/src/main/java/com/qihoo/qsql/planner/ExecutionPipeline.java](https://github.com/Qihoo360/Quicksql/blob/master/core/src/main/java/com/qihoo/qsql/planner/ExecutionPipeline.java)  
9. JSON/YAML models \- Apache Calcite, accessed February 16, 2026, [https://calcite.apache.org/docs/model.html](https://calcite.apache.org/docs/model.html)  
10. calcite elasticsearch adapter efficient join \- Stack Overflow, accessed February 16, 2026, [https://stackoverflow.com/questions/53326546/calcite-elasticsearch-adapter-efficient-join](https://stackoverflow.com/questions/53326546/calcite-elasticsearch-adapter-efficient-join)  
11. SQL language \- Apache Calcite \- Apache Software Foundation, accessed February 16, 2026, [https://calcite.apache.org/docs/reference.html](https://calcite.apache.org/docs/reference.html)  
12. SqlToRelConverter (Apache Calcite API), accessed February 16, 2026, [https://calcite.apache.org/javadocAggregate/org/apache/calcite/sql2rel/SqlToRelConverter.html](https://calcite.apache.org/javadocAggregate/org/apache/calcite/sql2rel/SqlToRelConverter.html)  
13. Assembling a query optimizer with Apache Calcite | Querify Labs, accessed February 16, 2026, [https://www.querifylabs.com/blog/assembling-a-query-optimizer-with-apache-calcite](https://www.querifylabs.com/blog/assembling-a-query-optimizer-with-apache-calcite)  
14. RelToSqlConverter (Apache Calcite core API) \- Javadoc.io, accessed February 16, 2026, [https://javadoc.io/doc/org.apache.calcite/calcite-core/1.25.0/org/apache/calcite/rel/rel2sql/RelToSqlConverter.html](https://javadoc.io/doc/org.apache.calcite/calcite-core/1.25.0/org/apache/calcite/rel/rel2sql/RelToSqlConverter.html)  
15. RelToSqlConverter (Apache Calcite API), accessed February 16, 2026, [https://calcite.apache.org/javadocAggregate/org/apache/calcite/rel/rel2sql/RelToSqlConverter.html](https://calcite.apache.org/javadocAggregate/org/apache/calcite/rel/rel2sql/RelToSqlConverter.html)  
16. SqlDialect (Apache Calcite API), accessed February 16, 2026, [https://calcite.apache.org/javadocAggregate/org/apache/calcite/sql/SqlDialect.html](https://calcite.apache.org/javadocAggregate/org/apache/calcite/sql/SqlDialect.html)  
17. How to push down project, filter, aggregation to TableScan in Calcite \- Stack Overflow, accessed February 16, 2026, [https://stackoverflow.com/questions/40217160/how-to-push-down-project-filter-aggregation-to-tablescan-in-calcite](https://stackoverflow.com/questions/40217160/how-to-push-down-project-filter-aggregation-to-tablescan-in-calcite)  
18. Apache Calcite: A Foundational Framework for Optimized Query Processing Over Heterogeneous Data Sources, accessed February 16, 2026, [https://15799.courses.cs.cmu.edu/spring2025/papers/20-calcite/p221-begoli.pdf](https://15799.courses.cs.cmu.edu/spring2025/papers/20-calcite/p221-begoli.pdf)  
19. Spark vs SQL: A Comprehensive Comparison | by James Fahey \- Medium, accessed February 16, 2026, [https://medium.com/@fahey\_james/spark-vs-sql-a-comprehensive-comparison-b310bdf0211b](https://medium.com/@fahey_james/spark-vs-sql-a-comprehensive-comparison-b310bdf0211b)  
20. SQL JDBC driver API | Apache® Druid, accessed February 16, 2026, [https://druid.apache.org/docs/latest/api-reference/sql-jdbc/](https://druid.apache.org/docs/latest/api-reference/sql-jdbc/)  
21. Background \- Apache Calcite, accessed February 16, 2026, [https://calcite.apache.org/docs/](https://calcite.apache.org/docs/)  
22. calcite-avatica/core/src/main/java/org/apache/calcite/avatica/AvaticaConnection.java at main \- GitHub, accessed February 16, 2026, [https://github.com/apache/calcite-avatica/blob/master/core/src/main/java/org/apache/calcite/avatica/AvaticaConnection.java](https://github.com/apache/calcite-avatica/blob/master/core/src/main/java/org/apache/calcite/avatica/AvaticaConnection.java)  
23. calcite-avatica/tck/README.md at main \- GitHub, accessed February 16, 2026, [https://github.com/apache/calcite-avatica/blob/main/tck/README.md](https://github.com/apache/calcite-avatica/blob/main/tck/README.md)  
24. MetaImpl (Apache Calcite Avatica API), accessed February 16, 2026, [https://calcite.apache.org/avatica/javadocAggregate/org/apache/calcite/avatica/MetaImpl.html](https://calcite.apache.org/avatica/javadocAggregate/org/apache/calcite/avatica/MetaImpl.html)  
25. How To Establish JDBC Connections From the CLI | by Jonathan Merlevede | Dataminded, accessed February 16, 2026, [https://medium.com/datamindedbe/connecting-to-databases-using-jdbc-from-the-cli-d2f5b1c30f5d](https://medium.com/datamindedbe/connecting-to-databases-using-jdbc-from-the-cli-d2f5b1c30f5d)  
26. SQLLine 1.12.0, accessed February 16, 2026, [https://julianhyde.github.io/sqlline/manual.html](https://julianhyde.github.io/sqlline/manual.html)  
27. Customizing banner content \- IBM, accessed February 16, 2026, [https://www.ibm.com/docs/en/sig-and-i/10.0.2?topic=customization-customizing-banner-content](https://www.ibm.com/docs/en/sig-and-i/10.0.2?topic=customization-customizing-banner-content)  
28. Streaming \- Apache Calcite, accessed February 16, 2026, [https://calcite.apache.org/docs/stream.html](https://calcite.apache.org/docs/stream.html)  
29. Apache Calcite: A Foundational Framework for Optimized ery Processing Over Heterogeneous Data Sources \- OSTI, accessed February 16, 2026, [https://www.osti.gov/servlets/purl/1474637](https://www.osti.gov/servlets/purl/1474637)  
30. Meta (Apache Calcite Avatica API), accessed February 16, 2026, [https://calcite.apache.org/avatica/javadocAggregate/org/apache/calcite/avatica/Meta.html](https://calcite.apache.org/avatica/javadocAggregate/org/apache/calcite/avatica/Meta.html)  
31. 4.15. The SQLLine Utility \- Oracle Help Center, accessed February 16, 2026, [https://docs.oracle.com/cd/E13189\_01/kodo/docs40/full/html/ref\_guide\_schema\_sqlline.html](https://docs.oracle.com/cd/E13189_01/kodo/docs40/full/html/ref_guide_schema_sqlline.html)