# 配置模式契约

**Version**: 1.0.0
**Date**: 2026-02-17
**Feature**: 001-sql-federation-translation

本文档规定了 IntelliSql 的 YAML 配置模式，包括数据源定义、连接设置和环境变量替换。

## 配置文件位置

**Default Path**（默认路径）: `./conf/model.yaml`

**Override**（覆盖）: 通过系统属性指定
```bash
java -Dintellisql.config.path=/path/to/config.yaml -jar intellisql-server.jar
```

## 模式结构

### 根对象

```yaml
dataSources:
  <datasource-id>:
    type: <TYPE>
    url: <URL>
    ...
props:
  ...
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| dataSources | Map<String, DataSource> | Yes | 数据源配置映射（key 为数据源 ID） |
| props | Props | No | 全局属性配置（提供默认值） |

---

## 数据源配置

### 完整示例

```yaml
dataSources:
  mysql-orders:
    type: MYSQL
    url: jdbc:mysql://localhost:3306/orders?useSSL=false&serverTimezone=UTC
    username: app_user
    password: ${MYSQL_PASSWORD}
    maximumPoolSize: 20
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000
    healthCheckIntervalSeconds: 30
    healthCheckTimeoutSeconds: 5
    healthCheckFailureThreshold: 3

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
  enableQueryLogging: true
  logLevel: INFO
```

### 数据源字段

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| type | String | Yes | 数据源类型：`MYSQL`、`POSTGRESQL`、`ELASTIC_SEARCH` |
| url | String | Yes | 连接 URL（JDBC 或 HTTP） |
| username | String | Conditional | 数据库用户名（MYSQL、POSTGRESQL 必需） |
| password | String | Conditional | 数据库密码（支持环境变量替换） |
| maximumPoolSize | Integer | No | 连接池中的最大连接数（默认 20） |
| minimumIdle | Integer | No | 最小空闲连接数（默认 5） |
| connectionTimeout | Long | No | 连接超时时间（毫秒，默认 30000） |
| idleTimeout | Long | No | 空闲连接超时时间（毫秒，默认 600000） |
| maxLifetime | Long | No | 最大连接生存时间（毫秒，默认 1800000） |
| healthCheckIntervalSeconds | Integer | No | 健康检查间隔（秒，> 0 时开启，默认 0 关闭） |
| healthCheckTimeoutSeconds | Integer | No | 健康检查超时时间（秒，默认 5） |
| healthCheckFailureThreshold | Integer | No | 标记为不健康前的失败次数（默认 3） |

### 验证规则

1. **type**：必须是 `MYSQL`、`POSTGRESQL`、`ELASTIC_SEARCH` 之一
2. **url**：
   - MYSQL：`jdbc:mysql://host:port/database?options`
   - POSTGRESQL：`jdbc:postgresql://host:port/database?options`
   - ELASTIC_SEARCH：`http://host:port` 或 `https://host:port`
3. **username**：MYSQL 和 POSTGRESQL 必需
4. **password**：支持通过 `${ENV_VAR}` 语法进行环境变量替换
5. **maximumPoolSize**：必须 > 0 且 ≤ 100
6. **minimumIdle**：必须 ≤ maximumPoolSize
7. **healthCheckIntervalSeconds**：> 0 时开启健康检查，= 0 或不设置时关闭

---

## 健康检查配置

### 健康检查逻辑

当 `healthCheckIntervalSeconds > 0` 时，系统自动启用健康检查：

1. 每隔 `healthCheckIntervalSeconds` 秒，执行测试查询：
   - MYSQL：`SELECT 1`
   - POSTGRESQL：`SELECT 1`
   - ELASTIC_SEARCH：`GET /_cluster/health`
2. 如果超时或失败，增加失败计数
3. 如果失败计数 ≥ `healthCheckFailureThreshold`，将数据源标记为不健康
4. 不健康的数据源返回清晰的错误消息
5. 检查成功时健康数据源自动恢复

### 关闭健康检查

设置 `healthCheckIntervalSeconds: 0` 或不设置该字段：

```yaml
dataSources:
  mysql-orders:
    type: MYSQL
    url: jdbc:mysql://localhost:3306/orders
    username: root
    password: ""
    healthCheckIntervalSeconds: 0  # 关闭健康检查
```

---

## 数据源类型规范

### MySQL 配置

```yaml
dataSources:
  mysql-prod:
    type: MYSQL
    url: jdbc:mysql://host:3306/database?useSSL=true&serverTimezone=UTC&useServerPrepStmts=true
    username: user
    password: ${MYSQL_PASSWORD}
    maximumPoolSize: 20
    minimumIdle: 5
    healthCheckIntervalSeconds: 30
    healthCheckTimeoutSeconds: 5
    healthCheckFailureThreshold: 3
```

**必需的 URL 参数**：
- `useSSL`：生产环境设置为 `true`（加密连接）
- `serverTimezone`：JDK 8+ 时区处理所需

**推荐的 URL 参数**：
- `useServerPrepStmts=true`：使用服务器端准备语句
- `cachePrepStmts=true`：缓存准备语句
- `prepStmtCacheSize=250`：语句缓存大小

---

### PostgreSQL 配置

```yaml
dataSources:
  postgres-prod:
    type: POSTGRESQL
    url: jdbc:postgresql://host:5432/database?sslmode=require&prepareThreshold=0
    username: user
    password: ${PG_PASSWORD}
    maximumPoolSize: 20
    minimumIdle: 5
    healthCheckIntervalSeconds: 30
```

**必需的 URL 参数**：
- `sslmode`：生产环境设置为 `require` 或 `verify-full`

---

### Elasticsearch 配置

