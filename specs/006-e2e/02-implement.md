# JDBC E2E 测试程序实现方案

更新时间：2026-05-04

本文基于 `01-research.md` 和当前源码，描述 JDBC E2E 测试程序的详细实现方案。方案优先完成 P0 的 Statement DQL 闭环，再按阶段补齐 PreparedStatement、fetch、DML、batch、并发和高级 JDBC 兼容性。

## 1. 实现目标

P0 交付一个能真实穿透 IntelliSQL JDBC Driver、Avatica Protobuf HTTP Server、Kernel、Connector 和底层数据库的自动化测试框架。

P0 成功标准：

1. 测试程序能启动 PostgreSQL Testcontainer。
2. 测试程序能生成当前 `ConfigLoader` 可识别的临时 `model.yaml`。
3. `IntelliSqlServer` 能通过显式 `configPath` 加载临时模型。
4. 测试程序能通过 `jdbc:intellisql://localhost:${serverPort}/intellisql` 建立连接。
5. 测试程序能执行 `SHOW TABLES` 和表查询。
6. 查询结果能和原生 PostgreSQL JDBC 基准结果进行 order-aware 对比。
7. DSL、结果快照和结果比较器有独立单元测试。

P0 不覆盖：

- `PreparedStatement` 参数矩阵。
- `executeBatch`。
- 真实 update count。
- 大结果集跨 frame fetch。
- 可滚动、可更新、多结果集。

## 2. 模块边界

主要改动集中在两个模块：

| 模块 | 改动类型 | 说明 |
| --- | --- | --- |
| `intellisql-server` | 生产代码小改动 | 支持显式模型配置路径 |
| `intellisql-tests/intellisql-test-e2e` | 测试框架和测试资源 | 新增 JDBC E2E runner、fixture、用例 DSL 和断言 |

P0 不改动 `intellisql-jdbc`。当前 JDBC 驱动的 Statement 查询路径已经能作为 P0 主路径使用。

## 3. 生产代码改动

### 3.1 `ServerConfig` 增加配置路径

文件：

```text
intellisql-server/src/main/java/com/intellisql/server/ServerConfig.java
```

新增字段：

```java
private Path configPath;
```

实现要点：

- 引入 `java.nio.file.Path`。
- 字段保持可空，`null` 表示沿用当前默认加载行为。
- `defaultConfig()` 和 `fromPort(int port)` 保持兼容。
- 新增 `fromPortAndConfigPath(int port, Path configPath)` 便于测试和命令行复用。

推荐结构：

```java
@Getter
@Builder
public class ServerConfig {
    private static final int DEFAULT_PORT = 8765;
    private static final int DEFAULT_MAX_CONNECTIONS = 100;
    private static final int DEFAULT_IDLE_TIMEOUT_MS = 300000;
    @Builder.Default
    private int port = DEFAULT_PORT;
    @Builder.Default
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    @Builder.Default
    private long idleTimeoutMs = DEFAULT_IDLE_TIMEOUT_MS;
    @Builder.Default
    private String host = "0.0.0.0";
    private Path configPath;
}
```

测试要求：

- `ServerConfig.defaultConfig().getConfigPath()` 为 `null`。
- `ServerConfig.builder().configPath(path).build().getConfigPath()` 返回传入路径。

### 3.2 `IntelliSqlServer` 使用显式配置路径

文件：

```text
intellisql-server/src/main/java/com/intellisql/server/IntelliSqlServer.java
```

当前问题：

- `initializeKernel()` 固定读取 `conf/model.yaml`。
- classpath resource 优先于工作目录文件。
- E2E 需要让每个测试动态生成模型配置，当前无法注入。

改动策略：

1. `initializeKernel()` 先判断 `config.getConfigPath()`。
2. 显式路径存在时直接调用 `IntelliSqlKernel.create(config.getConfigPath())`。
3. 显式路径不存在时沿用当前 classpath resource 和工作目录 fallback。
4. 抽取小方法，避免 `initializeKernel()` 内部混杂多个抽象层级。

推荐方法结构：

