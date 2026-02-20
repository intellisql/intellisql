# Data Model: IntelliSql SQL Federation and Translation

**Date**: 2026-02-17
**Feature**: 001-sql-federation-translation

本文档定义了 IntelliSql 系统的核心实体、它们之间的关系、验证规则以及状态转换。

## 概述

IntelliSql 采用多层架构，实体主要在配置文件中定义，并在运行时在内存中管理。系统不维护持久化存储，而是将数据持久化委托给底层数据源。

## 核心实体

### 1. DataSource (数据源)

**Purpose**: 表示到外部数据源（MySQL、PostgreSQL、Elasticsearch）的连接

**Package**: `org.intellisql.kernel.metadata`

**字段**：

| Field | Type | Required | Description | Validation |
|-------|------|----------|-------------|------------|
| id | String | Yes | 唯一标识符 | UUID 格式，不可变 |
| name | String | Yes | 人类可读的名称 | 非空，在所有数据源中唯一 |
| type | DataSourceType | Yes | 数据源类型 | 枚举：MYSQL、POSTGRESQL、ELASTICSEARCH |
| url | String | Yes | 连接 URL | 有效的 JDBC URL 或 ES 连接字符串 |
| username | String | Conditional | 数据库用户名 | MYSQL 和 POSTGRESQL 必需 |
| password | String | Conditional | 数据库密码 | MYSQL 和 POSTGRESQL 必需；支持环境变量替换 |
| schemaMappings | List<SchemaMapping> | No | 模式映射规则 | 默认：自动发现 |
| connectionPoolConfig | ConnectionPoolConfig | No | 连接池配置 | 未指定时使用默认值 |
| healthCheckConfig | HealthCheckConfig | No | 健康检查设置 | 默认：启用，30秒间隔 |
| createdAt | Instant | Yes | 创建时间戳 | 创建时自动设置 |
| updatedAt | Instant | Yes | 最后更新时间戳 | 修改时自动更新 |

**Lombok 注解**：
```java
@Getter
@RequiredArgsConstructor
@Builder(toBuilder = true)
public final class DataSource {
    private final String id;
    private final String name;
    private final DataSourceType type;
    private final String url;
    private final String username;
    private final String password; // Environment variable substituted
    private final List<SchemaMapping> schemaMappings;
    private final ConnectionPoolConfig connectionPoolConfig;
    private final HealthCheckConfig healthCheckConfig;
    private final Instant createdAt;
    private Instant updatedAt;
}
```

**状态转换**：
```
[已创建] -> [连接中] -> [已连接] -> [活跃]
                                    |
                                    v
                              [已断开] -> [连接中]
                                    |
                                    v
                                [失败]
```

**验证规则**：
- `name` 在所有 DataSource 实例中必须唯一
- `url` 格式必须与 `type` 匹配（SQL 数据库用 JDBC，ES 用 HTTP）
- `password` 支持环境变量替换：`${DB_PASSWORD}`
- `connectionPoolConfig.maxSize` 必须 > 0 且 ≤ 100
- 健康检查间隔必须 ≥ 5 秒

**业务规则**：
- 连接失败应触发指数退避重试（NFR-006）
- 失败的数据源不应阻止其他数据源（边缘情况）
- 密码必须在日志和错误消息中遮蔽

---

### 2. Schema (逻辑模式)

**Purpose**: 表示映射到 DataSource 或联邦视图的逻辑数据库模式

**Package**: `org.intellisql.kernel.metadata`

**字段**：

| Field | Type | Required | Description | Validation |
|-------|------|----------|-------------|------------|
| name | String | Yes | 模式名称 | 非空，每个 DataSource 下唯一 |
| dataSourceId | String | Yes | 父 DataSource ID | 必须引用已存在的 DataSource |
| tables | Map<String, Table> | Yes | 该模式中的表 | 键：表名，值：Table |
| type | SchemaType | Yes | 模式类型 | 枚举：PHYSICAL、FEDERATED |
| metadata | Map<String, String> | No | 附加元数据 | 键值对 |

