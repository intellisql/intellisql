# JDBC 协议契约

**Version**: 1.0.0
**Date**: 2026-02-17
**Feature**: 001-sql-federation-translation

本文档规定了 IntelliSql 的 JDBC 协议契约，包括连接 URL、驱动注册和线协议细节。

## 连接 URL 格式

### 标准格式

```
jdbc:intellisql://<host>:<port>/<database>?<properties>
```

### 示例

```java
// 基本连接
String url = "jdbc:intellisql://localhost:8765/intellisql";

// 带配置属性
String url = "jdbc:intellisql://localhost:8765/intellisql?fetchSize=1000&queryTimeout=300";

// 高可用（未来增强）
String url = "jdbc:intellisql://host1:8765,host2:8765/intellisql?failover=true";
```

### URL 组件

| Component | Required | Default | Description |
|-----------|----------|---------|-------------|
| host | Yes | - | 服务器主机名或 IP 地址 |
| port | No | 8765 | 服务器端口号 |
| database | No | intellisql | 逻辑数据库名（用于兼容性） |
| properties | No | - | 配置用的查询参数 |

### 支持的属性

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| fetchSize | int | 1000 | 结果集的默认获取大小 |
| queryTimeout | int | 300 | 查询超时时间（秒） |
| serialization | String | protobuf | 序列化格式：`protobuf` 或 `json` |
| connectTimeout | int | 30 | 连接超时时间（秒） |
| socketTimeout | int | 60 | Socket 读取超时时间（秒） |
| maxRows | long | 0 | 返回的最大行数（0 = 无限制） |

## 驱动注册

### 驱动类

```java
public final class IntelliSqlDriver implements java.sql.Driver {
    static {
        try {
            java.sql.DriverManager.registerDriver(new IntelliSqlDriver());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register IntelliSqlDriver", e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        // Implementation
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url != null && url.startsWith("jdbc:intellisql:");
    }

    // ... other Driver methods
}
```

### META-INF/services/java.sql.Driver

```
org.intellisql.jdbc.IntelliSqlDriver
```

### 连接流程

```
Client                        Driver                      Server
  |                             |                            |
  |-- 1. DriverManager.getConnection(url) -->|               |
  |                             |                            |
  |                             |-- 2. Driver.connect(url) ->|
  |                             |                            |
  |                             |       3. HTTP POST /connect
  |                             |          (Protobuf)      -->|
  |                             |                            |
  |                             |<- 4. ConnectionResponse ---|
  |                             |    (connectionId, sessionId)|
  |                             |                            |
  |<-- 5. Connection object ----|                            |
  |                             |                            |
```

## 线协议（基于 HTTP 的 Avatica）

### 基础协议

IntelliSql 使用 Apache Avatica 的 JSON/Protobuf 协议，基于 HTTP 传输。默认的序列化格式是 **Protobuf**（根据 NFR-011），以获得更好的性能。

### HTTP 端点

```
POST http://<host>:<port>/api/protobuf
Content-Type: application/x-protobuf
```

### 请求格式（Protobuf）

所有请求都封装在 `Service.Request` protobuf 消息中：

```protobuf
message Request {
  oneof request {
    OpenConnectionRequest open_connection = 1;
    CloseConnectionRequest close_connection = 2;
    CreateStatementRequest create_statement = 3;
    CloseStatementRequest close_statement = 4;
    PrepareRequest prepare = 5;
    ExecuteRequest execute = 6;
    FetchRequest fetch = 7;
    // ... other request types
  }
}
```

### 响应格式（Protobuf）

所有响应都封装在 `Service.Response` protobuf 消息中：

```protobuf
message Response {
  oneof response {
    OpenConnectionResponse open_connection = 1;
    CloseConnectionResponse close_connection = 2;
    CreateStatementResponse create_statement = 3;
    CloseStatementResponse close_statement = 4;
    PrepareResponse prepare = 5;
    ExecuteResponse execute = 6;
    FetchResponse fetch = 7;
    ErrorResponse error = 100;
    // ... other response types
  }
}
```

## 核心操作

### 1. 打开连接

**Request**（请求）: `OpenConnectionRequest`
```protobuf
message OpenConnectionRequest {
  string connection_id = 1;
  map<string, string> info = 2;  // Connection properties
}
```

**Response**（响应）: `OpenConnectionResponse`
```protobuf
message OpenConnectionResponse {
  MetaData metadata = 1;  // Server metadata
}
```

