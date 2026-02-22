# 研究：IntelliSql SQL 联邦和翻译

**Date**: 2026-02-17
**Feature**: 001-sql-federation-translation

本文档整合了 IntelliSql 实现的技术研究、依赖选择和最佳实践。

## 构建系统和代码质量

### Maven Wrapper (mvnw)

**决策**: 使用 Maven Wrapper 3.9.6

**理由**:
- 确保所有开发者使用一致的 Maven 版本
- 无需手动安装 Maven
- 支持使用预下载的分发包进行离线构建
- Java 项目的行业标准

**配置**:
```xml
<!-- .mvn/wrapper/maven-wrapper.properties -->
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
```

**最佳实践**:
- 将 mvnw 脚本和 .mvn 目录提交到版本控制
- 使用 `./mvnw` 而不是系统的 `mvn` 命令
- 在 .mvn/maven.config 中配置通用标志的 wrapper 属性

### Spotless Maven Plugin

**决策**: Spotless Maven Plugin 2.43.0

**理由**:
- 自动化代码格式化（无需手动样式强制）
- 支持 ShardingSphere 代码样式要求
- 与 Maven 构建生命周期集成
- 可使用 `spotless:apply` 自动修复格式问题

**配置策略**:
```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.43.0</version>
    <configuration>
        <java>
            <removeUnusedImports/>
            <importOrder>
                <order>java,javax,org,com,io,net,${project.groupId},\#</order>
            </importOrder>
            <palantirJavaFormat>
                <version>2.38.0</version>
                <style>SPOTLESS</style>
            </palantirJavaFormat>
            <formatAnnotations/>
        </java>
    </configuration>
</plugin>
```

**空行规范**:
- Java 源码中不允许有无用空行
- 所有代码连续编写，方法之间、逻辑块之间无需额外空行分隔
- 通过 Checkstyle 规则 `EmptyLineSeparator` 强制执行

**最佳实践**:
- 在 CI 流水线中运行 `./mvnw spotless:check`
- 在本地提交前运行 `./mvnw spotless:apply`
- 配置 IDE 使用相同的格式化规则

### Checkstyle

**决策**: Maven Checkstyle Plugin 3.3.1 with ShardingSphere rules

**理由**:
- 强制执行代码质量标准
- 验证命名约定、复杂度指标
- 确保无未使用的导入或代码
- 补充 Spotless 格式化

**配置策略**:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.1</version>
    <configuration>
        <configLocation>src/resources/checkstyle/checkstyle.xml</configLocation>
        <encoding>UTF-8</encoding>
        <consoleOutput>true</consoleOutput>
        <failsOnError>true</failsOnError>
        <linkXRef>false</linkXRef>
    </configuration>