**Lombok 注解**：
```java
@Getter
@RequiredArgsConstructor
@Builder
public final class Schema {
    private final String name;
    private final String dataSourceId;
    private final Map<String, Table> tables;
    private final SchemaType type;
    private final Map<String, String> metadata;
}
```

**关系**：
- 与 DataSource 多对一关系（一个 DataSource 可以有多个 Schema）
- 与 Table 一对多关系（一个 Schema 有多个 Table）

**验证规则**：
- `name` 必须遵循标识符命名规则（除下划线外无特殊字符）
- PHYSICAL 模式必须有有效的 `dataSourceId`
- FEDERATED 模式可以有 null `dataSourceId`（虚拟模式）

---

### 3. Table (表/索引)

**Purpose**: 表示模式中的表（关系型）或索引（Elasticsearch）

**Package**: `org.intellisql.kernel.metadata`

**字段**：

| Field | Type | Required | Description | Validation |
|-------|------|----------|-------------|------------|
| name | String | Yes | 表/索引名称 | 非空 |
| schemaName | String | Yes | 父模式名称 | 必须引用已存在的 Schema |
| columns | List<Column> | Yes | 列定义 | 至少一列 |
| primaryKey | List<String> | No | 主键列 | 必须引用已存在的列 |
| indexes | List<Index> | No | 索引定义 | 可选 |
| type | TableType | Yes | 表类型 | 枚举：TABLE、VIEW、INDEX |
| rowCount | long | No | 预估行数 | ≥ 0 |
| metadata | Map<String, String> | No | 附加元数据 | 键值对 |

**Lombok 注解**：
```java
@Getter
@RequiredArgsConstructor
@Builder
public final class Table {
    private final String name;
    private final String schemaName;
    private final List<Column> columns;
    private final List<String> primaryKey;
    private final List<Index> indexes;
    private final TableType type;
    private final Long rowCount;
    private final Map<String, String> metadata;
}
```

**验证规则**：
- 列名在表中必须唯一
- `primaryKey` 列必须存在于 `columns` 列表中
- 对于 Elasticsearch 索引，将嵌套字段映射到列结构

---

### 4. Column (列定义)

**Purpose**: 表示表中具有类型信息的列

**Package**: `org.intellisql.kernel.metadata`

**字段**：

| Field | Type | Required | Description | Validation |
|-------|------|----------|-------------|------------|
| name | String | Yes | 列名 | 非空 |
| dataType | DataType | Yes | 数据类型 | 枚举：STRING、INTEGER、LONG、DOUBLE、BOOLEAN、DATE、TIMESTAMP、BINARY、JSON、ARRAY |
| nullable | boolean | Yes | 是否允许空值 | 默认：true |
| defaultValue | String | No | 默认值 | 必须与 dataType 兼容 |
| comment | String | No | 列注释 | 可选 |
| size | Integer | No | 列大小/长度 | 用于 STRING 类型 |
| precision | Integer | No | 数值精度 | 用于 DECIMAL 类型 |
| scale | Integer | No | 数值标度 | 用于 DECIMAL 类型 |

**Lombok 注解**：
```java
@Getter
@RequiredArgsConstructor
@Builder
public final class Column {
    private final String name;
    private final DataType dataType;
    private final boolean nullable;
    private final String defaultValue;
    private final String comment;
    private final Integer size;
    private final Integer precision;
    private final Integer scale;
}
```

**类型映射**：
- MySQL: VARCHAR → STRING, INT → INTEGER, BIGINT → LONG, DATETIME → TIMESTAMP
- PostgreSQL: TEXT → STRING, INTEGER → INTEGER, BIGINT → LONG, TIMESTAMP → TIMESTAMP
- Elasticsearch: keyword → STRING, long → LONG, double → DOUBLE, boolean → BOOLEAN, nested → JSON

---

### 5. Query (查询)

**Purpose**: 表示带有执行上下文的用户提交的 SQL 查询

**Package**: `org.intellisql.kernel.executor`

**字段**：

