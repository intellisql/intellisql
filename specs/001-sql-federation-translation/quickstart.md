# IntelliSql 快速入门指南

**Version**: 1.0.0
**Date**: 2026-02-17
**Feature**: 001-sql-federation-translation

本指南帮助您在 10 分钟内开始 IntelliSql 开发。

## 前置条件

### 必需软件

| Software | Version | Purpose |
|----------|---------|---------|
| JDK | 8 or later | Java 开发工具包 |
| Maven | 3.6+ (via mvnw) | 构建工具 |
| Docker | Latest | 用于集成测试的 TestContainers |
| Git | Latest | 版本控制 |
| IntelliJ IDEA | 2023+ (recommended) | 集成开发环境 |

### 验证前置条件

```bash
# Check Java version (must be 8+)
java -version

# Check Docker is running
docker ps

# Check Git
git --version
```

## 快速开始

### 1. 克隆并构建（2 分钟）

```bash
# Clone repository
git clone https://github.com/your-org/intellisql.git
cd intellisql

# Build project (first build downloads dependencies)
./mvnw clean install -DskipTests

# [Optional] Build isql native image (Requires GraalVM, ~5 mins)
# ./mvnw -Pnative -pl intellisql-client package

# Expected output:
# [INFO] BUILD SUCCESS
# [INFO] Total time:  02:15 min
```

### 2. 配置数据源（2 分钟）

创建 `conf/model.yaml`：

```yaml
dataSources:
  dev-mysql:
    type: MYSQL
    url: jdbc:mysql://localhost:3306/dev_db?useSSL=false
    username: root
    password: ""
    maximumPoolSize: 10
```

### 3. 启动服务器（1 分钟）

```bash
# Start IntelliSql Server
./bin/start.sh

# Check server is running
curl http://localhost:8765/health

# Expected output:
# {"status":"UP"}
```

### 4. 连接并查询（5 分钟）

#### 使用 JDBC

```java
import java.sql.*;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        // Connect to IntelliSql
        String url = "jdbc:intellisql://localhost:8765/intellisql";
        try (Connection conn = DriverManager.getConnection(url)) {
            // Execute cross-source query
            String sql = "SELECT * FROM dev_mysql.orders LIMIT 10";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    System.out.println(rs.getString("id"));
                }
            }
        }
    }
}
```

#### 使用 isql CLI

```bash
# Start isql client (JVM mode)
./bin/isql

# OR Start native executable (if built)
# ./intellisql-client/target/isql

# Execute queries
isql> SHOW SCHEMAS;
+-------------------+
| SCHEMA_NAME       |
+-------------------+
| dev_mysql         |
+-------------------+

isql> SELECT * FROM dev_mysql.orders LIMIT 5;
+----+----------+-------+
| ID | CUSTOMER | TOTAL |
+----+----------+-------+
| 1  | Alice    | 99.99 |
| 2  | Bob      | 149.99|
+----+----------+-------+

isql> exit
```

## 项目结构

```
intellisql/
├── intellisql-common/              # Common infrastructure (config, logging, retry, metadata entities)
├── intellisql-parser/              # SQL parsing
├── intellisql-features/            # Feature modules parent
│   ├── intellisql-feature-optimizer/       # Query optimization (RBO + CBO)
│   ├── intellisql-feature-translator/      # SQL translation
│   └── intellisql-feature-federation/      # Federation query core (kernel + executor)
├── intellisql-connector/           # Data source adapters
│   ├── src/main/java/
│   │   ├── api/                    # Connector SPI
│   │   ├── mysql/                  # MySQL adapter
│   │   ├── postgresql/             # PostgreSQL adapter
│   │   └── elasticsearch/          # Elasticsearch adapter
├── intellisql-jdbc/                # JDBC driver
├── intellisql-server/              # Server implementation (Avatica protocol)
├── intellisql-client/              # isql CLI
├── intellisql-distributions/       # Packaging and distribution
│   ├── intellisql-distribution-jdbc/
│   └── intellisql-distribution-server/
├── intellisql-test/                # Testing
│   ├── intellisql-test-it/         # Integration tests
│   └── intellisql-test-e2e/        # End-to-end tests
└── conf/
    └── model.yaml                  # Configuration (YAML format)
```

## 开发工作流

### 1. IDE 设置

#### IntelliJ IDEA

1. **导入项目**：
   - File → Open → 选择 `intellisql` 目录
   - 选择 "Import project from external model" → Maven
   - 勾选 "Import Maven projects automatically"

2. **安装 Lombok 插件**：
   - Settings → Plugins → 搜索 "Lombok" → Install
   - 重启 IDEA

3. **启用注解处理**：
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - 勾选 "Enable annotation processing"

