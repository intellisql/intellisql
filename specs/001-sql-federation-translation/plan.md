# 实现计划：IntelliSql SQL 联邦和翻译

**分支**: `001-sql-federation-translation` | **日期**: 2026-02-17 | **规格**: [spec.md](./spec.md)
**输入**: 来自 `/specs/001-sql-federation-translation/spec.md` 的功能规格

## 概述

IntelliSql 是一个聚焦于 SQL 能力的平台，本次迭代实现 SQL 联邦查询和 SQL 方言翻译功能。通过 Apache Calcite 提供多方言 SQL 解析和翻译，通过 Avatica 提供标准 JDBC 服务。支持 MySQL、PostgreSQL、Elasticsearch 等异构数据源的联邦查询。

## 技术上下文

**语言/版本**: Java 8+（保持 JDK 8 兼容性）
**主要依赖**: Apache Calcite 1.41.0, Apache Avatica 1.27.0, Lombok 1.18.30
**存储**: 无持久化存储（内存中执行跨源 JOIN）
**测试**: JUnit 5.10.2（单元测试）, TestContainers 1.19.4（E2E 集成测试）
**目标平台**: Linux 服务器（JVM）；isql 支持 Linux/macOS/Windows（Native Image）
**项目类型**: 多模块 Maven 项目
**性能目标**: 单表查询额外开销 <50ms；跨源 JOIN（10万行）<5s；isql 启动 <0.5s
**约束**: JDK 8 兼容（Server）；GraalVM 兼容（Client）；100 并发连接；100万行结果集不 OOM
**规模**: 支持 5 种数据库方言；100+ 并发用户

## 宪法检查

*门禁：必须在阶段 0 研究前通过。阶段 1 设计后重新检查。*

| 原则 | 状态 | 合规说明 |
|------|------|----------|
| 用心 | ✅ 通过 | 遵循 ShardingSphere 代码风格，工匠精神 |
| 可读 | ✅ 通过 | 清晰命名，无歧义代码，自文档化 |
| 整洁 | ✅ 通过 | 遵循整洁代码原则，重构是必须的 |
| 一致 | ✅ 通过 | 统一代码风格（Spotless + Checkstyle） |
| 精简 | ✅ 通过 | 极简代码，DRY 原则，无重复配置 |
| 抽象 | ✅ 通过 | 模块分层清晰，职责单一 |
| 极致 | ✅ 通过 | 无无用空行，无占位符代码 |

**技术约束检查**:
- ✅ JDK 8 兼容性（服务端）
- ✅ GraalVM Native Image 兼容性（客户端）
- ✅ ShardingSphere 代码风格（Spotless + Checkstyle）
- ✅ 无 CVE 漏洞依赖
- ✅ 多模块架构（参考 360 QuickSql）

## 项目结构

### 文档（本功能）

```text
specs/001-sql-federation-translation/
├── plan.md              # 本文件（/speckit.plan 命令输出）
├── research.md          # 阶段 0 输出（技术研究）
├── data-model.md        # 阶段 1 输出（数据模型）
├── quickstart.md        # 阶段 1 输出（快速入门）
├── contracts/           # 阶段 1 输出（契约定义）
│   ├── jdbc-protocol.md
│   ├── config-schema.md
│   └── isql-cli.md      # isql 命令行接口定义
└── tasks.md             # 阶段 2 输出（/speckit.tasks 命令）
```

### 源码结构（仓库根目录）

```text
intellisql/
├── intellisql-common/              # 公共基础设施
│   └── src/main/java/com/intellisql/common/
│       ├── config/                 # 配置加载（ConfigLoader, ModelConfig, DataSourceConfig）
│       ├── logger/                 # 日志（StructuredLogger, QueryContext）
│       ├── retry/                  # 重试机制（RetryPolicy, ExponentialBackoffRetry）
│       ├── metadata/               # 元数据实体（Column, Table, Schema, DataSource）
│       │   └── enums/              # 枚举（DataSourceType, DataType, TableType）
│       └── dialect/                # 方言枚举（SqlDialect）
├── intellisql-parser/              # SQL 解析与翻译
│   └── src/
│       ├── main/java/
│       └── test/java/
├── intellisql-features/            # 功能特性父模块
│   ├── intellisql-optimizer/       # SQL 优化器
│   │   └── src/main/java/com/intellisql/optimizer/
│   │       ├── cost/               # 代价模型（FederatedCost, CostFactor）
│   │       ├── metadata/           # 元数据提供者
│   │       ├── plan/               # 逻辑执行计划
│   │       └── rule/               # 优化规则
│   ├── intellisql-translator/      # SQL 翻译器
│   │   └── src/main/java/com/intellisql/translator/
│   │       ├── dialect/            # 方言转换器
│   │       ├── SqlTranslator.java
│   │       └── Translation.java
│   └── intellisql-federation/      # 联邦查询核心
│       └── src/main/java/com/intellisql/federation/
│           ├── IntelliSqlKernel.java       # 内核入口
│           ├── QueryProcessor.java         # 查询处理器
│           ├── DataSourceManager.java      # 数据源管理
│           ├── executor/                   # 执行引擎
│           │   ├── FederatedQueryExecutor.java
│           │   ├── QueryExecutor.java
│           │   ├── iterator/               # Volcano 迭代器
│           │   └── plan/                   # 物理计划转换
│           └── metadata/                   # 元数据管理
├── intellisql-connector/           # 数据源连接器
│   └── src/
│       ├── main/java/
│       │   ├── api/                # Connector SPI
│       │   ├── mysql/              # MySQL 适配器
│       │   ├── postgresql/         # PostgreSQL 适配器
│       │   └── elasticsearch/      # Elasticsearch 适配器
│       └── test/java/
├── intellisql-jdbc/                # JDBC 驱动
│   └── src/
│       ├── main/java/
│       └── test/java/
├── intellisql-server/              # 服务端实现
│   └── src/
│       ├── main/java/
│       ├── main/resources/
│       └── test/java/
├── intellisql-client/              # isql CLI 工具
│   ├── src/
│   │   ├── main/java/
│   │   ├── main/resources/
│   │   │   ├── META-INF/native-image/ # GraalVM 配置
│   │   │   └── sql.nanorc             # 语法高亮配置
│   │   └── test/java/
│   └── pom.xml                     # 含 native-maven-plugin
├── intellisql-distribution/        # 打包分发父模块
│   ├── intellisql-distribution-jdbc/
│   └── intellisql-distribution-server/
├── intellisql-test/                # 测试模块父模块
│   ├── intellisql-test-it/         # 集成测试
│   └── intellisql-test-e2e/        # 端到端测试
├── conf/                           # 配置文件
│   └── model.yaml
├── mvnw                            # Maven Wrapper
├── mvnw.cmd
├── pom.xml
└── .mvn/
    └── wrapper/
```

