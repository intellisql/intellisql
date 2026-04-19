# SQL 翻译实现说明

更新时间：2026-04-19

本文件描述翻译模块当前真实实现，按代码入口、服务分层、方言插件和测试覆盖展开。

## 1. 统一入口

### `com.intellisql.translator.SqlTranslator`

这是翻译模块的主入口类，内部持有两个服务：

- `OnlineTranslationService onlineService`
- `OfflineTranslationService offlineService`

关键方法：

- `translate(Translation translation)`
  - 读取 `translation.getMode()`
  - `ONLINE` 走 `onlineService.translate(...)`
  - 其他模式走 `offlineService.translate(...)`
  - 捕获 `TranslationException`
  - 成功时返回 `translation.withResult(targetSql, unsupportedFeatures)`
  - 失败时返回 `translation.withError(...)`
- `translateOffline(sourceSql, sourceDialect, targetDialect)`
  - 组装 `Translation.create(..., TranslationMode.OFFLINE)`
  - 再委托给 `translate(...)`
- `translateOnline(sourceSql, sourceDialect, targetDialect)`
  - 同理组装 `ONLINE`
- `validateSyntax(sql, dialect)`
  - 调用 `parse(sql, dialect)`，能 parse 即返回 `true`
- `parse(sql, dialect)`
  - 委托给 `SqlParserFactory.parse(sql, dialect)`
- `format(node, targetDialect)`
  - 委托给 `DialectConverterFactory.toSql(...)`

## 2. 离线翻译路径

### `com.intellisql.translator.OfflineTranslationService`

关键方法：

- `translate(sourceSql, sourceDialect, targetDialect, unsupportedFeatures)`
  - 校验 SQL 非空
  - `SqlParserFactory.parse(sourceSql, sourceDialect)`
  - `DialectConverterFactory.toSql(ast, targetDialect)`
  - 返回目标 SQL

当前实际调用链：

1. `SqlTranslator.translateOffline(...)`
2. `OfflineTranslationService.translate(...)`
3. `SqlParserFactory.parse(...)`
4. `DialectConverterFactory.toSql(...)`

这是当前最稳定、最真实的翻译主路径。

## 3. 在线翻译路径

### `com.intellisql.translator.OnlineTranslationService`

关键方法：

- `translate(sourceSql, sourceDialect, targetDialect, unsupportedFeatures)`
  - 校验 SQL 非空
  - `SqlParserFactory.parse(...)`
  - `analyzeAndValidate(...)`
  - `DialectConverterFactory.toSql(...)`
- `analyzeAndValidate(...)`
  - 调用 `analyzeFunctionMappings(...)`
  - 调用 `analyzePaginationSyntax(...)`

当前真实状态：

- `analyzeFunctionMappings(...)` 只有占位逻辑
- `analyzePaginationSyntax(...)` 只有占位逻辑
- 没有真实数据库元数据接入
- 没有 schema-aware validation

所以 `ONLINE` 模式当前是“接口先行”，不是“元数据增强翻译已完成”。

## 4. 方言解析与输出

### 源 SQL 解析

- `com.intellisql.parser.SqlParserFactory`
  - `createParserConfig(dialect)`：
    - 通过 `DatabaseDialectRegistry.getDialect(dialect)` 取方言定义
    - 读取 `Lex`
    - 读取 `SqlConformance`
  - `parse(sql, dialect)`：
    - 创建 `SqlParser`
    - 调用 `parser.parseQuery()`

### 目标 SQL 输出

- `com.intellisql.translator.dialect.DialectConverterFactory`
  - `toSql(sqlNode, targetDialect)`
    - 调用 `DatabaseDialectRegistry.getDialect(targetDialect)`
    - 读取目标方言的 Calcite `SqlDialect`
    - 调用 `sqlNode.toSqlString(dialect).getSql()`

## 5. 方言插件

当前方言实现都遵循 `DatabaseDialect` 接口。

### MySQL

- `MySQLDatabaseDialect`
  - `getType() -> "MYSQL"`
  - `getCalciteDialect() -> MysqlSqlDialect.DEFAULT`
  - `getLex() -> Lex.MYSQL`
  - `getConformance() -> SqlConformanceEnum.MYSQL_5`

### PostgreSQL

- `PostgreSQLDatabaseDialect`
  - `getType() -> "POSTGRESQL"`
  - aliases: `POSTGRES`, `PG`
  - `getCalciteDialect() -> PostgresqlSqlDialect.DEFAULT`

### Oracle

- `OracleDatabaseDialect`
  - `getType() -> "ORACLE"`
  - `getCalciteDialect() -> OracleSqlDialect.DEFAULT`
  - `getLex() -> Lex.ORACLE`

### SQL Server

- `SQLServerDatabaseDialect`
  - `getType() -> "SQLSERVER"`
  - aliases: `MSSQL`, `SQL_SERVER`
  - `getCalciteDialect() -> MssqlSqlDialect.DEFAULT`

### Hive

- `HiveDatabaseDialect`
  - `getType() -> "HIVE"`
  - `getCalciteDialect() -> HiveSqlDialect.DEFAULT`

### STANDARD

- `StandardDatabaseDialect`
  - `getType() -> "STANDARD"`
  - alias: `ANSI`
  - `getCalciteDialect() -> AnsiSqlDialect.DEFAULT`

## 6. 结果模型与错误模型

### `Translation`

承担翻译请求和结果模型：

- 源 SQL
- 源方言
- 目标方言
- 模式
- targetSql
- unsupportedFeatures
- error

### `TranslationError`

承担翻译失败信息。

### `TranslationException`

封装模块内部异常，当前主要用于：

- 语法错误
- 通用翻译失败

## 7. 测试覆盖

### `SqlTranslatorTest`

覆盖内容：

- MySQL -> PostgreSQL
- PostgreSQL -> MySQL
- JOIN
- 聚合
- INSERT / UPDATE / DELETE
- `validateSyntax`
- MySQL -> Oracle / SQL Server / Hive

### `CrossDialectTranslationTest`

覆盖内容：

- MySQL 与 PostgreSQL 双向基础转换
- MySQL -> Oracle / SQL Server / Hive
- 子查询、JOIN、UNION、CASE、GROUP BY 等复杂查询
- 非法 SQL 的错误路径

## 8. 当前代码级限制

- `ONLINE` 模式没有真实 metadata hub
- `unsupportedFeatures` 目前没有建立完整规则库
- 过程式 SQL、存储过程、触发器没有实现
- 翻译输出主要依赖 Calcite 方言渲染，不包含更多定制重写规则
- 语义风险还没有显式等级化结果