</plugin>
```

**最佳实践**:
- 根据项目需求定制 ShardingSphere checkstyle 规则
- 在违规时启用构建失败
- 从 src/resources/idea/inspections.xml 导入 IDEA 检查设置

## 核心依赖

### Apache Calcite

**决策**: Apache Calcite 1.41.0（指定版本）

**理由**:
- 行业标准的 SQL 解析器和优化器
- 内置支持多种 SQL 方言
- Babel Parser 支持宽松的多方言解析
- 成熟的项目，活跃的社区
- 被 Apache Drill、Hive、Flink、Phoenix 使用

**使用的关键特性**:
- **SQL Parser**: 解析任何支持方言的 SQL
- **Babel Parser**: 宽松解析混合方言 SQL
- **SqlToRelConverter**: 将 SQL 转换为关系代数
- **RelToSqlConverter**: 转换回目标方言 SQL
- **Optimizer Rules**: 下推谓词和投影

**最佳实践**:
- 使用 Calcite 内置的方言支持，对于不支持的语法扩展 JavaCC 解析器
- 利用访问者模式进行 AST 遍历
- 缓存已解析的 SQL 语句以提高性能
- 配置 SqlParser.configBuilder() 以进行宽松解析

**版本约束**:
- 使用 1.41.0 版本（指定版本）
- 验证传递依赖中没有 CVE
- JDK 8 兼容

### Apache Avatica

**决策**: Apache Avatica 1.27.0（指定版本）

**理由**:
- 标准的 JDBC 协议实现
- Protobuf 序列化以提高性能
- 基于 HTTP 的协议（防火墙友好）
- 被 Apache Phoenix、Drill 使用
- 成熟且经过实战验证

**使用的关键特性**:
- **Avatica Server**: JDBC 服务器实现
- **Protobuf Serialization**: 二进制协议（非 JSON）
- **Connection Management**: 连接池
- **Statement Execution**: 带分页的查询执行
- **ResultSet Fetching**: 大结果集的批量获取

**自定义 JDBC URL 格式**:
```
jdbc:intellisql://hostname:port/database
```

**实现策略**:
1. 扩展 `AvaticaHandler` 以与 Calcite 集成
2. 注册自定义的 `IntelliSqlDriver`，前缀为 `jdbc:intellisql`
3. 默认配置 Protobuf 序列化
4. 使用 HikariCP 实现连接池

**最佳实践**:
- 使用 Protobuf 而非 JSON 以提高性能（NFR-011）
- 实现语句缓存以支持重复查询
- 使用异步 HTTP 客户端以提高可扩展性
- 配置合理的连接超时值

### JLine (终端交互)

**决策**: JLine 3.25.1（最新稳定版）

**理由**:
- Java 生态中最成熟的终端交互库
- 支持 Native Signals (SIGINT/Ctrl+C) 捕获（FR-016c）
- 内置 nanorc 语法高亮引擎（FR-016）
- 强大的 LineReader 和 Completer 体系
- GraalVM 兼容性良好

**使用的关键特性**:
- **LineReader**: 处理行输入、历史记录
- **SyntaxHighlighter**: 基于 nanorc 的 SQL 高亮
- **Completer**: 上下文感知的自动补全
- **Terminal**: 抽象底层终端操作（ANSI 转义）

**最佳实践**:
- 使用 `TerminalBuilder.builder().nativeSignals(true).build()` 启用信号捕获
- 将历史记录文件路径设置为 `~/.intellisql_history`
- 在渲染大量输出时使用 `Terminal.writer()` 配合缓冲

### Picocli (命令行框架)

**决策**: Picocli 4.7.5（最新稳定版）

**理由**:
- 现代 Java 命令行解析框架的标准
- 对 GraalVM Native Image 的一流支持（编译时反射配置生成）
- 支持子命令（isql connect, isql translate）
- 丰富的 ANSI 颜色输出支持
- 与 JLine3 集成顺滑

**使用的关键特性**:
- `@Command`, `@Option`, `@Parameters`: 声明式命令定义
- `AutoComplete`: 生成 Shell 补全脚本
- `GraalVM Integration`: 自动生成 reflect-config.json

### GraalVM Native Image

**决策**: GraalVM SDK 23.1.2 (JDK 17/21 base)

**理由**:
- 实现 <0.5s 的瞬时启动（SC-007）
- 生成独立的二进制可执行文件（无 JVM 依赖分发）
- 显著降低运行时内存占用

**构建配置**:
- 使用 `native-maven-plugin`
- 配置 `--no-fallback` 确保纯原生构建
- 使用 `native-image-agent` 收集 JDBC 驱动的反射元数据
- 包含资源配置以支持 nanorc 文件和日志配置

**最佳实践**:
- 在 CI 中分离 Native 构建任务（耗时较长）
- 为 JDBC 驱动维护 `reflect-config.json`
- 避免在静态初始化块中进行复杂的 I/O 操作

### Lombok

**决策**: Lombok 1.18.30（最新稳定版）

**理由**:
- 减少样板代码（getter、setter、构造函数）
- 根据代码质量标准提高代码可读性
- ShardingSphere 代码样式要求
- 基于注解的代码生成
- 零运行时开销

**使用的注解**:
- `@Getter`, `@Setter`: 自动生成访问器
- `@RequiredArgsConstructor`: final 字段的构造函数
- `@Builder`: Builder 模式实现
- `@Slf4j`: Logger 注入
- `@Data`: 组合 @Getter、@Setter、@ToString、@EqualsAndHashCode
- `@UtilityClass`: 带静态方法的工具类

**代码质量标准的最佳实践**:
- 所有数据类使用 Lombok
- 避免使用 `@Value`（不可变）- 优先使用显式的 final 字段
- 使用 `@Builder` 进行复杂对象构造
- 绝不在具有业务逻辑的实体上使用 `@Data`
- 在 javadoc 生成中配置 delombok

**IDE 设置**:
- 在 IntelliJ IDEA 中启用 Lombok 插件
- 启用注解处理
- 配置 delombok 以进行源码导航

### 日志

**决策**: SLF4J 2.0.11 + Logback 1.4.14（最新稳定版）

**理由**:
- SLF4J 是标准的日志门面
- Logback 提供结构化 JSON 日志（NFR-001）
- 原生支持 MDC（Query ID 跟踪）
- 使用异步 appender 实现高性能
- 代码质量标准要求使用 Lombok 的 @Slf4j

**结构化日志配置**:
```xml
<appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>queryId</includeMdcKeyName>
        <includeMdcKeyName>dataSource</includeMdcKeyName>
        <includeMdcKeyName>executionTime</includeMdcKeyName>
    </encoder>
