# JDBC E2E 测试程序任务拆分

更新时间：2026-05-05

## P0：真实 IntelliSQL JDBC DQL 闭环

- [x] 任务 1：Server 显式模型配置路径
  - [x] 在 `ServerConfig` 增加 `configPath`
  - [x] 增加 `ServerConfig.fromPortAndConfigPath(int, Path)`
  - [x] 在 `IntelliSqlServer` 优先使用显式 `configPath`
  - [x] 保留默认 `conf/model.yaml` fallback 行为

- [x] 任务 2：E2E runner 配置模型
  - [x] 增加 `E2ERunnerConfig`
  - [x] 增加 `E2ERunnerConfigLoader`
  - [x] 读取 `e2e/runner.yaml`
  - [x] 为缺省字段提供稳定默认值

- [x] 任务 3：SQL case DSL
  - [x] 增加 `E2ETestCase`
  - [x] 增加 `AssertionSpec`
  - [x] 增加 `StatementSpec`
  - [x] 增加 `E2ECaseParser`
  - [x] 增加 `E2ECaseScanner`
  - [x] 支持 `@case`、`@source`、`@assert`、`@statement`

- [x] 任务 4：E2E 环境 fixture
  - [x] 增加 `PostgreSQLContainerFixture`
  - [x] 增加 `SqlScriptExecutor`
  - [x] 增加 `ModelConfigRenderer`
  - [x] 增加 `PortAllocator`
  - [x] 增加 `IntelliSqlServerFixture`
  - [x] 增加 `E2EEnvironment`

- [x] 任务 5：结果快照与比较器
  - [x] 增加 `ColumnSnapshot`
  - [x] 增加 `RowSnapshot`
  - [x] 增加 `ResultSetSnapshot`
  - [x] 增加 `ValueNormalizer`
  - [x] 增加 `ResultSetSnapshotReader`
  - [x] 增加 `OrderMode`
  - [x] 增加 `SqlOrderAnalyzer`
  - [x] 增加 `ComparisonResult`
  - [x] 增加 `ResultSetComparator`

- [x] 任务 6：JDBC case 执行器
  - [x] 增加 `ExecutionResult`
  - [x] 增加 `JdbcCaseExecutor`
  - [x] 增加 `BaselineExecutor`
  - [x] 支持 mirror assertion
  - [x] 支持 CSV file assertion

- [x] 任务 7：测试资源
  - [x] 增加 `e2e/runner.yaml`
  - [x] 增加 `e2e/models/basic/model.yaml`
  - [x] 增加 `e2e/init/basic/postgresql.sql`
  - [x] 增加 `e2e/cases/smoke/show-tables.sql`
  - [x] 增加 `e2e/cases/dql/customer-select.sql`
  - [x] 增加 `e2e/cases/dql/customer-filter.sql`
  - [x] 增加 `e2e/cases/dql/customer-order-by.sql`
  - [x] 增加 `e2e/expected/smoke/show-tables.csv`

- [x] 任务 8：JUnit 覆盖
  - [x] 增加 `JdbcAssertionFrameworkTest`
  - [x] 增加 `JdbcSmokeE2ETest`
  - [x] 增加 `JdbcDqlE2ETest`
  - [x] 测试方法以 `assert` 开头

- [x] 任务 9：P0 验证
  - [x] 运行 `./mvnw -pl intellisql-server -am test`
  - [x] 运行 `./mvnw -pl intellisql-tests/intellisql-test-e2e -Dtest=JdbcAssertionFrameworkTest test`
  - [x] 运行 `./mvnw -pl intellisql-tests/intellisql-test-e2e -Dtest=JdbcSmokeE2ETest,JdbcDqlE2ETest test`
  - [x] 修复失败并重试

## P1：PreparedStatement、fetch、DML 状态断言

- [x] 任务 10：PreparedStatement 参数传递
  - [x] 在客户端保存参数值和 JDBC 类型
  - [x] 转换为 Avatica `TypedValue`
  - [x] 执行时传递参数
  - [x] 增加 prepared select E2E

- [x] 任务 11：服务端 fetch
  - [x] 服务端 statement 保存完整查询结果或 cursor
  - [x] first frame 按 fetch size 截断
  - [x] `fetch(offset, fetchMaxRowCount)` 返回后续 frame
  - [x] 客户端 `ResultSet.next()` 消耗完 frame 后拉取下一批

- [x] 任务 12：DML 状态断言
  - [x] DSL 支持 `state`
  - [x] 执行 DML 后运行 `@expected-sql`
  - [x] 和 baseline 状态查询结果对比

## P2：batch、并发、跨源

- [x] 任务 13：batch API
  - [x] `PreparedStatement.addBatch()`
  - [x] `PreparedStatement.executeBatch()`
  - [x] `Statement.addBatch(String)`
  - [x] `Statement.executeBatch()`

- [x] 任务 14：并发 E2E
  - [x] 并发连接测试
  - [x] 并发只读查询测试
  - [x] 并发读写隔离测试

- [x] 任务 15：跨源场景
  - [x] 增加 MySQL fixture
  - [x] 增加 federation model
  - [x] 增加不同表名的跨源 case

## P3：JDBC 兼容性扩展

- [x] 任务 16：metadata
  - [x] `DatabaseMetaData.getTables`
  - [x] `DatabaseMetaData.getColumns`
  - [x] `DatabaseMetaData.getSchemas`
  - [x] `DatabaseMetaData.getCatalogs`

