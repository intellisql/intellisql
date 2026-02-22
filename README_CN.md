# IntelliSql

IntelliSql - SQL 联邦查询与翻译平台

[English](README.md) | 简体中文

## 概述

IntelliSql 是一个分布式 SQL 联邦查询与翻译平台，支持通过统一的 SQL 接口查询多个异构数据源。基于 Apache Calcite 构建，提供：

- **SQL 联邦查询**：通过单一 SQL 接口查询多个数据库（MySQL、PostgreSQL、Elasticsearch）
- **SQL 方言翻译**：在不同数据库系统之间翻译 SQL 方言
- **JDBC 协议**：标准 JDBC 接口，兼容现有工具
- **查询优化**：基于代价的查询优化，支持谓词下推

## 特性

- 多数据库联邦查询支持
- SQL 方言翻译（MySQL、PostgreSQL、Elasticsearch）
- 基于 Avatica 的标准 JDBC 协议
- 查询优化与下推
- 基于 HikariCP 的连接池
- JSON 结构化日志
- 健康监控与指标

## 快速开始

### 环境要求

- Java 8 或更高版本
- Maven 3.6+
- Docker（可选，用于测试）

### 构建

```bash
# 克隆仓库
git clone https://github.com/intellisql/intellisql.git
cd intellisql

# 构建项目（跳过测试）
./mvnw clean install -DskipTests

# 构建项目（包含测试）
./mvnw clean install
```

### 配置

1. 解压发布包后，配置文件位于 `conf/` 目录：

```bash
cd intellisql-server-1.0.0-SNAPSHOT
ls conf/
# model.yaml  logback.xml
```

2. 编辑 `conf/model.yaml` 添加数据源：

```yaml
dataSources:
  - name: mysql_source
    type: mysql
    host: localhost
    port: 3306
    database: source_db
    username: root
    password: ${MYSQL_PASSWORD}
```

### 运行

```bash
# 构建并解压发布包
./mvnw clean package -DskipTests
cd intellisql-distribution/intellisql-distribution-server/target
unzip intellisql-server-1.0.0-SNAPSHOT.zip
cd intellisql-server-1.0.0-SNAPSHOT

# 启动服务器
./bin/start.sh

# 启动服务器（开启远程调试）
./bin/start.sh --debug

# 停止服务器
./bin/stop.sh
```

### 命令行客户端 (isql)

#### 连接选项

| 选项 | 说明 | 默认值 |
|------|------|--------|
| `-h, --host` | 服务器地址 | localhost |
| `-P, --port` | 服务器端口 | 8765 |
| `-u, --user` | 用户名 | root |
| `-p, --password` | 密码 | 无 |
| `-d, --database` | 数据库名 | intellisql |

#### 使用示例

```bash
# 连接本地服务器（交互模式）
./bin/isql

# 连接远程服务器
./bin/isql -h db.example.com -P 8765 -u admin -p secret

# 连接指定数据库
./bin/isql -d mydb

# 执行单条查询并退出
./bin/isql -e "SELECT * FROM users LIMIT 10"

# 执行 SQL 脚本文件
./bin/isql -f /path/to/queries.sql
```

#### 交互模式命令

连接成功后，可使用以下命令：

| 命令 | 说明 |
|------|------|
| `\h` 或 `\help` | 显示帮助 |
| `\q` 或 `\quit` | 退出客户端 |
| `\c` 或 `\clear` | 清空输入缓冲区 |
| `\t <sql>` | 翻译 SQL 方言 |
| `\s <file>` | 执行脚本文件 |
| `\d` | 列出数据源 |

```bash
# 交互会话示例
isql> SELECT * FROM users WHERE id = 1;
isql> \t SELECT DATE_FORMAT(created_at, '%Y-%m') FROM users;
isql> \s /path/to/batch.sql
isql> \q
```

## 项目结构