```java
private void initializeKernel() {
    try {
        if (config.getConfigPath() != null) {
            initializeKernelFromPath(config.getConfigPath());
            return;
        }
        initializeKernelFromDefaultConfig();
    } catch (final Exception ex) {
        log.warn("Failed to initialize kernel: {}. Server will start with empty metadata.", ex.getMessage());
    }
}

private void initializeKernelFromPath(final Path configPath) throws IOException {
    log.info("Loading configuration from {}", configPath);
    kernel = IntelliSqlKernel.create(configPath);
    initializeKernelMetadata();
}

private void initializeKernelMetadata() {
    kernel.initialize();
    MetadataManager metadataManager = kernel.getMetadataManager();
    meta.setMetadataManager(metadataManager);
    meta.setKernel(kernel);
    log.info("Kernel initialized with {} tables", metadataManager.getAllTables().size());
}
```

保留默认配置加载：

- classpath `conf/model.yaml`。
- 工作目录 `conf/model.yaml`。
- 复制 classpath stream 到临时文件的逻辑可保留，因为 `IntelliSqlKernel.create` 接收 `Path`。

测试要求：

- 使用临时 `model.yaml` 启动 Server 时，`server.getMeta().getKernel()` 非空。
- 临时模型中的表能通过 `SHOW TABLES` 返回。
- 未传 `configPath` 时仍能按默认配置启动。

### 3.3 命令行入口可选支持配置路径

当前 `IntelliSqlServer.main(args)` 只解析第一个参数为端口。P0 测试使用 `ServerConfig.builder()` 即可，不强制修改 main。

推荐 P1 增强：

```text
java -jar intellisql-server.jar 8765 /path/to/model.yaml
```

解析规则：

- `args[0]`：端口。
- `args[1]`：模型配置路径。

## 4. 测试框架目录

新增测试框架包：

```text
intellisql-tests/intellisql-test-e2e/src/test/java/com/intellisql/test/e2e/framework/
```

新增测试入口包：

```text
intellisql-tests/intellisql-test-e2e/src/test/java/com/intellisql/test/e2e/jdbc/
```

新增资源目录：

```text
intellisql-tests/intellisql-test-e2e/src/test/resources/e2e/
├── runner.yaml
├── models/
│   └── basic/
│       └── model.yaml
├── init/
│   └── basic/
│       └── postgresql.sql
├── cases/
│   ├── smoke/
│   │   └── show-tables.sql
│   └── dql/
│       ├── customer-select.sql
│       ├── customer-filter.sql
│       └── customer-order-by.sql
└── expected/
    └── smoke/
        └── show-tables.csv
```

P0 的 `basic` 模型只配置 PostgreSQL 一个数据源，避免 `MetadataManager` 以表名注册 root schema 时出现跨数据源同名表覆盖。

## 5. 测试资源设计

### 5.1 `runner.yaml`

文件：

```text
intellisql-tests/intellisql-test-e2e/src/test/resources/e2e/runner.yaml
```

内容：

```yaml
execution:
  mode: docker
  serverPort: 0
  jdbcDatabase: intellisql
  caseRoot: e2e/cases
  defaultModel: basic
  defaultFetchSize: 1000
  timeoutSeconds: 60
containers:
  postgresql:
    enabled: true
    image: postgres:15-alpine
    database: testdb
    username: testuser
    password: testpass
assertion:
  orderMode: auto
  nullToken: "<NULL>"
```

解析方式：

- 使用 SnakeYAML 读取为 `Map<String, Object>`。
- 不引入新的配置框架。
- `E2ERunnerConfigLoader` 负责从 Map 映射为强类型对象。

### 5.2 `model.yaml`

文件：

```text
intellisql-tests/intellisql-test-e2e/src/test/resources/e2e/models/basic/model.yaml
```

内容：

```yaml
dataSources:
  postgresql_source:
    type: postgresql
    url: jdbc:postgresql://${POSTGRESQL_HOST}:${POSTGRESQL_PORT}/${POSTGRESQL_DATABASE}
    username: ${POSTGRESQL_USERNAME}
    password: ${POSTGRESQL_PASSWORD}
    pool:
      maxPoolSize: 5
      minIdle: 1
      connectionTimeout: 30000
      idleTimeout: 600000
      maxLifetime: 1800000
    healthCheck:
      enabled: true
      intervalSeconds: 30
      timeoutSeconds: 5
      failureThreshold: 3
props:
  maxIntermediateRows: 100000
  queryTimeoutSeconds: 300
  defaultFetchSize: 1000
  enableQueryLogging: true
  logLevel: INFO
```

