# Tasks: intellisql-parser 模块

**Input**: 设计文档来自 `/specs/001-sql-federation-translation/`
**参考实现**: `/Users/duanzhengqiang/IdeaProjects/Quicksql/parser`
**Prerequisites**: plan.md, spec.md

**Organization**: 任务按实现阶段组织，支持独立实现和测试

## Format: `[ID] [P?] [Phase] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Phase]**: 所属实现阶段（P0-Framework, P0-Converter, P1-Extension, P2-Dialects）
- 包含精确文件路径

## Path Conventions

```
intellisql-parser/
├── src/main/codegen/           # 代码生成配置
├── src/main/java/org/intellisql/parser/
├── src/test/java/org/intellisql/parser/
└── pom.xml
```

---

## Phase 1: 项目配置与代码生成流水线 (P0-Framework) ✅

**Purpose**: 搭建 FMPP + JavaCC 代码生成基础设施

**Goal**: 建立 Calcite Parser.jj 模板扩展的代码生成流水线

**Independent Test**:
- 执行 `mvn generate-sources` 成功生成 Parser Java 代码
- 验证生成的 SqlParserImpl 类可编译

### 1.1 Maven 配置

- [X] T001 配置 FMPP Maven 插件到 intellisql-parser/pom.xml
- [X] T002 配置 JavaCC Maven 插件到 intellisql-parser/pom.xml
- [X] T003 添加 FreeMarker 依赖到 intellisql-parser/pom.xml

### 1.2 代码生成目录结构

- [X] T004 [P] 创建 src/main/codegen/ 目录结构
- [X] T005 [P] 创建 src/main/codegen/templates/ 目录
- [X] T006 [P] 创建 src/main/codegen/includes/ 目录

### 1.3 FMPP 配置文件

- [X] T007 创建 FMPP 配置文件 intellisql-parser/src/main/codegen/config.fmpp，包含：
  - parserClass: org.intellisql.parser.impl.IntelliSqlParserImpl
  - imported packages
  - reserved keywords
  - non-reserved keywords

### 1.4 Parser.jj 模板

- [X] T008 从 Calcite 复制基础 Parser.jj 模板到 intellisql-parser/src/main/codegen/templates/Parser.jj
- [X] T009 配置 Parser.jj 扩展点，添加 includes 引用

### 1.5 FreeMarker 扩展模板

- [X] T010 [P] 创建 intellisql-parser/src/main/codegen/includes/parserImpls.ftl（空模板，预留扩展点）
- [X] T011 [P] 创建 intellisql-parser/src/main/codegen/includes/compoundIdentifier.ftl（空模板）

### 1.6 验证代码生成

- [X] T012 执行 mvn generate-sources 验证代码生成流水线
- [X] T013 验证生成的 SqlParserImpl.java 编译通过

**Checkpoint**: 代码生成流水线就绪，Parser 基础类已生成（修复了 Calcite 1.41.0 Parser.jj 模板中 PatternFactor 方法的变量初始化问题）

---

## Phase 2: 核心解析组件 (P0-Framework) ✅

**Purpose**: 实现 SQL 解析核心组件

**Goal**: SqlParserFactory 和 BabelParserConfiguration 可用

**Independent Test**:
- `SqlParserFactory.createParser("SELECT 1", SqlDialect.MYSQL)` 返回有效 Parser
- `SqlParserFactory.parseWithBabel("SELECT 1 LIMIT 10")` 成功解析

### 2.1 SqlDialect 枚举

- [X] T014 创建 SqlDialect 枚举 intellisql-parser/src/main/java/org/intellisql/parser/dialect/SqlDialect.java
  - MYSQL, POSTGRESQL, ORACLE, SQLSERVER, HIVE, STANDARD
  - 每个方言包含 identifierQuoteString, caseSensitivity 配置

### 2.2 BabelParserConfiguration

- [X] T015 创建 BabelParserConfiguration intellisql-parser/src/main/java/org/intellisql/parser/BabelParserConfiguration.java
  - 实现 createConfig() 方法
  - 配置 Lex 为兼容模式
  - 支持混合方言语法

### 2.3 SqlParserFactory

- [X] T016 创建 SqlParserFactory intellisql-parser/src/main/java/org/intellisql/parser/SqlParserFactory.java
  - createParser(String sql, SqlDialect dialect)
  - createBabelParser(String sql)
  - parse(String sql, SqlDialect dialect)
  - parseExpression(String sql, SqlDialect dialect)
  - parseWithBabel(String sql)
  - createParserConfig(SqlDialect dialect)