**结构决策**: 采用多模块 Maven 架构，分为四层：
1. **公共基础层**: common（配置、日志、重试、元数据实体）
2. **功能特性层**: parser、features（optimizer、translator、federation）、connector
3. **协议适配层**: jdbc、server
4. **工具与支撑层**: client、distribution、test

## 依赖版本汇总

| 依赖 | 版本 | 用途 |
|------|------|------|
| Apache Calcite | 1.41.0 | SQL 解析与优化 |
| Apache Avatica | 1.27.0 | JDBC 协议 |
| JLine | 3.25.1 | 终端交互与高亮 |
| Picocli | 4.7.5 | 命令行参数解析 |
| Lombok | 1.18.30 | 精简代码 |
| Spotless | 2.43.0 | 代码格式化 |
| Checkstyle | 3.3.1 | 代码检查 |
| JUnit 5 | 5.10.2 | 单元测试 |
| TestContainers | 1.19.4 | E2E 集成测试 |
| HikariCP | 5.1.0 | 连接池 |
| SnakeYAML | 2.2 | YAML 配置解析 |
| SLF4J | 2.0.11 | 日志门面 |
| Logback | 1.4.14 | 日志实现 |

## 复杂度追踪

> **仅在宪法检查有违规需要说明时填写**

无违规。

## 下一步

1. ✅ 阶段 0: research.md 已完成
2. ✅ 阶段 1: data-model.md 已完成
3. ✅ 阶段 1: contracts/ 已完成
4. ✅ 阶段 1: quickstart.md 已完成
5. ⏭️ 阶段 2: 运行 `/speckit.tasks` 生成任务列表

---

# 模块实现计划：intellisql-parser

**参考实现**: `/Users/duanzhengqiang/IdeaProjects/Quicksql/parser`
**日期**: 2026-02-19
**目标**: 扩展 Calcite Parser.jj 模板，支持完整的 MySQL 和 PostgreSQL 方言语法

## 模块概述

intellisql-parser 是 IntelliSql 的 SQL 解析与翻译核心模块，采用模板化可扩展解析器架构（参考 Quicksql 实现），基于 JavaCC + FreeMarker 模板生成技术，支持多方言 SQL 解析和方言间转换。

### 核心能力

- **多方言解析**: 支持 MySQL、PostgreSQL、Oracle、SQL Server、Hive 五种数据库方言
- **完整 DML 支持**: SELECT、INSERT、UPDATE、DELETE 全部语句
- **宽松解析模式**: 基于 Babel Parser 支持跨方言混合语法
- **精准翻译**: 在线模式（连接数据库获取元数据）和离线模式（纯语法转换）
- **详细错误处理**: 包含位置、上下文片段和修复建议

## 技术架构

### Parser 架构模式

采用 **Template-Based Extensible Parser Architecture**：

