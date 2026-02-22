# Tasks: intellisql-client (isql) 模块

**Input**: 设计文档来自 `/specs/001-sql-federation-translation/`
**参考实现**: SQLLine, JLine3 Demo
**Prerequisites**: plan.md, spec.md

**Organization**: 任务按实现阶段组织，支持独立实现和测试

## Format: `[ID] [P?] [Phase] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Phase]**: 所属实现阶段
- 包含精确文件路径

## Path Conventions

```
intellisql-client/
├── src/main/java/com/intellisql/client/
├── src/main/resources/
├── src/test/java/com/intellisql/client/
└── pom.xml
```

---

## Phase 1: 基础骨架 & 原生构建 (Setup)

**Purpose**: 搭建模块结构和 Native Image 构建流水线

**Goal**: Hello World 级原生可执行文件编译通过

### 1.1 Maven 配置

- [ ] T001 配置 `intellisql-client/pom.xml`，引入 JLine 3.25.1, Picocli 4.7.5 依赖
- [ ] T002 配置 `native-maven-plugin` 和 `graalvm` 依赖到 `intellisql-client/pom.xml`

### 1.2 入口与配置

- [ ] T003 创建 `IntelliSqlClient` 入口类 in `intellisql-client/src/main/java/com/intellisql/client/IntelliSqlClient.java` (Picocli @Command)
- [ ] T004 创建 GraalVM 资源配置目录 `intellisql-client/src/main/resources/META-INF/native-image/`
- [ ] T005 验证原生编译流水线 (mvn -Pnative package)

---

## Phase 2: 终端交互增强 (Interactive)

**Purpose**: 实现基于 JLine3 的现代 REPL 体验

**Goal**: 支持语法高亮、历史记录、信号捕获的交互式 Shell

### 2.1 核心交互组件

- [ ] T006 [P] 实现 `ConsoleReader` 封装 JLine Reader in `intellisql-client/src/main/java/com/intellisql/client/console/ConsoleReader.java`
- [ ] T007 [P] 创建 `sql.nanorc` 高亮规则文件 in `intellisql-client/src/main/resources/sql.nanorc`
- [ ] T008 [P] 实现 `SyntaxHighlighter` 适配器 in `intellisql-client/src/main/java/com/intellisql/client/console/SyntaxHighlighter.java`
- [ ] T009 [P] 实现 `SignalHandler` 捕获 Ctrl+C in `intellisql-client/src/main/java/com/intellisql/client/console/SignalHandler.java`
- [ ] T010 [P] 实现 `TerminalPrinter` 线程安全输出 in `intellisql-client/src/main/java/com/intellisql/client/console/TerminalPrinter.java`

### 2.2 集成测试

- [ ] T011 在 `IntelliSqlClient` 中集成 Reader, Highlighter, SignalHandler 并验证交互循环

---

## Phase 3: JDBC 集成与补全 (Connectivity)

**Purpose**: 连接 Server 并提供上下文感知的补全

**Goal**: 可连接数据库，执行 SQL，支持表名补全

### 3.1 连接管理

- [ ] T012 添加 `intellisql-jdbc` 依赖并配置原生反射配置
- [ ] T013 实现 `ConnectCommand` (处理 \connect) in `intellisql-client/src/main/java/com/intellisql/client/command/ConnectCommand.java`

### 3.2 智能补全

- [ ] T014 实现 `MetaDataLoader` 异步加载元数据 in `intellisql-client/src/main/java/com/intellisql/client/console/MetaDataLoader.java`
- [ ] T015 实现 `CompleterFactory` (关键字+元数据补全) in `intellisql-client/src/main/java/com/intellisql/client/console/CompleterFactory.java`

### 3.3 执行命令

- [ ] T016 实现 `ExecuteCommand` (JDBC Statement 执行) in `intellisql-client/src/main/java/com/intellisql/client/command/ExecuteCommand.java`

---

## Phase 4: 结果渲染 (Rendering)

**Purpose**: 高效展示查询结果

**Goal**: 支持分页、CJK 对齐的表格渲染

### 4.1 渲染组件

- [ ] T017 [P] 实现 `WidthCalculator` (CJK 字符宽度) in `intellisql-client/src/main/java/com/intellisql/client/renderer/WidthCalculator.java`
- [ ] T018 [P] 实现 `ResultSetFormatter` (格式化行) in `intellisql-client/src/main/java/com/intellisql/client/renderer/ResultSetFormatter.java`
- [ ] T019 [P] 实现 `PagingRenderer` (less-like 分页) in `intellisql-client/src/main/java/com/intellisql/client/renderer/PagingRenderer.java`

### 4.2 集成

- [ ] T020 将 PagingRenderer 集成到 ExecuteCommand

---

## Phase 5: SQL 翻译命令 (Translation)

**Purpose**: 暴露 SQL 翻译能力

**Goal**: `isql --translate` 可用

### 5.1 翻译命令

- [ ] T021 实现 `TranslateCommand` in `intellisql-client/src/main/java/com/intellisql/client/command/TranslateCommand.java`
- [ ] T022 添加 `TranslateCommandTest` 单元测试

---

## Phase 6: 完善与验证 (Polish)

**Purpose**: 确保原生体验

**Goal**: 启动速度 < 0.5s，无反射报错

### 6.1 原生验证

- [ ] T023 运行 `native-image-agent` 收集缺失配置
- [ ] T024 验证原生包启动时间和体积
- [ ] T025 验证帮助信息和版本输出

---

## Summary

| Phase | Tasks |
|-------|-------|
| Phase 1 (Setup) | 5 |
| Phase 2 (Interactive) | 6 |
| Phase 3 (Connectivity) | 5 |
| Phase 4 (Rendering) | 4 |
| Phase 5 (Translation) | 2 |
| Phase 6 (Polish) | 3 |
| **Total** | **25** |
