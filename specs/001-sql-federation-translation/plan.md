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
**目标平台**: Linux 服务器（JVM）
**项目类型**: 多模块 Maven 项目
**性能目标**: 单表查询额外开销 <50ms；跨源 JOIN（10万行）<5s
**约束**: JDK 8 兼容；100 并发连接；100万行结果集不 OOM
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
- ✅ JDK 8 兼容性
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
│   └── config-schema.md
└── tasks.md             # 阶段 2 输出（/speckit.tasks 命令）
```

### 源码结构（仓库根目录）

```text
intellisql/
├── intellisql-parser/          # SQL 解析与翻译
│   └── src/
│       ├── main/java/
│       └── test/java/
├── intellisql-optimizer/       # SQL 优化器
│   └── src/
│       ├── main/java/
│       └── test/java/
├── intellisql-executor/        # SQL 执行引擎
│   └── src/
│       ├── main/java/
│       └── test/java/
├── intellisql-connector/       # 数据源连接器
│   └── src/
│       ├── main/java/
│       │   ├── api/            # Connector SPI
│       │   ├── mysql/          # MySQL 适配器
│       │   ├── postgresql/     # PostgreSQL 适配器
│       │   └── elasticsearch/  # Elasticsearch 适配器
│       └── test/java/
├── intellisql-kernel/          # 核心编排层
│   └── src/
│       ├── main/java/
│       └── test/java/
├── intellisql-jdbc/            # JDBC 驱动
│   └── src/
│       ├── main/java/
│       └── test/java/
├── intellisql-server/          # 服务端实现
│   └── src/
│       ├── main/java/
│       ├── main/resources/
│       └── test/java/
├── intellisql-client/          # isql CLI 工具
│   └── src/
│       ├── main/java/
│       └── test/java/
├── intellisql-distribution/    # 打包分发父模块
│   ├── intellisql-distribution-jdbc/
│   └── intellisql-distribution-server/
├── intellisql-test/            # 测试模块父模块
│   ├── intellisql-test-it/     # 集成测试
│   └── intellisql-test-e2e/    # 端到端测试
├── conf/                       # 配置文件
│   └── model.yaml
├── mvnw                        # Maven Wrapper
├── mvnw.cmd
├── pom.xml
└── .mvn/
    └── wrapper/
```

**结构决策**: 采用多模块 Maven 架构，分为四层：
1. **基础功能层**: parser、optimizer、executor、connector
2. **核心处理层**: kernel（编排各功能模块）
3. **协议适配层**: jdbc、server
4. **工具与支撑层**: client、distribution、test

## 依赖版本汇总

| 依赖 | 版本 | 用途 |
|------|------|------|
| Apache Calcite | 1.41.0 | SQL 解析与优化 |
| Apache Avatica | 1.27.0 | JDBC 协议 |
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