```
┌─────────────────────────────────────────────────────────────┐
│                    SqlParserFactory                          │
│              (Parser 创建与配置入口)                          │
├─────────────────────────────────────────────────────────────┤
│                    BabelParserConfiguration                   │
│            (多方言宽松解析配置)                                │
├─────────────────────────────────────────────────────────────┤
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐   │
│  │ SqlDialect    │  │ SqlDialect    │  │ SqlDialect    │   │
│  │ (MySQL)       │  │ (PostgreSQL)  │  │ (Others...)   │   │
│  └───────────────┘  └───────────────┘  └───────────────┘   │
├─────────────────────────────────────────────────────────────┤
│              DialectConverterFactory                         │
│          (方言转换器工厂，SQL 生成)                            │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Code Generation Pipeline                │   │
│  │  FMPP (FreeMarker) → Parser.jj → JavaCC → Java      │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 源码结构

```text
intellisql-parser/
├── pom.xml                              # Maven 配置（含 FMPP、JavaCC 插件）
├── src/
│   ├── main/
│   │   ├── codegen/                     # 代码生成配置
│   │   │   ├── config.fmpp              # FMPP 配置文件
│   │   │   ├── templates/
│   │   │   │   └── Parser.jj            # JavaCC 模板
│   │   │   └── includes/
│   │   │       ├── parserImpls.ftl      # 自定义语法规则扩展
│   │   │       └── compoundIdentifier.ftl
│   │   └── java/
│   │       └── org/intellisql/parser/
│   │           ├── SqlParserFactory.java        # Parser 工厂
│   │           ├── BabelParserConfiguration.java # Babel 配置
│   │           ├── SqlTranslator.java           # 翻译入口
│   │           ├── SqlNodeToStringConverter.java # AST → SQL
│   │           ├── Translation.java             # 翻译结果
│   │           ├── TranslationMode.java         # 在线/离线模式
│   │           ├── TranslationException.java    # 翻译异常
│   │           ├── ast/                         # 自定义 AST 节点
│   │           │   ├── SqlShowTables.java
│   │           │   ├── SqlShowSchemas.java
│   │           │   └── SqlUseSchema.java
│   │           └── dialect/
│   │               ├── SqlDialect.java              # 方言枚举
│   │               ├── DialectConverterFactory.java # 方言转换器工厂
│   │               ├── MySQLDialectConverter.java   # MySQL 转换器
│   │               ├── PostgreSQLDialectConverter.java
│   │               ├── OracleDialectConverter.java
│   │               ├── SQLServerDialectConverter.java
│   │               └── HiveDialectConverter.java
│   └── test/
│       ├── codegen/                     # 测试用代码生成配置
│       │   └── config.fmpp
│       └── java/
│           └── org/intellisql/parser/
│               ├── SqlParserFactoryTest.java
│               ├── SqlTranslatorTest.java
│               ├── dialect/
│               │   ├── MySQLDialectConverterTest.java
│               │   └── PostgreSQLDialectConverterTest.java
│               └── extension/
│                   └── ExtensionSqlParserTest.java
└── target/
    └── generated-sources/
        ├── fmpp/                        # FMPP 生成的 Parser.jj
        └── javacc/                      # JavaCC 生成的 Java 源码
```

## 核心组件设计

### 1. SqlParserFactory

**职责**: 创建配置好的 SqlParser 实例

**关键方法**:
- `createParser(String sql, SqlDialect dialect)`: 创建方言特定的解析器
- `createBabelParser(String sql)`: 创建 Babel 宽松解析器
- `parse(String sql, SqlDialect dialect)`: 解析 SQL 为 AST
- `parseWithBabel(String sql)`: 使用 Babel 解析

### 2. BabelParserConfiguration

**职责**: 配置多方言宽松解析

**实现要点**:
- 继承 Calcite 的 SqlParser.Config
- 配置 Lex 为兼容模式
- 支持混合方言语法

### 3. DialectConverterFactory

**职责**: 根据目标方言创建对应的转换器

**实现要点**:
```java
public final class DialectConverterFactory {
    public static DialectConverter create(SqlDialect dialect) {
        switch (dialect) {
            case MYSQL: return new MySQLDialectConverter();
            case POSTGRESQL: return new PostgreSQLDialectConverter();
            case ORACLE: return new OracleDialectConverter();
            case SQLSERVER: return new SQLServerDialectConverter();
            case HIVE: return new HiveDialectConverter();
            default: throw new IllegalArgumentException("Unsupported dialect: " + dialect);
        }
    }
}
```

### 4. DialectConverter (接口)

**职责**: 方言特定的 SQL 转换逻辑

**关键方法**:
- `convert(SqlNode node)`: 转换 AST 节点
- `getDialect()`: 返回方言类型
- `supportsFeature(SqlFeature feature)`: 检查特性支持

### 5. AST 扩展节点

**继承体系**:
```
SqlNode (Calcite)
└── SqlCall (Calcite)
    ├── SqlShowTables (自定义)
    ├── SqlShowSchemas (自定义)
    └── SqlUseSchema (自定义)
```

**实现模式** (参考 Quicksql SqlShowTables):
```java
public class SqlShowTables extends SqlCall {
    private final SqlIdentifier db;
    private final SqlNode likePattern;
    public static final SqlSpecialOperator OPERATOR =
        new SqlSpecialOperator("SHOW_TABLES", SqlKind.OTHER);

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        writer.keyword("SHOW");
        writer.keyword("TABLES");
        // ... 方言特定的 unparsing 逻辑
    }
}
```

## 代码生成流水线

### Maven 插件配置

```xml
<!-- FMPP: FreeMarker 模板预处理 -->
<plugin>
    <groupId>org.apache.drill.tools</groupId>
    <artifactId>drill-fmpp-maven-plugin</artifactId>
    <version>1.21.0</version>
    <executions>
        <execution>
            <id>generate-fmpp-sources</id>
            <phase>validate</phase>
            <goals><goal>generate</goal></goals>
            <configuration>
                <config>src/main/codegen/config.fmpp</config>
                <templates>src/main/codegen/templates</templates>
                <output>${project.build.directory}/generated-sources/fmpp</output>
            </configuration>
        </execution>
    </executions>
</plugin>

<!-- JavaCC: 语法解析器生成 -->
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>javacc-maven-plugin</artifactId>
    <version>3.1.0</version>
    <executions>
        <execution>
            <id>javacc</id>
            <goals><goal>javacc</goal></goals>
            <configuration>
                <sourceDirectory>${project.build.directory}/generated-sources/fmpp</sourceDirectory>
                <includes><include>**/Parser.jj</include></includes>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 生成流程

```
1. FMPP Phase (validate)
   Parser.jj.ftl + config.fmpp → Parser.jj

2. JavaCC Phase (generate-sources)
   Parser.jj → SqlParserImpl.java

3. Compile Phase
   SqlParserImpl.java → SqlParserImpl.class
```

