# IntelliSQL JDBC E2E 测试程序调研与技术方案

## 目标

JDBC E2E 测试程序用于验证 IntelliSQL 从 JDBC 客户端到联邦执行内核的完整链路：

1. `DriverManager` 加载 `com.intellisql.jdbc.IntelliSqlDriver`。
2. `IntelliSqlDriver` 解析 `jdbc:intellisql://` URL 并创建 `AvaticaClient`。
3. `AvaticaClient` 通过 Protobuf HTTP 调用 `IntelliSqlServer`。
4. `IntelliSqlServer` 将 Avatica 请求交给 `IntelliSqlMeta`。
5. `IntelliSqlMeta` 调用 `IntelliSqlKernel` 执行 SQL。
6. `IntelliSqlKernel` 通过 `DataSourceManager`、`MetadataManager`、`QueryProcessor` 和连接器访问底层数据源。

测试程序的核心价值是把 JDBC 驱动、Server、联邦查询、连接器、数据源配置和结果断言放在同一个自动化闭环中验证。

## 当前源码约束

### JDBC 驱动

当前 JDBC 驱动位于 `intellisql-jdbc`：

- 驱动类：`com.intellisql.jdbc.IntelliSqlDriver`
- SPI 注册文件：`intellisql-jdbc/src/main/resources/META-INF/services/java.sql.Driver`
- URL 前缀：`jdbc:intellisql:`
- URL 格式：`jdbc:intellisql://host[:port][/database][?key=value&key2=value2]`
- 默认端口：`8765`
- 默认 database/catalog：`intellisql`
- 实际 HTTP endpoint：`http://host:port/api/protobuf`
- 默认连接属性：
  - `fetchSize=1000`
  - `queryTimeout=300`
  - `serialization=protobuf`
  - `connectTimeout=30`
  - `socketTimeout=60`
  - `maxRows=0`

当前 JDBC API 支持边界：

| API | 当前行为 | E2E 方案 |
| --- | --- | --- |
| `Connection` | 可打开、关闭，`autoCommit` 和事务方法基本为空实现 | P0 验证连接生命周期，事务作为后续能力 |
| `Statement.executeQuery` | 可执行并返回 `ResultSet` | P0 主执行路径 |
| `Statement.execute` | 总是创建结果集并返回 `true` | P0 可用于 smoke test |
| `Statement.executeUpdate` | 执行后固定返回 `0` | P1 只做状态断言，不依赖 update count |
| `PreparedStatement` 参数绑定 | setter 保存参数，执行时尚未传递给 Avatica | P1 先补参数序列化，再纳入矩阵 |
| `addBatch` / `executeBatch` | 抛出 `Batch updates not supported in MVP` | P2 补齐后启用批量用例 |
| `ResultSet` | `TYPE_FORWARD_ONLY`、`CONCUR_READ_ONLY` | P0 只测 forward-only/read-only |
| `ResultSet` fetch | 只消费 first frame，服务端 `fetch` 返回空 | P0 限制结果集小于 `fetchSize`，P1 补 fetch |
| `DatabaseMetaData` | 多数能力为本地常量或空结果 | P0 只验证基础元信息和 `SHOW TABLES` |
| 多结果集、可滚动结果集、可更新结果集、Callable、LOB、Savepoint | 当前不支持 | P2/P3 扩展 |

### Server

当前 Server 位于 `intellisql-server`：

- `IntelliSqlServer` 使用 `HttpServer.Builder().withPort(config.getPort()).withHandler(new AvaticaProtobufHandler(service))` 启动服务。
- 默认端口来自 `ServerConfig`，值为 `8765`。
- `intellisql-server/src/main/resources/conf/model.yaml` 中的 `server.avaticaPort: 8766` 当前没有被 `ServerConfig` 使用。
- `IntelliSqlServer` 初始化内核时固定读取 `conf/model.yaml`，先查 classpath resource，再查当前工作目录文件。
- `ServerConfig` 当前只有 `port`、`maxConnections`、`idleTimeoutMs`、`host`。

这意味着测试程序需要先为 Server 增加显式 `configPath`，或调整 `conf/model.yaml` 的加载优先级。推荐增加 `ServerConfig.configPath`，测试程序通过临时文件传入模型配置，避免依赖 classpath 中的示例配置。

### 配置模型

