# SQL 解析实现说明

更新时间：2026-04-19

本文件按 parser 工厂、宽松解析、自定义 AST 和测试覆盖来描述当前解析模块实现。

## 1. 方言感知 parser 工厂

### `com.intellisql.parser.SqlParserFactory`

这是解析模块的主入口。

关键方法：

- `createParser(sql, dialect)`
  - 调用 `createParserConfig(dialect)`
  - 创建 `SqlParser`
- `createParserConfig(dialect)`
  - 通过 `DatabaseDialectRegistry.getDialect(dialect)` 获取方言实现
  - 读取 `Lex`
  - 读取 `SqlConformance`
  - 使用 `SqlParser.configBuilder()` 构造 parser config
- `parse(sql, dialect)`
  - 构造方言 parser
  - 调用 `parser.parseQuery()`
- `parseExpression(sql, dialect)`
  - 调用 `parser.parseExpression()`
- `createBabelParser(sql)`
  - 使用 `BabelParserConfiguration.createConfig()`
  - 再设置 `IntelliSqlParserImpl.FACTORY`
- `parseWithBabel(sql)`
  - 创建宽松 parser
  - 调用 `parser.parseQuery()`

## 2. 宽松解析配置

### `com.intellisql.parser.BabelParserConfiguration`

关键方法：

- `createConfig()`
  - `Lex.JAVA`
  - `caseSensitive=false`
  - `identifierMaxLength=128`
  - `SqlConformanceEnum.LENIENT`
- `createConfig(factory)`
  - 与上面相同，但可指定 parser factory
- `createLenientConfig(lex)`
  - 基于指定 `Lex` 构建 lenient 配置

用途：

- 为混合方言和扩展语句提供更宽容的解析能力

## 3. 自定义 parser factory

### `com.intellisql.parser.impl.IntelliSqlParserImpl`

当前作用：

- 作为自定义 parser factory 挂接到 Babel parser 路线
- 让扩展语句 AST 能进入统一 parse 流程

## 4. 自定义 AST

### `SqlShowTables`

位置：

- `com.intellisql.parser.ast.SqlShowTables`

结构：

- 继承 `SqlCall`
- 定义 `OPERATOR`
- 持有：
  - `SqlIdentifier db`
  - `SqlNode likePattern`
  - `SqlNode where`

关键方法：

- `getOperator()`
- `getOperandList()`
- `unparse(...)`
- `getDb()`
- `getLikePattern()`
- `getWhere()`

支持语义：

- `SHOW TABLES`
- `SHOW TABLES FROM schema`
- `SHOW TABLES LIKE 'pattern'`
- 组合形式

### `SqlShowSchemas`

位置：

- `com.intellisql.parser.ast.SqlShowSchemas`

作用：

- 表示 `SHOW SCHEMAS`
- 也被用于 `SHOW DATABASES` 这类管理语句

### `SqlUseSchema`

位置：

- `com.intellisql.parser.ast.SqlUseSchema`

结构：

- 继承 `SqlCall`
- 持有 `SqlIdentifier schema`

关键方法：

- `getOperator()`
- `getOperandList()`
- `unparse(...)`
- `getSchema()`

支持语义：

- `USE schema_name`

## 5. 与方言 SPI 的关系

- parser 不自己硬编码 MySQL / PostgreSQL / Oracle / SQL Server / Hive 规则
- 它通过 `DatabaseDialectRegistry` 读取方言插件提供的：
  - `Lex`
  - `SqlConformance`

因此 parser 模块和翻译模块共享同一套方言注册机制。

## 6. 测试覆盖

### `SqlParserFactoryTest`

验证：

- parser 工厂能按方言创建 parser

### `BabelParserConfigurationTest`

验证：

- 宽松配置能正确构建

### `ExtensionSqlParserTest`

验证：

- `SHOW TABLES`
- `SHOW TABLES FROM ...`
- `SHOW TABLES LIKE ...`
- `SHOW SCHEMAS`
- `SHOW DATABASES`
- `USE`
- 标准 `SELECT` 与扩展语句混合使用时仍可解析

## 7. 当前代码级限制

- 当前以 `parseQuery()` 为主，没有脚本级多语句解析器
- 扩展 AST 目前只解决“能 parse”，不等于执行层已经完整支持
- 管理类语句覆盖仍有限，没有 `DESCRIBE`、`SHOW COLUMNS`、`EXPLAIN` 等