## 方言转换策略

### MySQL 方言特性

| 特性 | 处理方式 |
|------|----------|
| 反引号标识符 | `identifierQuoteString: "\`"` |
| LIMIT/OFFSET | `unparseFetchUsingLimit()` |
| IFNULL 函数 | 转换为 COALESCE |
| STRAIGHT_JOIN | 语法扩展支持 |
| UNSIGNED 类型 | 类型映射处理 |

### PostgreSQL 方言特性

| 特性 | 处理方式 |
|------|----------|
| 双引号标识符 | `identifierQuoteString: "\""` |
| FETCH FIRST | `unparseOffsetFetch()` |
| RETURNING 子句 | 语法扩展支持 |
| LATERAL JOIN | 语法扩展支持 |
| :: 类型转换 | unparse 处理 |

### 错误处理

**错误信息结构**:
```java
public class TranslationError {
    private final int line;
    private final int column;
    private final String context;      // 错误上下文片段
    private final String message;      // 错误消息
    private final String suggestion;   // 修复建议
}
```

**示例输出**:
```
Translation Error at line 3, column 15:
  SELECT * FROM users LIMT 10
                    ^
  Message: "LIMT" is not a valid keyword
  Suggestion: Did you mean "LIMIT"?
```

## 测试策略

### 单元测试

| 测试类 | 覆盖范围 |
|--------|----------|
| SqlParserFactoryTest | Parser 创建、配置、基础解析 |
| MySQLDialectConverterTest | MySQL 特定语法转换 |
| PostgreSQLDialectConverterTest | PostgreSQL 特定语法转换 |
| SqlNodeToStringConverterTest | AST → SQL 转换 |

### 扩展测试

```java
public class ExtensionSqlParserTest extends SqlParserTest {
    @Override
    protected SqlParserImplFactory parserImplFactory() {
        return IntelliSqlParserImpl.FACTORY;
    }

    @Test
    void assertShowTablesParsing() {
        check("SHOW TABLES FROM mydb", "SHOW TABLES FROM `mydb`");
    }
}
```

### 测试数据管理

- 使用资源文件存储测试 SQL
- 参数化测试覆盖多种方言组合
- 边界值测试（超长 SQL、特殊字符、嵌套查询）

## 宪法检查（Parser 模块）

| 原则 | 状态 | 合规说明 |
|------|------|----------|
| 用心 | ✅ | 参考 Quicksql 成熟实现，工匠精神 |
| 可读 | ✅ | 清晰的分层架构，自文档化命名 |
| 整洁 | ✅ | 无无用空行，遵循编码规范 |
| 一致 | ✅ | 与 Quicksql 风格保持一致 |
| 精简 | ✅ | DRY 原则，复用 Calcite 基础设施 |
| 抽象 | ✅ | Factory、Converter 层次清晰 |
| 极致 | ✅ | 无占位符代码，每行有意义 |

## 实现优先级

### Phase 1: 基础框架 (P0)

1. 搭建 FMPP + JavaCC 代码生成流水线
2. 实现 SqlParserFactory 基础功能
3. 实现 BabelParserConfiguration
4. 基础单元测试

### Phase 2: 方言转换 (P0)

1. 实现 DialectConverterFactory
2. 实现 MySQLDialectConverter
3. 实现 PostgreSQLDialectConverter
4. 方言转换测试

### Phase 3: 语法扩展 (P1)

1. 扩展 Parser.jj 模板支持 MySQL/PostgreSQL 特有语法
2. 实现自定义 AST 节点 (SqlShowTables 等)
3. 扩展测试

### Phase 4: 其他方言 (P2)

1. OracleDialectConverter
2. SQLServerDialectConverter
3. HiveDialectConverter

## 下一步行动

1. ⏭️ 运行 `/speckit.tasks` 生成 intellisql-parser 详细任务列表
2. 开始 Phase 1 基础框架实现

---

# 模块实现计划：联邦查询增强 (intellisql-optimizer & intellisql-federation)

**参考实现**: ShardingSphere sql-federation
**日期**: 2026-02-20
**目标**: 实现混合优化器策略、完整代价模型、Volcano 迭代器执行模型

## 模块概述

基于 spec.md Session 2026-02-20 的澄清，在现有联邦查询实现基础上增强以下能力：

| 功能 | 说明 | 参考实现 |
|------|------|----------|
| 混合优化器策略 | HepPlanner (RBO) + VolcanoPlanner (CBO) | ShardingSphere sql-federation |
| 完整代价模型 | CPU + I/O + 网络 + 内存 | Calcite RelOptCost |
| 扩展 RBO 规则集 | filter/projection pushdown, join reorder, subquery rewrite, aggregate split | ShardingSphere |
| Volcano 迭代器模型 | open-next-close 协议 | Volcano Executor |
| RelMetadataQuery | 标准 Calcite 统计信息集成 | Calcite Metadata |

## 现有实现分析

### 关键文件

```
intellisql-features/intellisql-optimizer/
├── src/main/java/com/intellisql/optimizer/
│   ├── Optimizer.java                    # 当前仅 HepPlanner (RBO)
│   ├── plan/
│   │   ├── ExecutionPlan.java
│   │   └── ExecutionStage.java
│   └── rule/
│       ├── PredicatePushDownRule.java    # 已实现
│       └── ProjectionPushDownRule.java   # 已实现

intellisql-features/intellisql-federation/
├── src/main/java/com/intellisql/federation/
│   ├── IntelliSqlKernel.java             # 核心入口
│   ├── QueryProcessor.java               # 查询处理管道
│   ├── DataSourceManager.java            # 数据源管理
│   ├── executor/
│   │   ├── FederatedQueryExecutor.java   # 跨源 JOIN 执行
│   │   ├── QueryExecutor.java
│   │   └── IntermediateResultLimiter.java
│   └── metadata/
│       └── MetadataManager.java
```