当前 `ConfigLoader` 只解析这些顶层配置：

- `dataSources`
- `props`

`dataSources` 支持 list 和 map 两种写法。每个数据源支持：

- `name`，list 写法需要
- `type`
- `url`
- `host`
- `port`
- `database`
- `username`
- `password`
- `pool` 或 `connectionPool`
- `healthCheck`

`props` 支持：

- `maxIntermediateRows`
- `queryTimeoutSeconds`
- `defaultFetchSize`
- `enableQueryLogging`
- `logLevel`

当前 `ConfigLoader` 会忽略这些示例配置：

- `server`
- `logging`
- `federation`
- `translation`
- `schemaMapping`
- `optimization`
- `monitoring`
- `security`
- 数据源内的 `properties`

数据源类型当前由 `DataSourceType` 限定为：

- `MYSQL`
- `POSTGRESQL`
- `ELASTICSEARCH`

因此，JDBC E2E 第一阶段的矩阵以 MySQL 和 PostgreSQL 为主，Elasticsearch 作为连接器级验证或专项场景。Oracle、SQL Server、Hive 依赖和插件目录存在，但当前公共配置枚举没有纳入这些类型，测试矩阵先不声明为已支持。

### 现有 E2E 测试模块

当前 `intellisql-tests/intellisql-test-e2e` 已经有 Testcontainers 驱动的 MySQL/PostgreSQL 测试，覆盖连接、CRUD、并发、大结果集和性能场景。现有测试直接使用原生 MySQL/PostgreSQL JDBC URL，主要验证底层数据库和通用 JDBC 行为。

新的 JDBC E2E 测试程序需要把执行入口切换为 `jdbc:intellisql://`，让断言覆盖 IntelliSQL JDBC Driver、Server、Kernel 和 Connector 的完整链路。原生 MySQL/PostgreSQL 连接在新方案中作为基准库连接保留，用于 mirror assertion 和初始化数据。

## 对原调研稿的调整

| 原调研稿设计点 | 当前源码对齐后的方案 |
| --- | --- |
| IntelliSQL JDBC URL 示例使用 `jdbc:intellisql://${CLUSTER_IP}:5432/federated_db` | 使用 `jdbc:intellisql://localhost:${serverPort}/intellisql`，默认端口为 `8765` |
| 使用 `com.intellisql.Driver` | 使用 `com.intellisql.jdbc.IntelliSqlDriver` |
| 使用独立 `avaticaPort=8766` 作为 JDBC 服务端口 | 当前自研 JDBC 驱动访问 `http://host:port/api/protobuf`，端口来自 `ServerConfig.port` |
| `env/${SCENARIO_NAME}/scenario-env.yaml` 和 `rules.yaml` 驱动运行 | 使用当前内核识别的 `model.yaml`，测试程序自己的运行配置独立放在 `runner.yaml` |
| `rules.yaml` 控制 federation、pushdown、translation | 当前 `ConfigLoader` 不解析规则配置，P0 只通过 `dataSources` 和 `props` 驱动 |
| `@Batch` 默认可用并路由到 `executeBatch` | 当前 JDBC 驱动明确不支持 batch，P2 补驱动和服务端后启用 |
| Additional JDBC API 入口覆盖滚动、并发更新、多结果集 | 当前 ResultSet 固定 forward-only/read-only，多结果集不支持，P2/P3 作为扩展入口 |
| PreparedStatement 作为基础矩阵维度 | 当前参数没有传递到 Avatica，P1 补 `TypedValue` 绑定后纳入 |
| DML/DDL 依赖 update count | 当前 update count 固定为 `0`，DML/DDL 采用后置查询或文件快照断言 |
| 大结果集 streaming 验证 | 当前只消费 first frame，P0 控制结果集大小，P1 补 fetch 后开启大结果集 |
| 每个 Feature 独立加载场景配置 | 当前 Server 固定加载 `conf/model.yaml`，P0 增加显式 config path 后按场景生成临时 model |

## 方案边界

P0 聚焦当前可落地能力：

