# isql 任务

更新时间：2026-04-19

## 已完成

- [x] 建立 Picocli 入口 `IntelliSqlClient`
- [x] 注册 `ConnectCommand`
- [x] 注册 `ExecuteCommand`
- [x] 注册 `TranslateCommand`
- [x] 注册 `HelpCommand`
- [x] 建立交互式命令循环
- [x] 支持退出命令
- [x] 支持 slash command 分发
- [x] 支持整行 SQL 执行
- [x] 实现 `\\connect`
- [x] 实现普通 SQL 执行
- [x] 实现 `\\translate`
- [x] 实现帮助命令
- [x] 接入 JLine
- [x] 实现历史记录文件
- [x] 实现语法高亮
- [x] 实现中断信号处理
- [x] 实现补全工厂
- [x] 基于 JDBC metadata 提供动态补全
- [x] 实现文本表格渲染
- [x] 实现列宽计算
- [x] 实现分页钩子
- [x] 编写 `IntelliSqlClientTest`
- [x] 编写 `ConnectCommandTest`
- [x] 编写 `ExecuteCommandTest`
- [x] 编写 `TranslateCommandTest`
- [x] 编写 `CompleterFactoryTest`
- [x] 编写 `MetaDataLoaderTest`
- [x] 编写 `ConsoleReaderTest`
- [x] 编写 `SignalHandlerTest`
- [x] 编写 `SyntaxHighlighterTest`
- [x] 编写 `TerminalPrinterTest`
- [x] 编写 `PagingRendererTest`
- [x] 编写 `ResultSetFormatterTest`
- [x] 编写 `WidthCalculatorTest`
- [x] 建立客户端 assembly 打包
- [x] 提供 `bin/isql.sh`

## P0

- [ ] 补齐 `-e/--execute` 单次执行模式
- [ ] 补齐 `-f/--file` 脚本执行模式
- [ ] 支持多行 SQL 输入
- [ ] 支持分号结束提交
- [ ] 实现真正的交互式分页器

## P1

- [ ] 明确 CLI 退出码
- [ ] 增强复杂输入解析
- [ ] 增加更多管理命令
- [ ] 增加连接状态展示
- [ ] 增加当前 schema 展示
- [ ] 增强高亮规则
- [ ] 增强补全上下文感知能力
- [ ] 为超大结果集增加更稳妥的流式渲染策略
- [ ] 增加可配置 page size / render 策略
- [ ] 增加单次执行模式测试
- [ ] 增加脚本执行模式测试
- [ ] 增加多行输入测试
- [ ] 增加真实分页交互测试

## P2

- [ ] 增加 Windows/macOS/Linux 更完整的运行验证
- [ ] 增加 native-image 打包
- [ ] 增加可达性元数据
- [ ] 增加自动发布流水线
