# IntelliSql 贡献指南

## 基础要求

IntelliSql 采用 Apache 软件基金会行为准则，并要求所有贡献体现责任心、专业性与对代码质量的敬畏。

## 核心原则

### 1. 用心

以工匠精神对待每一项贡献。

- 每一项变更都必须体现深思熟虑和专业敬业。
- 代码必须被视为工艺品，而不只是实现结果。
- 持续改进和对细节的关注属于强制要求。

理由：高质量软件依赖每位贡献者的真诚投入和专业承诺。

### 2. 可读

代码意图必须通过阅读自然显现。

- 通过清晰命名和结构实现自文档化。
- 读者必须能够在不调试的情况下理解代码逻辑。
- 复杂逻辑必须拆分为命名良好的方法。

理由：代码被阅读的次数远多于被编写；可读性直接决定可维护性。

### 3. 整洁

遵循整洁代码和重构理念。

- 必须持续贯彻整洁代码原则。
- 代码质量下降时必须重构。
- 技术债务必须及时偿还。

理由：整洁代码减少缺陷，提高可维护性，并保持交付效率。

### 4. 一致

风格、命名和使用方式在项目中保持一致。

- 所有代码都必须遵循相同的格式化和命名约定。
- 相似问题应当用相似方式解决。
- 命名、目录结构、异常处理、日志风格都应保持一致。

理由：一致性可以降低认知负担，加快理解速度。

### 5. 精简

用最少的代码表达最准确的意图。

- 及时删除无用代码。
- 每一行代码都必须有明确目的。
- DRY 原则属于强制要求。
- 重复配置同样属于重复，必须消除。

理由：更少的代码意味着更少的缺陷、更容易维护和更清晰的理解。

### 6. 抽象

保持职责边界和抽象层次清晰。

- 每一层都必须具有单一职责。
- 概念必须被正确提炼并准确命名。
- 单个类或方法中不得混合多个抽象层次。

理由：合理抽象使复杂系统可以在正确层次上被理解和推理。

### 7. 极致

每一行代码、每一个字符、每一个空格都要有存在价值。

- 禁止保留无跟踪的占位符代码和 `TODO`。
- 禁止无意义的导入、变量和语句。
- 代码审查应当质疑一切没有明确价值的元素。

理由：局部细节上的卓越会累积成整体系统的卓越。

## 编码规范

### 格式化规则

- 使用 LF 换行符。
- 单行代码在 200 字符以内时可保持单行。
- 禁止无意义的空行。
- 方法体内不要使用装饰性空行分隔语句。
- 变量声明与后续代码之间不要插入空行。
- 不要用空行分隔逻辑块；应提炼为私有方法。
- 配置文件命名使用 spinal-case。

示例：

```java
// 错误：包含不必要的空行
@Test
void assertConfigParsing() {
    String yaml = Files.readString(Paths.get("conf/model.yaml"));
    ModelConfig config = ConfigLoader.parse(yaml);

    assertThat(config.getDataSources(), hasSize(2));
    assertThat(config.getDataSources().get(0).getType(), is(DataSourceType.MYSQL));
}

// 正确：代码连续书写
@Test
void assertConfigParsing() {
    String yaml = Files.readString(Paths.get("conf/model.yaml"));
    ModelConfig config = ConfigLoader.parse(yaml);
    assertThat(config.getDataSources(), hasSize(2));
    assertThat(config.getDataSources().get(0).getType(), is(DataSourceType.MYSQL));
}
```

### 命名规范

通用规则：

- 命名必须做到顾名思义。
- 类名和方法名避免使用缩写。
- 部分变量名允许使用有限缩写。

标准缩写：

- `arguments` -> `args`
- `parameters` -> `params`
- `environment` -> `env`
- `properties` -> `props`
- `configuration` -> `config`

专有名词缩写：

- 三位以内缩写保持大写，例如 `SQL92Lexer`、`XMLTransfer`、`MySQLAdminExecutorCreator`。
- 超过三位的缩写使用驼峰形式，例如 `JdbcUrlAppender`、`YamlAgentConfigurationSwapper`。
- 变量始终使用小驼峰，例如 `mysqlAuthenticationMethod`、`sqlStatement`、`mysqlConfig`。