```
intellisql/
├── intellisql-common/      # 通用工具与共享模型
├── intellisql-parser/      # SQL 解析模块
├── intellisql-features/    # 核心特性聚合模块
│   ├── intellisql-federation/  # SQL 联邦引擎（元数据、执行）
│   ├── intellisql-translator/  # SQL 翻译模块
│   └── intellisql-optimizer/   # 查询优化模块
├── intellisql-connector/   # 数据库连接器
├── intellisql-jdbc/        # JDBC 驱动实现
├── intellisql-server/      # 服务器实现
│   └── src/main/resources/
│       └── conf/           # 配置文件目录
│           ├── model.yaml  # 主配置文件
│           └── logback.xml # 日志配置
├── intellisql-client/      # 命令行客户端
├── intellisql-distribution/
│   └── intellisql-distribution-server/
│       └── bin/            # 启动脚本
│           ├── start.sh    # 服务器启动脚本
│           ├── stop.sh     # 服务器停止脚本
│           └── isql        # 客户端脚本
└── intellisql-test/        # 集成测试
```

## 模块说明

### intellisql-common

通用模块，包含平台共享的工具类、配置管理、元数据定义和公共接口。

### intellisql-parser

基于 Apache Calcite 的 SQL 解析模块。将 SQL 语句解析为抽象语法树（AST）。

### intellisql-features

核心特性聚合模块：

- **intellisql-federation**：核心联邦引擎，处理元数据管理、模式发现和分布式查询执行。
- **intellisql-translator**：SQL 翻译模块，用于在不同 SQL 方言之间进行转换。
- **intellisql-optimizer**：基于代价的查询优化模块，支持谓词下推能力。

### intellisql-connector

数据库连接器实现。为 MySQL、PostgreSQL 和 Elasticsearch 提供统一接口。

### intellisql-jdbc

基于 Apache Avatica 的 JDBC 驱动实现。支持标准 JDBC 连接。

### intellisql-server

服务器模块，暴露 JDBC 协议并处理客户端连接。

### intellisql-client

命令行客户端，用于交互式 SQL 查询。

### intellisql-distribution

打包模块，将所有组件打包为可分发的归档文件。

### intellisql-test

使用 Testcontainers 的集成测试，支持对真实数据库实例进行测试。

## 开发指南

### 代码风格

本项目使用：

- **Spotless** 进行代码格式化（Palantir Java Format）
- **Checkstyle** 进行代码质量检查

```bash
# 格式化代码
./mvnw spotless:apply

# 检查代码风格
./mvnw checkstyle:check

# 运行所有检查
./mvnw clean verify -Pcheck
```

### 测试

```bash
# 运行单元测试
./mvnw test

# 运行集成测试
./mvnw verify -DskipUnitTests

# 运行所有测试
./mvnw verify
```

### IDE 配置

在 IDE 中作为 Maven 项目导入（推荐使用 IntelliJ IDEA）。

请确保：
1. 启用 Lombok 的注解处理
2. 安装 Lombok 插件
3. 设置 Java 8 为项目 SDK

## 配置参考

### 服务器配置

| 属性 | 说明 | 默认值 |
|------|------|--------|
| server.host | 服务器绑定地址 | 0.0.0.0 |
| server.port | HTTP 端口 | 8765 |
| server.avaticaPort | JDBC/Avatica 端口 | 8766 |

### 数据源配置

| 属性 | 说明 | 必填 |
|------|------|------|
| name | 数据源名称 | 是 |
| type | 数据库类型（mysql、postgresql、elasticsearch） | 是 |
| host | 数据库主机 | 是 |
| port | 数据库端口 | 是 |
| database | 数据库名称 | 是 |
| username | 连接用户名 | 是 |
| password | 连接密码 | 否 |

## 贡献指南

请参阅 [CONTRIBUTING.md](CONTRIBUTING.md) 了解贡献指南。

## 许可证

本项目基于 Apache License 2.0 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件。

## 致谢

- [Apache Calcite](https://calcite.apache.org/) - 动态数据管理框架
- [Apache Avatica](https://calcite.apache.org/avatica/) - JDBC 驱动框架
- [ShardingSphere](https://shardingsphere.apache.org/) - 架构和代码风格参考