### 当前优化流程

```
SQL → SqlParserFactory.parseWithBabel() → SqlNode
    → planner.rel() → RelNode (Logical Plan)
    → Optimizer.optimize() [HepPlanner only] → RelNode (Optimized)
    → Optimizer.generateExecutionPlan() → ExecutionPlan
    → QueryProcessor.executePlan() → QueryResult
```

## 技术架构设计

### 1. 混合优化器策略

**目标**: 实现两阶段优化 RBO → CBO

**新增文件**:

```
intellisql-features/intellisql-optimizer/src/main/java/com/intellisql/optimizer/
├── HybridOptimizer.java              # 混合优化器入口
├── RboOptimizer.java                 # RBO 优化器（重构自 Optimizer）
├── CboOptimizer.java                 # CBO 优化器（新增）
├── cost/
│   ├── FederatedCost.java            # 代价实现
│   ├── FederatedCostFactory.java     # 代价工厂
│   └── CostFactor.java               # 代价因子枚举
└── metadata/
    ├── FederatedMetadataProvider.java # 元数据提供者
    └── StatisticsHandler.java         # 统计信息处理器
```

**优化流程**:

```
                    ┌─────────────────────────────────────────┐
                    │              HybridOptimizer             │
                    └─────────────────────────────────────────┘
                                       │
                    ┌──────────────────┴──────────────────┐
                    ▼                                      ▼
        ┌───────────────────┐                  ┌───────────────────┐
        │   RboOptimizer    │                  │   CboOptimizer    │
        │   (HepPlanner)    │                  │ (VolcanoPlanner)  │
        └───────────────────┘                  └───────────────────┘
                    │                                      │
                    │  • PredicatePushDown                │  • JoinReorder
                    │  • ProjectionPushDown               │  • CostBasedJoinOrder
                    │  • SubqueryRewrite                  │  • AccessPathSelection
                    │  • AggregateSplit                   │
                    ▼                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │                   Optimized Plan                     │
        └─────────────────────────────────────────────────────┘
```

**关键实现**:

```java
public class HybridOptimizer {
    private final RboOptimizer rboOptimizer;
    private final CboOptimizer cboOptimizer;
    public RelNode optimize(RelNode logicalPlan) {
        // Phase 1: RBO - 规则优化（确定性转换）
        RelNode afterRbo = rboOptimizer.optimize(logicalPlan);
        // Phase 2: CBO - 代价优化（基于统计信息）
        return cboOptimizer.optimize(afterRbo);
    }
}
```

### 2. 完整代价模型

**目标**: CPU + I/O + 网络 + 内存四维代价

**代价因子**:

| 因子 | 权重 | 说明 |
|------|------|------|
| CPU | 1.0 | 计算代价（表达式求值、函数调用） |
| IO | 10.0 | 磁盘 I/O 代价（表扫描） |
| NETWORK | 100.0 | 网络传输代价（跨源 JOIN 关键） |
| MEMORY | 5.0 | 内存使用代价（中间结果缓存） |

**代价计算**:

```java
public class FederatedCost implements RelOptCost {
    private final double cpu;
    private final double io;
    private final double network;
    private final double memory;
    private static final double CPU_WEIGHT = 1.0;
    private static final double IO_WEIGHT = 10.0;
    private static final double NETWORK_WEIGHT = 100.0;
    private static final double MEMORY_WEIGHT = 5.0;
    @Override
    public double getValue() {
        return cpu * CPU_WEIGHT
             + io * IO_WEIGHT
             + network * NETWORK_WEIGHT
             + memory * MEMORY_WEIGHT;
    }
    public boolean isLt(RelOptCost other) {
        return this.getValue() < other.getValue();
    }
}
```

**跨源 JOIN 代价估算**:

```java
public class FederatedJoinCostEstimator {
    public FederatedCost estimateJoinCost(RelNode left, RelNode right, JoinInfo joinInfo) {
        double leftRows = estimateRowCount(left);
        double rightRows = estimateRowCount(right);
        double resultRows = estimateJoinResultRows(leftRows, rightRows, joinInfo);
        // 网络代价：需要传输的数据量
        double networkCost = (leftRows + rightRows) * getAvgRowSize(left, right);
        // CPU 代价：JOIN 计算
        double cpuCost = leftRows * rightRows * JOIN_CPU_FACTOR;
        // 内存代价：中间结果
        double memoryCost = resultRows * getAvgRowSize(left, right);
        return new FederatedCost(cpuCost, 0, networkCost, memoryCost);
    }
}
```

### 3. 扩展 RBO 规则集

**目标**: 参考 ShardingSphere 完善规则

**新增规则** (位于 `intellisql-features/intellisql-optimizer/src/main/java/com/intellisql/optimizer/rule/`):

| 规则 | 类名 | 功能 | 优先级 |
|------|------|------|--------|
| JOIN 重排序 | `JoinReorderRule.java` | 小表驱动大表，减少中间结果 | P0 |
| 子查询重写 | `SubqueryRewriteRule.java` | 子查询转 JOIN | P0 |
| 聚合拆分 | `AggregateSplitRule.java` | 聚合下推到数据源 | P1 |
| UNION 合并 | `UnionMergeRule.java` | UNION 优化合并 | P2 |
| LIMIT 下推 | `LimitPushDownRule.java` | LIMIT 下推到数据源 | P0 |