渲染规则：

- `ModelConfigRenderer` 从 classpath 读取模板。
- 使用容器 host/port 和凭据替换 `${...}`。
- 输出到 JUnit 临时目录。
- Server 使用渲染后的临时文件路径启动。

### 5.3 初始化 SQL

文件：

```text
intellisql-tests/intellisql-test-e2e/src/test/resources/e2e/init/basic/postgresql.sql
```

内容：

```sql
DROP TABLE IF EXISTS customers;
CREATE TABLE customers (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL
);
INSERT INTO customers (id, name, status) VALUES (1, 'Alice', 'ACTIVE');
INSERT INTO customers (id, name, status) VALUES (2, 'Bob', 'INACTIVE');
INSERT INTO customers (id, name, status) VALUES (3, 'Carol', 'ACTIVE');
```

执行方式：

- `SqlScriptExecutor` 使用原生 PostgreSQL JDBC 连接执行。
- 按分号切分语句即可满足 P0 初始化。
- P1 如需支持字符串内分号，再引入更严格 SQL splitter。

### 5.4 SQL case

`show-tables.sql`：

```sql
-- @case id=show-tables model=basic
-- @source intellisql
-- @assert file expected=expected/smoke/show-tables.csv order=any
SHOW TABLES;
```

`customer-select.sql`：

```sql
-- @case id=customer-select model=basic
-- @source intellisql
-- @assert mirror target=postgresql order=any
SELECT id, name, status FROM customers;
```

`customer-filter.sql`：

```sql
-- @case id=customer-filter model=basic
-- @source intellisql
-- @assert mirror target=postgresql order=any
SELECT id, name FROM customers WHERE status = 'ACTIVE';
```

`customer-order-by.sql`：

```sql
-- @case id=customer-order-by model=basic
-- @source intellisql
-- @assert mirror target=postgresql order=auto
SELECT id, name FROM customers ORDER BY id;
```

`expected/smoke/show-tables.csv`：

```csv
TABLE_NAME
customers
```

## 6. 测试框架类设计

### 6.1 `E2ERunnerConfig`

包：

```text
com.intellisql.test.e2e.framework.config
```

职责：

- 表示 `runner.yaml`。
- 提供执行模式、端口、默认模型、fetch size、容器配置和断言配置。

字段：

```java
private ExecutionConfig execution;
private Map<String, ContainerConfig> containers;
private AssertionConfig assertion;
```

嵌套模型：

- `ExecutionConfig`
  - `mode`
  - `serverPort`
  - `jdbcDatabase`
  - `caseRoot`
  - `defaultModel`
  - `defaultFetchSize`
  - `timeoutSeconds`
- `ContainerConfig`
  - `enabled`
  - `image`
  - `database`
  - `username`
  - `password`
- `AssertionConfig`
  - `orderMode`
  - `nullToken`

实现要求：

- 使用 Lombok `@Getter` 和 `@Builder`。
- 提供默认值，缺省配置也能运行。
- 不把 `Map<String, Object>` 暴露给调用方。

### 6.2 `E2ERunnerConfigLoader`

职责：

- 从 classpath 读取 `e2e/runner.yaml`。
- 使用 SnakeYAML 解析。
- 生成 `E2ERunnerConfig`。

关键方法：

```java
public E2ERunnerConfig loadDefault();
public E2ERunnerConfig load(String resourcePath);
```

实现要求：

- 资源不存在时抛出 `IllegalArgumentException`。
- 数值解析支持 YAML number 和 string。
- 布尔解析支持 YAML boolean 和 string。

### 6.3 `E2ETestCase`

包：

```text
com.intellisql.test.e2e.framework.casefile
```

职责：

- 表示一个 `.sql` case。

字段：

```java
private String id;
private String model;
private String source;
private AssertionSpec assertion;
private StatementSpec statement;
private String sql;
private Path resourcePath;
```

`AssertionSpec` 字段：

- `type`：`mirror`、`file`、`state`、`error`
- `target`
- `expected`
- `expectedSql`
- `order`

`StatementSpec` 字段：

- `mode`：P0 固定 `statement`
- `params`：P1 使用

### 6.4 `E2ECaseParser`

职责：

- 解析 SQL 注释 DSL。
- 将 SQL 文件转成 `E2ETestCase`。