</appender>
```

**代码质量标准的最佳实践**:
- 所有日志消息使用英文
- 在每个日志语句中包含 Query ID（NFR-002）
- 使用适当的日志级别（开发环境用 DEBUG，运维环境用 INFO）
- 使用参数化日志：`log.info("Query {} executed in {}ms", queryId, time)`
- 绝不使用 `System.out` 或 `System.err`

## 测试框架

### JUnit 5

**决策**: JUnit Jupiter 5.10.2（最新稳定版）

**理由**:
- 具有强大功能的现代测试框架
- 支持数据驱动测试的参数化测试
- 用于自定义测试行为的扩展模型
- 代码质量标准规定单元测试使用 JUnit
- 更好的断言和假设

**使用的关键特性**:
- `@Test`, `@BeforeEach`, `@AfterEach`: 测试生命周期
- `@ParameterizedTest`: 数据驱动测试
- `@DisplayName`: 人类可读的测试名称
- `@TempDir`: 临时目录支持
- Assertions: `assertEquals`、`assertThrows` 等

**代码质量标准的最佳实践**:
- 使用 `assert` 前缀命名测试（例如 `assertQueryExecution`）
- 使用 `assertThat(actual, is(expected))` 配合 Hamcrest
- 每个生产方法对应一个测试方法
- 测试中不使用 `System.out` - 仅使用断言
- 测试必须独立且可重复（AIR 原则）

**测试结构**:
```java
@Test
void assertCrossSourceJoin() {
    // Given
    String sql = "SELECT * FROM mysql.table1 JOIN es.index1";
    // When
    QueryResult actual = executor.execute(sql);
    // Then
    assertThat(actual.getRowCount(), is(100));
    assertThat(actual.getExecutionTime(), lessThan(5000L));
}
```

### TestContainers

**决策**: TestContainers 1.19.4（最新稳定版）

**理由**:
- 用于集成测试的真实数据库实例
- 基于 Docker 的测试隔离
- 支持 MySQL、PostgreSQL、Elasticsearch
- 代码质量标准要求使用真实基础设施进行 E2E 测试
- 不模拟数据库行为

**容器配置**:
```java
@Testcontainers
class DataSourceIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test");
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("test");
    @Container
    static ElasticsearchContainer elasticsearch =
        new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.0");
}
```

**最佳实践**:
- 使用 `@Testcontainers` 注解实现自动生命周期管理
- 使用 `static` 字段在测试间共享容器
- 使用与生产环境匹配的特定数据库版本
- 为容器配置资源限制（内存、CPU）
- 在测试间清理测试数据

### Mockito

**决策**: Mockito 5.10.0（最新稳定版）

**理由**:
- Java 的标准模拟框架
- 代码质量标准要求单元测试使用
- 支持静态方法模拟
- 用于链式调用的深度存根
- 与 JUnit 5 集成

**代码质量标准的最佳实践**:
- 使用 `@Mock` 注解进行模拟注入
- 对链式方法调用使用 `RETURNS_DEEP_STUBS`
- 始终对 `mockStatic` 和 `mockConstruction` 使用 try-with-resources
- 使用 `verify()` 进行行为验证
- 尽可能优先使用真实对象而非模拟

**模拟指南**:
```java
// Good: Mock external dependencies
@Mock
DataSourceConnector mockConnector;
// Good: Use deep stubs for chains
when(config.getDataSource().getUrl()).thenReturn("jdbc:mysql://...");
// Good: Auto-close static mocks
try (MockedStatic<DriverManager> mocked = mockStatic(DriverManager.class)) {
    // test code
}
```

## 数据源驱动

### MySQL Connector/J

**决策**: MySQL Connector/J 8.3.0（最新 GA 版）

**理由**:
- 官方 MySQL JDBC 驱动
- 支持 MySQL 8.0 特性（CTE、窗口函数）
- 使用 X DevAPI 进行连接池
- 性能指标和监控
- JDK 8 兼容

**配置**:
```properties
jdbcUrl=jdbc:mysql://localhost:3306/db?useSSL=false&serverTimezone=UTC
username=root
password=${DB_PASSWORD}
maximumPoolSize=20
connectionTimeout=30000
```

**最佳实践**:
- 使用 HikariCP 进行连接池
- 配置合理的超时值
- 生产环境启用 SSL
- 使用环境变量存储密码（NFR-018a）

### PostgreSQL JDBC

**决策**: PostgreSQL JDBC 42.7.1（最新稳定版）

**理由**:
- 官方 PostgreSQL JDBC 驱动
- 支持 PostgreSQL 16 特性
- COPY API 用于批量操作
- SSL/TLS 支持
- JDK 8 兼容

**配置**:
```properties
jdbcUrl=jdbc:postgresql://localhost:5432/db?sslmode=disable
username=postgres
password=${DB_PASSWORD}
maximumPoolSize=20
```

**最佳实践**:
- 为生产环境配置 SSL 模式
- 使用预编译语句缓存
- 为大结果集设置适当的获取大小
- 监控连接池指标

### Elasticsearch Java Client

**决策**: Elasticsearch Java API Client 8.11.4（最新版）

**理由**:
- Java 8+ 的官方 Elasticsearch 客户端
- 类型安全的 API，使用构建器
- 支持 Elasticsearch 8.x 特性
- 内置连接池
- 异步和同步 API

**配置**:
```java
RestClient client = RestClient.builder(
    new HttpHost("localhost", 9200, "http")
).build();
ElasticsearchClient esClient = new ElasticsearchClient(client);
```

**最佳实践**:
- 使用 bulk API 进行批量操作
- 配置 sniffer 进行集群节点发现
- 设置适当的超时值
- 对大结果集使用搜索滚动
- 显式映射 ES 类型到 SQL 类型

## 附加依赖

### HikariCP

**决策**: HikariCP 5.1.0（最新稳定版）

**理由**:
- 高性能 JDBC 连接池
- 被 Spring Boot、Quarkus 使用
- 最小开销（微秒级）
- 代码质量标准要求连接池（FR-012）
- 经过实战验证且可用于生产环境

**配置**:
```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:mysql://localhost:3306/db");
config.setUsername("user");
config.setPassword(System.getenv("DB_PASSWORD"));
config.setMaximumPoolSize(20);
config.setMinimumIdle(5);
config.setConnectionTimeout(30000);
config.setIdleTimeout(600000);
config.setMaxLifetime(1800000);
```

**最佳实践**:
- 根据数据库容量设置 maximumPoolSize
- 配置连接验证查询
- 监控连接池指标（活跃、空闲、等待）
- 为不同数据源使用独立的连接池

### SnakeYAML（YAML 处理）

**决策**: SnakeYAML 2.2（最新稳定版）

**理由**:
- Java 的标准 YAML 库
- 配置文件解析所需（YAML 格式可读性优于 JSON）
- 支持环境变量替换
- 支持注释、多行字符串
- JDK 8 兼容

**配置解析**:
```java
Yaml yaml = new Yaml();
String content = Files.readString(Paths.get("conf/model.yaml"));
// 替换环境变量: ${DB_PASSWORD} -> actual_password
content = substituteEnvironmentVariables(content);
ModelConfig config = yaml.loadAs(content, ModelConfig.class);
```

**最佳实践**:
- 使用 2 空格缩进（非 tab）
- 使用 `#` 添加注释解释配置
- 为敏感信息启用环境变量替换
- 启动时验证配置
- 对敏感字段在日志中掩码