**规则注册**:

```java
public class RboOptimizer {
    private List<RelOptRule> buildDefaultRules() {
        List<RelOptRule> rules = new LinkedList<>();
        // 已有规则
        rules.add(PredicatePushDownRule.INSTANCE);
        rules.add(ProjectionPushDownRule.INSTANCE);
        // 新增规则
        rules.add(JoinReorderRule.INSTANCE);
        rules.add(SubqueryRewriteRule.INSTANCE);
        rules.add(AggregateSplitRule.INSTANCE);
        rules.add(LimitPushDownRule.INSTANCE);
        return rules;
    }
}
```

**ShardingSphere 规则参考**:

```
shardingsphere-sql-parser/
└── sql-federation-features/
    └── optimizer/
        ├── src/main/java/org/apache/shardingsphere/sqlfederation/optimizer/
        │   ├── rule/
        │   │   ├── FilterPushDownRule.java
        │   │   ├── ProjectPushDownRule.java
        │   │   ├── JoinReorderRule.java
        │   │   └── SubqueryRewriteRule.java
        │   └── ...
```

### 4. Volcano 迭代器执行模型

**目标**: open-next-close 协议的流式执行

**新增文件**:

```
intellisql-features/intellisql-federation/src/main/java/com/intellisql/federation/executor/
├── iterator/
│   ├── QueryIterator.java            # 迭代器接口
│   ├── AbstractOperator.java         # 算子基类
│   ├── TableScanOperator.java        # 表扫描算子
│   ├── JoinOperator.java             # JOIN 算子
│   ├── FilterOperator.java           # 过滤算子
│   ├── ProjectOperator.java          # 投影算子
│   ├── AggregateOperator.java        # 聚合算子
│   └── SortOperator.java             # 排序算子
└── plan/
    └── PhysicalPlanConverter.java    # RelNode → Operator Tree
```

**接口设计**:

```java
public interface QueryIterator extends AutoCloseable {
    /**
     * 初始化资源（打开游标、建立连接等）
     */
    void open() throws SQLException;
    /**
     * 是否有更多数据
     */
    boolean hasNext() throws SQLException;
    /**
     * 获取下一行数据
     */
    Row next() throws SQLException;
    /**
     * 释放资源（关闭游标、释放连接等）
     */
    void close() throws SQLException;
}
```

**算子基类**:

```java
public abstract class AbstractOperator implements QueryIterator {
    protected final List<QueryIterator> children;
    protected boolean isOpened = false;
    protected AbstractOperator(List<QueryIterator> children) {
        this.children = children;
    }
    @Override
    public void open() throws SQLException {
        for (QueryIterator each : children) {
            each.open();
        }
        isOpened = true;
    }
    @Override
    public void close() throws SQLException {
        for (QueryIterator each : children) {
            each.close();
        }
        isOpened = false;
    }
}
```

**JOIN 算子示例**:

```java
public class JoinOperator extends AbstractOperator {
    private final JoinType joinType;
    private final JoinCondition condition;
    private final KeyExtractor leftKeyExtractor;
    private final KeyExtractor rightKeyExtractor;
    private Iterator<Row> resultIterator;
    @Override
    public void open() throws SQLException {
        super.open();
        // 构建哈希索引
        Map<Object, List<Row>> rightIndex = buildHashIndex(children.get(1));
        // 执行 JOIN
        List<Row> joined = performJoin(rightIndex);
        resultIterator = joined.iterator();
    }
    @Override
    public boolean hasNext() throws SQLException {
        return resultIterator != null && resultIterator.hasNext();
    }
    @Override
    public Row next() throws SQLException {
        return resultIterator.next();
    }
}
```

**执行计划转换**:

```java
public class PhysicalPlanConverter {
    public QueryIterator convert(RelNode relNode) {
        if (relNode instanceof TableScan) {
            return new TableScanOperator((TableScan) relNode);
        } else if (relNode instanceof Join) {
            Join join = (Join) relNode;
            QueryIterator left = convert(join.getLeft());
            QueryIterator right = convert(join.getRight());
            return new JoinOperator(left, right, join.getCondition());
        } else if (relNode instanceof Filter) {
            Filter filter = (Filter) relNode;
            QueryIterator child = convert(filter.getInput());
            return new FilterOperator(child, filter.getCondition());
        }
        // ... 其他算子
        throw new UnsupportedOperationException("Unsupported RelNode: " + relNode.getClass());
    }
}
```

### 5. RelMetadataQuery 元数据支持

**目标**: 标准 Calcite 统计信息集成

**新增文件**:

```
intellisql-features/intellisql-optimizer/src/main/java/com/intellisql/optimizer/metadata/
├── FederatedMetadataProvider.java    # 元数据提供者
├── FederatedRelMetadataQuery.java    # 元数据查询
├── StatisticsHandler.java            # 统计信息处理
└── TableStatistics.java              # 表统计信息
```

**统计信息类型**:

| 统计信息 | 方法 | 说明 |
|----------|------|------|
| 行数估计 | `getRowCount(RelNode)` | 表或表达式行数 |
| 唯一值数量 | `getDistinctCount(RelNode, int)` | 列的唯一值数量 |
| 选择性 | `getSelectivity(RelNode, RexNode)` | 谓词选择性 |
| 数据分布 | `getDistribution(RelNode)` | 数据分布信息 |

**元数据提供者**:

```java
public class FederatedMetadataProvider extends RelMetadataProvider {
    @Override
    public <M extends Metadata> UnboundMetadata<M> apply(Class<? extends RelNode> relClass, Class<M> metadataClass) {
        if (metadataClass == RelMdRowCount.class) {
            return (rel, mq) -> (M) new RelMdRowCount() {
                @Override
                public Double getRowCount(TableScan rel, RelMetadataQuery mq) {
                    return getTableStatistics(rel).getRowCount();
                }
            };
        }
        // ... 其他元数据类型
        return null;
    }
    private TableStatistics getTableStatistics(TableScan tableScan) {
        String tableName = tableScan.getTable().getQualifiedName().toString();
        return statisticsCache.get(tableName);
    }
}
```

**统计信息收集**:

```java
public class StatisticsHandler {
    private final MetadataManager metadataManager;
    public TableStatistics collectStatistics(String dataSourceId, String tableName) {
        DataSourceConnector connector = getConnector(dataSourceId);
        // 执行统计查询
        long rowCount = executeCountQuery(connector, tableName);
        Map<String, Long> distinctCounts = executeDistinctCountQuery(connector, tableName);
        return new TableStatistics(tableName, rowCount, distinctCounts);
    }
}
```

## 实现计划

### Phase 1: 优化器重构 (预估: 3天)

**任务列表**:

1. [ ] 重构 `Optimizer.java` → `RboOptimizer.java`
2. [ ] 实现 `CboOptimizer.java` (VolcanoPlanner)
3. [ ] 实现 `HybridOptimizer.java` (组合 RBO + CBO)
4. [ ] 更新 `QueryProcessor.java` 使用 `HybridOptimizer`
5. [ ] 单元测试

**依赖**: 无

### Phase 2: 代价模型 (预估: 2天)

**任务列表**:

1. [ ] 实现 `FederatedCost.java` (RelOptCost 接口)
2. [ ] 实现 `FederatedCostFactory.java` (代价工厂)
3. [ ] 实现 `CostFactor.java` (代价因子枚举)
4. [ ] 注册到 VolcanoPlanner
5. [ ] 单元测试

**依赖**: Phase 1

### Phase 3: 扩展规则 (预估: 3天)

**任务列表**:

1. [ ] 实现 `JoinReorderRule.java` (JOIN 重排序)
2. [ ] 实现 `SubqueryRewriteRule.java` (子查询重写)
3. [ ] 实现 `AggregateSplitRule.java` (聚合拆分)
4. [ ] 实现 `LimitPushDownRule.java` (LIMIT 下推)
5. [ ] 注册规则到 RboOptimizer
6. [ ] 单元测试

**依赖**: Phase 1

### Phase 4: 迭代器模型 (预估: 4天)

**任务列表**:

1. [ ] 定义 `QueryIterator` 接口
2. [ ] 实现 `AbstractOperator` 基类
3. [ ] 实现 `TableScanOperator`
4. [ ] 实现 `JoinOperator`
5. [ ] 实现 `FilterOperator`
6. [ ] 实现 `ProjectOperator`
7. [ ] 实现 `PhysicalPlanConverter`
8. [ ] 集成到 `FederatedQueryExecutor`
9. [ ] 集成测试

**依赖**: Phase 1, Phase 2, Phase 3

### Phase 5: 元数据集成 (预估: 2天)

**任务列表**:

1. [ ] 实现 `FederatedMetadataProvider.java`
2. [ ] 实现 `FederatedRelMetadataQuery.java`
3. [ ] 实现 `StatisticsHandler.java`
4. [ ] 实现 `TableStatistics.java`
5. [ ] 集成到优化器
6. [ ] 单元测试

**依赖**: Phase 2

## 宪法检查（联邦查询增强）

| 原则 | 状态 | 合规说明 |
|------|------|----------|
| 用心 | ✅ | 参考 ShardingSphere 成熟实现，工匠精神 |
| 可读 | ✅ | 清晰的分层架构，接口设计明确 |
| 整洁 | ✅ | 无无用空行，遵循编码规范 |
| 一致 | ✅ | 与 ShardingSphere 风格保持一致 |
| 精简 | ✅ | DRY 原则，复用 Calcite 基础设施 |
| 抽象 | ✅ | Optimizer、Cost、Iterator 层次清晰 |
| 极致 | ✅ | 无占位符代码，每行有意义 |

## 测试策略

### 单元测试

| 测试类 | 覆盖范围 |
|--------|----------|
| HybridOptimizerTest | 混合优化流程测试 |
| CboOptimizerTest | CBO 优化测试 |
| FederatedCostTest | 代价计算测试 |
| JoinReorderRuleTest | JOIN 重排序测试 |
| QueryIteratorTest | 迭代器协议测试 |
| PhysicalPlanConverterTest | 计划转换测试 |

### 集成测试

```java
@Test
void assertFederatedJoinWithCostBasedOptimization() {
    // 配置两个数据源
    configureDataSource("mysql_db", DataSourceType.MYSQL);
    configureDataSource("es_logs", DataSourceType.ELASTICSEARCH);
    // 执行跨源 JOIN
    String sql = "SELECT o.id, o.customer_name, l.access_time " +
                 "FROM mysql_db.orders o " +
                 "JOIN es_logs.access_logs l ON o.id = l.order_id " +
                 "WHERE o.status = 'completed'";
    QueryResult result = kernel.query(sql);
    // 验证结果
    assertThat(result.isSuccess(), is(true));
    assertThat(result.getRowCount(), greaterThan(0));
}
```