- 启动 MySQL/PostgreSQL Testcontainers。
- 生成当前 `ConfigLoader` 可识别的 `model.yaml`。
- 增加并使用 `ServerConfig.configPath`，让测试程序传入临时模型配置。
- 启动单个 `IntelliSqlServer`。
- 使用 `com.intellisql.jdbc.IntelliSqlDriver` 连接 IntelliSQL。
- 使用 `Statement.executeQuery` 执行 DQL。
- 使用原生 JDBC 连接基准库执行对等 SQL 或断言 SQL。
- 比较结果集内容、列名、行数和排序语义。
- 验证 `SHOW TABLES` 和基本 metadata。

P1 补齐驱动和服务端基础缺口：

- `PreparedStatement` 参数 `TypedValue` 序列化
- 服务端 `fetch`
- DML 状态断言
- 错误断言

P2 扩展矩阵：

- batch
- transaction 行为
- 大结果集
- 并发读写
- 多数据源 cross-source query

P3 覆盖高级 JDBC：

- `DatabaseMetaData` 表、列、schema
- 多结果集
- scrollable result set
- read-only contract 和 unsupported API contract

## 目录设计

JDBC E2E 测试程序放在 `intellisql-tests/intellisql-test-e2e`，保持当前 Maven 模块边界。

推荐资源目录：

```text
intellisql-tests/intellisql-test-e2e/src/test/resources/e2e/
├── runner.yaml
├── models/
│   ├── basic/
│   │   └── model.yaml
│   └── federation/
│       └── model.yaml
├── init/
│   ├── basic/
│   │   ├── mysql.sql
│   │   └── postgresql.sql
│   └── federation/
│       ├── mysql.sql
│       └── postgresql.sql
├── cases/
│   ├── smoke/
│   │   ├── show-tables.sql
│   │   └── select-one.sql
│   ├── dql/
│   │   ├── basic-select.sql
│   │   ├── filter.sql
│   │   ├── order-by.sql
│   │   └── aggregate.sql
│   └── dml/
│       └── update-with-state-assertion.sql
└── expected/
    └── smoke/
        └── show-tables.csv
```

命名遵循 spinal-case。`model.yaml` 是内核模型配置；`runner.yaml` 是测试程序自己的执行配置，两者职责分离。

## 配置设计

### runner.yaml

`runner.yaml` 控制测试程序，不传给 IntelliSQL 内核：

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
  mysql:
    enabled: true
    image: mysql:8.0
    database: testdb
    username: testuser
    password: testpass
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

`serverPort: 0` 表示测试程序选择空闲端口，然后通过 `ServerConfig.port` 传给 `IntelliSqlServer`。

### model.yaml

`model.yaml` 使用当前 `ConfigLoader` 可识别字段。推荐 map 写法，保证数据源名称稳定：

```yaml
dataSources:
  mysql_source:
    type: mysql
    url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}
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

配置生成规则：

- Testcontainers 模式下，测试程序用容器映射端口替换 `${MYSQL_PORT}`、`${POSTGRESQL_PORT}` 等变量，生成临时 `model.yaml`。
- Native 模式下，变量来自环境变量或本地 `runner.yaml` override。
- 数据源特定 JDBC 属性写入 URL query string，因为当前 `ConfigLoader` 尚未把 `properties` 传递给连接池。
- 多数据源场景避免不同数据源出现相同表名，因为当前 `MetadataManager` 会把表按表名直接注册到 root schema。

## SQL 用例 DSL

用例继续使用 `.sql` 文件，SQL 注释承载元信息。P0 选择少量稳定指令，降低解析复杂度。

```sql
-- @case id=basic-select model=basic
-- @source intellisql
-- @assert mirror target=postgresql_expected order=auto
SELECT id, name FROM customers WHERE status = 'ACTIVE' ORDER BY id;
```

支持指令：

| 指令 | 示例 | 说明 |
| --- | --- | --- |
| `@case` | `-- @case id=basic-select model=basic` | 用例标识和模型目录 |
| `@source` | `-- @source intellisql` | 当前固定为 IntelliSQL JDBC |
| `@assert` | `-- @assert mirror target=mysql_expected order=auto` | 断言类型、基准库、排序策略 |
| `@expected-sql` | `-- @expected-sql SELECT id, name FROM customers ORDER BY id` | 基准库使用的 SQL，省略时使用被测 SQL |
| `@expected-file` | `-- @expected-file expected/smoke/show-tables.csv` | 文件快照断言 |
| `@error` | `-- @error contains="Table not found"` | 异常断言 |
| `@statement` | `-- @statement mode=statement` | P0 固定 `statement`，P1 支持 `prepared` |
| `@params` | `-- @params 1:int=100 2:string=ACTIVE` | P1 启用 |

P0 解析约定：

- 一个 `.sql` 文件对应一个 case。
- 指令位于 SQL 前置注释区。
- SQL 正文从第一条非指令行开始。
- SQL 正文允许多行，以文件末尾为结束。
- `@expected-sql` 的值较长时可放在同目录 `.expected.sql` 文件中。

## 执行流程

### 1. 环境启动

1. 读取 `runner.yaml`。
2. 根据 `execution.mode` 启动 Docker 或读取 Native 连接信息。
3. 执行 `init/${model}/mysql.sql` 和 `init/${model}/postgresql.sql`，初始化底层数据库。
4. 渲染 `models/${model}/model.yaml` 为临时文件。
5. 启动 `IntelliSqlServer`：
   - P0 前置修改：`ServerConfig` 增加 `configPath`。
   - 服务端使用随机空闲端口或 runner 指定端口。
6. 通过 `jdbc:intellisql://localhost:${serverPort}/${jdbcDatabase}?fetchSize=${defaultFetchSize}` 建立被测连接。
7. 建立基准库原生 JDBC 连接。