```yaml
dataSources:
  es-logs:
    type: ELASTIC_SEARCH
    url: http://localhost:9200
    username: elastic
    password: ${ES_PASSWORD}
    healthCheckIntervalSeconds: 60
    healthCheckTimeoutSeconds: 10
```

**连接选项**：
- HTTP：`http://host:9200`
- HTTPS：`https://host:9200`（生产环境推荐）

**类型映射规则**：
- `keyword` → STRING
- `text` → STRING
- `long` → LONG
- `integer` → INTEGER
- `double` → DOUBLE
- `boolean` → BOOLEAN
- `date` → TIMESTAMP
- `nested` → JSON
- `object` → JSON

---

## 环境变量替换

### 语法

在配置值的任何位置使用 `${ENVIRONMENT_VARIABLE_NAME}`：

```yaml
dataSources:
  mysql-orders:
    type: MYSQL
    url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
```

### 替换规则

1. 模式：`${VAR_NAME}`，其中 VAR_NAME 是环境变量名称
2. 如果未设置环境变量，替换将失败并返回错误
3. 替换适用于所有字符串值（不包括键或数字）
4. 不支持嵌套替换：`${${NESTED}}` 将不起作用

### 安全最佳实践

1. **永不提交密码**：对所有密钥使用环境变量
2. **记录所需变量**：在 README 中列出所有必需的环境变量
3. **启动时验证**：如果缺少必需的环境变量，快速失败
4. **日志中掩码**：永不记录解析后的密码值

---

## 全局属性配置

### Props 字段

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| maxIntermediateRows | Long | No | 100000 | 中间结果的最大行数（NFR-010） |
| queryTimeoutSeconds | Integer | No | 300 | 默认查询超时时间 |
| defaultFetchSize | Integer | No | 1000 | 默认 JDBC 获取大小 |
| enableQueryLogging | Boolean | No | true | 记录查询执行详细信息 |
| logLevel | String | No | INFO | 日志级别：DEBUG、INFO、WARN、ERROR |

### Example

```yaml
props:
  maxIntermediateRows: 100000
  queryTimeoutSeconds: 300
  defaultFetchSize: 1000
  enableQueryLogging: true
  logLevel: INFO
```

---

## 配置验证

### 启动时验证

服务器启动时验证配置：

1. **解析 YAML**：确保有效的 YAML 语法
2. **模式验证**：根据 YAML 模式验证
3. **环境变量替换**：替换所有 `${VAR}` 占位符
4. **跨字段验证**：检查引用完整性
5. **连接测试**：测试每个数据源连接
6. **快速失败**：在启动服务器之前报告所有错误

### 验证错误

| Error Code | Message | Resolution |
|------------|---------|------------|
| CONFIG_PARSE_ERROR | YAML 解析失败 | 修复 YAML 语法错误 |
| CONFIG_SCHEMA_ERROR | 无效模式 | 检查字段类型和必需字段 |
| ENV_VAR_MISSING | 未找到环境变量：{VAR_NAME} | 设置缺失的环境变量 |
| DS_INVALID_TYPE | 无效的数据源类型：{type} | 使用 MYSQL、POSTGRESQL 或 ELASTIC_SEARCH |
| DS_CONNECTION_FAILED | 无法连接到数据源：{id} | 检查 URL、凭证、网络连接 |
| POOL_CONFIG_INVALID | 无效的连接池配置 | 检查连接池大小和超时值 |

---

## 示例配置

### 开发环境设置

```yaml
dataSources:
  dev-mysql:
    type: MYSQL
    url: jdbc:mysql://localhost:3306/dev_db?useSSL=false
    username: root
    password: ""
    maximumPoolSize: 5
    minimumIdle: 2

  dev-es:
    type: ELASTIC_SEARCH
    url: http://localhost:9200

props:
  maxIntermediateRows: 10000
  enableQueryLogging: true
  logLevel: DEBUG
```

### 生产环境设置

```yaml
dataSources:
  prod-mysql:
    type: MYSQL
    url: jdbc:mysql://prod-db.example.com:3306/prod_db?useSSL=true&serverTimezone=UTC
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
    maximumPoolSize: 50
    minimumIdle: 10
    connectionTimeout: 30000
    maxLifetime: 1800000
    healthCheckIntervalSeconds: 30
    healthCheckTimeoutSeconds: 5
    healthCheckFailureThreshold: 3

  prod-pg:
    type: POSTGRESQL
    url: jdbc:postgresql://prod-pg.example.com:5432/analytics?sslmode=require
    username: ${PG_USER}
    password: ${PG_PASSWORD}
    maximumPoolSize: 30
    minimumIdle: 5
    healthCheckIntervalSeconds: 30

  prod-es:
    type: ELASTIC_SEARCH
    url: https://prod-es.example.com:9200
    username: ${ES_USER}
    password: ${ES_PASSWORD}
    healthCheckIntervalSeconds: 60

props:
  maxIntermediateRows: 100000
  queryTimeoutSeconds: 300
  defaultFetchSize: 5000
  enableQueryLogging: true
  logLevel: INFO
```

---

## YAML 最佳实践

1. **使用 2 空格缩进**（非 tab）
2. **使用 `#` 添加注释**解释配置
3. **字符串通常无需引号**（特殊字符除外）
4. **数据源 ID 使用 kebab-case**（如 `mysql-orders`）

---

## 更新日志

### Version 1.0.0 (2026-02-17)

- 初始配置模式
- 使用 YAML 格式（更好的可读性）
- 扁平化数据源配置结构
- 支持 MySQL、PostgreSQL、Elasticsearch
- 环境变量替换
- 连接池配置（HikariCP 风格）
- 健康检查配置（通过 healthCheckIntervalSeconds 控制）
- 不支持 schemaMappings（后续根据情况再支持）
- 全局属性配置