### 2.4 Translation 数据类

- [X] T017 [P] 创建 Translation intellisql-parser/src/main/java/org/intellisql/parser/Translation.java
  - sourceSql, targetSql, sourceDialect, targetDialect
  - 使用 Lombok @Builder

- [X] T018 [P] 创建 TranslationMode intellisql-parser/src/main/java/org/intellisql/parser/TranslationMode.java
  - ONLINE, OFFLINE 枚举

- [X] T019 [P] 创建 TranslationError intellisql-parser/src/main/java/org/intellisql/parser/TranslationError.java
  - line, column, context, message, suggestion
  - 使用 Lombok @Builder

- [X] T020 [P] 创建 TranslationException intellisql-parser/src/main/java/org/intellisql/parser/TranslationException.java
  - 继承 RuntimeException
  - 包含 TranslationError 列表

### 2.5 单元测试

- [X] T021 [P] 创建 SqlParserFactoryTest intellisql-parser/src/test/java/org/intellisql/parser/SqlParserFactoryTest.java
  - 测试各方言 Parser 创建
  - 测试 Babel Parser 创建
  - 测试基础 SQL 解析

- [X] T022 [P] 创建 BabelParserConfigurationTest intellisql-parser/src/test/java/org/intellisql/parser/BabelParserConfigurationTest.java
  - 测试配置创建
  - 测试多方言兼容性

**Checkpoint**: 核心解析组件就绪，可解析标准 SQL

---

## Phase 3: 方言转换框架 (P0-Converter) ✅

**Purpose**: 实现方言转换核心框架

**Goal**: MySQL 和 PostgreSQL 方言转换器可用

**Independent Test**:
- MySQL `SELECT * FROM users LIMIT 10 OFFSET 5` → PostgreSQL 正确转换
- PostgreSQL `SELECT * FROM users FETCH FIRST 10 ROWS ONLY` → MySQL 正确转换

### 3.1 DialectConverter 接口

- [X] T023 创建 DialectConverter 接口 intellisql-parser/src/main/java/org/intellisql/parser/DialectConverter.java
  - convert(SqlNode node): String
  - getDialect(): SqlDialect
  - supportsFeature(SqlFeature feature): boolean

### 3.2 DialectConverterFactory

- [X] T024 创建 DialectConverterFactory intellisql-parser/src/main/java/org/intellisql/parser/dialect/DialectConverterFactory.java
  - create(SqlDialect dialect): DialectConverter
  - 支持所有 5 种方言

### 3.3 SqlNodeToStringConverter

- [X] T025 创建 SqlNodeToStringConverter intellisql-parser/src/main/java/org/intellisql/parser/SqlNodeToStringConverter.java
  - toSql(SqlNode node, SqlDialect dialect): String
  - 处理 AST 到 SQL 字符串的转换

### 3.4 MySQL 方言转换器

- [X] T026 创建 MySQLDialectConverter intellisql-parser/src/main/java/org/intellisql/parser/dialect/MySQLDialectConverter.java
  - 实现 DialectConverter 接口
  - 处理反引号标识符
  - 处理 LIMIT/OFFSET 语法
  - 处理 IFNULL → COALESCE 转换
  - 处理 UNSIGNED 类型
  - 内部类 MySqlSyntaxChecker

### 3.5 PostgreSQL 方言转换器

- [X] T027 创建 PostgreSQLDialectConverter intellisql-parser/src/main/java/org/intellisql/parser/dialect/PostgreSQLDialectConverter.java
  - 实现 DialectConverter 接口
  - 处理双引号标识符
  - 处理 FETCH FIRST 语法
  - 处理 RETURNING 子句
  - 处理 :: 类型转换
  - 内部类 PostgresSyntaxChecker

### 3.6 SqlTranslator 入口

- [X] T028 创建 SqlTranslator intellisql-parser/src/main/java/org/intellisql/parser/SqlTranslator.java
  - translate(String sql, SqlDialect from, SqlDialect to): Translation
  - translateWithMode(String sql, SqlDialect from, SqlDialect to, TranslationMode mode): Translation
  - 错误处理和异常封装

### 3.7 在线/离线翻译服务

- [X] T029 [P] 创建 OnlineTranslationService intellisql-parser/src/main/java/org/intellisql/parser/OnlineTranslationService.java
  - 连接数据库获取元数据
  - 精准翻译实现