| Field | Type | Required | Description | Validation |
|-------|------|----------|-------------|------------|
| id | String | Yes | 查询 ID（用于日志） | UUID 格式 |
| sql | String | Yes | 原始 SQL 语句 | 非空，有效的 SQL 语法 |
| sourceDialect | SqlDialect | No | 源 SQL 方言 | 默认：STANDARD；枚举：MYSQL、POSTGRESQL、ORACLE、SQLSERVER、HIVE |
| targetDataSources | Set<String> | No | 目标数据源 ID | 未指定时自动检测 |
| executionPlan | ExecutionPlan | No | 优化后的执行计划 | 由优化器生成 |
| status | QueryStatus | Yes | 执行状态 | 枚举：PENDING、RUNNING、COMPLETED、FAILED、CANCELLED |
| startTime | Instant | No | 执行开始时间 | 状态变为 RUNNING 时设置 |
| endTime | Instant | No | 执行结束时间 | 状态变为 COMPLETED/FAILED/CANCELLED 时设置 |
| rowCount | Long | No | 返回的行数 | 完成时设置 |
| error | QueryError | No | 错误信息 | 失败时设置 |

**Lombok 注解**：
```java
@Getter
@Builder
public class Query {
    private final String id;
    private final String sql;
    private final SqlDialect sourceDialect;
    private final Set<String> targetDataSources;
    private ExecutionPlan executionPlan;
    private QueryStatus status;
    private Instant startTime;
    private Instant endTime;
    private Long rowCount;
    private QueryError error;

    public void markStarted() {
        this.status = QueryStatus.RUNNING;
        this.startTime = Instant.now();
    }

    public void markCompleted(long rowCount) {
        this.status = QueryStatus.COMPLETED;
        this.endTime = Instant.now();
        this.rowCount = rowCount;
    }

    public void markFailed(QueryError error) {
        this.status = QueryStatus.FAILED;
        this.endTime = Instant.now();
        this.error = error;
    }
}
```

**状态转换**：
```
[待处理] -> [运行中] -> [已完成]
                |
                +-----> [失败]
                |
                +-----> [已取消]
```

**验证规则**：
- `sql` 必须可被 Calcite 解析
- 跨源查询不得超过中间结果限制（NFR-010）
- 查询 ID 必须包含在所有日志语句中（NFR-002）

---

### 6. Translation (SQL翻译任务)

**Purpose**: 表示从一种方言到另一种方言的 SQL 翻译任务

**Package**: `org.intellisql.parser`

**字段**：

| Field | Type | Required | Description | Validation |
|-------|------|----------|-------------|------------|
| id | String | Yes | 翻译 ID | UUID 格式 |
| sourceSql | String | Yes | 源 SQL 语句 | 非空，有效的 SQL 语法 |
| sourceDialect | SqlDialect | Yes | 源方言 | 枚举：MYSQL、POSTGRESQL、ORACLE、SQLSERVER、HIVE |
| targetDialect | SqlDialect | Yes | 目标方言 | 枚举：MYSQL、POSTGRESQL、ORACLE、SQLSERVER、HIVE |
| mode | TranslationMode | Yes | 翻译模式 | 枚举：ONLINE（带元数据）、OFFLINE（仅语法） |
| targetSql | String | No | 翻译后的 SQL | 翻译成功后设置 |
| unsupportedFeatures | List<String> | No | 目标不支持的功能 | 翻译过程中填充 |
| error | TranslationError | No | 错误信息 | 失败时设置 |

**Lombok 注解**：
```java
@Getter
@Builder
public class Translation {
    private final String id;
    private final String sourceSql;
    private final SqlDialect sourceDialect;
    private final SqlDialect targetDialect;
    private final TranslationMode mode;
    private String targetSql;
    private List<String> unsupportedFeatures;
    private TranslationError error;

    public boolean isSuccessful() {
        return targetSql != null && error == null;
    }
}
```

**验证规则**：
- `sourceDialect` 和 `targetDialect` 必须不同
- ONLINE 模式需要数据库连接以获取元数据
- OFFLINE 模式可能产生不太准确的翻译