**示例**：
```java
Properties props = new Properties();
props.setProperty("user", "app");
props.setProperty("password", "${DB_PASSWORD}");

Connection conn = DriverManager.getConnection(
    "jdbc:intellisql://localhost:8765/intellisql",
    props
);
```

### 2. 创建语句

**Request**（请求）: `CreateStatementRequest`
```protobuf
message CreateStatementRequest {
  string connection_id = 1;
}
```

**Response**（响应）: `CreateStatementResponse`
```protobuf
message CreateStatementResponse {
  string statement_id = 1;
}
```

### 3. 准备语句

**Request**（请求）: `PrepareRequest`
```protobuf
message PrepareRequest {
  string connection_id = 1;
  string sql = 2;
  int64 max_row_count = 3;  // Max rows to return (0 = unlimited)
}
```

**Response**（响应）: `PrepareResponse`
```protobuf
message PrepareResponse {
  StatementHandle statement = 1;
  Meta.Signature signature = 2;  // Parameter metadata
}
```

### 4. 执行查询

**Request**（请求）: `ExecuteRequest`
```protobuf
message ExecuteRequest {
  StatementHandle statementHandle = 1;
  repeated TypedValue parameter_values = 2;
  int64 first_frame_max_size = 3;  // Max rows in first frame
}
```

**Response**（响应）: `ExecuteResponse`
```protobuf
message ExecuteResponse {
  Meta.StatementHandle statementHandle = 1;
  bool missing_statement = 2;
  Meta.Frame frame = 3;  // First batch of results
  Meta.Signature signature = 4;  // Result set metadata
  bool update_count = 5;  // For DML statements
}
```

**示例**：
```java
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery("SELECT * FROM orders WHERE status = 'completed'");

// 或使用 PreparedStatement
PreparedStatement pstmt = conn.prepareStatement(
    "SELECT * FROM orders WHERE status = ?"
);
pstmt.setString(1, "completed");
ResultSet rs = pstmt.executeQuery();
```

### 5. 获取结果

**Request**（请求）: `FetchRequest`
```protobuf
message FetchRequest {
  string connection_id = 1;
  string statement_id = 2;
  int64 offset = 3;  // Starting row offset
  int32 fetch_max_row_count = 4;  // Max rows to fetch
}
```

**Response**（响应）: `FetchResponse`
```protobuf
message FetchResponse {
  Meta.Frame frame = 1;  // Result batch
  bool missing_statement = 2;
}
```

**示例**：
```java
// JDBC 驱动自动获取批次
while (rs.next()) {
    // 处理每一行
    String orderId = rs.getString("id");
    // ...
}

// 显式控制获取大小
stmt.setFetchSize(5000);  // 每批获取 5000 行
```

### 6. 关闭语句

**Request**（请求）: `CloseStatementRequest`
```protobuf
message CloseStatementRequest {
  string connection_id = 1;
  string statement_id = 2;
}
```

**Response**（响应）: `CloseStatementResponse`
```protobuf
message CloseStatementResponse {
  // 空 - 无错误表示成功
}
```

### 7. 关闭连接

**Request**（请求）: `CloseConnectionRequest`
```protobuf
message CloseConnectionRequest {
  string connection_id = 1;
}
```

**Response**（响应）: `CloseConnectionResponse`
```protobuf
message CloseConnectionResponse {
  // 空 - 无错误表示成功
}
```

**示例**：
```java
try (Connection conn = DriverManager.getConnection(url);
     Statement stmt = conn.createStatement();
     ResultSet rs = stmt.executeQuery(sql)) {
    // 处理结果
} // 通过 try-with-resources 自动关闭
```

## 错误处理

### 错误响应

**Response**（响应）: `ErrorResponse`
```protobuf
message ErrorResponse {
  repeated Rpc.Metadata.PbSeverity.pbSeverity severity = 1;
  int32 error_code = 2;
  string error_message = 3;
  string sql_state = 4;  // SQLSTATE code
  int32 vendor_code = 5;  // Vendor-specific error code
  repeated string stack_traces = 6;
}
```

### 错误码

| Error Code | SQL State | Description | Retryable |
|------------|-----------|-------------|-----------|
| 08001 | 08001 | 连接失败 | Yes (NFR-006) |
| 08004 | 08004 | 连接被拒绝 | No |
| 08S01 | 08S01 | 通信链路失败 | Yes |
| 22000 | 22000 | 数据异常 | No |
| 23000 | 23000 | 完整性约束违规 | No |
| 42000 | 42000 | 语法错误或访问规则违规 | No |
| 54000 | 54000 | 超出程序限制（如中间结果限制） | No |
| HY000 | HY000 | 一般错误 | Depends |
| HYT00 | HYT00 | 超时已过期 | Yes |