4. **导入代码风格**：
   - Settings → Editor → Code Style → Java → Gear icon → Import Scheme
   - 选择 `src/resources/idea/code-style.xml`

5. **导入检查规则**：
   - Settings → Editor → Inspections → Gear icon → Import
   - 选择 `src/resources/idea/inspections.xml`

### 2. 构建命令

```bash
# Quick build (skip tests)
./mvnw clean install -DskipTests

# Full build with tests
./mvnw clean install

# Build with checkstyle
./mvnw clean install -Pcheck

# Apply code formatting
./mvnw spotless:apply

# Check code formatting
./mvnw spotless:check

# Build specific module
./mvnw clean install -pl intellisql-parser
```

### 3. 运行测试

```bash
# Run all tests
./mvnw test

# Run unit tests only
./mvnw test -Dtest=*Test

# Run integration tests
./mvnw test -Dtest=*IT

# Run specific test class
./mvnw test -Dtest=QueryExecutorTest

# Run with TestContainers (requires Docker)
./mvnw test -Dtestcontainers.enabled=true
```

### 4. 代码质量检查

```bash
# Run all quality checks (required before commit)
./mvnw clean install -B -T1C -Pcheck

# This runs:
# 1. Apache license header check
# 2. Checkstyle validation
# 3. Compilation
# 4. Unit tests

# Format code before commit
./mvnw spotless:apply -Pcheck
```

## 常见任务

### 添加新的数据源类型

1. **创建连接器**：
```java
// intellisql-connector/src/main/java/org/intellisql/connector/oracle/
package com.intellisql.connector.oracle;

import com.intellisql.connector.api.DataSourceConnector;

public class OracleConnector implements DataSourceConnector {
    @Override
    public Connection connect(DataSourceConfig config) {
        // Implementation
    }

    @Override
    public Schema discoverSchema() {
        // Implementation
    }
}
```

2. **注册连接器**：
```java
// intellisql-connector/src/main/java/org/intellisql/connector/ConnectorRegistry.java
public class ConnectorRegistry {
    static {
        register(DataSourceType.ORACLE, OracleConnector::new);
    }
}
```

3. **添加测试**：
```java
// intellisql-connector/src/test/java/org/intellisql/connector/oracle/
@Testcontainers
class OracleConnectorTest {
    @Container
    static OracleContainer oracle = new OracleContainer("oracle:latest");

    @Test
    void assertConnection() {
        // Test implementation
    }
}
```

### 添加新的 SQL 方言

1. **扩展 Calcite 方言**：
```java
// intellisql-parser/src/main/java/org/intellisql/parser/dialect/
public class ClickHouseDialect extends SqlDialect {
    // Override methods for ClickHouse-specific syntax
}
```

2. **添加翻译测试**：
```java
@Test
void assertMySQLToClickHouseTranslation() {
    String mysql = "SELECT * FROM users LIMIT 10";
    String expected = "SELECT * FROM users LIMIT 10";

    Translation translation = translator.translate(
        mysql, SqlDialect.MYSQL, SqlDialect.CLICKHOUSE
    );

    assertThat(translation.getTargetSql(), is(expected));
}
```

### 添加新的优化器规则

1. **创建规则**：
```java
// intellisql-feature-optimizer/src/main/java/org/intellisql/optimizer/rule/
public class MyPushDownRule extends RelOptRule {
    public MyPushDownRule() {
        super(operand(Filter.class, any()));
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
        // Optimization logic
    }
}
```

2. **注册规则**：
```java
// intellisql-feature-optimizer/src/main/java/org/intellisql/optimizer/Optimizer.java
public class Optimizer {
    private void registerRules(RelOptPlanner planner) {
        planner.addRule(MyPushDownRule.INSTANCE);
    }
}
```

## 测试指南

### 单元测试

遵循 constitution 指南（AIR 原则）：

```java
@Test
void assertQueryExecution() {
    // Given
    String sql = "SELECT * FROM orders";
    QueryExecutor executor = new QueryExecutor();

    // When
    QueryResult actual = executor.execute(sql);

    // Then
    assertThat(actual.isSuccess(), is(true));
    assertThat(actual.getRowCount(), greaterThan(0L));
}
```

### 使用 TestContainers 进行集成测试

```java
@Testcontainers
class MySQLConnectorIT {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test");

    @Test
    void assertConnection() {
        DataSourceConfig config = DataSourceConfig.builder()
            .url(mysql.getJdbcUrl())
            .username(mysql.getUsername())
            .password(mysql.getPassword())
            .build();

        MySQLConnector connector = new MySQLConnector();
        boolean connected = connector.testConnection(config);

        assertTrue(connected);
    }
}
```

### Mock 指南

遵循 constitution：