---

### 7. Connection (客户端连接会话)

**Purpose**: 表示 JDBC 客户端连接会话

**Package**: `org.intellisql.server`

**字段**：

| Field | Type | Required | Description | Validation |
|-------|------|----------|-------------|------------|
| id | String | Yes | 连接 ID | UUID 格式 |
| clientId | String | No | 客户端标识符 | 可选的客户端提供的 ID |
| status | ConnectionStatus | Yes | 连接状态 | 枚举：ACTIVE、IDLE、CLOSED |
| createdAt | Instant | Yes | 连接创建时间 | 自动设置 |
| lastActivityAt | Instant | Yes | 最后活动时间戳 | 每次查询时更新 |
| queryCount | long | Yes | 执行的查询数 | 每次查询递增 |
| properties | Map<String, String> | No | 连接属性 | 客户端提供的设置 |

**Lombok 注解**：
```java
@Getter
@Builder
public class Connection {
    private final String id;
    private final String clientId;
    private ConnectionStatus status;
    private final Instant createdAt;
    private Instant lastActivityAt;
    private long queryCount;
    private final Map<String, String> properties;

    public void recordActivity() {
        this.lastActivityAt = Instant.now();
        this.queryCount++;
    }

    public void close() {
        this.status = ConnectionStatus.CLOSED;
    }
}
```

**状态转换**：
```
[活跃] <-> [空闲]
    |
    v
[已关闭]
```

**验证规则**：
- 连接 ID 必须唯一
- 最多 100 个并发连接（SC-008）
- 空闲连接应在可配置的时间段后超时

---

### 8. SchemaMapping (模式映射)

**Purpose**: 定义外部模式如何映射到 IntelliSql 模式

**Package**: `org.intellisql.kernel.metadata`

**字段**：

| Field | Type | Required | Description | Validation |
|-------|------|----------|-------------|------------|
| externalSchema | String | Yes | 外部模式名称 | 非空 |
| internalSchema | String | Yes | 内部（IntelliSql）模式名称 | 非空 |
| tableMappings | Map<String, String> | No | 表名映射 | 键：外部，值：内部 |
| columnMappings | Map<String, ColumnMapping> | No | 列级映射 | 复杂映射规则 |

**Lombok 注解**：
```java
@Getter
@RequiredArgsConstructor
@Builder
public final class SchemaMapping {
    private final String externalSchema;
    private final String internalSchema;
    private final Map<String, String> tableMappings;
    private final Map<String, ColumnMapping> columnMappings;
}
```

---

### 9. ExecutionPlan (执行计划)

**Purpose**: 表示查询的优化执行计划

**Package**: `org.intellisql.optimizer`

**字段**：

| Field | Type | Required | Description | Validation |
|-------|------|----------|-------------|------------|
| id | String | Yes | 计划 ID | UUID 格式 |
| queryId | String | Yes | 父查询 ID | 引用 Query.id |
| stages | List<ExecutionStage> | Yes | 执行阶段 | 有序的操作列表 |
| estimatedCost | long | Yes | 预估执行成本 | 由优化器计算 |
| pushdownPredicates | List<String> | Yes | 下推到数据源的谓词 | WHERE 子句组件 |
| pushdownProjections | List<String> | Yes | 下推到数据源的投影 | SELECT 子句列 |
| intermediateResultLimit | Integer | Yes | 最大中间结果行数 | 默认：100000（NFR-010） |

**Lombok 注解**：
```java
@Getter
@RequiredArgsConstructor
@Builder
public final class ExecutionPlan {
    private final String id;
    private final String queryId;
    private final List<ExecutionStage> stages;
    private final long estimatedCost;
    private final List<String> pushdownPredicates;
    private final List<String> pushdownProjections;
    private final Integer intermediateResultLimit;
}
```

---

### 10. QueryResult (查询结果)

**Purpose**: 表示查询执行的结果

**Package**: `org.intellisql.kernel.executor`

**字段**：

