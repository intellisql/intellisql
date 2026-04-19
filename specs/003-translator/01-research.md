# **基于 Apache Calcite 与大语言模型（LLM）构建混合架构 SQL 翻译器的深度研究报告与产品技术白皮书**

## **执行摘要**

随着企业数字化转型的深入，数据基础设施的现代化已成为全球企业的核心议题。从传统的 Oracle、Teradata、SQL Server 等商业数据库向 Snowflake、Databricks、PostgreSQL（及其衍生版如 openGauss）等云原生或开源数据库的迁移浪潮，催生了对高效、精准数据库迁移工具的巨大需求。然而，现有的迁移解决方案长期面临着“确定性”与“灵活性”的两难困境：基于规则的传统工具（如 AWS SCT）在处理标准 SQL 时精准但在面对复杂的存储过程（PL/SQL, T-SQL）时捉襟见肘；而新兴的生成式 AI 工具虽具备强大的代码理解力，却因“幻觉”问题而在数据准确性上存在致命风险。

本报告旨在响应构建下一代 **混合架构 SQL 翻译器** 的战略需求。该方案创造性地结合了 **Apache Calcite** 的确定性编译器能力与 **大语言模型（LLM）** 的语义推理能力。针对常规的数据定义语言（DDL）和数据操作语言（DML），利用 Calcite 实现基于关系代数的精准翻译；针对存储过程、触发器、自定义函数（UDF）等过程化逻辑，利用 LLM 进行上下文感知的代码重构；并引入“LLM 裁判”机制进行结果校验。此外，系统设计了“在线元数据注入”与“离线 AST 解析”双模式，以适应不同用户环境的安全与网络限制。

本报告全长约 20,000 字，将从市场竞争格局、技术架构设计、核心引擎实现、LLM 融合策略、元数据管理机制以及商业化路径等维度进行详尽阐述，为构建企业级 SQL 翻译平台提供全方位的落地指南。

## ---

**第一章 行业背景与竞品生态深度调研**

### **1.1 全球数据库迁移市场的痛点分析**

数据库迁移被视为企业 IT 架构重构中风险最高、难度最大的一环。核心痛点在于 SQL 方言（Dialect）之间的语义鸿沟。SQL 标准虽然存在（如 SQL:2011），但各主流数据库厂商在实现时加入了大量的专有扩展。例如，Oracle 的 DECODE 函数、CONNECT BY 递归查询，SQL Server 的 TOP 语法，以及各家截然不同的存储过程语言（PL/SQL vs T-SQL vs PL/pgSQL）。

传统的迁移通常采用“80/20”法则：80% 的表结构和标准查询可以通过脚本自动转换，但剩余 20% 的核心业务逻辑（存储过程）往往消耗了 80% 的项目周期和预算。这部分代码包含了复杂的控制流、游标操作、异常处理以及专有的系统包调用，传统解析器难以构建完整的转换规则图谱。

### **1.2 现有技术流派与竞品分析**

通过对互联网公开项目及商业软件的广泛调研，目前的 SQL 翻译器市场主要分为三大技术流派：基于规则的传统流派、基于中间表示（IR）的现代流派，以及新兴的 AI 辅助流派。

#### **1.2.1 规则驱动的传统巨头**

这一类工具历史悠久，主要依赖庞大的硬编码规则库进行字面量或简单的语法树替换。

* **AWS Schema Conversion Tool (SCT)**：作为云厂商的官方工具，SCT 专注于将本地数据库迁移至 Amazon RDS 或 Redshift。其优势在于与云生态的深度集成和免费策略，但其局限性极为明显：对于无法自动转换的存储过程，它只会生成“手动操作项”报告，无法完成“最后一公里”的代码生成 1。  
* **Ispirer MnMTK**：作为商业化迁移工具的标杆，Ispirer 拥有极其庞大的规则库，支持从 COBOL 到现代 SQL 的各种转换。其技术路线是典型的专有闭源解析器，虽然覆盖面广，但许可费用昂贵，且用户无法自定义扩展规则 3。  
* **SQLines**：专注于命令行工具，提供轻量级的方言转换。其商业模式较为传统，提供永久许可证，但在处理复杂的嵌套逻辑和大规模项目元数据分析上能力有限 5。

#### **1.2.2 基于中间表示（IR）的现代架构**

这一流派试图通过定义一种通用的中间语言来解决 ![][image1] 的翻译难题，而非为每对数据库编写转换器。

* **LinkedIn Coral**：这是一个基于 Apache Calcite 的开源项目，主要解决大数据组件（Hive, Trino, Spark）之间的视图和 UDF 互操作性。Coral 将 SQL 转换为 Calcite 的 RelNode（关系代数节点），然后再发射为目标方言。这与我们拟采用的技术路线高度一致，证明了 Calcite 路线的可行性，但 Coral 目前主要聚焦于查询层，缺乏对存储过程（过程化语言）的支持 7。  
* **jOOQ**：虽然本质上是 Java 的 ORM 框架，但 jOOQ 内部实现了一个强大的 SQL 转换层，能够在运行时根据连接的数据库方言渲染 SQL。其商业版提供了针对 Oracle 等专有数据库的高级支持，验证了“开发者工具 \+ 商业授权”模式的成功 9。