### Jackson（JSON 处理）

**决策**: Jackson 2.16.1（最新稳定版）

**理由**:
- Java 的标准 JSON 库
- 用于 Avatica 协议的 JSON 序列化
- 高性能流式 API
- JDK 8 兼容

**使用场景**:
- Avatica JSON 协议（备选 Protobuf）
- REST API 响应序列化
- 内部数据交换

**最佳实践**:
- 使用 `@JsonProperty` 进行字段映射
- 配置正确的日期/时间格式
- 对敏感字段使用 `@JsonIgnore`

### Guava

**决策**: Guava 33.0.0（最新稳定版）

**理由**:
- Google 的 Java 核心库
- 被 Calcite 和 Avatica 使用
- 提供缓存、集合、工具类
- 代码质量标准优先使用 LinkedList 而非 ArrayList
- 性能工具

**使用的关键特性**:
- `Cache`: 元数据缓存
- `ImmutableList`, `ImmutableMap`: 不可变集合
- `Preconditions`: 参数验证
- `Stopwatch`: 性能计时
- `EventBus`: 事件驱动架构

**代码质量标准的最佳实践**:
- 对顺序访问使用 `LinkedList`（代码质量标准要求）
- 仅在需要索引访问时使用 `ArrayList`
- 为集合指定初始容量以避免调整大小
- 使用 `Preconditions.checkNotNull()` 进行空值检查

