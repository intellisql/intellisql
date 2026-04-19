# 联邦查询任务

更新时间：2026-04-19

## 已完成

- [x] 实现 YAML 配置加载
- [x] 支持环境变量替换
- [x] 支持 `dataSources` list/map 两种配置写法
- [x] 支持从 `host/port/database` 组装 JDBC URL
- [x] 实现 `DataSourceManager`
- [x] 实现初始化时的数据源连接测试
- [x] 实现数据源状态管理
- [x] 实现后台健康检查调度
- [x] 实现 `DataSourceConnector` 抽象
- [x] 实现 `ConnectorRegistry`
- [x] 通过 SPI 自动发现连接器
- [x] 实现 MySQL 连接器
- [x] 实现 PostgreSQL 连接器
- [x] 实现 Elasticsearch 连接器
- [x] 为 MySQL 插件提供 schema discoverer
- [x] 为 PostgreSQL 插件提供 schema discoverer
- [x] 为 Elasticsearch 插件提供 schema discoverer
- [x] 实现 `MetadataManager`
- [x] 实现 schema 注册
- [x] 实现 table 注册
- [x] 实现从连接器批量 discovery schema
- [x] 实现 Calcite root schema 构建
- [x] 支持根层直接表访问
- [x] 支持 `schema.table` 形式访问
- [x] 联邦查询主链接入 Babel parser
- [x] 实现 `RelConverter`
- [x] 创建独立 `VolcanoPlanner`
- [x] 注册基础 CBO rules
- [x] 实现 `SqlNode -> RelNode` 转换
- [x] 实现 `QueryProcessor.process(...)`
- [x] 实现 Parse -> Rel -> Optimize -> ExecutionPlan -> Execute 主链
- [x] 实现基于 `ExecutionStage` 的阶段执行
- [x] 实现阶段 `RelNode -> SQL` 下推
- [x] 实现执行重试
- [x] 实现 `FederatedQueryExecutor`
- [x] 实现 `PhysicalPlanConverter`
- [x] 实现 `TableScan` / `Filter` / `Project` / `Join` / `Aggregate` / `Sort` 对应 iterator/operator
- [x] 在服务端注入 `MetadataManager`
- [x] 为 `SHOW TABLES` 提供最小可见支持
- [x] 编写 MySQL 连接器集成测试
- [x] 编写 PostgreSQL 连接器集成测试
- [x] 编写 Elasticsearch 连接器集成测试

## P0

- [ ] 为 `FederatedTable` 注入真实 `SqlTypeName`
- [ ] 去掉未知数据源默认回落 MySQL 的临时逻辑
- [ ] 编写主联邦查询链路集成测试
- [ ] 实现 `Meta.getTables`
- [ ] 实现 `Meta.getColumns`
- [ ] 实现 `Meta.getSchemas`

## P1

- [ ] 将备用执行器正式接入主执行链
- [ ] 或删除备用执行器并统一为单一路线
- [ ] 为阶段执行增加更明确的错误分类
- [ ] 实现 `SHOW SCHEMAS`
- [ ] 实现 `SHOW DATABASES`
- [ ] 支持表级刷新
- [ ] 支持 schema 级刷新
- [ ] 支持元数据缓存和失效策略
- [ ] 编写跨源联邦端到端测试
- [ ] 编写错误路径和降级路径测试
- [ ] 为 JVM 内存执行器补完整集成测试

## P2

- [ ] 支持运行时配置热更新
- [ ] 支持更完整的数据源参数校验
- [ ] 扩展更多可连接数据源类型
- [ ] 补充更多联邦相关规则验证
- [ ] 加强 validator 与实际类型系统的一致性
- [ ] 实现更多 JDBC metadata API
- [ ] 实现真正的跨源 JOIN 执行闭环
- [ ] 实现跨源聚合闭环
- [ ] 实现跨源排序闭环
- [ ] 实现跨源分页闭环
- [ ] 控制中间结果大小和内存上限
- [ ] 明确 Elasticsearch 可下推与不可下推算子边界
- [ ] 增加主链与备用执行器之间的能力对齐策略

## 范围外

- [ ] `006-e2e`
- [ ] `007-optimizer`