### 2. 用例发现

1. 扫描 `e2e/cases/**/*.sql`。
2. 解析 case 指令和 SQL 正文。
3. 根据 `@case model=...` 绑定环境模型。
4. 根据 `@assert` 生成断言计划。
5. 根据 `@statement` 生成 JDBC 执行计划。

### 3. 执行与断言

P0 DQL mirror 流程：

1. 使用 IntelliSQL JDBC 的 `Statement.executeQuery(sql)` 获取实际结果。
2. 使用基准库原生 JDBC 执行 `expectedSql`。
3. 将双方 `ResultSet` 转换为 `ResultSetSnapshot`。
4. 根据 `order=auto` 判断比对模式：
   - SQL 含顶层 `ORDER BY` 时逐行比对。
   - SQL 无顶层 `ORDER BY` 时按 multiset 比对。
5. 输出差异：
   - 列名差异
   - 行数差异
   - 缺失行
   - 多余行
   - 首个位置不一致的行

P1 DML 状态断言流程：

1. 在 IntelliSQL JDBC 执行 DML。
2. 在基准库执行相同 DML，或执行用例声明的 setup/mutation SQL。
3. 使用 `@expected-sql` 查询状态。
4. 比较状态查询结果。
5. 不依赖 `executeUpdate` 返回值，直到驱动和服务端 update count 能返回真实影响行数。

## 结果集快照模型

`ResultSetSnapshot` 推荐结构：

```text
ResultSetSnapshot
├── columns: List<ColumnSnapshot>
│   ├── label
│   ├── typeName
│   └── jdbcType
└── rows: List<RowSnapshot>
    └── values: List<String>
```

归一化规则：

- 列名默认大小写不敏感，比较时转为 lower-case。
- `null` 使用内部哨兵值，不和字符串 `"null"` 混淆。
- 数值统一使用 `BigDecimal.stripTrailingZeros().toPlainString()`。
- 布尔统一为 `true` / `false`。
- 日期时间统一按 JDBC 对象的标准字符串表达。
- 其他类型使用 `String.valueOf(value)`。
- 当前服务端结果元数据会把列类型构造成 `VARCHAR`，P0 以值和列名为主，类型检查作为弱校验。

无序比较使用行签名计数：

```text
row-signature = normalizedValue[0] + "\u001f" + normalizedValue[1] + ...
```

比较结果保留重复行计数，避免 `Set` 掩盖重复行错误。

## Order-Aware 断言

`order=auto` 是默认模式。

判定策略：

1. 优先使用 `intellisql-parser` 的 Calcite SQL AST 判断顶层 `ORDER BY`。
2. 解析失败时回退到 SQL 文本扫描。
3. `@assert order=strict` 强制逐行比较。
4. `@assert order=any` 强制 multiset 比较。

该策略匹配分布式查询语义：SQL 声明顺序时校验顺序；SQL 未声明顺序时校验集合内容。

## 核心类设计

建议新增包：`com.intellisql.test.e2e.framework`。