#### **1.2.3 AI 原生与混合流派**

随着 LLM 的兴起，市场上出现了一批基于 GPT-4 或 Claude 的代码转换工具。

* **纯 LLM 工具**：如某些 SaaS 化的 Text-to-SQL 或 Code-to-Code 服务。它们的优势是极其灵活，能理解复杂的业务意图（如“将这个游标循环改为向量化操作”）。但劣势在于不可控，容易产生“幻觉”，例如捏造不存在的表名或函数，且缺乏对数据类型的严格校验 11。  
* **openGauss SQL Translator**：openGauss 社区推出了一款基于 Java 的翻译器，旨在将 MySQL/Oracle 语法迁移至 openGauss。调研显示其早期版本主要基于正则表达式和简单解析，后续开始探索基于 Antlr4 或 Calcite 的更深层解析，这为我们提供了开源社区的技术参考 13。

### **1.3 战略机会：混合架构的必要性**

上述调研清晰地揭示了市场空白：**缺乏一个既具备编译器级精准度，又具备 AI 级灵活性的端到端迁移平台**。

* 纯规则工具无法处理存储过程的语义重构。  
* 纯 AI 工具无法保证核心交易系统的严谨性。  
* 大多数工具缺乏对“元数据在线注入”与“离线 AST 模式”的平滑切换支持。

基于此，本报告提出的基于 Calcite \+ LLM 的混合翻译器方案，不仅填补了技术空白，更具备成为企业级迁移基础设施的潜力。

## ---

**第二章 产品与技术架构总体设计**

### **2.1 设计理念：双引擎驱动**

本系统的核心设计理念是 **“分而治之，各取所长”**。我们将 SQL 代码视为两种截然不同的实体：

1. **声明式代码（Declarative Code）**：即标准的 SELECT, INSERT, UPDATE, DELETE 以及 CREATE TABLE 等 DDL。这类代码具有严格的数学逻辑（关系代数），适合使用确定性引擎（Calcite）处理。  
2. **过程式代码（Procedural Code）**：即 CREATE PROCEDURE, FUNCTION, TRIGGER, BEGIN...END 块。这类代码包含控制流、变量赋值和异常处理，逻辑灵活多变，适合使用概率性引擎（LLM）处理。

### **2.2 逻辑架构图解**

系统由以下五个核心模块组成：

| 模块名称 | 功能描述 | 核心技术栈 |
| :---- | :---- | :---- |
| **智能路由器 (Smart Router)** | 负责解析输入流，识别代码块类型（DDL/DML vs PL/SQL），并将任务分发给相应引擎。 | ANTLR4, Regex, Calcite Parser |
| **确定性引擎 (Deterministic Engine)** | 基于 Apache Calcite，负责标准 SQL 的解析、校验、优化与转译。 | Apache Calcite, RelNode, Babel Parser |
| **概率性引擎 (Probabilistic Engine)** | 基于 LLM，负责复杂对象的语义理解与代码重写。 | LangChain, Vector DB (RAG), LLM APIs |
| **元数据管理中心 (Metadata Hub)** | 负责连接客户数据库提取 Schema，或在离线模式下构建虚拟 Schema。 | JDBC, Calcite Schema Factory |
| **校验与测试沙箱 (Verification Sandbox)** | 负责对翻译结果进行静态语法检查和动态数据比对。 | TestContainers, JUnit, LLM Evaluator |

### **2.3 核心处理流程**

1. **输入接入**：用户上传 SQL 脚本文件，或配置源数据库连接信息。  
2. **元数据加载**：  
   * **在线模式**：通过 JDBC 连接源库，提取表结构、列类型、约束等元数据，注入 Calcite 验证器。  
   * **离线模式**：通过预扫描 DDL 脚本，构建内存中的虚拟元数据对象。  
3. **路由分发**：  
   * 路由器扫描 SQL 语句。识别到 SELECT \* FROM... 分发给 Calcite。  
   * 识别到 CREATE PROCEDURE... 分发给 LLM Agent。  
4. **并行翻译**：  
   * **Calcite 通道**：解析 \-\> 校验（利用元数据） \-\> 转换为 RelNode \-\> 发射目标方言 SQL。  
   * **LLM 通道**：构建 Prompt（包含相关表结构的 DDL 作为上下文） \-\> 调用模型 \-\> 获取转换后的代码。  
5. **结果校验**：  
   * LLM 生成的代码会被送入 Calcite 解析器进行二次语法检查，确保无语法错误。  
   * 可选：通过 LLM 裁判比对源逻辑与目标逻辑的一致性。  