- [x] 任务 17：unsupported API contract
  - [x] CallableStatement 断言
  - [x] Savepoint 断言
  - [x] LOB 断言
  - [x] scrollable result set 断言
  - [x] updatable result set 断言

## P4：MySQL 查询 case 覆盖

- [x] 任务 18：MySQL 单源模型与数据
  - [x] 增加 `e2e/models/mysql/model.yaml`
  - [x] 增加 `e2e/init/mysql/mysql.sql`
  - [x] 增加 `mysql_customers` join 维表
  - [x] 增加 `created_on`、`note` 查询边界字段
  - [x] 在 `E2EEnvironment` 增加 MySQL baseline 连接
  - [x] 在 `JdbcE2ECaseRunner` 支持 `@assert mirror target=mysql`

- [x] 任务 19：MySQL 查询 case
  - [x] 增加 `mysql-orders-select.sql`
  - [x] 增加 `mysql-orders-filter.sql`
  - [x] 增加 `mysql-orders-order-by.sql`
  - [x] 增加 `mysql-orders-limit.sql`
  - [x] 增加 `mysql-orders-aggregate.sql`
  - [x] 增加 `SELECT *`、`DISTINCT`、`COUNT(DISTINCT)` case
  - [x] 增加 `BETWEEN`、`IN`、`LIKE`、`IS NULL`、`IS NOT NULL`、布尔组合 case
  - [x] 增加 `DATE` 谓词、`LIMIT OFFSET`、多列排序 case
  - [x] 增加表达式、函数、`CASE WHEN`、多聚合函数、`HAVING` case
  - [x] 增加 `INNER JOIN`、`LEFT JOIN`、`IN` 子查询、`EXISTS`、`UNION` case
  - [x] 增加 MySQL 反引号标识符 case
  - [x] 增加比较运算组合、否定谓词、`NOT BETWEEN` case
  - [x] 增加 `ORDER BY` alias、ordinal、多列 `GROUP BY`、`HAVING COUNT` case
  - [x] 增加 `COALESCE`、`CAST`、字符串函数、`CONCAT`、日期 `EXTRACT`、`NULLIF` case
  - [x] 增加派生表、CTE、标量子查询、相关 `EXISTS`、`NOT EXISTS` case
  - [x] 增加 `JOIN ON`、`RIGHT JOIN`、`CROSS JOIN`、`NATURAL JOIN` case
  - [x] 增加 `UNION ALL`、`ALL`/`ANY` 量词子查询、`NOT IN` 子查询 case
  - [x] 增加窗口函数 `ROW_NUMBER`、条件聚合、simple `CASE` case
  - [x] 增加 `WITH RECURSIVE` 递归 CTE case
  - [x] 增加 `DATE_ADD`、`DATE_SUB`、`DATEDIFF`、`TIMESTAMPDIFF` 日期函数 case
  - [x] 增加 `LEFT`、`RIGHT`、`REPLACE`、`LPAD`、`LOCATE` 字符串函数 case
  - [x] 增加 `ABS`、`MOD`、`ROUND` 数值函数 case
  - [x] 增加 `&`、`|`、`^` 位运算符 case
  - [x] 增加 `REGEXP_LIKE`、`GROUP_CONCAT`、`<=>` null-safe equals case
  - [x] 增加窗口聚合 frame、`LAG`、`LEAD` case
  - [x] 增加 `INTERSECT`、`EXCEPT`、`HAVING` alias case

- [x] 任务 20：MySQL 查询 JUnit 覆盖
  - [x] 增加 `JdbcMySQLQueryE2ETest`
  - [x] 覆盖 72 个 MySQL statement 查询 case
  - [x] 覆盖 2 个 MySQL PreparedStatement 查询 case
  - [x] 使用 MySQL mirror baseline 断言结果

- [x] 任务 21：MySQL 查询能力修复
  - [x] Calcite `RelOptCluster` 设置默认 metadata provider
  - [x] Calcite 表行类型使用真实数据类型
  - [x] Calcite 表行类型保留字段 nullability
  - [x] Avatica 查询列元数据使用 `QueryResult` 类型
  - [x] Avatica DATE 值使用稳定序列化
  - [x] E2E 快照比较支持 DATE 数值归一化
  - [x] Avatica 查询行按列类型转换数值对象
  - [x] Calcite validator 接入 MySQL 函数 operator table
  - [x] 单 MySQL 数据源查询支持原 SQL 直推执行

## 本轮完成标准

- [x] P0 所有任务完成。
- [x] P0 E2E 测试能通过。
- [x] P1 所有任务完成。
- [x] P2 所有任务完成。
- [x] P3 所有任务完成。
- [x] P4 所有任务完成。
- [x] MySQL 查询 E2E 测试通过：`./mvnw -pl intellisql-tests/intellisql-test-e2e -am -Dtest=JdbcMySQLQueryE2ETest -Dsurefire.failIfNoSpecifiedTests=false test`。
- [x] 完整 E2E 测试通过：`./mvnw -pl intellisql-tests/intellisql-test-e2e -am -Dsurefire.failIfNoSpecifiedTests=false test`。
- [x] Checkstyle 通过：`./mvnw -pl intellisql-tests/intellisql-test-e2e -am checkstyle:check`。
- [x] Diff 空白检查通过：`git diff --check`。