| 类 | 职责 |
| --- | --- |
| `E2ERunnerConfig` | 读取 `runner.yaml` |
| `E2EModel` | 表示一个模型场景 |
| `E2ETestCase` | 表示 `.sql` 用例 |
| `E2ECaseParser` | 解析 SQL 注释 DSL |
| `E2EEnvironment` | 管理容器、Server、连接生命周期 |
| `ModelConfigRenderer` | 渲染当前源码兼容的 `model.yaml` |
| `IntelliSqlServerFixture` | 启停 `IntelliSqlServer` |
| `JdbcCaseExecutor` | 执行 Statement / PreparedStatement |
| `BaselineExecutor` | 执行基准库 SQL |
| `ResultSetSnapshot` | 保存结果集快照 |
| `ResultSetSnapshotReader` | 从 JDBC `ResultSet` 构建快照 |
| `ResultSetComparator` | 顺序/无序比对 |
| `SqlOrderAnalyzer` | 判断 SQL 是否声明排序 |
| `E2EDifferenceFormatter` | 输出可读差异 |

JUnit 入口：

| 测试类 | 阶段 | 覆盖 |
| --- | --- | --- |
| `JdbcSmokeE2ETest` | P0 | 连接、`SELECT 1`、`SHOW TABLES` |
| `JdbcDqlE2ETest` | P0 | Statement DQL mirror |
| `JdbcAssertionFrameworkTest` | P0 | DSL、快照、比较器单元测试 |
| `JdbcPreparedStatementE2ETest` | P1 | 参数绑定 |
| `JdbcDmlE2ETest` | P1 | DML 状态断言 |
| `JdbcLargeResultSetE2ETest` | P1 | fetch/分页/大结果集 |
| `JdbcBatchE2ETest` | P2 | batch |
| `JdbcConcurrencyE2ETest` | P2 | 并发读写 |
| `JdbcMetadataE2ETest` | P3 | metadata |

测试方法命名遵循项目规则，以 `assert` 开头。

## 源码前置改动

### 1. Server 支持显式配置路径

当前 Server 固定加载 `conf/model.yaml`，且 classpath 示例配置优先。E2E 需要让测试程序传入临时模型配置。

推荐改动：

- `ServerConfig` 增加 `Path configPath` 或 `String configPath`。
- `IntelliSqlServer.initializeKernel()` 优先使用 `config.getConfigPath()`。
- 没有显式路径时维持当前 `conf/model.yaml` 兼容行为。

验收：

- 测试能用临时 `model.yaml` 启动 Server。
- classpath 示例配置不会污染 E2E。

### 2. PreparedStatement 参数传递

当前 `IntelliSqlPreparedStatement` 保存了参数，但执行时调用：

```java
connection.getClient().execute(statementHandle, null, fetchSize);
```

推荐改动：

- 将 `parameters` 转换为 `List<TypedValue>`。
- 对 `setInt`、`setLong`、`setString`、`setBoolean`、`setBigDecimal`、`setDate`、`setTime`、`setTimestamp` 建立明确映射。
- `executeQuery()`、`executeUpdate()`、`execute()` 传递 `TypedValue`。
- 服务端 `execute` 使用 Avatica 参数进入实际查询。

验收：

- `SELECT * FROM customers WHERE id = ?` 能返回正确结果。
- 参数类型覆盖常用标量。

### 3. fetch 支持

当前 `IntelliSqlResultSet` 只读取 first frame，`IntelliSqlMeta.fetch` 返回空 frame。

推荐改动：

- 服务端保存 statement 的完整结果或可继续读取的 cursor。
- `fetch(offset, fetchMaxRowCount)` 返回对应 frame。
- 客户端 `ResultSet.next()` 在本地 frame 消耗完后调用 fetch。
- 保持 forward-only 语义。

验收：

- `fetchSize=100` 下读取 1000 行结果完整。
- 大结果集测试无需把所有结果放入 first frame。

### 4. update count

当前 `Statement.executeUpdate` 和 `PreparedStatement.executeUpdate` 返回 `0`。

推荐改动：

- `QueryResult` 或执行响应携带 affected rows。
- 服务端将 affected rows 映射到 Avatica update count。
- E2E 先用状态断言，后续增加 update count 断言。

### 5. 数据源属性传递

