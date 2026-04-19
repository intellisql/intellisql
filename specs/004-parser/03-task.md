# SQL 解析任务

更新时间：2026-04-19

## 已完成

- [x] 建立 `SqlParserFactory`
- [x] 建立按方言生成 parser config 的逻辑
- [x] 接入 `DatabaseDialectRegistry`
- [x] 建立 `parse(...)`
- [x] 建立 `parseExpression(...)`
- [x] 建立 `parseWithBabel(...)`
- [x] 建立 `BabelParserConfiguration`
- [x] 建立 `SqlShowTables`
- [x] 建立 `SqlShowSchemas`
- [x] 建立 `SqlUseSchema`
- [x] 让扩展 AST 接入宽松 parser
- [x] 编写 `SqlParserFactoryTest`
- [x] 编写 `BabelParserConfigurationTest`
- [x] 编写 `ExtensionSqlParserTest`

## P0

- [ ] 将 `SHOW TABLES` / `SHOW SCHEMAS` / `SHOW DATABASES` / `USE` 与服务端执行语义一一对齐
- [ ] 实现多语句脚本解析
- [ ] 实现分号分段执行支持

## P1

- [ ] 建立扩展语句解释器
- [ ] 建立扩展语句错误码和错误提示规范
- [ ] 增加 `DESCRIBE`
- [ ] 增加 `SHOW COLUMNS`
- [ ] 增加 `EXPLAIN`
- [ ] 增加更多管理类语句
- [ ] 增加多语句解析测试
- [ ] 增加更多扩展语句测试
- [ ] 增加方言边界测试

## P2

- [ ] 增强更多 parser 配置项
- [ ] 明确多语句错误恢复策略
- [ ] 建立 parser 输出与翻译器、联邦模块统一的错误语义
