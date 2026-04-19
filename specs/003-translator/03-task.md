# SQL 翻译任务

更新时间：2026-04-19

## 已完成

- [x] 建立 `SqlTranslator`
- [x] 建立 `Translation` 请求/结果模型
- [x] 建立 `TranslationMode`
- [x] 建立 `TranslationError`
- [x] 建立 `TranslationException`
- [x] 建立离线翻译主路径
- [x] 接入方言 parser
- [x] 接入目标方言 SQL 输出
- [x] 支持常见查询/DML 翻译
- [x] 建立在线翻译接口形态
- [x] 预留分析与校验钩子
- [x] 建立 `DatabaseDialect` SPI
- [x] 注册 MySQL 方言
- [x] 注册 PostgreSQL 方言
- [x] 注册 Oracle 方言
- [x] 注册 SQL Server 方言
- [x] 注册 Hive 方言
- [x] 注册 STANDARD 方言
- [x] 编写 `SqlTranslatorTest`
- [x] 编写 `CrossDialectTranslationTest`

## P0

- [ ] 接入真实 schema 元数据
- [ ] 实现 schema-aware validation
- [ ] 建立函数差异分析
- [ ] 建立分页差异分析
- [ ] 建立标识符引用差异分析
- [ ] 输出 `unsupportedFeatures` 的真实检测结果

## P1

- [ ] 增加更细粒度的错误分类
- [ ] 增加语义风险等级输出
- [ ] 增加更多 DDL 覆盖
- [ ] 增加窗口函数覆盖
- [ ] 增加日期函数覆盖
- [ ] 增加更多数据库特有表达式覆盖
- [ ] 扩展更多目标方言
- [ ] 增加方言别名和兼容性测试
- [ ] 增加在线模式测试
- [ ] 增加更多失败场景测试
- [ ] 增加函数差异和分页差异测试
- [ ] 增加 DDL/复杂语义测试

## P2

- [ ] 支持更复杂的 DDL 翻译
- [ ] 支持视图定义翻译
- [ ] 支持过程式 SQL / 存储过程翻译
- [ ] 支持触发器翻译
- [ ] 支持函数定义翻译
- [ ] 支持 explain/diff 输出
- [ ] 接入 LLM 辅助翻译
- [ ] 接入自动校验或裁判机制
- [ ] 建立可用于数据库迁移项目的完整翻译工作流