```java
@ExtendWith(MockitoExtension.class)
class QueryPlannerTest {
    @Mock
    private MetadataManager metadataManager;

    @InjectMocks
    private QueryPlanner planner;

    @Test
    void assertPlanGeneration() {
        // Use mocks for external dependencies
        when(metadataManager.getTable(anyString()))
            .thenReturn(mockTable);

        // Test behavior
        ExecutionPlan plan = planner.plan("SELECT * FROM orders");

        assertThat(plan.getStages(), hasSize(2));
    }
}
```

## 调试

### 启用调试日志

编辑 `conf/logback.xml`：

```xml
<logger name="com.intellisql" level="DEBUG"/>
<logger name="org.apache.calcite" level="DEBUG"/>
```

### 远程调试

```bash
# Start server with remote debugging
./bin/start.sh --debug

# Server will pause and wait for debugger on port 5005
# Attach IntelliJ IDEA debugger to localhost:5005
```

### 常见问题

#### 问题：测试失败，提示 "Could not find or load main class"

**解决方案**：重新构建项目
```bash
./mvnw clean install -DskipTests
```

#### 问题：Spotless 检查失败

**解决方案**：应用格式化
```bash
./mvnw spotless:apply
```

#### 问题：TestContainers 启动失败

**解决方案**：确保 Docker 正在运行
```bash
docker ps
```

#### 问题：Lombok 未生成代码

**解决方案**：在 IDE 中启用注解处理
- Settings → Compiler → Annotation Processors → Enable

## 性能测试

### 基准测试查询性能

```java
@Test
void assertQueryPerformance() {
    String sql = "SELECT * FROM large_table";

    long start = System.currentTimeMillis();
    QueryResult result = executor.execute(sql);
    long duration = System.currentTimeMillis() - start;

    assertThat(duration, lessThan(5000L));  // < 5 seconds
    assertThat(result.getRowCount(), is(100000L));
}
```

### 负载测试

```bash
# Use JMeter or Gatling for load testing
# Example: 100 concurrent connections
./mvnw test -Dtest=LoadTest -Dconnections=100
```

## 贡献指南

### 提交前

1. **运行质量检查**：
```bash
./mvnw clean install -B -T1C -Pcheck
```

2. **格式化代码**：
```bash
./mvnw spotless:apply -Pcheck
```

3. **验证覆盖率**：
```bash
# Coverage must not be lower than master branch
./mvnw jacoco:report
```

### 提交消息格式

```
[MODULE] Brief description

- Detailed change 1
- Detailed change 2

Closes #123
```

示例：
```
[PARSER] Add Oracle SQL dialect support

- Implement OracleSqlDialect extending SqlDialect
- Add ROWNUM pagination translation
- Add unit tests for Oracle dialect

Closes #42
```

### Pull Request 检查清单

- [ ] Code compiles: `./mvnw clean install`
- [ ] Tests pass: `./mvnw test`
- [ ] Checkstyle passes: `./mvnw checkstyle:check`
- [ ] Spotless passes: `./mvnw spotless:check`
- [ ] Coverage maintained or improved
- [ ] Documentation updated
- [ ] PR description explains changes

## 下一步

1. **阅读架构文档**：
   - [Implementation Plan](./plan.md)
   - [Data Model](./data-model.md)
   - [Research](./research.md)

2. **理解契约**：
   - [JDBC Protocol](./contracts/jdbc-protocol.md)
   - [Configuration Schema](./contracts/config-schema.md)

3. **审查 Constitution**：
   - 阅读 `.specify/memory/constitution.md`
   - 遵循编码标准和原则

4. **选择任务**：
   - 查看 `tasks.md` 获取可用任务（由 `/speckit.tasks` 生成）
   - 从 "good first issue" 标签开始

5. **提问**：
   - 为 bug 创建 GitHub issue
   - 使用 discussions 进行提问
   - 加入社区聊天（如果可用）

## 额外资源

### 文档

- [Apache Calcite Documentation](https://calcite.apache.org/docs/)
- [Apache Avatica Documentation](https://avatica.apache.org/docs/)
- [TestContainers Documentation](https://www.testcontainers.org/)
- [HikariCP Documentation](https://github.com/brettwooldridge/HikariCP)

### 示例

- Example configurations: `conf/examples/` (YAML format)
- Sample queries: `examples/queries/`
- Integration test scenarios: `intellisql-test-e2e/src/test/resources/`

### 支持

- GitHub Issues: https://github.com/your-org/intellisql/issues
- Documentation: https://intellisql.org/docs
- Community: https://intellisql.org/community

---

**Happy Coding!** 🚀

今天就开启您的第一次贡献吧。查看 `tasks.md` 获取适合新手的任务。