### 重试逻辑

根据 NFR-006 到 NFR-009，驱动应自动重试：
- 连接失败（错误码 08001、08S01）
- 超时错误（错误码 HYT00）
- 网络错误

**重试策略**：
- 最大重试次数：3
- 延迟：1000ms、2000ms、4000ms（指数退避）
- 仅重试幂等操作

### 错误处理示例

```java
try {
    ResultSet rs = stmt.executeQuery(sql);
    // 处理结果
} catch (SQLException e) {
    if (isRetryable(e)) {
        // 根据 NFR-006，驱动将自动重试
        log.warn("Transient error (will retry): {}", e.getMessage());
    } else {
        // 永久性错误
        log.error("Permanent error: {}", e.getMessage());
        throw e;
    }
}

private boolean isRetryable(SQLException e) {
    int errorCode = e.getErrorCode();
    return errorCode == 08001 || errorCode == 08004 ||
           errorCode == 08S01 || errorCode == HYT00;
}
```

## 结果集批处理

### 帧结构

结果以帧（批次）形式返回，以支持大型结果集而不会 OOM（SC-009、NFR-013）。

```protobuf
message Frame {
  int64 offset = 1;  // Offset of first row in this frame
  bool done = 2;  // True if this is the last frame
  repeated Row rows = 3;  // Row data
}

message Row {
  repeated TypedValue value = 1;  // Column values
}

message TypedValue {
  PbType type = 1;  // Data type
  bytes value = 2;  // Serialized value
  bool null = 3;  // True if value is NULL
}
```

### 批量获取策略

1. 客户端使用 `ExecuteRequest.first_frame_max_size` 请求第一帧
2. 服务器返回第一帧（例如 1000 行）
3. 客户端遍历行
4. 当客户端到达帧末尾时，驱动自动发送 `FetchRequest` 获取下一帧
5. 重复此过程直到 `Frame.done = true`

### 示例

```java
Statement stmt = conn.createStatement();
stmt.setFetchSize(5000);  // 每批获取 5000 行

ResultSet rs = stmt.executeQuery("SELECT * FROM large_table");

// 内部驱动自动获取帧：
// 帧 1：行 0-4999
// 帧 2：行 5000-9999
// 以此类推

while (rs.next()) {
    // 处理每一行
    // 驱动透明地获取下一帧
}
```

## 数据类型映射

### JDBC 类型到 IntelliSql 类型

| JDBC Type | IntelliSql Type | Protobuf Type |
|-----------|-----------------|---------------|
| CHAR | STRING | string |
| VARCHAR | STRING | string |
| LONGVARCHAR | STRING | string |
| NUMERIC | DOUBLE | double |
| DECIMAL | DOUBLE | double |
| BIT | BOOLEAN | bool |
| BOOLEAN | BOOLEAN | bool |
| TINYINT | INTEGER | int32 |
| SMALLINT | INTEGER | int32 |
| INTEGER | INTEGER | int32 |
| BIGINT | LONG | int64 |
| REAL | DOUBLE | double |
| FLOAT | DOUBLE | double |
| DOUBLE | DOUBLE | double |
| DATE | DATE | int64 (epoch days) |
| TIME | TIMESTAMP | int64 (epoch millis) |
| TIMESTAMP | TIMESTAMP | int64 (epoch millis) |
| BINARY | BINARY | bytes |
| VARBINARY | BINARY | bytes |
| LONGVARBINARY | BINARY | bytes |
| ARRAY | ARRAY | repeated TypedValue |
| STRUCT | JSON | string (JSON) |

### 示例

```java
ResultSet rs = stmt.executeQuery("SELECT id, name, created_at FROM users");

while (rs.next()) {
    String id = rs.getString("id");           // VARCHAR -> STRING
    String name = rs.getString("name");       // VARCHAR -> STRING
    Timestamp created = rs.getTimestamp("created_at"); // TIMESTAMP -> TIMESTAMP
}
```

## 元数据操作

### 获取数据库元数据