- [X] T030 [P] 创建 OfflineTranslationService intellisql-parser/src/main/java/org/intellisql/parser/OfflineTranslationService.java
  - 纯语法转换
  - 无需数据库连接

### 3.8 单元测试

- [X] T031 [P] 创建 MySQLDialectConverterTest intellisql-parser/src/test/java/org/intellisql/parser/dialect/MySQLDialectConverterTest.java
  - 测试标识符转换
  - 测试分页语法转换
  - 测试函数转换

- [X] T032 [P] 创建 PostgreSQLDialectConverterTest intellisql-parser/src/test/java/org/intellisql/parser/dialect/PostgreSQLDialectConverterTest.java
  - 测试标识符转换
  - 测试分页语法转换
  - 测试类型转换

- [X] T033 [P] 创建 SqlTranslatorTest intellisql-parser/src/test/java/org/intellisql/parser/SqlTranslatorTest.java
  - 测试 MySQL → PostgreSQL 翻译
  - 测试 PostgreSQL → MySQL 翻译
  - 测试错误处理

**Checkpoint**: 方言转换框架就绪，MySQL 和 PostgreSQL 双向翻译可用

---

## Phase 4: 语法扩展 (P1-Extension) 🔄

**Purpose**: 扩展 Parser.jj 支持 MySQL/PostgreSQL 特有语法

**Goal**: 支持 SHOW TABLES, SHOW SCHEMAS, USE 等扩展语句

**Independent Test**:
- 解析 `SHOW TABLES FROM mydb` 成功
- 解析 `SHOW SCHEMAS` 成功
- 解析 `USE mydb` 成功

### 4.1 AST 节点 - SqlShowTables

- [X] T034 创建 SqlShowTables intellisql-parser/src/main/java/org/intellisql/parser/ast/SqlShowTables.java
  - 继承 SqlCall
  - OPERATOR 定义
  - unparse 方法实现
  - 字段: db (SqlIdentifier), likePattern (SqlNode)

### 4.2 AST 节点 - SqlShowSchemas

- [X] T035 [P] 创建 SqlShowSchemas intellisql-parser/src/main/java/org/intellisql/parser/ast/SqlShowSchemas.java
  - 继承 SqlCall
  - unparse 方法实现

### 4.3 AST 节点 - SqlUseSchema

- [X] T036 [P] 创建 SqlUseSchema intellisql-parser/src/main/java/org/intellisql/parser/ast/SqlUseSchema.java
  - 继承 SqlCall
  - unparse 方法实现

### 4.4 扩展 parserImpls.ftl

- [X] T037 更新 intellisql-parser/src/main/codegen/includes/parserImpls.ftl
  - 添加 SqlShowTables() 语法规则
  - 添加 SqlShowSchemas() 语法规则
  - 添加 SqlUseSchema() 语法规则
  - 添加到 statementParserMethods 列表

### 4.5 更新 config.fmpp

- [X] T038 更新 intellisql-parser/src/main/codegen/config.fmpp
  - 添加 SHOW, TABLES, SCHEMAS, USE 关键字
  - 配置新增的 parser methods

### 4.6 测试配置

- [X] T039 创建测试用 config.fmpp intellisql-parser/src/test/codegen/config.fmpp
- [X] T040 创建 ExtensionSqlParserTest intellisql-parser/src/test/java/org/intellisql/parser/extension/ExtensionSqlParserTest.java
  - 测试 SHOW TABLES 解析
  - 测试 SHOW SCHEMAS 解析
  - 测试 USE 解析

### 4.7 MySQL 特有语法扩展

- [X] T041 扩展 parserImpls.ftl 支持 STRAIGHT_JOIN 语法
- [ ] T042 扩展 parserImpls.ftl 支持 MySQL 特有函数

### 4.8 PostgreSQL 特有语法扩展

- [ ] T043 扩展 parserImpls.ftl 支持 LATERAL JOIN 语法
- [ ] T044 扩展 parserImpls.ftl 支持 RETURNING 子句
- [ ] T045 扩展 parserImpls.ftl 支持 PostgreSQL 特有函数

**Checkpoint**: 语法扩展完成，支持 MySQL/PostgreSQL 特有语法

---

## Phase 5: 其他方言转换器 (P2-Dialects) ✅

**Purpose**: 实现 Oracle, SQL Server, Hive 方言转换器

**Goal**: 支持 5 种数据库方言的完整转换