解析规则：

1. 逐行读取文件。
2. 以 `-- @` 开头的行为指令。
3. 第一条非指令行开始作为 SQL 正文。
4. SQL 正文保留原始换行。
5. 指令使用空格分隔 token。
6. token 支持 `key=value`。
7. value 两端双引号可去除。

示例：

```text
-- @assert mirror target=postgresql order=auto
```

解析结果：

- `type=mirror`
- `target=postgresql`
- `order=auto`

关键方法：

```java
public E2ETestCase parse(Path casePath);
```

校验规则：

- `@case id=...` 必填。
- `model` 缺省时使用 runner 的 `defaultModel`。
- `@assert` 必填。
- `@source` 缺省为 `intellisql`。
- P0 只接受 `statement`，遇到 `prepared` 抛出跳过异常或断言错误。

### 6.5 `E2ECaseScanner`

职责：

- 扫描 `e2e/cases/**/*.sql`。
- 调用 `E2ECaseParser`。
- 返回稳定排序的用例列表。

关键方法：

```java
public List<E2ETestCase> scan(String caseRoot);
```

排序规则：

- 按资源路径字典序。
- 保证本地和 CI 执行顺序一致。

### 6.6 `ModelConfigRenderer`

包：

```text
com.intellisql.test.e2e.framework.environment
```

职责：

- 渲染 `e2e/models/${model}/model.yaml`。
- 写入临时目录。
- 返回临时文件 `Path`。

关键方法：

```java
public Path render(String model, Map<String, String> variables, Path targetDirectory);
```

实现细节：

- 读取 classpath resource。
- 使用明确变量表替换 `${NAME}`。
- 渲染后检查是否仍存在 `${`，存在则报错。
- 使用 UTF-8。

变量来源：

- `POSTGRESQL_HOST`
- `POSTGRESQL_PORT`
- `POSTGRESQL_DATABASE`
- `POSTGRESQL_USERNAME`
- `POSTGRESQL_PASSWORD`

### 6.7 `PostgreSQLContainerFixture`

职责：

- 管理 PostgreSQL Testcontainer。
- 提供 JDBC URL、host、port、database、username、password。

关键方法：

```java
public void start();
public Connection createConnection();
public Map<String, String> toModelVariables();
public void close();
```

实现要求：

- 使用 `PostgreSQLContainer<?>`。
- P0 不启用容器复用，避免脏数据影响。
- `close()` 停止容器。

### 6.8 `SqlScriptExecutor`

职责：

- 执行初始化 SQL。

关键方法：

```java
public void execute(Connection connection, String resourcePath);
```

P0 SQL splitter：

- 移除空白语句。
- 按 `;` 切分。
- 逐条用 `Statement.execute` 执行。

### 6.9 `PortAllocator`

职责：

- 当 `serverPort=0` 时选择可用端口。

关键方法：

```java
public int allocate();
```

实现方式：

- 使用 `ServerSocket(0)` 获取端口。
- 关闭 socket 后立即启动 Server。

风险：

- 端口存在短窗口竞争。
- P0 可接受；P1 可改为让 Avatica server 绑定 0 并读取实际端口。

### 6.10 `IntelliSqlServerFixture`

职责：

- 使用临时 `model.yaml` 启停 `IntelliSqlServer`。
- 生成 IntelliSQL JDBC URL。

字段：

```java
private IntelliSqlServer server;
private int port;
private Path configPath;
private String database;
```

关键方法：

```java
public void start(Path configPath, int requestedPort, String database);
public String getJdbcUrl();
public void close();
```

实现细节：

- 使用 `ServerConfig.builder().port(port).configPath(configPath).build()`。
- `start()` 后通过 `server.getPort()` 记录实际端口。
- `getJdbcUrl()` 返回 `jdbc:intellisql://localhost:${port}/${database}`。
- `close()` 调用 `server.stop()`。

### 6.11 `E2EEnvironment`

职责：

- 聚合容器、初始化脚本、模型渲染、Server 和 JDBC 连接。
- 为测试入口提供统一 fixture。

生命周期：

1. 加载 runner。
2. 启动 PostgreSQL 容器。
3. 执行初始化 SQL。
4. 渲染 `model.yaml`。
5. 启动 `IntelliSqlServer`。
6. 创建 IntelliSQL JDBC 连接。
7. 创建 PostgreSQL baseline 连接。