### 性能测试

| 场景 | 目标 | 验证方式 |
|------|------|----------|
| 单表查询 | 额外开销 <50ms | 基准测试 |
| 跨源 JOIN (10万行) | <5s | 集成测试 |
| 大结果集 (100万行) | 无 OOM | 内存监控 |

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| VolcanoPlanner 学习曲线 | 中 | 参考 ShardingSphere 实现 |
| 统计信息缺失导致 CBO 不准确 | 中 | 提供默认统计信息 + 可配置 |
| 迭代器模型内存管理 | 高 | 严格遵循 open-close 协议 |
| 跨源 JOIN 网络延迟 | 高 | 网络代价权重设置较高 |

## 参考资料

- Apache Calcite 官方文档: https://calcite.apache.org/docs/
- ShardingSphere SQL Federation: https://shardingsphere.apache.org/document/current/en/features/sharding/overview/
- Volcano Executor Model: https://paperhub.s3.amazonaws.com/18e91eb4db2114a07ea42010c985778f.pdf

## 下一步行动

1. ⏭️ 运行 `/speckit.tasks` 生成联邦查询增强详细任务列表
2. 开始 Phase 1 优化器重构实现

---

# 模块实现计划：intellisql-client (isql)

**参考实现**: SQLLine, JLine3 Demo
**日期**: 2026-02-22
**目标**: 实现高性能、原生编译的交互式 SQL 命令行工具

## 模块概述

intellisql-client 是 IntelliSql 的官方命令行工具，提供交互式查询（REPL）和脚本执行能力。基于 GraalVM Native Image 技术，实现毫秒级启动和零 JVM 依赖分发。

### 核心能力

- **原生启动**: 启动时间 < 0.5s，单文件分发
- **交互体验**: 语法高亮（nanorc）、自动补全（Context-aware）、历史记录（持久化）
- **结果展示**: 自定义分页渲染（less-like）、CJK 字符对齐、流式处理
- **多模式**: 支持交互模式（REPL）和批处理模式（-f script.sql）

## 技术架构

### 核心组件

```
intellisql-client/
├── src/main/java/com/intellisql/client/
│   ├── IsqlClient.java               # main 入口 (Picocli)
│   ├── command/                      # 客户端命令
│   │   ├── ConnectCommand.java
│   │   ├── TranslateCommand.java
│   │   └── HelpCommand.java
│   ├── console/                      # 终端交互
│   │   ├── ConsoleReader.java        # JLine Reader 封装
│   │   ├── SyntaxHighlighter.java    # nanorc 高亮适配
│   │   ├── CompleterFactory.java     # 补全器工厂
│   │   └── TerminalPrinter.java      # 输出打印
│   ├── renderer/                     # 结果渲染
│   │   ├── PagingRenderer.java       # 分页渲染器
│   │   └── ResultSetFormatter.java   # 格式化逻辑
│   └── util/
│       └── NativeUtils.java          # 原生环境适配
└── src/main/resources/
    ├── META-INF/native-image/        # GraalVM 配置
    └── sql.nanorc                    # 高亮规则
```

### 交互流程

```
Start → Picocli Parse Args 
      → If batch mode: Execute Script & Exit
      → If interactive mode:
           Initialize JLine Terminal
           Load History & Config
           Connect to Server (Optional at start)
           Loop:
              Read Line (with highlight & complete)
              Handle Client Command (e.g., \c, \quit)
              OR Execute SQL via JDBC
              Render Result (Paging)
```

## 实现计划

### Phase 1: 基础骨架 & 原生构建 (预估: 2天)

**任务列表**:

1. [ ] 搭建 `intellisql-client` 模块 Maven 配置 (引入 Picocli, JLine)
2. [ ] 配置 `native-maven-plugin` 和 GraalVM 环境
3. [ ] 实现 `IsqlClient` 入口和基础参数解析
4. [ ] 验证 Hello World 级原生编译

### Phase 2: 终端交互增强 (预估: 3天)

**任务列表**:

1. [ ] 集成 JLine3 `LineReader`
2. [ ] 配置 `sql.nanorc` 实现语法高亮
3. [ ] 实现 `SignalHandler` 捕获 Ctrl+C
4. [ ] 实现历史记录持久化

### Phase 3: JDBC 集成与补全 (预估: 3天)

**任务列表**:

1. [ ] 集成 `intellisql-jdbc` 驱动
2. [ ] 实现连接管理命令 (`\connect`)
3. [ ] 实现 `MetaDataCompleter` (基于 DatabaseMetaData)
4. [ ] 异步加载元数据以优化补全性能

### Phase 4: 结果渲染 (预估: 3天)

**任务列表**:

1. [ ] 实现 `PagingRenderer` (流式读取 ResultSet)
2. [ ] 实现 CJK 字符宽度计算逻辑
3. [ ] 实现交互式分页控制 (n: next page, q: quit)
4. [ ] 验证大结果集 (100万行) 渲染内存占用

## 宪法检查（Client 模块）

| 原则 | 状态 | 合规说明 |
|------|------|----------|
| 用心 | ✅ | 追求极致的启动速度和交互体验 |
| 可读 | ✅ | 清晰的 Command/Console 分层 |
| 极致 | ✅ | 原生编译，无额外 JVM 开销 |

## 测试策略

- **原生测试**: 使用 `native-image-agent` 辅助生成配置，并在 CI 中运行 native-image 测试
- **交互测试**: 模拟 PTY 进行自动化交互测试 (使用 Expect 脚本或 Java PTY 库)