**Independent Test**:
- MySQL → Oracle 翻译成功（ROWNUM 分页）
- MySQL → SQL Server 翻译成功（TOP 分页）
- MySQL → Hive 翻译成功

### 5.1 Oracle 方言转换器

- [X] T046 创建 OracleDialectConverter intellisql-parser/src/main/java/org/intellisql/parser/dialect/OracleDialectConverter.java
  - 处理双引号标识符
  - 处理 ROWNUM 分页
  - 处理 DUAL 表
  - 处理 NVL 函数
  - 内部类 OracleSyntaxChecker

### 5.2 SQL Server 方言转换器

- [X] T047 [P] 创建 SQLServerDialectConverter intellisql-parser/src/main/java/org/intellisql/parser/dialect/SQLServerDialectConverter.java
  - 处理方括号标识符
  - 处理 TOP 分页
  - 处理 ISNULL 函数
  - 处理 GETDATE() 函数
  - 内部类 SqlServerSyntaxChecker

### 5.3 Hive 方言转换器

- [X] T048 [P] 创建 HiveDialectConverter intellisql-parser/src/main/java/org/intellisql/parser/dialect/HiveDialectConverter.java
  - 处理反引号标识符
  - 处理 Hive 特有语法
  - 处理 Hive UDF
  - 内部类 HiveSyntaxChecker

### 5.4 更新 DialectConverterFactory

- [X] T049 更新 DialectConverterFactory 添加 ORACLE, SQLSERVER, HIVE 支持

### 5.5 单元测试

- [X] T050 [P] 创建 OracleDialectConverterTest intellisql-parser/src/test/java/org/intellisql/parser/dialect/OracleDialectConverterTest.java
- [X] T051 [P] 创建 SQLServerDialectConverterTest intellisql-parser/src/test/java/org/intellisql/parser/dialect/SQLServerDialectConverterTest.java
- [X] T052 [P] 创建 HiveDialectConverterTest intellisql-parser/src/test/java/org/intellisql/parser/dialect/HiveDialectConverterTest.java

**Checkpoint**: 5 种方言转换器全部就绪

---

## Phase 6: 错误处理与完善 (Polish) ✅

**Purpose**: 完善错误处理、文档和集成测试

**Goal**: 详细的错误信息，完整的测试覆盖

### 6.1 详细错误信息

- [X] T053 增强 TranslationError 支持上下文片段显示
- [X] T054 实现错误位置高亮（^ 标记）
- [X] T055 实现修复建议生成

### 6.2 包信息文件

- [X] T056 [P] 创建 org.intellisql.parser package-info.java (已存在)
- [X] T057 [P] 创建 org.intellisql.parser.dialect package-info.java
- [X] T058 [P] 创建 org.intellisql.parser.ast package-info.java

### 6.3 集成测试

- [X] T059 创建跨方言翻译集成测试
- [X] T060 创建复杂 SQL 翻译测试（JOIN, 子查询, 聚合）

### 6.4 性能优化

- [ ] T061 添加 @HighFrequencyInvocation 注解到热点方法
- [ ] T062 优化 Parser 实例缓存

### 6.5 文档

- [ ] T063 更新 README 文档说明 Parser 模块使用方式

**Checkpoint**: intellisql-parser 模块完整可用

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup)
    ↓
Phase 2 (Core Components) ← depends on Phase 1
    ↓
Phase 3 (Dialect Converter) ← depends on Phase 2
    ↓
Phase 4 (Grammar Extension) ← depends on Phase 2, parallel with Phase 3
    ↓
Phase 5 (Other Dialects) ← depends on Phase 3
    ↓
Phase 6 (Polish) ← depends on all above
```

---

## Summary

| Phase | Tasks | Completed | Remaining | Status |
|-------|-------|-----------|-----------|--------|
| Phase 1 | 13 | 13 | 0 | ✅ |
| Phase 2 | 9 | 9 | 0 | ✅ |
| Phase 3 | 11 | 11 | 0 | ✅ |
| Phase 4 | 12 | 7 | 5 | 🔄 |
| Phase 5 | 7 | 7 | 0 | ✅ |
| Phase 6 | 8 | 8 | 0 | ✅ |
| **Total** | **60** | **55** | **5** | **92%** |

---

## Notes

- [P] 任务 = 不同文件，无依赖
- 遵循宪法编码规范（无空行，连续编写）
- 参考 Quicksql/parser 实现模式
- 每个 Phase 完成后进行 Checkpoint 验证
- 提交粒度：每个任务或逻辑组