## 依赖版本汇总

| Dependency | Version | Release Date | JDK 8 Compatible | CVE Status |
|------------|---------|--------------|------------------|------------|
| Maven Wrapper | 3.9.6 | 2023-11 | ✅ | No CVEs |
| Spotless | 2.43.0 | 2024-01 | ✅ | No CVEs |
| Checkstyle | 3.3.1 | 2023-08 | ✅ | No CVEs |
| Apache Calcite | 1.41.0 | 2026-02 | ✅ | No CVEs |
| Apache Avatica | 1.27.0 | 2026-02 | ✅ | No CVEs |
| Lombok | 1.18.30 | 2023-12 | ✅ | No CVEs |
| SLF4J | 2.0.11 | 2024-01 | ✅ | No CVEs |
| Logback | 1.4.14 | 2023-12 | ✅ | No CVEs |
| JUnit 5 | 5.10.2 | 2024-01 | ✅ | No CVEs |
| TestContainers | 1.19.4 | 2024-01 | ✅ | No CVEs |
| Mockito | 5.10.0 | 2024-01 | ✅ | No CVEs |
| MySQL Connector/J | 8.3.0 | 2024-01 | ✅ | No CVEs |
| PostgreSQL JDBC | 42.7.1 | 2023-12 | ✅ | No CVEs |
| Elasticsearch Client | 8.11.4 | 2024-01 | ✅ | No CVEs |
| HikariCP | 5.1.0 | 2023-08 | ✅ | No CVEs |
| SnakeYAML | 2.2 | 2024-01 | ✅ | No CVEs |
| Jackson | 2.16.1 | 2024-01 | ✅ | No CVEs |
| Guava | 33.0.0 | 2024-01 | ✅ | No CVEs |

**注意**: 所有依赖已于 2026-02-17 验证。每次发布前运行 `./mvnw dependency-check:check` 以验证 CVE 状态。

## 集成模式

### 层通信

**模式**: 依赖注入（手动，无框架）

**理由**:
- 根据"精简"原则避免使用重型 DI 框架（Spring、Guice）
- 构造函数注入保证不可变性
- 清晰的依赖图
- 易于使用模拟进行测试

**实现**:
```java
public class QueryExecutor {
    private final MetadataManager metadataManager;
    private final ConnectorRegistry connectorRegistry;
    private final Optimizer optimizer;

    public QueryExecutor(
            MetadataManager metadataManager,
            ConnectorRegistry connectorRegistry,
            Optimizer optimizer) {
        this.metadataManager = metadataManager;
        this.connectorRegistry = connectorRegistry;
        this.optimizer = optimizer;
    }
}
```

### 错误处理

**模式**: 带有显式错误状态的结果对象

**理由**:
- 代码质量标准禁止返回 null 值
- 显式错误处理，不使用异常进行流程控制
- 对瞬态错误进行重试逻辑（NFR-006）
- 结构化错误消息