关键方法：

```java
public void start(String model, Path tempDirectory);
public Connection createIntelliSqlConnection();
public Connection createPostgreSQLConnection();
public E2ERunnerConfig getConfig();
public void close();
```

实现要求：

- `close()` 按反向顺序释放。
- 关闭连接、Server、容器时捕获异常并继续释放后续资源。
- 测试入口使用 try-with-resources 或 `@AfterEach` 保证释放。

## 7. 执行器设计

### 7.1 `JdbcCaseExecutor`

包：

```text
com.intellisql.test.e2e.framework.execute
```

职责：

- 使用 IntelliSQL JDBC 执行 case。

关键方法：

```java
public ExecutionResult execute(Connection connection, E2ETestCase testCase);
```

P0 行为：

- 只接受 `statement`。
- `mirror` 和 `file` 断言类型走 `Statement.executeQuery(sql)`。
- `error` 断言捕获 SQLException。
- DML/state 在 P0 报告 unsupported。

`ExecutionResult` 字段：

- `ResultSetSnapshot snapshot`
- `SQLException exception`
- `long durationMillis`

### 7.2 `BaselineExecutor`

职责：

- 使用原生 PostgreSQL JDBC 执行期望 SQL 或读取期望文件。

关键方法：

```java
public ResultSetSnapshot executeMirror(Connection connection, E2ETestCase testCase);
public ResultSetSnapshot loadExpectedFile(E2ETestCase testCase);
```

mirror 规则：

- `@expected-sql` 存在时使用它。
- `@expected-sql` 不存在时使用 case SQL。

file 规则：

- P0 支持 CSV。
- 第一行为列名。
- 后续行为数据。
- 空文件非法。

## 8. 结果快照与断言

### 8.1 `ResultSetSnapshot`

包：

```text
com.intellisql.test.e2e.framework.assertion
```

字段：

```java
private List<ColumnSnapshot> columns;
private List<RowSnapshot> rows;
```

`ColumnSnapshot`：

```java
private String label;
private int jdbcType;
private String typeName;
```

`RowSnapshot`：

```java
private List<String> values;
```

实现要求：

- 类使用 `final`。
- 字段不可变。
- 构造时复制传入集合。

### 8.2 `ResultSetSnapshotReader`

职责：

- 从 JDBC `ResultSet` 构建 `ResultSetSnapshot`。

关键方法：

```java
public ResultSetSnapshot read(ResultSet resultSet);
```

读取规则：

- 使用 `ResultSetMetaData.getColumnLabel(i)` 读取列名。
- 使用 `getColumnType(i)` 和 `getColumnTypeName(i)` 保存类型。
- 行值使用 `resultSet.getObject(i)` 获取。
- 值交给 `ValueNormalizer`。

### 8.3 `ValueNormalizer`

职责：

- 将 JDBC 值归一化为稳定字符串。

规则：

| 类型 | 归一化 |
| --- | --- |
| `null` | runner 配置的 `nullToken` |
| `BigDecimal` | `stripTrailingZeros().toPlainString()` |
| `Number` | `new BigDecimal(value.toString()).stripTrailingZeros().toPlainString()` |
| `Boolean` | `true` / `false` |
| `Date` / `Time` / `Timestamp` | JDBC 标准 `toString()` |
| `byte[]` | Base64 |
| 其他 | `String.valueOf(value)` |

### 8.4 `ResultSetComparator`

职责：

- 比较 actual 和 expected。

关键方法：

```java
public ComparisonResult compare(ResultSetSnapshot actual, ResultSetSnapshot expected, OrderMode orderMode);
```

列比较：

- 列数必须一致。
- P0 列名使用大小写不敏感比较。
- P0 类型不作为强断言，因为当前服务端统一构造为 `VARCHAR`。

行比较：

- `STRICT`：逐行逐列比较。
- `ANY`：使用 multiset 行签名计数。

`ComparisonResult`：

- `boolean matched`
- `String message`
- `List<String> details`

失败信息必须包含：

- case id。
- 列差异。
- expected row count。
- actual row count。
- 首个不匹配位置或缺失/多余行。

### 8.5 `SqlOrderAnalyzer`

职责：

- 实现 `order=auto`。

关键方法：

