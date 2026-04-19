# IntelliSQL 架构任务

更新时间：2026-04-19

## 已完成

- [x] 建立多模块 Maven 工程
- [x] 建立父 `pom.xml` 和统一依赖版本管理
- [x] 划分 `common`、`spi`、`parser`、`features`、`connector`、`plugins`、`jdbc`、`server`、`client`、`distributions`、`tests` 模块
- [x] 建立通用配置模型与元数据模型
- [x] 建立统一日志上下文与重试基础设施
- [x] 建立数据库方言 SPI
- [x] 建立连接器 SPI
- [x] 建立统一 `ServiceLoader` 装载机制
- [x] 为 MySQL、PostgreSQL、Elasticsearch 注册连接器插件
- [x] 为 MySQL、PostgreSQL、Oracle、SQL Server、Hive 注册方言插件
- [x] 建立方言感知 SQL parser 工厂
- [x] 建立宽松 Babel parser 入口
- [x] 建立扩展 SQL AST 机制
- [x] 建立 SQL 翻译统一入口
- [x] 建立离线翻译主路径
- [x] 建立在线翻译接口形态
- [x] 建立数据源管理器
- [x] 建立连接器注册中心
- [x] 建立 schema discovery 主流程
- [x] 建立 `MetadataManager` 和 Calcite root schema 构建流程
- [x] 建立 `RelConverter`
- [x] 建立 `QueryProcessor` 查询主链
- [x] 建立优化器接入点
- [x] 建立按阶段下推执行机制
- [x] 保留 JVM 内存执行器备用实现
- [x] 建立 Avatica HTTP Server
- [x] 建立 `Meta` 实现骨架
- [x] 建立 IntelliSQL JDBC Driver
- [x] 建立 IntelliSQL JDBC URL 解析
- [x] 建立 CLI 入口与交互式 shell
- [x] 建立客户端 assembly 发布包
- [x] 建立服务端 assembly 发布包
- [x] 提供 `isql.sh`、`start.sh`、`stop.sh`
- [x] 将 `specs` 目录按功能子目录重组
- [x] 为 `001` 到 `005` 建立 `01-research.md`、`02-implement.md`、`03-task.md`
- [x] 迁移旧规格、计划、契约、研究稿到 `archive/`

## P0

- [ ] 修正服务端启动脚本主类名
- [ ] 让服务端读取启动参数指定的配置路径
- [ ] 补全 `IntelliSqlMeta.getSchemas`
- [ ] 补全 `IntelliSqlMeta.getTables`
- [ ] 补全 `IntelliSqlMeta.getColumns`
- [ ] 为 Calcite schema 注入真实列类型
- [ ] 消除未知数据源默认回落 MySQL 的临时逻辑
- [ ] 对客户端和服务端发布包增加冒烟验证

## P1

- [ ] 统一主执行路径和备用执行路径
- [ ] 打通生产可用的跨源 JOIN / 聚合 / 排序执行闭环
- [ ] 补全更多 JDBC metadata 接口
- [ ] 在线翻译接入真实元数据上下文
- [ ] 建立函数/分页/标识符差异诊断
- [ ] 建立仓库级 `specs` 编写规范
- [ ] 建立文档与代码变更同步校验规则
- [ ] 收敛脚本、配置、文档之间的运行方式差异

## P2

- [ ] 建立 DDL/过程式 SQL 翻译能力
- [ ] 收敛联邦执行、翻译、协议层的统一错误码体系
- [ ] 扩展更多数据源和方言插件
- [ ] 建立运行时配置热更新和更强的配置校验体系

## 范围外

- [ ] `006-e2e`
- [ ] `007-optimizer`
