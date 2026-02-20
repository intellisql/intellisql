<!--
================================================================================
同步影响报告 / SYNC IMPACT REPORT
================================================================================
版本变更 / Version Change: 1.0.0 → 1.0.1
修改的原则 / Modified Principles:
  - 编码规范 > 格式化规则 (明确空行规范)
新增章节 / Added Sections: 无
移除章节 / Removed Sections: 无
模板更新状态 / Templates Requiring Updates:
  - .specify/templates/plan-template.md ✅ 兼容
  - .specify/templates/spec-template.md ✅ 兼容
  - .specify/templates/tasks-template.md ✅ 兼容
后续待办 / Follow-up TODOs: 无
================================================================================
-->

# IntelliSql 项目章程

*基于 Apache 软件基金会行为准则*

## 核心原则

### 一、用心

保持责任心和敬畏心，以工匠精神持续雕琢。

- 每一项贡献都必须体现深思熟虑和专业敬业
- 代码必须被视为工艺品，而不仅仅是实现
- 持续改进和对细节的关注是不可妥协的

**理由**：高质量的软件需要每位贡献者真诚的关注和专业的承诺。

### 二、可读

代码必须无歧义，通过阅读而非调试手段浮现代码意图。

- 代码应该通过清晰的命名和结构自文档化
- 读者必须能够在不运行调试器的情况下理解代码逻辑
- 复杂逻辑必须拆分为命名良好的方法

**理由**：代码被阅读的次数远多于被编写的次数；可读性直接影响可维护性。

### 三、整洁

认同《重构》和《代码整洁之道》的理念，追求整洁优雅代码。

- 代码必须遵循整洁代码原则
- 当代码质量下降时，重构不是可选的，而是必须的
- 技术债务必须及时处理

**理由**：整洁的代码减少缺陷，提高可维护性，加快开发速度。

### 四、一致

代码风格、命名以及使用方式保持完全一致。

- 所有代码必须遵循相同的格式化和命名约定
- 相似的问题必须以相似的方式解决
- 一致性适用于：命名、文件结构、错误处理、日志记录

**理由**：一致性减少认知负担，使代码理解更快。

### 五、精简

极简代码，以最少的代码表达最正确的意思。高度复用，无重复代码和配置。

- 及时删除无用代码
- 每一行代码必须有其目的
- DRY（不要重复自己）原则是强制性的
- 配置重复也是禁止的

**理由**：更少的代码意味着更少的缺陷、更容易的维护和更好的理解。

### 六、抽象

层次划分清晰，概念提炼合理。保持方法、类、包以及模块处于同一抽象层级。

- 每一层必须有单一职责
- 概念必须被正确提取和命名
- 抽象层级不得在单个类或方法中混合

**理由**：正确的抽象使得能够在适当的层次上对复杂系统进行推理。

### 七、极致

拒绝随意，保证任何一行代码、任何一个字母、任何一个空格都有其存在价值。

- 禁止没有跟踪的占位符代码或"TODO"
- 禁止不必要的导入、变量或语句
- 代码审查必须质疑任何没有明确价值的元素

**理由**：小细节上的卓越会带来整体系统的卓越。

## 编码规范

### 格式化规则

- 使用 Linux 换行符（LF）
- 每行代码不超过 200 字符无需换行
- **禁止使用无意义的空行**：
  - 方法体内不允许有空行分隔
  - 变量声明与后续代码之间不需要空行
  - 逻辑块之间不需要空行分隔
  - 如需分隔逻辑，应提炼为私有方法而非使用空行
- 配置文件使用 Spinal Case 命名（使用 `-` 分割单词）

**空行规范示例（错误 vs 正确）**：

```java
// ❌ 错误：包含不必要的空行
@Test
void assertConfigParsing() {
    String yaml = Files.readString(Paths.get("conf/model.yaml"));
    ModelConfig config = ConfigLoader.parse(yaml);

    assertThat(config.getDataSources(), hasSize(2));
    assertThat(config.getDataSources().get(0).getType(), is(DataSourceType.MYSQL));
}

// ✅ 正确：代码连续编写，无空行
@Test
void assertConfigParsing() {
    String yaml = Files.readString(Paths.get("conf/model.yaml"));
    ModelConfig config = ConfigLoader.parse(yaml);
    assertThat(config.getDataSources(), hasSize(2));
    assertThat(config.getDataSources().get(0).getType(), is(DataSourceType.MYSQL));
}
```

### 命名规范

**通用规则：**
- 命名要做到顾名思义
- 类、方法名避免使用缩写，部分变量名可以使用缩写