局部变量规则：

- 除了直接返回入参的场景，返回变量统一命名为 `result`。
- 集合循环变量使用 `each`。
- `Map` 条目使用 `entry`。
- 捕获异常命名为 `ex`。
- 明确忽略的异常命名为 `ignored`。
- 方法入参禁止命名为 `result`、`each`、`entry`。
- 工具类统一使用 `XxxUtils` 命名。

条件表达式规则：

- `equals` 和 `==` 比较时，常量放左侧，变量放右侧。
- 大于小于比较时，变量放左侧，常量放右侧。

### 代码风格规则

- 除构造器参数消歧或字段同名赋值外，避免使用 `this`。
- 局部变量不要声明为 `final`。
- 除为继承设计的抽象类外，优先将类设计为 `final`。
- 嵌套循环应尽量提炼为方法。
- 成员变量定义顺序和参数传递顺序在相关类和方法中保持一致。
- 优先使用卫语句。
- 类和方法使用最小可行访问权限。
- 私有辅助方法应紧跟其调用方，并按出现顺序排列。
- 方法参数和返回值不应为 `null`。
- 方法参数禁止使用 `Optional`；应传递普通值。仅在 API 合约要求时允许 `null`。
- 优先使用 Lombok 生成构造器、访问器和日志字段。
- 禁止内联全限定类名，必须通过 import 引入。
- 优先使用 `LinkedList`；仅在需要按下标访问时使用 `ArrayList`。
- `ArrayList`、`HashMap` 等可扩容集合必须显式指定初始容量。
- 简单返回和赋值分支优先使用三目运算符。
- 三目运算符保持单层结构。
- 条件表达式优先使用正向语义。

### 性能注解

使用 `@HighFrequencyInvocation` 标记需要重点优化性能的代码路径。

适用场景：

- 某个类、方法或构造器位于高频请求链路上。
- `canBeCached = true` 用于标识可复用缓存资源，例如数据库连接。

在被标记代码中，避免：

- Java Stream API
- 通过 `+` 拼接字符串
- `LinkedList#get(int index)`

### 注释与日志

- 日志和注释统一使用英文。
- 仅允许 `JAVADOC`、`TODO`、`FIXME` 三类注释。
- 公开类和公开方法必须提供 `JAVADOC`。
- 面向用户的 API 和 SPI 文档必须完整清晰。
- 其他类、内部方法和覆写方法无需 `JAVADOC`。
- 优先通过拆分小方法表达意图，而不是依赖行内注释。

## 测试标准

### AIR 原则

Automatic：

- 单元测试必须全自动执行。
- 禁止人工检查输出结果。
- 必须使用断言完成验证，不能依赖 `System.out` 或日志。

Independent：

- 测试用例之间不能互相调用。
- 测试执行顺序不能影响结果。
- 每个单元测试都必须可独立运行。

Repeatable：

- 测试不能依赖外部环境。
- 测试必须可重复执行。

### BCDE 原则

- Border：覆盖循环边界、特殊值和数据顺序等边界场景。
- Correct：验证合法输入下的预期结果。
- Design：测试设计应服务于生产代码设计质量。
- Error：验证非法输入和异常流程下的预期结果。

### 测试命名与结构

- 所有测试用例统一使用 `assert` 前缀。
- 仅通过公共 API 验证行为。
- 禁止通过反射调用私有成员。
- 如必须通过反射访问字段，应使用 `Plugins.getMemberAccessor()`。
- 若一个生产方法仅由一个测试覆盖，测试名使用 `assert<MethodName>`。
- 在可行情况下，每个公有生产方法对应一个独立测试方法。
- 在可行情况下，测试方法顺序与生产方法顺序保持一致。
- 参数化测试必须通过参数提供展示名，并使用 `"{0}"` 作为模板。
- 断言应保持精确，减少 `containsString` 这类模糊断言。
- 环境准备代码和验证代码应明确分离。
- static import 仅限 Mockito、JUnit assertions、Hamcrest `CoreMatchers` 和 `MatcherAssert`。