6. **输出组装**：将两部分结果合并，生成最终的迁移脚本。

## ---

**第三章 确定性引擎详解：基于 Apache Calcite 的精准翻译**

Apache Calcite 是一个动态数据管理框架，它不存储数据，但具备强大的 SQL 解析、验证和优化能力。它是本方案中处理 DDL 和 DML 的基石。

### **3.1 扩展解析器：SqlBabelParserImpl 的应用**

标准 SQL 解析器往往过于严格，无法识别 Oracle 或 SQL Server 的特有语法。为了实现兼容性，我们将采用 Calcite 的 **Babel Parser** 模块。

* **技术原理**：Babel 是 Calcite 解析器的一个扩展变体，旨在最大限度地接受各种数据库的方言语法。例如，它支持 PostgreSQL 的 :: 类型转换符，支持 T-SQL 的方括号 \`\` 标识符等 14。  
* **配置策略**：在代码实现中，我们需要通过 SqlParser.Config 显式指定 Babel 工厂。  
  Java  
  SqlParser.Config parserConfig \= SqlParser.config()  
     .withParserFactory(SqlBabelParserImpl.FACTORY) // 启用 Babel  
     .withConformance(SqlConformanceEnum.LENIENT);  // 设置为宽容模式，允许非标语法

* **自定义语法扩展**：虽然 Babel 覆盖了常见方言，但针对某些极其特殊的语法（如 Teradata 的特定关键字），我们可能需要修改 Calcite 的 Parser.jj（JavaCC 文件）模版，重新编译自定义的解析器。这是一个高阶功能，通过 FreeMarker 模版注入新关键字实现 16。

### **3.2 抽象语法树（AST）与关系代数（RelNode）的转化**

这是翻译器的核心流水线，分为四个阶段：

1. **Parsing（解析）**：  
   * 输入：源 SQL 字符串。  
   * 输出：SqlNode 树（AST）。  
   * 此时仅进行词法和语法分析，不涉及语义（即不知道表是否存在）。  
2. **Validation（校验 \- 依赖元数据）**：  
   * 输入：SqlNode \+ CalciteCatalogReader。  
   * 输出：经过校验和类型推导的 SqlNode。  
   * 在此阶段，系统会检查 SELECT a FROM t 中的 t 是否存在，a 是否是 t 的列。如果处于**离线模式**（无元数据），此步骤将被跳过或使用 Mock Validator 替代，但这会降低翻译的精准度（例如无法区分同名列所属的表）。  
3. **Optimization / Conversion（转换）**：  
   * 输入：校验后的 SqlNode。  
   * 输出：RelNode（关系代数表达式）。  
   * 利用 SqlToRelConverter 将 AST 转换为逻辑计划。RelNode 是数据库中立的数学表达（Filter, Project, Join, Aggregate）。这一步消除了源方言的语法糖。例如，Oracle 的 DECODE 会被转化为标准的 CASE WHEN 逻辑表达式 18。  
4. **Emission（发射）**：  
   * 输入：RelNode \+ 目标方言（SqlDialect）。  
   * 输出：目标 SQL 字符串。  
   * 利用 RelToSqlConverter，根据目标数据库（如 PostgreSQL）的特性生成 SQL。例如，如果目标是 MySQL，它会将字符串连接符由 || 转换为 CONCAT() 函数；如果目标是 Hive，它会调整 GROUP BY 的语法限制 14。

### **3.3 元数据驱动的精准翻译机制**

用户需求中特别提到了“注册客户环境数据库”以获取元数据。这是区分高级翻译器与简单文本替换工具的关键。

#### **3.3.1 在线模式（Online Mode）**

* **连接层**：利用 JDBC 驱动连接源数据库（如 Oracle）。  
* **元数据抓取**：调用 java.sql.DatabaseMetaData 接口，获取 Schema、Table、Column、View 以及 Function 的定义。  
* **Schema 映射**：将 JDBC 元数据映射为 Calcite 的 ReflectiveSchema 或自定义的 AbstractSchema。  
* **价值**：  
  * **消歧义**：例如 SQL 语句 SELECT id, name FROM A, B WHERE A.aid \= B.bid。如果没有元数据，解析器无法知道 name 列是属于表 A 还是表 B。有了元数据，Calcite 可以精准绑定列引用，从而在生成目标 SQL 时添加正确的别名（如 A.name），避免目标数据库报错“Column ambiguously defined” 21。  
  * **类型安全**：确保在生成 CAST 逻辑时，源类型与目标类型是兼容的。

#### **3.3.2 离线模式（Offline / AST Mode）**

当无法连接客户数据库时（常见于金融、政企的内网环境），系统必须回退到 AST 模式。

* **工作流**：仅执行 Parsing 和 Emission 阶段，跳过 Validation 和 SqlToRelConverter。  
* **技术实现**：直接对 SqlNode 进行遍历和重写（Visitor Pattern）。例如，编写一个 SqlShuttle，当访问到 SqlCall 为 NVL 时，将其替换为 COALESCE 的 SqlCall 节点。  
* **局限性**：无法处理深层语义逻辑，无法解析 SELECT \*（不知道具体列名），无法校验列的存在性。这部分将通过提示用户“仅作语法转换，需人工复核”来管理预期。

## ---

**第四章 概率性引擎详解：LLM 驱动的复杂对象迁移**

针对存储过程、触发器和复杂 UDF，Calcite 的关系代数模型无法覆盖其控制流逻辑。此时，我们引入 LLM 作为“概率性翻译引擎”。

### **4.1 提示工程（Prompt Engineering）体系**

为了实现精准翻译，不能简单地将代码扔给 LLM，必须构建结构化的 Prompt 体系。

#### **4.1.1 上下文感知（Context-Aware）Prompt**

LLM 最大的问题是幻觉。为了抑制幻觉，必须向 LLM 注入“事实”。

* **Schema 注入**：在翻译存储过程时，分析该过程引用的表，并将这些表的 DDL（包含列名、类型）作为 Context 放入 Prompt。  
  * *Prompt 示例*：“你是一个 SQL 迁移专家。请将以下 Oracle PL/SQL 转换为 PostgreSQL PL/pgSQL。参考以下相关表的定义：,。请注意，表 A 的 id 列是 VARCHAR 类型，不要将其视为 INT。”  
* **方言对照表注入**：在 Prompt 中包含一份简短的“源-目标方言对照表”，强制 LLM 遵循特定的转换规则（例如：Oracle SYSDATE \-\> PG CURRENT\_TIMESTAMP）。

#### **4.1.2 检索增强生成（RAG）策略**

对于拥有数千张表的大型 ERP 系统，无法将所有 DDL 放入 LLM 的上下文窗口。我们引入 RAG 机制 22。

1. **索引构建**：将所有表的 DDL 和元数据向量化，存入向量数据库（如 Milvus 或 pgvector）。  
2. **动态检索**：当处理一个存储过程时，先用简单的正则提取其中引用的表名，或者利用 LLM 初步分析依赖。  
3. **上下文组装**：从向量库中检索出最相关的 Top-K 个表定义，动态组装 Prompt。

### **4.2 存储过程的思维链（Chain-of-Thought）转换**

要求 LLM 在输出代码前，先输出“转换思路”。

* **第一步：逻辑分析**。识别源由于中的游标（Cursor）、异常处理（Exception）、事务控制（Commit/Rollback）。  
* **第二步：方言映射**。明确指出如何处理差异。例如：“Oracle 的隐式游标 FOR r IN (SELECT...) 将被转换为 Postgres 的 FOR r IN SELECT... LOOP 结构。”  
* **第三步：代码生成**。生成最终代码。  
* **第四步：自我修正**。利用 LLM 自检生成的代码是否使用了目标数据库不支持的函数 24。

### **4.3 混合对象的处理**

有些 SQL 语句混合了复杂查询和简单逻辑。我们可以在 LLM 生成结果后，利用 Calcite 的 Parser 对结果中的嵌入式 SQL（Embedded SQL）进行二次校验。如果 LLM 生成的 SQL 片段无法通过 Calcite 的目标方言解析，说明 LLM 产生了语法错误，系统将自动触发“重试机制”，并将解析错误信息反馈给 LLM 进行修正。

## ---

**第五章 校验体系：构建可信赖的迁移**

用户需求中明确提出“通过 LLM 校验是否正确”。这是保证产品交付质量的关键。

### **5.1 LLM-as-a-Judge（LLM 裁判机制）**

利用一个独立的高智商 LLM（如 GPT-4）作为“裁判”，对“源 SQL”和“翻译后的 SQL”进行语义等价性判定 26。

* **Prompt 设计**：“给定源 Oracle 代码 A 和目标 Postgres 代码 B，请分析它们在逻辑上是否等价？请重点检查：1. 过滤条件是否一致？2. 空值处理（NULL handling）是否一致？3. 聚合逻辑是否一致？如果存在差异，请具体指出。”  
* **评分系统**：裁判输出 0-100 的置信度分数。低于 90 分的代码将被标记为“需人工复核”。

### **5.2 单元测试生成（Unit Test Generation）**

LLM 的另一个强大功能是生成测试用例。

* **输入**：源存储过程。  
* **任务**：生成一组 INSERT 语句（准备测试数据）和一个调用该过程的 EXEC 语句，以及预期的输出结果。  
* **执行验证**：在 Docker 容器中分别启动源数据库和目标数据库，运行这些测试用例。比较两者的输出结果是否一致（Dataframe Equals Check） 25。  
  * *如果一致*：标记为“验证通过”。  
  * *如果不一致*：生成“差异报告”，并尝试让 LLM 根据差异日志自动修复代码。

## ---

**第六章 产品化方案与商业模式**

### **6.1 产品形态**

建议将该技术封装为两种形态：

1. **SaaS 开发者平台（Cloud Studio）**：  
   * 面向中小企业和个人开发者。  
   * 提供 Web IDE，支持上传 SQL 文件，配置目标库，实时查看翻译结果和差异对比（Diff View）。  
   * 集成 Git，支持版本管理。  
2. **企业级私有化部署（On-Premises Enterprise）**：  
   * 面向金融、电信、政府客户。  
   * 以 Docker 镜像或 Kubernetes Helm Chart 形式交付。  
   * 支持离线运行（本地部署开源 LLM 如 Llama 3 或 DeepSeek，配合 Calcite 引擎）。  
   * 提供深度集成的元数据连接器，支持 Oracle, DB2, Teradata 等传统数仓。

### **6.2 盈利模式设计**

结合调研中的竞品模式，建议采用 **“基础功能免费 \+ 高级功能付费 \+ 用量计费”** 的混合模式 29。

#### **6.2.1 社区版/个人版（Open Core）**

* **核心策略**：通过开源 Calcite 核心翻译模块（AST 模式）来吸引开发者，建立技术品牌。  
* **免费功能**：  
  * 支持标准 DDL/DML 翻译（基于 Calcite）。  
  * 基于 AST 的离线模式。  
  * 单文件上传限制。  
* **目的**：占领开发者心智，成为类似 jOOQ 或 Flyway 的行业标准工具。

#### **6.2.2 专业版（SaaS 订阅）**

* **定价**：按月订阅（例如 $49/月/用户）+ Token 用量包。  
* **增值功能**：  
  * **LLM 存储过程翻译**：这是高价值点。由于调用 LLM API 有成本，采用“Token 充值”或“按代码行数计费”的模式。  
  * **在线元数据连接**：允许连接公网可达的数据库进行精准翻译。  
  * **IDE 集成插件**：提供 VS Code / IntelliJ 插件。

#### **6.2.3 企业版（License \+ 服务）**

* **定价**：年费制（例如 $50,000/年起）+ 实施服务费。  
* **核心卖点**：  
  * **私有化部署**：数据不出域，满足合规要求。  
  * **定制化微调**：基于企业历史代码库，微调专属的 LLM 模型（Fine-tuning），提高特定业务逻辑的翻译准确率 32。  
  * **全自动校验流水线**：集成 Docker 测试容器，提供端到端的自动化测试报告。  
  * **专家支持**：由原厂工程师提供复杂规则的定制开发。

### **6.3 市场推广策略（Go-to-Market）**

* **以开发者为中心**：撰写高质量的技术博客（如“如何将 Oracle 游标迁移到 Postgres”），在 GitHub、StackOverflow 和 Reddit 上建立影响力 33。  
* **云厂商合作**：加入 AWS/Azure/Google Cloud 的合作伙伴网络。云厂商急需将客户从本地迁上云，你的工具可以作为加速器被云厂商推荐。  
* **开源引流**：将 Calcite 的方言扩展部分回馈给 Apache 社区，确立在 SQL 解析领域的技术权威地位。

## ---

**第七章 实施路线图（Roadmap）**

为了确保项目落地，建议分三阶段实施：

### **第一阶段：确定性基石（MVP，1-3个月）**

* **目标**：完成基于 Calcite 的 DDL/DML 翻译核心。  
* **关键任务**：  
  * 集成 calcite-server 和 SqlBabelParserImpl。  
  * 实现 Oracle、MySQL 到 PostgreSQL 的 RelToSqlConverter 映射。  
  * 开发 CLI 工具，支持基于文件的批量转换。  
  * *交付物*：一个高准确率的命令行翻译工具，支持表结构和简单查询迁移。

### **第二阶段：智能增强与元数据集成（4-6个月）**

* **目标**：引入 LLM 和元数据能力。  
* **关键任务**：  
  * 开发 JDBC 元数据提取器，实现 CalciteCatalogReader。  
  * 搭建 LLM Agent 框架（使用 LangChain），处理存储过程。  
  * 实现基于 RAG 的 Schema 注入机制。  
  * *交付物*：支持在线连接数据库的混合翻译引擎 Beta 版。

### **第三阶段：校验闭环与企业化（7-9个月）**

* **目标**：构建信任与商业化。  
* **关键任务**：  
  * 开发 LLM 裁判系统和单元测试生成器。  
  * 构建 Web 控制台（SaaS 平台）。  
  * 完成 Docker 化封装，支持私有化部署。  
  * *交付物*：完整的企业级迁移平台，正式发布商业版。

## ---

**结论**

构建基于 Calcite 和 LLM 的混合 SQL 翻译器，是解决数据库迁移这一世界级难题的最佳技术路径。Calcite 提供了数学上的严谨性，确保了数据结构的绝对正确；LLM 提供了语义上的灵活性，解决了传统工具无法处理的过程化逻辑。

通过“在线元数据注入”大幅提升翻译精度，结合“离线 AST 模式”覆盖全场景需求，再辅以“LLM 自动化校验”构建用户信任，该产品在技术上具有极高的壁垒。在商业上，通过“开源核心获客 \+ 企业服务变现 \+ AI 用量计费”的组合拳，能够有效平衡研发成本与商业回报，具备广阔的市场前景。

---

*(以下为技术附录，展示关键模块的伪代码实现，以供工程团队参考)*

### **附录 A: Calcite Babel Parser 配置示例**

Java

// 使用 Babel 解析器以支持宽松语法  
SqlParser.Config parserConfig \= SqlParser.config()  
   .withParserFactory(SqlBabelParserImpl.FACTORY)  
   .withConformance(SqlConformanceEnum.BABEL) // 开启所有方言支持  
   .withCaseSensitive(false);

SqlParser parser \= SqlParser.create(sqlContent, parserConfig);  
SqlNode sqlNode \= parser.parseStmtList(); // 解析多条语句

### **附录 B: LLM Prompt 模板 (用于存储过程)**

# **Role**

You are an expert Database Migration Engineer specialized in Oracle to PostgreSQL migration.

# **Context**

We are migrating a legacy Oracle database to PostgreSQL.

The user provides a PL/SQL stored procedure.

You must rewrite it into PostgreSQL PL/pgSQL.

# **Schema Information (Relevant Tables)**

CREATE TABLE employees (

emp\_id INT PRIMARY KEY,

salary DECIMAL(10,2),

...

);

# **Instructions**

1. Analyze the logic flow, cursors, and exception handling.  
2. Convert Oracle 'DECODE' to 'CASE WHEN'.  
3. Convert Oracle 'SYSDATE' to 'CURRENT\_TIMESTAMP'.  
4. Ensure variable types match the PostgreSQL schema provided above.  
5. IF logic is ambiguous, generate a comment "-- TODO: Manual Check Required".

# **Input Code**

### **附录 C: 数据对比分析表 (Calcite vs LLM 适用性)**

| 数据库对象 | 推荐引擎 | 原因分析 |
| :---- | :---- | :---- |
| **Table DDL** | Calcite | 类型映射需绝对精确，LLM 容易搞错精度（如 Number(38) vs Numeric）。 |
| **SELECT 查询** | Calcite | 关系代数可保证逻辑等价，且能自动处理 Group By 语法的方言差异。 |
| **Views** | Calcite | 视图本质上是存储的查询，适合确定性翻译。 |
| **Stored Procedures** | LLM | 包含 IF/ELSE、循环、变量赋值，Calcite 解析器无法完全覆盖。 |
| **Triggers** | LLM | 触发器语法差异极大（Before/After），需要语义理解进行重写。 |
| **Proprietary Functions** | Hybrid | 简单函数（如 NVL）用 Calcite 替换；复杂函数（如 XML 解析）用 LLM 重写。 |

*(报告结束)*

#### **Works cited**

1. AWS Schema Conversion Tool \- AWS Documentation, accessed February 7, 2026, [https://docs.aws.amazon.com/SchemaConversionTool/latest/userguide/CHAP\_Welcome.html](https://docs.aws.amazon.com/SchemaConversionTool/latest/userguide/CHAP_Welcome.html)  
2. AWS DMS Schema Conversion Overview, accessed February 7, 2026, [https://aws.amazon.com/video/watch/81b4fefc406/](https://aws.amazon.com/video/watch/81b4fefc406/)  
3. The real cost of database migration • Blog \- Ispirer, accessed February 7, 2026, [https://www.ispirer.com/blog/real-cost-of-database-migration](https://www.ispirer.com/blog/real-cost-of-database-migration)  
4. Plan price calculation \- Ispirer, accessed February 7, 2026, [https://www.ispirer.com/price-calculation](https://www.ispirer.com/price-calculation)  
5. SQLines Tool \- Oracle to PostgreSQL Migration, accessed February 7, 2026, [https://www.sqlines.com/oracle-to-postgresql-tool](https://www.sqlines.com/oracle-to-postgresql-tool)  
6. SQLines Tool \- SQL Server to PostgreSQL, accessed February 7, 2026, [https://www.sqlines.com/sql-server-to-postgresql-tool](https://www.sqlines.com/sql-server-to-postgresql-tool)  
7. linkedin/coral: Coral is a translation, analysis, and query rewrite engine for SQL and other relational languages. \- GitHub, accessed February 7, 2026, [https://github.com/linkedin/coral](https://github.com/linkedin/coral)  
8. Coral: A SQL translation and rewrite engine for modern data lakes \- GitHub Pages, accessed February 7, 2026, [https://cdmsworkshop.github.io/2022/Proceedings/InvitedTalks/Abstract\_WalaaEldinMoustafa.pdf](https://cdmsworkshop.github.io/2022/Proceedings/InvitedTalks/Abstract_WalaaEldinMoustafa.pdf)  
9. All price plans \- jOOQ, accessed February 7, 2026, [https://www.jooq.org/download/price-plans](https://www.jooq.org/download/price-plans)  
10. With Commercial Licensing, Invest in Innovation, not Protection \- jOOQ blog, accessed February 7, 2026, [https://blog.jooq.org/with-commercial-licensing-invest-in-innovation-not-protection/](https://blog.jooq.org/with-commercial-licensing-invest-in-innovation-not-protection/)  
11. RISE: Rule-Driven SQL Dialect Translation via Query Reduction \- arXiv, accessed February 7, 2026, [https://arxiv.org/html/2601.05579](https://arxiv.org/html/2601.05579)  
12. LLM vs Rule-Based Queries: Which Is More Accurate? | Yodaplus Technologies, accessed February 7, 2026, [https://yodaplus.com/blog/llm-vs-rule-based-queries/](https://yodaplus.com/blog/llm-vs-rule-based-queries/)  
13. openGauss-tools-sql-translator \- GitHub, accessed February 7, 2026, [https://github.com/opengauss-mirror/openGauss-tools-sql-translator](https://github.com/opengauss-mirror/openGauss-tools-sql-translator)  
14. Calcite program representations \- Feldera, accessed February 7, 2026, [https://www.feldera.com/blog/calcite-irs](https://www.feldera.com/blog/calcite-irs)  
15. Casting operator :: not being parsed by Apache Calcite \- Stack Overflow, accessed February 7, 2026, [https://stackoverflow.com/questions/77170989/casting-operator-not-being-parsed-by-apache-calcite](https://stackoverflow.com/questions/77170989/casting-operator-not-being-parsed-by-apache-calcite)  
16. How to change Calcite's default sql grammar?" \- Stack Overflow, accessed February 7, 2026, [https://stackoverflow.com/questions/44382826/how-to-change-calcites-default-sql-grammar](https://stackoverflow.com/questions/44382826/how-to-change-calcites-default-sql-grammar)  
17. Extending Calcite parser to Adding Custom relational operators \- Stack Overflow, accessed February 7, 2026, [https://stackoverflow.com/questions/56351095/extending-calcite-parser-to-adding-custom-relational-operators](https://stackoverflow.com/questions/56351095/extending-calcite-parser-to-adding-custom-relational-operators)  
18. FlinkSQL field lineage solution and source code | by HamaWhite \- Medium, accessed February 7, 2026, [https://medium.com/@HamaWhite/flinksql-field-lineage-solution-and-source-code-d5666c4a321a](https://medium.com/@HamaWhite/flinksql-field-lineage-solution-and-source-code-d5666c4a321a)  
19. How can i convert SqlNode expression(not query\!) to RelNode? \- Stack Overflow, accessed February 7, 2026, [https://stackoverflow.com/questions/37949678/how-can-i-convert-sqlnode-expressionnot-query-to-relnode](https://stackoverflow.com/questions/37949678/how-can-i-convert-sqlnode-expressionnot-query-to-relnode)  
20. RelToSqlConverter (Apache Calcite API), accessed February 7, 2026, [https://calcite.apache.org/javadocAggregate/org/apache/calcite/rel/rel2sql/RelToSqlConverter.html](https://calcite.apache.org/javadocAggregate/org/apache/calcite/rel/rel2sql/RelToSqlConverter.html)  
21. Assembling a query optimizer with Apache Calcite | Querify Labs, accessed February 7, 2026, [https://www.querifylabs.com/blog/assembling-a-query-optimizer-with-apache-calcite](https://www.querifylabs.com/blog/assembling-a-query-optimizer-with-apache-calcite)  
22. Mallet: SQL Dialect Translation with LLM Rule Generation \- DSpace@MIT, accessed February 7, 2026, [https://dspace.mit.edu/bitstream/handle/1721.1/155537/3663742.3663973.pdf?sequence=1\&isAllowed=y](https://dspace.mit.edu/bitstream/handle/1721.1/155537/3663742.3663973.pdf?sequence=1&isAllowed=y)  
23. Knowledge Graph-based Retrieval-Augmented Generation for Schema Matching \- arXiv, accessed February 7, 2026, [https://arxiv.org/html/2501.08686v1](https://arxiv.org/html/2501.08686v1)  
24. Improving Text-to-SQL Accuracy with Schema-Aware Reasoning | by EzInsights AI, accessed February 7, 2026, [https://pub.towardsai.net/improving-text-to-sql-accuracy-with-schema-aware-reasoning-528eadfdc99b](https://pub.towardsai.net/improving-text-to-sql-accuracy-with-schema-aware-reasoning-528eadfdc99b)  
25. LLM-SQL-Solver: Can LLMs Determine SQL Equivalence? \- arXiv, accessed February 7, 2026, [https://arxiv.org/html/2312.10321v4](https://arxiv.org/html/2312.10321v4)  
26. Evaluating SQL Generation with LLM as a Judge | by Aparna Dhinakaran \- Medium, accessed February 7, 2026, [https://medium.com/data-science/evaluating-sql-generation-with-llm-as-a-judge-1ff69a70e7cf](https://medium.com/data-science/evaluating-sql-generation-with-llm-as-a-judge-1ff69a70e7cf)  
27. Text To SQL: Evaluating SQL Generation with LLM as a Judge \- Arize Phoenix, accessed February 7, 2026, [https://phoenix.arize.com/text-to-sql-evaluating-sql-generation-with-llm-as-a-judge/](https://phoenix.arize.com/text-to-sql-evaluating-sql-generation-with-llm-as-a-judge/)  
28. defog-ai/sql-eval: Evaluate the accuracy of LLM generated outputs \- GitHub, accessed February 7, 2026, [https://github.com/defog-ai/sql-eval](https://github.com/defog-ai/sql-eval)  
29. Usage‑Based AI Pricing Models: The Future of SaaS | by Kanhasoft | Medium, accessed February 7, 2026, [https://medium.com/@kanhasoftt/usage-based-ai-pricing-models-the-future-of-saas-39e124f3b9a5](https://medium.com/@kanhasoftt/usage-based-ai-pricing-models-the-future-of-saas-39e124f3b9a5)  
30. Usage-Based Pricing Is Reshaping SaaS—Here's How to Stay in Control \- Zylo, accessed February 7, 2026, [https://zylo.com/blog/a-new-trend-in-saas-pricing-enter-the-usage-based-model/](https://zylo.com/blog/a-new-trend-in-saas-pricing-enter-the-usage-based-model/)  
31. Three Models for Commercializing Open Source Software | by Joe Morrison \- Medium, accessed February 7, 2026, [https://joemorrison.medium.com/three-models-for-commercializing-open-source-software-84d3130c82cd](https://joemorrison.medium.com/three-models-for-commercializing-open-source-software-84d3130c82cd)  
32. \[2512.13515\] Fine-tuned LLM-based Code Migration Framework \- arXiv, accessed February 7, 2026, [https://www.arxiv.org/abs/2512.13515](https://www.arxiv.org/abs/2512.13515)  
33. How to Market DevTools: 7 Tactics for Reaching Developers \- daily.dev Ads, accessed February 7, 2026, [https://business.daily.dev/resources/how-to-market-devtools-7-tactics-for-reaching-developers](https://business.daily.dev/resources/how-to-market-devtools-7-tactics-for-reaching-developers)  
34. Developer (DevTools) Marketing Strategy, Best Practices, and Examples \- Inflection.io, accessed February 7, 2026, [https://www.inflection.io/post/developer-devtools-marketing-strategy-best-practices-and-examples](https://www.inflection.io/post/developer-devtools-marketing-strategy-best-practices-and-examples)

[image1]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAD4AAAAYCAYAAACiNE5vAAACNklEQVR4Xu2WSWtUQRSFr8NGHMhC8A/EAYyYiAqCO/MLBMnepUoE8SeIA5Kt6EYERSEgClkEEnTvPovEWYgxcUg0ETM43uOtou87/V53vU0vtD449Ktz6r2u2zW8FslkMv8TN1VLqt9Btwqp8VMaOXSsGHeED1IcQyt6pdEPY39dSIl2D32s6mazw1xTXZfqMUbeivVZ4IBZpxpVPRC74Xgx/ku7L+sEmL2DYmPZTlnkvmqzWJ9Bypo4K/ZAUDXrP9howXk2iB1sJBLHhc8BHwS2iBV7UawPJrQln9z1Z7Gbtjlvl+qKa7fjqGqCzQCehaVYFxR1J1xjfDdcFlkNn9+lfPKa8J2wj9Gect5dsS+uAw7Ap+TtVr0jL5VL0jhjML7nLgNYtVjiAPmcy0rBchghj5d70q9XQr/qWbhG0bMuqwtmMcLjA9jbABOE7JTLSvH723u4+Wpor7msLrH49xzUxBf6hdrT7vqyNP8opcyzEYi/6l7VBcrqcEBspl9xUIOtqnuu/VAaxR1R7XMZDuGkwqs6jYtlL1WbKEsFRcc9fVia92UqWHl7XDuuSPDC+QB+29W1UfWIzcB6Kd9LqfSpZshD8XzgpfCL2j1i4+Kiu4J/hvwCG1QfVU84cHxTLbOZAAb2hs3AIdUkmy04IVYMJskD7yR5t4Nf+f4eFjsg8P7Gexv/iMrYrzrNZgJDbBA72ajgq9gY8ddzRXXOZf51O6ZaFDuv0BeTFd/pmUwmk8n8S/wBHX2OWan/fNcAAAAASUVORK5CYII=>