| Field | Type | Required | Description | Validation |
|-------|------|----------|-------------|------------|
| queryId | String | Yes | 查询 ID | 引用 Query.id |
| success | boolean | Yes | 成功标志 | 查询成功时为 true |
| resultSet | Iterator<Row> | Conditional | 结果行 | success=true 时存在 |
| columnMetadata | List<ColumnMetadata> | Conditional | 列信息 | success=true 时存在 |
| rowCount | long | Conditional | 总行数 | success=true 时存在 |
| truncated | boolean | No | 结果截断标志 | 达到中间限制时为 true |
| warning | String | No | 警告消息 | truncated=true 时存在 |
| error | QueryError | Conditional | 错误信息 | success=false 时存在 |
| executionTimeMs | long | Yes | 执行持续时间 | ≥ 0 |
| retryCount | int | No | 重试次数 | 0-3（NFR-007） |

**Lombok 注解**：
```java
@Getter
@Builder
public class QueryResult {
    private final String queryId;
    private final boolean success;
    private final Iterator<Row> resultSet;
    private final List<ColumnMetadata> columnMetadata;
    private final long rowCount;
    private final boolean truncated;
    private final String warning;
    private final QueryError error;
    private final long executionTimeMs;
    private final int retryCount;

    public static QueryResult success(String queryId, Iterator<Row> resultSet,
                                      List<ColumnMetadata> columns, long rowCount) {
        return QueryResult.builder()
            .queryId(queryId)
            .success(true)
            .resultSet(resultSet)
            .columnMetadata(columns)
            .rowCount(rowCount)
            .build();
    }

    public static QueryResult truncated(String queryId, Iterator<Row> resultSet,
                                       List<ColumnMetadata> columns, long rowCount,
                                       String warning) {
        return QueryResult.builder()
            .queryId(queryId)
            .success(true)
            .resultSet(resultSet)
            .columnMetadata(columns)
            .rowCount(rowCount)
            .truncated(true)
            .warning(warning)
            .build();
    }

    public static QueryResult failure(String queryId, QueryError error, int retryCount) {
        return QueryResult.builder()
            .queryId(queryId)
            .success(false)
            .error(error)
            .retryCount(retryCount)
            .build();
    }
}
```

---

## 值对象

### ConnectionPoolConfig (连接池配置)

**Purpose**: HikariCP 连接池配置

**字段**：
- `maximumPoolSize`: int (default: 20)
- `minimumIdle`: int (default: 5)
- `connectionTimeout`: long (default: 30000ms)
- `idleTimeout`: long (default: 600000ms)
- `maxLifetime`: long (default: 1800000ms)

**验证**：所有值必须为正数；maximumPoolSize ≤ 100

---

### HealthCheckConfig (健康检查配置)

**Purpose**: 数据源健康检查配置

**字段**：
- `enabled`: boolean (default: true)
- `intervalSeconds`: int (default: 30)
- `timeoutSeconds`: int (default: 5)
- `failureThreshold`: int (default: 3)

**验证**：intervalSeconds ≥ 5，timeoutSeconds < intervalSeconds

---

### QueryError (查询错误)

**Purpose**: 结构化错误信息

**字段**：
- `code`: String（错误代码，如 "CONN_TIMEOUT"）
- `message`: String（人类可读的消息）
- `dataSourceId`: String（可选，哪个数据源失败）
- `retryable`: boolean（可根据 NFR-009 重试）
- `stackTrace`: String（可选，用于调试）

---

### ColumnMetadata (列元数据)

**Purpose**: 结果集列的元数据

**字段**：
- `name`: String
- `label`: String（显示名称）
- `dataType`: DataType
- `nullable`: boolean
- `precision`: int
- `scale`: int

---

### Row (行数据)

**Purpose**: 表示结果集中的单行

**字段**：
- `values`: Object[]（列值）

**方法**：
- `getObject(int index)`: 按列索引获取值
- `getObject(String columnName)`: 按列名获取值
- `getString(int index)`: 获取值作为 String
- `getLong(int index)`: 获取值作为 Long
- 等。

