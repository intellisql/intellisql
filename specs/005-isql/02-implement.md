# isql 实现说明

更新时间：2026-04-19

本文件按 CLI 入口、交互循环、命令实现、终端增强、结果渲染和发布方式来描述当前 `isql` 的真实实现。

## 1. CLI 主入口

### `com.intellisql.client.IntelliSqlClient`

当前 `isql` 入口类，实现 `Callable<Integer>`。

字段：

- `url`
- `user`
- `password`
- `command`
- `file`
- `Map<String, ClientCommand> commands`
- `Connection connection`

构造逻辑：

- 注册：
  - `ConnectCommand`
  - `ExecuteCommand`
  - `TranslateCommand`
  - `HelpCommand`

关键方法：

- `call()`
  - 若传入 `url` 则尝试自动连接
  - 若设置 `command`，当前只打印 `Executing command: ...`
  - 若设置 `file`，当前只打印 `Executing file: ...`
  - 否则进入交互模式
  - 初始化 `MetaDataLoader`
  - 创建 `Completer`
  - 构造 `ConsoleReader`
  - 调用 `runInteractiveLoop(...)`
- `runInteractiveLoop(console, metaDataLoader)`
  - 持续读取 `isql> ` 提示符输入
  - 空行跳过
  - 处理退出命令
  - 以 `\\` 开头走 slash command
  - 其他输入走 SQL 执行
- `handleSlashCommand(...)`
  - 根据命令名从 `commands` 取 `ClientCommand`
  - 执行后根据返回值更新连接
- `handleSqlCommand(...)`
  - 直接找到 `execute` 命令处理当前整行 SQL
- `updateConnection(...)`
  - 当连接对象变化时刷新 metadata loader

## 2. 命令实现

### `ConnectCommand`

关键方法：

- `execute(console, connection, args)`
  - 校验参数
  - 如当前已有连接先尝试关闭
  - 构造 `Properties`
  - 调用 `DriverManager.getConnection(url, props)`
  - 成功后返回新连接

### `ExecuteCommand`

关键方法：

- `execute(console, connection, args)`
  - 若未连接则输出错误
  - 将 `args` 拼成 SQL
  - 去掉末尾分号
  - `connection.createStatement()`
  - `stmt.execute(sql)`
  - 若返回 `ResultSet`，调用 `PagingRenderer.render(rs, printer)`
  - 否则输出 update count

### `TranslateCommand`

关键方法：

- `execute(...)`
  - 解析参数选项
  - 支持：
    - `-s/--source`
    - `-t/--target`
    - `-m/--mode`
  - 调用 `doTranslation(...)`
- `doTranslation(...)`
  - 根据模式调用：
    - `translator.translateOnline(...)`
    - `translator.translateOffline(...)`
  - 成功时输出 translated SQL
  - 失败时输出错误信息

## 3. 终端输入与交互

### `ConsoleReader`

关键逻辑：

- 通过 `TerminalBuilder.builder().system(true).build()` 构造终端
- 创建 `DefaultParser`
  - 重写 `isEscapeChar(...)` 禁用转义字符判断
  - `parser.setEscapeChars(null)`
- 使用 `LineReaderBuilder` 构建 `LineReader`
- 配置：
  - `completer`
  - `IntelliSqlSyntaxHighlighter`
  - `LineReader.HISTORY_FILE = ~/.isql_history`
  - `DISABLE_EVENT_EXPANSION = true`
- 创建 `SignalHandler(terminal).handleInterrupt()`

### `SignalHandler`

负责 Ctrl+C 中断处理。

### `IntelliSqlSyntaxHighlighter`

负责 SQL 语法高亮。

## 4. Metadata 补全

### `MetaDataLoader`

结构：

- `Set<String> tables`
- `Set<String> columns`
- `Set<String> schemas`
- 单线程后台 executor

关键方法：

- `load(connection)`
  - 后台线程异步执行 `loadMetadataInternal(connection)`
- `loadMetadataInternal(connection)`
  - `connection.getMetaData()`
  - `loadSchemas(metaData)`
  - `loadTables(metaData)`
  - `loadColumns(metaData)`
- `clear()`
  - 清空当前缓存

### `CompleterFactory`

关键逻辑：

- 建立 SQL 关键字列表
- 建立命令列表
- 用 `AggregateCompleter` 组合：
  - `StringsCompleter(SQL_KEYWORDS)`
  - `StringsCompleter(ISQL_COMMANDS)`
  - 基于 `MetaDataLoader` 的动态 completer

## 5. 结果渲染

### `PagingRenderer`

关键逻辑：

- 默认 `DEFAULT_PAGE_SIZE = 20`
- `render(rs, printer)`
  - 读取 `ResultSetMetaData`
  - 先缓存前 1000 行计算列宽
  - 调用 `ResultSetFormatter.calculateColumnWidths(...)`
  - 输出 header
  - 输出缓冲区行
  - 输出剩余行
  - 每到页边界调用 `handlePageBoundary(rowCount)`
- `handlePageBoundary(...)`
  - 当前只保留分页钩子
  - `checkPageBreak()` 仍为空实现

### `ResultSetFormatter`

负责：

- 列宽计算
- header/separator/row 格式化

### `WidthCalculator`

负责显示宽度计算。

## 6. 测试覆盖

### 命令层

- `ConnectCommandTest`
- `ExecuteCommandTest`
- `TranslateCommandTest`

### 交互层

- `IntelliSqlClientTest`
- `ConsoleReaderTest`
- `CompleterFactoryTest`
- `MetaDataLoaderTest`
- `SignalHandlerTest`
- `SyntaxHighlighterTest`
- `TerminalPrinterTest`

### 渲染层

- `PagingRendererTest`
- `ResultSetFormatterTest`
- `WidthCalculatorTest`

## 7. 发布方式

### 客户端发布包

- `intellisql-distribution-client/pom.xml`
  - 用 `maven-assembly-plugin` 生成 `isql-${project.version}` 归档

### 启动脚本

- `intellisql-distribution-client/bin/isql.sh`
  - 检查 Java
  - 组装 `lib/*` classpath
  - 执行 `com.intellisql.client.IntelliSqlClient`

## 8. 当前代码级限制

- `-e/--execute` 还没有真实执行逻辑
- `-f/--file` 还没有真实脚本执行逻辑
- 多行 SQL 输入尚未实现
- `PagingRenderer` 没有真实交互式分页
- 参数拆分目前是简单空格切分
- 当前还是 JVM CLI，不是 native-image 客户端