```java
DatabaseMetaData meta = conn.getMetaData();

// 获取模式（数据源）
ResultSet schemas = meta.getSchemas();
while (schemas.next()) {
    String schema = schemas.getString("TABLE_SCHEM");
    // 例如，"mysql_db"、"es_logs"
}

// 获取表
ResultSet tables = meta.getTables("mysql_db", null, "%", null);
while (tables.next()) {
    String tableName = tables.getString("TABLE_NAME");
    String tableType = tables.getString("TABLE_TYPE"); // "TABLE"、"VIEW"
}

// 获取列
ResultSet columns = meta.getColumns("mysql_db", null, "orders", "%");
while (columns.next()) {
    String columnName = columns.getString("COLUMN_NAME");
    String typeName = columns.getString("TYPE_NAME");
    int dataType = columns.getInt("DATA_TYPE");
}
```

## 连接池

### HikariCP 集成

```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:intellisql://localhost:8765/intellisql");
config.setMaximumPoolSize(20);
config.setMinimumIdle(5);
config.setConnectionTimeout(30000);

HikariDataSource ds = new HikariDataSource(config);

try (Connection conn = ds.getConnection()) {
    // 使用连接
}
```

## 安全考虑

### 无身份验证（MVP）

根据规格说明，MVP 不支持身份验证。驱动假定在可信网络环境中部署。

**未来增强**：
```java
Properties props = new Properties();
props.setProperty("user", "admin");
props.setProperty("password", "${DB_PASSWORD}");
Connection conn = DriverManager.getConnection(url, props);
```

### 密码掩码

驱动必须在以下位置掩码密码：
- 日志输出
- 错误消息
- 调试跟踪
- URL 字符串（记录时）

```java
// 错误：密码暴露在日志中
log.info("Connecting to: {}", url);  // 不要这样做

// 正确：掩码密码
log.info("Connecting to server at {}:{}", host, port);
```

## 性能特征

### 往返时间

| Operation | Expected RTT | Notes |
|-----------|-------------|-------|
| Connect | 10-50ms | 包括连接池设置 |
| Prepare | 5-20ms | SQL 解析和验证 |
| Execute (first frame) | 50-500ms | 取决于查询复杂度 |
| Fetch (subsequent frames) | 10-100ms | 取决于批大小 |
| Close | 1-5ms | 清理 |

### 吞吐量

- 单连接：1000+ 查询/秒（简单查询）
- 并发连接：支持 100 个连接（SC-008）
- 大结果集：100 万行不 OOM（SC-009）

## 合规性

### JDBC 4.2 合规性

IntelliSql 驱动实现了 JDBC 4.2 规范，说明如下：

- ✅ 核心 JDBC 接口（Connection、Statement、ResultSet 等）
- ✅ 支持参数化查询的 PreparedStatement
- ✅ 用于模式发现的 DatabaseMetaData
- ✅ ResultSet 滚动（TYPE_FORWARD_ONLY）
- ✅ 批量更新（未来增强）
- ❌ CallableStatement（不支持 - 无存储过程）
- ❌ SQLXML 类型（MVP 中不支持）

### SQL 合规性

- SQL:2003 标准语法（通过 Apache Calcite）
- 针对 MySQL、PostgreSQL、Oracle、SQL Server、Hive 的特定扩展

## 版本兼容性

| Driver Version | Server Version | Protocol Version | JDK Version |
|----------------|----------------|------------------|-------------|
| 1.0.x | 1.0.x | 1.0 | JDK 8+ |
| (future) | (future) | 1.1 | JDK 8+ |

**兼容性规则**：
- 驱动和服务器的主版本必须匹配
- 服务器可以支持较旧的协议版本（向后兼容）
- 驱动应在较新的服务器版本上优雅地失败

## 测试要求

### 单元测试（JUnit 5）

- 驱动注册
- URL 解析
- 连接创建
- 语句执行（模拟）
- 结果集迭代（模拟）

### 集成测试（TestContainers）

- 真实服务器连接
- 针对真实数据源的查询执行
- 错误处理和重试逻辑
- 大结果集获取

### 性能测试

- 连接池吞吐量
- 负载下的查询延迟
- 并发连接处理
- 大结果集处理（100 万行）

## 更新日志

### Version 1.0.0 (2026-02-17)

- 初始 JDBC 驱动实现
- 自定义 `jdbc:intellisql` URL 方案
- Protobuf 序列化（默认）
- 无身份验证（MVP）
- 支持 MySQL、PostgreSQL、Elasticsearch 数据源
- 大结果集的批量获取
- 指数退避重试逻辑