```java
public OrderMode analyze(String sql, String configuredOrderMode);
```

规则：

- `strict` 返回 `STRICT`。
- `any` 返回 `ANY`。
- `auto` 时判断 SQL 顶层是否有 `ORDER BY`。

实现策略：

1. 优先使用 `SqlParserFactory.parseWithBabel(sql)`。
2. AST 为 `SqlOrderBy` 时返回 `STRICT`。
3. AST 为 `SqlSelect` 且 `getOrderList()` 非空时返回 `STRICT`。
4. 解析失败时使用文本 fallback。
5. fallback 只扫描顶层 `ORDER BY`，忽略括号内部子查询。

P0 可以先实现文本 fallback，P1 再接入 AST。若 P0 接入 AST，需要给 `intellisql-test-e2e` 显式添加 `intellisql-parser` test 依赖。

## 9. JUnit 测试入口

### 9.1 `JdbcAssertionFrameworkTest`

包：

```text
com.intellisql.test.e2e.jdbc
```

类型：普通单元测试。

覆盖：

- `assertParseCaseWithMirrorAssertion`
- `assertParseCaseWithFileAssertion`
- `assertCompareStrictSnapshots`
- `assertCompareAnyOrderSnapshots`
- `assertNormalizeNullAndNumberValues`
- `assertAnalyzeOrderBySql`

该测试不启动 Docker。

### 9.2 `JdbcSmokeE2ETest`

类型：Testcontainers + IntelliSQL Server E2E。

覆盖：

- `assertConnectToIntelliSqlServer`
- `assertShowTables`

说明：

- `SHOW TABLES` 是当前 `IntelliSqlMeta` 明确实现的 smoke path。
- P0 可暂缓 `SELECT 1`，因为当前 `QueryProcessor` 对无表常量查询未作为优先验证对象。

### 9.3 `JdbcDqlE2ETest`

类型：Testcontainers + IntelliSQL Server E2E。

覆盖：

- `assertExecuteCustomerSelect`
- `assertExecuteCustomerFilter`
- `assertExecuteCustomerOrderBy`

执行方式：

- 使用 `E2ECaseScanner` 获取 `dql` 用例。
- 每个 case 执行 IntelliSQL JDBC。
- mirror 到 PostgreSQL baseline。
- 使用 `ResultSetComparator` 断言。

### 9.4 现有测试处理

现有 `ConcurrentConnectionTest`、`JdbcE2ETest`、`LargeResultSetE2ETest`、`QueryPerformanceTest` 等测试直接连接 PostgreSQL 或 MySQL。P0 不删除这些测试。

后续处理建议：

- 将直接数据库测试改名或移动为 connector/database baseline 测试。
- 新增 IntelliSQL JDBC 链路测试使用 `jdbc` 子包隔离。
- P2 后逐步把并发、大结果集、性能场景迁移到 IntelliSQL JDBC 链路。

## 10. Maven 配置

当前 `intellisql-test-e2e` 已依赖：

- `intellisql-server`
- `intellisql-client`
- `intellisql-jdbc`
- Testcontainers PostgreSQL/MySQL/Elasticsearch
- MySQL JDBC
- PostgreSQL JDBC

P0 推荐增加直接 test 依赖：

```xml
<dependency>
    <groupId>com.intellisql</groupId>
    <artifactId>intellisql-common</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
```

原因：

- 测试框架直接使用项目配置模型或工具类时，依赖关系清晰。
- 即使已有传递依赖，显式声明能降低后续重构风险。

如果 `SqlOrderAnalyzer` 使用 `SqlParserFactory`，再增加：

```xml
<dependency>
    <groupId>com.intellisql</groupId>
    <artifactId>intellisql-parser</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
```

P0 若先用文本 fallback，可暂缓添加 parser 依赖。

## 11. 实施顺序

### Step 1：Server 支持 `configPath`

改动文件：

- `ServerConfig.java`
- `IntelliSqlServer.java`

新增测试：

- `IntelliSqlServerConfigTest` 或在 E2E fixture 中覆盖。

验收：

```bash
./mvnw -pl intellisql-server -am test
```

### Step 2：建立 E2E framework 基础模型

新增类：

- `E2ERunnerConfig`
- `E2ERunnerConfigLoader`
- `E2ETestCase`
- `AssertionSpec`
- `StatementSpec`
- `E2ECaseParser`
- `E2ECaseScanner`