当前 `model.yaml` 数据源 `properties` 没有进入 `IntelliSQLDataSourceConfig`。

推荐改动：

- `DataSourceConfig` 增加 `Map<String, String> properties`。
- `ConfigLoader.parseDataSourceConfig` 解析 properties。
- `DataSourceConfigs.fromCommonConfig` 传递 properties。
- `JdbcConnectionPool` 已支持 `config.getProperties()`，补齐后即可生效。

## 分阶段实施计划

### P0：Statement DQL 闭环

交付内容：

- `runner.yaml` 读取。
- Testcontainers 启动 MySQL/PostgreSQL。
- 临时 `model.yaml` 渲染。
- `ServerConfig.configPath` 支持。
- `IntelliSqlServerFixture`。
- SQL case parser。
- `Statement.executeQuery` 执行。
- mirror/file assertion。
- order-aware comparator。
- smoke cases 和基础 DQL cases。

验收命令：

```bash
./mvnw -pl intellisql-tests/intellisql-test-e2e -am test
```

### P1：PreparedStatement、DML、fetch

交付内容：

- PreparedStatement 参数传递。
- DML 状态断言。
- 服务端 fetch。
- 大结果集 case。
- 错误断言。

验收命令：

```bash
./mvnw -pl intellisql-tests/intellisql-test-e2e -am verify
```

### P2：批量、并发、跨源场景

交付内容：

- batch JDBC API。
- 并发连接与并发查询。
- 多数据源 cross-source query。
- 生命周期隔离：只读复用、变更重建。
- Native 模式连接外部数据库。

验收：

- Docker 模式在 CI 可重复。
- Native 模式可连接开发者本地数据库。

### P3：JDBC 兼容性扩展

交付内容：

- metadata 表/列/schema。
- unsupported API contract。
- 多结果集和高级 ResultSet 行为。
- 独立可执行测试 jar 或 Maven profile 分发。

## 风险与处理

| 风险 | 处理 |
| --- | --- |
| Server 默认 classpath `conf/model.yaml` 污染测试 | P0 增加显式 `configPath` |
| 当前 `PreparedStatement` 参数未生效 | P0 排除 prepared 矩阵，P1 补齐 |
| 当前 fetch 为空导致大结果集缺行 | P0 限制小结果集，P1 补 fetch |
| 当前 metadata 类型统一为 `VARCHAR` | P0 弱化类型断言，P3 完善 metadata |
| 相同表名跨数据源覆盖 | P0 测试数据避免重名，P2 设计 schema-qualified 场景 |
| `model.yaml` 示例字段多于 loader 支持 | 测试模型只使用 `dataSources` 和 `props` |
| DML update count 不可靠 | 使用后置查询断言状态 |
| Docker 环境不可用 | 提供 Native 模式和跳过策略 |

## 最小可用用例

### show-tables.sql

```sql
-- @case id=show-tables model=basic
-- @source intellisql
-- @assert file expected=expected/smoke/show-tables.csv order=any
SHOW TABLES;
```

### basic-select.sql

```sql
-- @case id=basic-select model=basic
-- @source intellisql
-- @assert mirror target=postgresql_expected order=auto
SELECT id, name FROM customers ORDER BY id;
```

### filter.sql

```sql
-- @case id=filter-active-customers model=basic
-- @source intellisql
-- @assert mirror target=postgresql_expected order=any
SELECT id, name FROM customers WHERE status = 'ACTIVE';
```

### update-with-state-assertion.sql

```sql
-- @case id=update-customer-status model=basic
-- @source intellisql
-- @statement mode=statement
-- @assert state target=postgresql_expected order=strict
-- @expected-sql SELECT status FROM customers WHERE id = 1
UPDATE customers SET status = 'INACTIVE' WHERE id = 1;
```

该 DML 用例放入 P1，因为当前 update count 和 DML 执行语义还需要专项验证。

## 推荐结论

JDBC E2E 测试程序先建设成“源码内 Maven 测试框架”，以 `intellisql-tests/intellisql-test-e2e` 为边界。P0 用当前可运行的 Statement DQL 链路形成闭环，同时补 `ServerConfig.configPath` 解决模型配置注入问题。PreparedStatement、batch、transaction、大结果集和高级 JDBC API 作为后续阶段进入矩阵，避免测试方案声明超过当前驱动和服务端能力。