---

## 枚举

### DataSourceType

```java
public enum DataSourceType {
    MYSQL,
    POSTGRESQL,
    ELASTICSEARCH
}
```

### SchemaType

```java
public enum SchemaType {
    PHYSICAL,  // 映射到真实数据源
    FEDERATED  // 虚拟联邦模式
}
```

### TableType

```java
public enum TableType {
    TABLE,  // 普通表
    VIEW,   // 数据库视图
    INDEX   // Elasticsearch 索引
}
```

### DataType

```java
public enum DataType {
    STRING,
    INTEGER,
    LONG,
    DOUBLE,
    BOOLEAN,
    DATE,
    TIMESTAMP,
    BINARY,
    JSON,
    ARRAY
}
```

### QueryStatus

```java
public enum QueryStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
```

### SqlDialect

```java
public enum SqlDialect {
    MYSQL,
    POSTGRESQL,
    ORACLE,
    SQLSERVER,
    HIVE,
    STANDARD  // 标准 SQL
}
```

### TranslationMode

```java
public enum TranslationMode {
    ONLINE,   // 带数据库元数据
    OFFLINE   // 仅语法翻译
}
```

### QueryStatus

```java
public enum ConnectionStatus {
    ACTIVE,
    IDLE,
    CLOSED
}
```

---

## 实体关系图

```
┌─────────────┐
│ DataSource  │
└──────┬──────┘
       │ 1
       │
       │ N
┌──────┴──────┐       ┌──────────┐
│   Schema    ├───────┤ Schema   │
└──────┬──────┘   N   │ Mapping  │
       │ 1            └──────────┘
       │
       │ N
┌──────┴──────┐       ┌──────────┐
│    Table    ├───────┤  Index   │
└──────┬──────┘   N   └──────────┘
       │ 1
       │
       │ N
┌──────┴──────┐
│   Column    │
└─────────────┘

┌─────────────┐
│ Connection  │
└──────┬──────┘
       │ 1
       │
       │ N
┌──────┴──────┐       ┌────────────────┐
│    Query    ├───────┤ ExecutionPlan  │
└──────┬──────┘   1   └────────────────┘
       │ 1
       │
       │ 1
┌──────┴──────┐
│ QueryResult │
└─────────────┘

┌─────────────┐
│ Translation │
└─────────────┘
```

---

## 配置模式

### model.yaml Structure

```yaml
dataSources:
  mysql-orders:
    type: MYSQL
    url: jdbc:mysql://localhost:3306/orders
    username: app_user
    password: ${DB_PASSWORD}
    maximumPoolSize: 20
    minimumIdle: 5
    healthCheckIntervalSeconds: 30

  es-logs:
    type: ELASTIC_SEARCH
    url: http://localhost:9200
    username: elastic
    password: ${ES_PASSWORD}
    healthCheckIntervalSeconds: 60

props:
  maxIntermediateRows: 100000
  queryTimeoutSeconds: 300
  defaultFetchSize: 1000
```

---

## 验证摘要

### 跨实体规则

1. **DataSource 完整性**：所有 Schema.dataSourceId 必须引用有效的 DataSource.id
2. **Schema 完整性**：所有 Table.schemaName 必须引用有效的 Schema.name
3. **Table 完整性**：primaryKey 中的所有 Column 引用必须存在于 columns 中
4. **查询限制**：跨源查询必须遵守 intermediateResultLimit（NFR-010）
5. **连接限制**：活动连接不得超过 100（SC-008）
6. **重试限制**：重试次数不得超过 3（NFR-007）

### 数据完整性

1. **级联删除**：不适用（无持久化存储）
2. **引用完整性**：在查询规划期间运行时验证
3. **类型安全**：所有类型转换必须显式（除 NFR-005 外无隐式转换）

---

## 下一步

1. ✅ 数据模型已定义
2. ⏭️ 生成 contracts/ 目录及 API 规范
3. ⏭️ 为开发者生成 quickstart.md
4. ⏭️ 更新代理上下文