新增测试：

- `JdbcAssertionFrameworkTest`

验收：

```bash
./mvnw -pl intellisql-tests/intellisql-test-e2e -Dtest=JdbcAssertionFrameworkTest test
```

### Step 3：建立 environment fixture

新增类：

- `PostgreSQLContainerFixture`
- `SqlScriptExecutor`
- `ModelConfigRenderer`
- `PortAllocator`
- `IntelliSqlServerFixture`
- `E2EEnvironment`

新增资源：

- `runner.yaml`
- `models/basic/model.yaml`
- `init/basic/postgresql.sql`

验收：

```bash
./mvnw -pl intellisql-tests/intellisql-test-e2e -Dtest=JdbcSmokeE2ETest test
```

### Step 4：建立结果快照和比较器

新增类：

- `ResultSetSnapshot`
- `ColumnSnapshot`
- `RowSnapshot`
- `ResultSetSnapshotReader`
- `ValueNormalizer`
- `ResultSetComparator`
- `ComparisonResult`
- `OrderMode`
- `SqlOrderAnalyzer`

新增测试：

- `JdbcAssertionFrameworkTest` 中补比较器覆盖。

验收：

```bash
./mvnw -pl intellisql-tests/intellisql-test-e2e -Dtest=JdbcAssertionFrameworkTest test
```

### Step 5：建立 JDBC case executor

新增类：

- `JdbcCaseExecutor`
- `BaselineExecutor`
- `ExecutionResult`

新增资源：

- `cases/smoke/show-tables.sql`
- `expected/smoke/show-tables.csv`
- `cases/dql/customer-select.sql`
- `cases/dql/customer-filter.sql`
- `cases/dql/customer-order-by.sql`

新增测试：

- `JdbcSmokeE2ETest`
- `JdbcDqlE2ETest`

验收：

```bash
./mvnw -pl intellisql-tests/intellisql-test-e2e -Dtest=JdbcSmokeE2ETest,JdbcDqlE2ETest test
```

### Step 6：模块级验证

运行：

```bash
./mvnw -pl intellisql-tests/intellisql-test-e2e -am test
```

如果触发 Docker 资源不足，保留失败原因，并单独运行非 Docker 单元测试：

```bash
./mvnw -pl intellisql-tests/intellisql-test-e2e -Dtest=JdbcAssertionFrameworkTest test
```

## 12. P1 实现设计

### 12.1 PreparedStatement 参数传递

改动文件：

- `IntelliSqlPreparedStatement.java`
- `AvaticaClient.java` 可保持不变，已有 `execute(..., List<TypedValue>, ...)`
- `IntelliSqlMeta.java`

客户端改动：

- `parameters` 从 `List<Object>` 扩展为带 JDBC 类型信息的 `List<ParameterValue>`。
- setter 保存值和 SQL type。
- `executeQuery()`、`executeUpdate()`、`execute()` 调用 `toTypedValues()`。

服务端改动：

- `IntelliSqlMeta.execute(...)` 接收 `parameterValues`。
- P1 可先做 SQL 字面量替换，后续再传递到内核参数化执行。

测试：

- `JdbcPreparedStatementE2ETest.assertExecutePreparedSelect`
- `JdbcPreparedStatementE2ETest.assertExecutePreparedFilter`

### 12.2 fetch 支持

改动文件：

- `IntelliSqlResultSet.java`
- `IntelliSqlMeta.java`

服务端：

- `StatementInfo` 增加 rows 缓存。
- `executeQuery` 将完整结果暂存到 `StatementInfo`。
- first frame 返回前 `fetchSize` 行。
- `fetch(offset, fetchMaxRowCount)` 根据 offset 返回后续 rows。

客户端：

- `IntelliSqlResultSet.next()` 在本地 iterator 结束且服务端未 done 时调用 `client.fetch(...)`。
- 合并新 frame 后继续迭代。

测试：

- `JdbcLargeResultSetE2ETest.assertFetchRowsAcrossFrames`

### 12.3 DML 状态断言

框架：

- `JdbcCaseExecutor` 支持 `state` assertion。
- 执行 DML 后运行 `@expected-sql`。
- baseline 执行同一 DML 或自定义 baseline SQL。

驱动：