**标准缩写：**
- `arguments` 缩写为 `args`
- `parameters` 缩写为 `params`
- `environment` 缩写为 `env`
- `properties` 缩写为 `props`
- `configuration` 缩写为 `config`

**专有名词缩写：**
- 三位以内字符：使用大写（如 `SQL92Lexer`、`XMLTransfer`、`MySQLAdminExecutorCreator`）
- 超过三位字符：使用驼峰形式（如 `JdbcUrlAppender`、`YamlAgentConfigurationSwapper`）
- 变量：始终使用小驼峰形式（如 `mysqlAuthenticationMethod`、`sqlStatement`、`mysqlConfig`）

**局部变量命名：**
- 除了直接返回方法入参，返回变量使用 `result` 命名
- 循环中使用 `each` 命名循环变量
- map 中使用 `entry` 代替 `each`
- 捕获的异常名称命名为 `ex`
- 捕获异常且不做任何事情，异常名称命名为 `ignored`
- 方法入参名禁止使用 `result`、`each`、`entry`
- 工具类名称命名为 `XxxUtils`

**条件表达式：**
- `equals` 和 `==`：常量在左，变量在右
- 大于小于等：变量在左，常量在右

### 代码风格规则

- 除了构造器入参与全局变量名称相同的赋值语句外，避免使用 `this` 修饰符
- 局部变量不应设置为 `final`
- 除了用于继承的抽象类之外，尽量将类设计为 `final`
- 嵌套循环尽量提成方法
- 成员变量定义顺序以及参数传递顺序在各个类和方法中保持一致
- 优先使用卫语句
- 类和方法的访问权限控制为最小
- 方法所用到的私有方法应紧跟该方法，如果有多个私有方法，书写私有方法应与私有方法在原方法的出现顺序相同
- 方法入参和返回值不允许为 `null`
- 方法入参禁止使用 `Optional`；应传递普通值（必要时允许为 `null`）
- 优先使用 Lombok 代替构造器、getter/setter 方法和 log 变量
- 禁止内联全限定类名，必须通过 import 引入
- 优先考虑使用 `LinkedList`，只有在需要通过下标获取集合中元素值时再使用 `ArrayList`
- `ArrayList`、`HashMap` 等可能产生扩容的集合类型必须指定集合初始大小，避免扩容
- 优先使用三目运算符代替 if else 的返回和赋值语句
- 禁止嵌套使用三目运算符
- 条件表达式中，优先使用正向语义，以便于理解代码逻辑

### 性能注解

使用 `@HighFrequencyInvocation` 注解聚焦关键方法性能的优化：

**使用时机：**
- 请求频繁调用的链路，标注其中高频调用的类、方法或构造器，标注范围精确匹配
- `canBeCached = true` 表示该目标为可复用的缓存资源（如：数据库连接）

**标注代码段内的禁止项：**
- 禁止调用 Java Stream API
- 禁止通过 `+` 拼接字符串
- 禁止调用 `LinkedList` 的 `get(int index)` 方法

### 注释 & 日志规范

- 日志与注释一律使用英文
- 注释只能包含 JAVADOC、TODO 和 FIXME
- 公开的类和方法必须有 JAVADOC，对用户的 API 和 SPI 的 JAVADOC 需要写的清晰全面
- 其他类和方法以及覆盖自父类的方法无需 JAVADOC
- 需要注释解释的代码尽量提成小方法，用方法名称解释

## 测试标准

### AIR 设计理念（不可妥协）

**自动化（Automatic）：**
- 单元测试应全自动执行，而非交互式
- 禁止人工检查输出结果
- 不允许使用 `System.out`、`log` 等，必须使用断言进行验证

**独立性（Independent）：**
- 禁止单元测试用例间的互相调用
- 禁止依赖执行的先后次序
- 每个单元测试均可独立运行

**可重复执行（Repeatable）：**
- 单元测试不能受到外界环境的影响
- 可以重复执行

### BCDE 设计原则

**边界值测试（Border）：**
- 通过循环边界、特殊数值、数据顺序等边界的输入，得到预期结果

**正确性测试（Correct）：**
- 通过正确的输入，得到预期结果

**合理性设计（Design）：**
- 与生产代码设计相结合，设计高质量的单元测试

**容错性测试（Error）：**
- 通过非法数据、异常流程等错误的输入，得到预期结果

### 测试命名与结构