### 断言规范

- 布尔断言使用 `assertTrue` 和 `assertFalse`。
- 空值断言使用 `assertNull` 和 `assertNotNull`。
- 其他值断言使用 `assertThat(actual, is(expected))`，替代 `assertEquals`。
- 类型断言使用 `assertThat(..., isA(...))`，替代 `instanceOf`。
- 用值断言替代 `assertSame` 和 `assertNotSame`。
- 使用 Hamcrest 匹配器如 `is()`、`not()` 提高精确性和可读性。
- 实际值变量命名为 `actualXxx`。
- 期望值变量命名为 `expectedXxx`。
- 测试类和 `@Test` 方法不需要 `JAVADOC`。

### Mock 规范

以下场景使用 mock：

- 单元测试原本需要连接外部环境。
- 单元测试依赖难以构造且与验证无关的复杂对象，例如多层嵌套对象图。

静态方法和构造器模拟：

- 优先使用测试框架提供的 `AutoMockExtension` 和 `StaticMockSettings`，以自动释放资源。
- 若使用 Mockito `mockStatic` 或 `mockConstruction`，必须结合 try-with-resources 或显式清理。

其他规则：

- 验证单次调用时，直接使用 `verify(...)`，不要写 `times(1)`。
- 深层链式交互使用 Mockito `RETURNS_DEEP_STUBS`，避免逐层手工 mock。
- 测试数据使用 `foo_`、`bar_` 等标准前缀标识用途。
- 使用 `PropertiesBuilder` 简化 `Properties` 构造。

## 技术约束

### JDK 兼容性

- 后端代码必须使用 JDK 8 语法。
- 最低兼容版本保持为 JDK 8。

### 风格强制执行

- 后端代码必须符合 ShardingSphere 的 `spotless` 和 `checkstyle` 规范。
- Java 代码必须保持无无意义空行。
- 命名必须便于理解。
- 通过合理拆分类和方法保持代码优雅。

### 架构设计

- 项目模块参考 360 QuickSql 的多模块方向。
- 设计应服务于可扩展性和可维护性。

### 安全要求

- 依赖统一使用最新 release 版本。
- 项目不得存在已知 CVE 漏洞。

## 提交流程要求

### 构建流程

提交代码前：

1. 确保完全遵守编码规范。
2. 确保完整构建流程全部通过：
   - Apache 协议文件头检查
   - Checkstyle
   - 编译
   - 单元测试
3. 执行 `./mvnw clean install -B -T1C -Pcheck`。
4. 执行 `./mvnw spotless:apply -Pcheck`。

### 覆盖率

- 覆盖率不得低于 `master` 分支。
- 除简单 getter 和 setter 外，生产代码应具备完整单元测试覆盖。

### 提交建议

- 设计变更要细粒度拆分。
- 提交尽量保持小而聚焦。
- 每次提交都必须保持语义完整。

### IDEA 配置

- 在 IntelliJ IDEA 中导入 `src/resources/idea/code-style.xml` 以统一格式。
- 在 IntelliJ IDEA 中导入 `src/resources/idea/inspections.xml` 以提前发现潜在问题。

## 治理规则

### 修订流程

1. 章程变更必须记录：
   - 具体修改章节
   - 修改原因
   - 现有代码迁移计划（如适用）
2. 每个 PR 和评审都必须验证本文档合规性。
3. 任何新增复杂度都必须依据“精简”原则给出论证。

### 版本策略

- MAJOR：不兼容的治理变更、原则删除或原则重定义
- MINOR：新增原则、新增章节或指导内容的实质扩展
- PATCH：澄清、措辞修正和非语义改进

### 合规审查

- 每次代码评审都必须包含本文档合规检查。
- 违规项必须在合并前修复或明确记录理由。
- 复杂度论证必须说明被拒绝的具体替代方案。

版本：1.0.1  
批准日期：2026-02-16  
最后修订：2026-02-17