- P1 不强制 update count 正确。

测试：

- `JdbcDmlE2ETest.assertUpdateWithStateAssertion`

## 13. P2 实现设计

### 13.1 batch

改动文件：

- `IntelliSqlPreparedStatement.java`
- `IntelliSqlStatement.java`
- `IntelliSqlMeta.java`

实现：

- PreparedStatement 保存 `List<List<ParameterValue>> batches`。
- `addBatch()` 拷贝当前参数。
- `executeBatch()` 调用 Avatica batch request。
- 服务端返回每条语句 affected rows。

测试：

- `JdbcBatchE2ETest.assertExecutePreparedBatchInsert`

### 13.2 并发

框架：

- `E2EEnvironment` 支持每个线程创建独立 IntelliSQL connection。
- 并发读测试共享只读数据。
- 并发写测试每个 case 前重建数据。

测试：

- `JdbcConcurrencyE2ETest.assertConcurrentSelect`
- `JdbcConcurrencyE2ETest.assertConcurrentConnections`

### 13.3 多数据源 cross-source query

资源：

```text
models/federation/model.yaml
init/federation/postgresql.sql
init/federation/mysql.sql
cases/federation/customer-order-join.sql
```

约束：

- 避免两个数据源使用相同表名。
- 需要先确认 `QueryProcessor` 对跨源计划的真实能力。

## 14. P3 实现设计

### 14.1 metadata

改动文件：

- `IntelliSqlDatabaseMetaData.java`
- `IntelliSqlMeta.java`

目标：

- `getTables`
- `getColumns`
- `getSchemas`
- `getCatalogs`

测试：

- `JdbcMetadataE2ETest.assertGetTables`
- `JdbcMetadataE2ETest.assertGetColumns`

### 14.2 unsupported API contract

测试当前明确不支持的 API：

- `CallableStatement`
- savepoint
- LOB creation
- scrollable result set
- updatable result set

断言：

- 抛出 `SQLException`。
- 错误信息稳定。

## 15. 代码质量要求

实现时遵循项目规则：

- JDK 8 兼容。
- 测试方法以 `assert` 开头。
- 不使用 Java 9+ API。
- 不使用 `List.of`、`Map.of`、`var`。
- 生产 public 类和 public 方法写 Javadoc。
- 测试断言优先使用 Hamcrest `assertThat(actual, is(expected))`。
- 布尔断言使用 `assertTrue` / `assertFalse`。
- 空值断言使用 `assertNull` / `assertNotNull`。
- 资源释放使用 try-with-resources 或 `@AfterEach`。
- Testcontainers 资源关闭由 fixture 统一处理。
- 配置文件命名使用 spinal-case。

## 16. 验收清单

P0 完成后必须满足：

- `ServerConfig` 支持显式 `configPath`。
- E2E 使用临时 `model.yaml`，不依赖 classpath 示例配置。
- `SHOW TABLES` case 通过。
- `customer-select` case 通过。
- `customer-filter` case 通过。
- `customer-order-by` case 通过。
- `JdbcAssertionFrameworkTest` 不启动 Docker 且通过。
- Docker 不可用时，最终说明明确标注未运行或失败原因。

推荐验收命令：

```bash
./mvnw -pl intellisql-server -am test
./mvnw -pl intellisql-tests/intellisql-test-e2e -Dtest=JdbcAssertionFrameworkTest test
./mvnw -pl intellisql-tests/intellisql-test-e2e -Dtest=JdbcSmokeE2ETest,JdbcDqlE2ETest test
./mvnw -pl intellisql-tests/intellisql-test-e2e -am test
```

P1 完成后增加：

```bash
./mvnw -pl intellisql-tests/intellisql-test-e2e -Dtest=JdbcPreparedStatementE2ETest,JdbcDmlE2ETest,JdbcLargeResultSetE2ETest test
```

P2/P3 完成后运行：

```bash
./mvnw -pl intellisql-tests/intellisql-test-e2e -am verify
```

## 17. 推荐落地路径

先实现 P0，不同时修改 PreparedStatement、fetch、batch 和 metadata。P0 的关键是把测试框架和真实 IntelliSQL JDBC 链路打通，并用小数据集建立稳定断言基础。P0 通过后，再按 P1/P2/P3 扩展 JDBC 能力和用例矩阵。