- 使用 `assert` 前缀命名所有的测试用例
- 单元测试必须通过公共 API 验证行为，禁止通过反射调用私有成员
- 若测试必须通过反射访问字段，应使用 `Plugins.getMemberAccessor()`
- 当某个生产方法只由一个测试用例覆盖时，测试方法命名为 `assert<MethodName>`，无额外后缀
- 每个公有方法使用一个独立的测试方法，测试方法顺序在可行时与生产方法保持一致
- 参数化测试需通过参数提供显示名，并使用 `"{0}"` 作为展示名模板
- 每个测试用例需精确断言，尽量不使用 `not`、`containsString` 断言
- 准备环境的代码和测试代码分离
- 只有 Mockito、JUnit Assertions、Hamcrest CoreMatchers 和 MatcherAssert 相关可以使用 static import

### 断言规范

- 布尔类型断言应使用 `assertTrue` 和 `assertFalse`
- 空值断言应使用 `assertNull` 和 `assertNotNull`
- 其他类型断言应使用 `assertThat(actual, is(expected))` 代替 `assertEquals`
- 类型断言使用 `assertThat(..., isA(...))` 代替 `instanceOf`
- 禁用 `assertSame` / `assertNotSame`，使用 `assertThat(actual, is(expected))` 或 `assertThat(actual, not(expected))`
- 使用 Hamcrest 匹配器（如 `is()`、`not()`）来进行精确且可读性高的断言
- 测试用例的真实值应命名为 `actual Xxx`，期望值应命名为 `expected Xxx`
- 测试类和 `@Test` 标注的方法无需 JAVADOC

### Mock 使用规范

使用 mock 的场景：
- 单元测试需要连接某个环境时，应使用 mock
- 单元测试包含不容易构建的对象时（例如：超过两层嵌套并且和测试无关的对象），应使用 mock

模拟静态方法或构造器：
- 应优先考虑使用测试框架提供的 `AutoMockExtension` 和 `StaticMockSettings` 自动释放资源
- 若使用 Mockito `mockStatic` 和 `mockConstruction` 方法，必须搭配 try-with-resource 或在清理方法中关闭，避免泄漏

其他规则：
- 校验仅有一次调用时，无需使用 `times(1)` 参数，使用 `verify` 的单参数方法即可
- 深度链式交互使用 Mockito 的 `RETURNS_DEEP_STUBS`，不要层层手动 mock
- 测试数据应使用标准化前缀（如 `foo_`/`bar_`）明确标识其测试用途
- 使用 `PropertiesBuilder` 简化 `Properties` 构造

## 技术约束

### JDK 兼容性

- 后端代码必须使用 JDK 8 语法
- 保持最低 JDK 8 的兼容

### 代码风格强制执行

- 后端代码风格必须遵循 ShardingSphere 的 spotless 与 checkstyle
- **Java 代码中不允许使用无用的空行，所有代码连续编写**
- 代码的命名必须方便理解
- 通过合理的类、方法拆分，保持优雅代码

### 架构设计

- 项目模块参考 360 QuickSql，采用多个子模块
- 保证良好的设计和扩展性

### 安全要求

- 项目中所有的依赖都使用最新 release 版本
- 不要有 CVE 安全漏洞

## 代码提交规范

### 构建流程

提交代码前必须：

1. 确保遵守编码规范
2. 确保构建流程中的各个步骤都成功完成：
   - Apache 协议文件头检查
   - Checkstyle 检查
   - 编译
   - 单元测试
3. 执行构建命令：`./mvnw clean install -B -T1C -Pcheck`
4. 通过 Spotless 统一代码风格：`./mvnw spotless:apply -Pcheck`

### 覆盖率要求

- 确保覆盖率不低于 master 分支
- 除去简单的 getter/setter 方法，单元测试需全覆盖

### 提交指南

- 应尽量将设计精细化拆分
- 做到小幅度修改，多次数提交
- 但应保证提交的完整性

### IDE 配置

- 如果您使用 IDEA，可导入 `src/resources/idea/code-style.xml`，用于保持代码风格一致性
- 如果您使用 IDEA，可导入 `src/resources/idea/inspections.xml`，用于检测代码潜在问题

## 治理规则

### 修订程序

1. 章程变更需要记录：
   - 修改的具体章节
   - 变更的理由
   - 现有代码的迁移计划（如适用）
2. 所有 PR 和审查必须验证是否符合本章程
3. 引入的任何复杂性必须对照"精简"原则进行论证

### 版本策略

- **主版本（MAJOR）**：不向后兼容的治理/原则删除或重新定义
- **次版本（MINOR）**：新增原则/章节或实质性扩展的指导
- **补丁版本（PATCH）**：澄清、措辞修正、非语义性的改进

### 合规审查

- 所有代码审查必须包含章程合规性检查
- 违规必须在合并前记录理由或修复
- 复杂性论证必须引用被拒绝的具体替代方案

**版本**: 1.0.1 | **批准日期**: 2026-02-16 | **最后修订**: 2026-02-17