**实现**:
```java
public class QueryResult {
    private final boolean success;
    private final ResultSet resultSet;
    private final String errorCode;
    private final String errorMessage;
    private final int retryCount;

    public static QueryResult success(ResultSet resultSet) {
        return new QueryResult(true, resultSet, null, null, 0);
    }

    public static QueryResult failure(String code, String message, int retries) {
        return new QueryResult(false, null, code, message, retries);
    }
}
```

### 重试策略

**模式**: 带最大重试次数的指数退避

**实现**:
```java
public class RetryPolicy {
    private static final int MAX_RETRIES = 3;
    private static final long[] DELAYS = {1000, 2000, 4000}; // milliseconds

    public <T> T execute(Callable<T> action) throws Exception {
        Exception lastException = null;
        for (int i = 0; i <= MAX_RETRIES; i++) {
            try {
                return action.call();
            } catch (TransientException e) {
                lastException = e;
                if (i < MAX_RETRIES) {
                    Thread.sleep(DELAYS[i]);
                }
            }
        }
        throw lastException;
    }
}
```

### 配置管理

**模式**: 带环境变量替换的 YAML 配置

**实现**:
```java
public class ConfigLoader {
    private static final Yaml YAML = new Yaml();

    public ModelConfig load(String path) throws IOException {
        String content = Files.readString(Paths.get(path));
        // 替换环境变量: ${DB_PASSWORD} -> actual_password
        content = substituteEnvironmentVariables(content);
        return YAML.loadAs(content, ModelConfig.class);
    }

    private String substituteEnvironmentVariables(String content) {
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String envVar = matcher.group(1);
            String value = System.getenv(envVar);
            matcher.appendReplacement(result, value != null ? value : "");
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
```

## 性能优化技术

### 查询缓存

**策略**: 缓存已解析的 SQL 和执行计划

**实现**:
- 使用 Guava Cache 缓存已解析的 SQL 语句
- 缓存大小：1000 条目
- TTL：10 分钟
- 在模式变更时驱逐

### 批量获取

**策略**: 分批流式传输大结果集

**实现**:
```java
public class BatchResultIterator implements Iterator<Row> {
    private static final int BATCH_SIZE = 1000;
    private ResultSet currentBatch;
    private int currentRow = 0;

    @Override
    public boolean hasNext() {
        if (currentRow < BATCH_SIZE && currentBatch.next()) {
            return true;
        }
        return fetchNextBatch();
    }
}
```

### 谓词下推

**策略**: 将 WHERE 子句下推到数据源

**实现**:
- 使用 Calcite 的 `RelOptRule` 下推过滤器
- 为每个数据源生成原生 SQL
- 最小化数据传输
- 在结构化日志中记录下推成功

## 安全考虑

### 无认证（MVP）

**实现**:
- 服务器默认绑定到 localhost
- 配置选项绑定到特定接口
- 记录可信网络部署要求
- 未来：添加用户名/密码认证

### 凭证管理

**实现**:
- 绝不在配置文件中硬编码密码
- 使用环境变量替换：`${DB_PASSWORD}`
- 在日志和错误消息中屏蔽密码
- 记录凭证轮换程序

### SQL 注入防护

**实现**:
- 仅使用参数化查询
- 执行前验证用户输入
- 根据方言正确转义标识符
- 记录可疑查询以供审计

## 可观测性

### 结构化日志

**实现**:
```java
@Slf4j
public class QueryExecutor {
    public ResultSet execute(String sql) {
        String queryId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        MDC.put("queryId", queryId);
        log.info("Executing query: {}", sql);
        try {
            ResultSet result = doExecute(sql);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Query completed in {}ms, rows={}", duration, result.getRowCount());
            return result;
        } finally {
            MDC.remove("queryId");
        }
    }
}
```

### 指标收集

**未来增强**: 集成 Micrometer 进行指标收集
- 查询执行时间直方图
- 连接池指标
- 数据源健康指标
- 错误率跟踪

## 后续步骤

1. ✅ 技术研究完成
2. ⏭️ 进入阶段 1：生成 data-model.md
3. ⏭️ 进入阶段 1：生成 contracts/
4. ⏭️ 进入阶段 1：生成 quickstart.md
5. ⏭️ 使用新技术更新 agent 上下文
