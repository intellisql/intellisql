# **现代分布式数据库与查询引擎 SQL 测试框架深度演进与 JDBC 集群化验证架构体系研究**

## **引言**

在现代数据管理系统与底层基础设施的演进浪潮中，分布式数据库架构、异构数据源的联邦查询处理以及多模态计算能力已成为驱动行业级的核心技术方向。随着诸如 Apache ShardingSphere 这样的高阶数据库中间件、Apache Calcite 这样的动态联邦查询引擎，以及 openGauss 这类企业级分布式数据库内核的复杂度呈指数级上升，传统的软件质量保障手段已无法满足当今的工程需求。保障 SQL 解析的准确性、分布式路由的严谨性、查询优化的成本效益以及最终执行结果的绝对正确性，促使各大顶级开源社区的测试框架从单纯的单元测试断言，全面演化为高度抽象化、矩阵化、并发化及快照驱动的庞大工程体系。

分布式数据库系统中的非确定性状态、多版本并发控制（MVCC）下的事务隔离级别、跨节点的网络延迟、异构方言的兼容性，以及复杂的系统错误注入，要求现代 SQL 测试框架不仅能够验证单一的结果集，还必须具备复杂的测试环境拓扑编排、底层状态机控制、多并发会话调度以及执行计划的差异比对能力。

基于前沿质量工程的最佳实践，本文将首先深入剖析孕育架构设计灵感的三大顶级开源框架（ShardingSphere、Calcite 与 openGauss YAT）的核心机制与解耦哲学。随后，本文将提出并设计一套全新的面向 IntelliSQL 的 E2E SQL 测试模块整体架构。该架构旨在纯集群化运行环境下，结合声明式快照、原生目标库（如 MySQL, PostgreSQL）的 SQL 对等执行比对，以及命令式宏调度的混合测试描述语言，为现代分布式引擎提供高价值的落地指南。

## **Apache ShardingSphere E2E 集成测试框架的矩阵化架构与解耦哲学**

Apache ShardingSphere 的核心设计理念为 Database Plus，其致力于在多源异构数据库的上层构建一个标准化的生态系统，提供数据分片、分布式事务处理、弹性伸缩以及读写分离等增强服务，而非从零开始创造一个全新的数据库底层引擎 1。为了在极度复杂的异构计算环境中保障中间件路由与重写逻辑的稳定性，ShardingSphere 的端到端（E2E）集成测试框架被精心设计为一个高度正交化的矩阵执行引擎，其底层架构展现了极致的模块解耦思想。

### **测试用例定义与结果集断言的深度物理物理解耦**

ShardingSphere 集成测试框架的拓扑结构主要由三大核心组件构成：测试用例（Test Case）、测试环境（Test Environment）以及测试执行引擎（Test Engine） 2。该框架最显著的工程特征是实现了 SQL 语句定义与预期结果集断言数据的物理层与逻辑层解耦。在项目结构中，所有的测试用例均被统一定义在位于 src/test/resources/cases/ 目录下的 XML 文件中（例如 \<SQL-TYPE\>-integration-test-cases.xml），每一个特定的 \<test-case\> 标签负责唯一绑定一条核心 SQL 语句 2。这种设计的精妙之处在于，同一条 SQL 语句可以通过注入不同的参数配置，驱动多种维度和场景的执行流程，从而极大地提升了测试用例的复用率。

在预期结果的断言机制上，该框架引入了独立的数据集文件（Dataset XML）机制。对于诸如 INSERT、UPDATE、DELETE 等非查询类（DML）或数据定义类（DDL）语句，系统通过测试用例内部 \<assertion\> 标签中的 expected-data-file 属性，精准指向一个包含预期终态的外部 XML 数据集文件 2。数据集文件的内部结构严谨，根元素 \<dataset\> 之下包含 \<metadata\> 标签以定义结果集的列名结构（\<column\>），并使用一系列 \<row\> 标签的 values 属性来声明以逗号分隔的期望行数据 2。执行引擎在运行时，会提取物理数据库或 Proxy 代理端返回的真实结果集，并与该数据集文件进行严格的行列维度对齐校验。

值得高度关注的是，为了适应异构方言和不同路由场景下可能产生的微小结果差异，执行引擎在查找 expected-data-file 时实施了一套严密的四层降级寻址策略。引擎首先尝试在同级目录下寻找 dataset\\${SCENARIO\_NAME}\\${DATABASE\_TYPE}\\${dataset\_file}.xml，若未命中，则降级查找 dataset\\${SCENARIO\_NAME}\\${dataset\_file}.xml，进而查找全局的 dataset\\${dataset\_file}.xml，若穷尽以上路径仍未找到目标文件，系统才会抛出运行时异常 2。这种带有层级继承特性的文件查找体系深刻反映了分布式系统的多态性本质，既保证了通用断言数据的最大化复用，又为特定底层数据库方言（如 openGauss 与 MySQL 之间的数据类型差异）或特定分片规则预留了特异性验证空间。

### **笛卡尔积测试矩阵与高维正交执行模型**

ShardingSphere 测试引擎的强大算力与其参数化驱动机制密不可分。由于 ShardingSphere 架构体系需要同时兼容轻量级的本地微内核 ShardingSphere-JDBC 以及对异构语言透明的独立数据库服务端 ShardingSphere-Proxy 1，并且底层必须无缝对接 MySQL、PostgreSQL、openGauss、SQLServer 和 Oracle 等多种关系型数据库产品 2，因此，仅仅执行一次单机验证是远远不够的。

系统基于 JUnit 的 Parameterized 参数化测试引擎模块，将所有的环境维度变量进行了笛卡尔积级别的正交组合。在实际执行过程中，一条简单的 SQL 语句可以自动裂变并驱动庞大的测试用例集。例如，通过结合 5 种底层关系型数据库类型、2 种接入端代理模式（JDBC 与 Proxy）、2 种底层 JDBC 协议通信模式（Statement 与 PreparedStatement）、2 种执行 API（execute 与 executeQuery/executeUpdate），以及包含单纯分库、单纯分表、分库分表结合读写分离（db, tbl, dbtbl\_with\_replica\_query, replica\_query）在内的多种数据治理场景，单条基础 SQL 语句的解析、改写与路由逻辑能够衍生出高达 160 个相互独立的细分测试生命周期 2。这种如同漏斗般严苛的数据流过滤机制，确保了底层解析引擎和路由引擎在面临各种极端跨界组合时的计算精确性与框架健壮性。

### **基础设施即代码：容器化与混合编排体系的落地**

在执行环境基础设施的搭建层面，ShardingSphere 提供了 Native、Docker 以及规划中的 Embed 三种混合编排策略，标志着其质量保障体系已经从单纯的业务逻辑校验跃升至基础设施即代码（IaC）的维度 2。

Native 环境主要依赖开发者本地物理机上已经部署好的数据库实例，系统通过读取 src/test/resources/env/ 目录下的 scenario-env.properties 及各种规则配置文件（如 rules.yaml, databases.xml 等）直连本地资源，该模式以其极低的启动延迟广泛应用于日常的代码调试与单元验证场景 3。相较之下，Docker 环境模式则代表了企业级 CI/CD 流水线的标准实践。当构建工具 Maven 触发特定参数（如 \-De2e.run.type=docker）时，框架会解析位于 src/test/resources/docker/${SCENARIO-TYPE}/ 目录下的 docker-compose.yml 拓扑配置文件 5。该流程会自动从远端拉取指定版本的数据库镜像容器（例如 mysql:5.7），并通过挂载 initdb.sql 初始化表结构与权限，同时拉起 ShardingSphere-Proxy 容器。Docker 环境配置不仅负责服务的自动化启停，还暴露了远程调试端口以供诊断，从而打造了一个无状态、自包含且完全一致的云原生沙箱编译环境，从根本上消除了“在我的机器上运行正常”的集成痛点 3。

| 架构维度 | Apache ShardingSphere E2E 集成框架技术特征 |
| :---- | :---- |
| **断言生命周期** | SQL 用例与期望数据集（Dataset XML）分离，支持多级目录 Fallback 匹配机制 |
| **执行驱动机制** | JUnit Parameterized 引擎，单 SQL 驱动超过 160 种维度的正交笛卡尔积测试矩阵 |
| **基础设施编排** | 支持 Native 直连调试与 Docker-Compose 容器化隔离，实现 IaC (基础设施即代码) 部署 |
| **场景拓展能力** | 原生集成数据分片、读写分离、分布式事务（XA/BASE）与全链路影子库安全隔离测试 |

## **Apache Calcite 查询联邦与成本优化器的快照与交互双引擎驱动**

区别于 ShardingSphere 对数据流动与分布治理的关注，Apache Calcite 是一个动态的数据管理框架与查询联邦引擎。Calcite 在设计上主动剥离了底层数据的物理存储机制与执行算法池，而是极度专注于实现符合行业标准的 SQL 解析器、关系代数（Relational Algebra）转换树，以及高度可扩展的基于成本的优化器（CBO）8。因为 Calcite 能够灵活地将多种后端异构数据源（如 Cassandra, Elasticsearch, Kafka）通过统一的关系抽象进行桥接与谓词下推，其测试框架的设计哲学展现出了应对极高复杂度和非确定性输出的前瞻性架构视角 9。

### **DiffRepository：基于黄金基线模式的防退化快照体系**

在 Calcite 的关系逻辑计划（Logical Plan）与物理执行计划（Physical Plan）优化测试中，引擎输出的往往是包含多层树状嵌套关系代数、谓词投影以及成本预估数值的复杂长字符串文本。传统基于代码硬编码的断言方式不仅难以应对高频变更的规则结构，且极其容易在复杂的字符串比对中引入人为错误。为彻底解决这一痛点，Calcite 创新性地引入了 DiffRepository 快照验证机制（Golden Master / Snapshot Testing Pattern）11。

DiffRepository 的核心运作机制是管理一组与测试用例类同名映射的 XML 资源基线文件。例如，名为 MyTest.java 的测试类对应着 src/test/resources/com/acme/test/MyTest.xml。该 XML 文件以 \<Root\> 为顶级容器，按测试方法名封装了多个 \<TestCase\> 块，每个块内部的 \<Resource\> 标签利用 CDATA 区块保存了多行查询树或执行结果文本 11。

在测试执行阶段，代码中通过调用 DiffRepository.lookup(MyTest.class) 获取单例共享的资源池。若断言请求（如 assertEquals）发现内存中实际生成的执行树文本与 XML 基线中的期望文本不一致，或者相应的 \<Resource\> 尚未建立，测试不仅会抛出异常中断构建，DiffRepository 还会极其宽容地在项目构建目录（例如 build/diffrepo/test/.../MyTest\_actual.xml）自动生成一份包含最新实际输出结果的日志副本 11。研发工程师在人工审查并确认新的查询规划确实属于优化改进（而非倒退）后，只需简单地执行文件复制替换命令（例如通过系统的 cp 指令覆盖源代码目录中的原始文件），即可轻松实现复杂基线数据的无缝更新 11。这种允许过滤函数定制（Callback Filter）且自动生成 actual 对照文件的“自愈式”快照设计，将重构关系代数转化规则和修正数据格式缺陷带来的测试维护成本降至了绝对下限。

### **Quidem 框架：数据驱动的交互式联邦脚本路由**

在复杂的联邦查询场景下，传统的 Java API 验证显得过于繁重。为此，Calcite 采用并深度集成了一套名为 Quidem 的交互式命令行驱动引擎（以 .iq 为后缀名的纯文本脚本文件）来进行端到端的 SQL 流转验证 12。

Quidem 脚本在结构上是对关系交互生命周期的完美模拟，允许将原生 SQL 语句与具有强控制属性的元命令高度交错。其工作流的启动通常依赖于 \!use 或 \!connect 等特定指令（例如 \!connect jdbc:calcite:model=src/test/resources/model.json）13。当脚本执行器拦截到这些命令时，底层会调用 ConnectionFactory.connect() 机制，在内存中动态注册基于反射的数据模式（ReflectiveSchema）或挂载外部的 CSV/Druid 数据源适配器 14。随后，脚本中的 SQL 语句将通过创建好的隔离 JDBC 上下文投递给后端的 Calcite 核心进行处理。

对于结果验证，Quidem 抛弃了代码层的断言操作，而是直接将预期结果表以直观的字符画形式（类似 Markdown 表格展示）写在查询语句下方，并在末尾紧跟 \!ok 指令声明这是一个合法预期的返回。如果查询被设计用来触发解析器或验证器的边界错误保护，测试编写者可以采用 \!error 命令来精确捕获包含语法异常关键字的系统抛出堆栈 15。此外，框架通过诸如 \!set planner-rules 等运行时修饰参数，允许在特定会话中动态开启或关闭特定的启发式（HepPlanner）或火山模型（VolcanoPlanner）优化规则 16。这种将环境构建、模型路由、用例执行与结果比对高度浓缩为一门领域特定语言（DSL）的测试形态，不仅大幅度降低了研发心智负担，也为生成可读性极强的框架演进示例文档提供了天然素材。

## **openGauss YAT 质量治理框架：分布式内核的全链路调度与并发压测**

作为聚焦于极高并发吞吐量与严苛事务一致性的大型企业级分布式数据库内核，openGauss 的整体架构深度融合了多线程服务器池化、NUMA 感知数据结构优化及轻量级 RDMA 远程过程调用 17。尤其在其核心组件中，诸如乐观并发控制（OCC）、多版本并发控制（MVCC）及内存优化表（MOT）等引擎，负责处理海量且非确定性的并发写冲突 18。为了从宏观层面守护并监测上述特性的规范与稳定性，openGauss 社区不仅贡献了超过 30,000 个高质量的自动测试用例，更孕育出了基于 Python 与 Kotlin 混合驱动的自动化测试系统——YAT (Yet Another Test) 20。

### **泛语言抽象适配与强隔离目录规范**

YAT 框架在顶层设计上是一个高度可扩展的语言无关执行器。与受限于特定虚拟机的框架不同，YAT 提供了一套基于 Python 3 的外部封装 API 与基于 Kotlin 语言的内核任务引擎 20。通过灵活的适配器挂载，框架在单个执行容器内无缝支持以 .sql 为后缀的原生数据库语言、.sh Shell 环境单元测试、.py 的 Python unittest 驱动、基于 C++ 的 gtest，甚至包括运行在 JVM 之上的 Groovy (.groovy) 和行为驱动开发框架 Spock (.spec.groovy) 20。这种广泛的语言融合能力，使得性能评测、内核逻辑检查及外部驱动适配能够被收敛进同一个持续集成（CI）管道中。

为了规范测试工程的管理，YAT 强制要求通过 yat suite init 命令行生成标准化的测试套件目录拓扑。所有的资源被严格隔离在四个主要空间中：conf 目录存储目标集群物理节点的 nodes.yml 与环境宏替换变量 macro.yml；testcase 存放原始代码；except 管理所有的执行预期基线文件；而 schedule 则统筹测试用例的运行次序 20。这一强规范的目录体系，使得即使在包含数千个子模块的大型开源协作项目中，开发者的用例提交与 Review 标准也能够保持绝对的一致。

### **面向数据库分布式的超集 SQL 指令扩展**

YAT 对质量工程领域做出的最大贡献，在于其将一系列底层会话控制指令深度嵌入了原生 SQL 语法体系中，创造出了一门针对分布式并发冲突量身定做的“超集 SQL”（Superset SQL）20。

传统的 JDBC 客户端在发送单进程串行脚本时，极难模拟由于事务可见性和线程阻塞引发的内核级时序幽灵问题。YAT 通过预处理器拦截特定注解解决了这一痛点：

* **物理连接与多租户切换（@conn）**：允许在同一个用例脚本内部，通过执行 @conn user/passwd@host:port 瞬间将上下文切换至新角色或新节点的数据库视图。这在验证行级安全（Row-Level Security）及分布式主备节点只读扩展机制时显得尤为关键 20。  
* **精准会话追踪与并行模拟（@session 与 @parallel）**：测试人员可通过定义 @session(name: s1) 和 @session(name: s2)，在此后的代码块中模拟多个相互独立但并存的客户端会话。结合 @parallel 标记块，底层调度引擎会拉起相互独立的连接线程，同时向数据库施加写冲突负载，从而验证内核的死锁检测或行级排他锁（2PL）机制 20。  
* **跨会话步骤强制编排（@step 与 @steps）**：为了制造必现的读未提交或幻读漏洞，使用 @steps s1.0 s2.0 s1.1 语法可以强制系统在两个并行的线程中实施栅栏同步（Barrier Synchronization），从而将原本依赖硬件概率的争用情况转变为可重放的确定性事务测试逻辑 20。  
* **系统级破坏性操作织入（@sh）**：当需要验证集群容错（Fault Tolerance）与分布式数据同步时，YAT 支持在 SQL 会话流的间隙，利用 @sh 唤醒操作系统终端，去终止（Kill）特定主节点的底层进程。此后系统会继续监听备节点的 HA 切换响应，从而达成涵盖应用程序和底层硬件的交叉故障演练 20。

| YAT 测试维度 | 核心技术支持机制与实现策略 |
| :---- | :---- |
| **并发与事务控制** | 引入 @session 和 @parallel 注解，利用后台线程池驱动多连接并发写入与加锁阻塞验证 |
| **测试执行流时序** | @steps 拦截器跨线程实现精确时序屏障，保障分布式一致性异常（如脑裂、脏读）的确定性复现 |
| **多态用例兼容** | 基于后缀扩展名自动路由至对应的适配引擎（如 .z.sql 路由至特定交互客户端，.spec.groovy 启动 JVM Spock 引擎） |
| **大规模套件协同** | .ys 多套件调度器，构建 serial{} 与 parallel{} 执行树，实现数万个用例层级的智能并发压测调度 |

### **多层级套件调度与宏抽象拓扑**

在管理跨越不同功能模块的海量测试用例时，仅仅依靠操作系统目录层级的串行遍历效率极低。YAT 引入了一套独立于用例代码之外的 .schd 与 .ys 调度定义框架（YAT Schedule）20。通过在调度文件中声名 serial { suite 'a'; } 和 parallel { suite 'b'; } 闭包块，引擎能够构建一棵全局的无环调度执行树。底层节点连接、密码凭证及端口等敏感信息则全部被提取为宏（Macro），在运行时动态解析并与环境进行绑定 20。这不仅确保了测试运行计划可被高度复用于性能基准测试工具（如 Sysbench 或 BenchmarkSQL 模拟 TPC-C 测试集群）的环境中 23，还保证了其能够在包含多种网络分区的极度复杂的异构云化拓扑中高效落地。

## **面向 IntelliSQL 的 E2E SQL 测试模块整体架构设计**

通过对前述三款工业级开源测试框架在参数多维化、用例期望隔离、快照比对演进以及并发调度层面的深度剖析，我们可以提炼出一条清晰的现代测试平台架构脉络。鉴于 IntelliSQL 未来将完全脱离单机（Local Mode），仅在真实的分布式集群模式下运行，本节设计了一套面向 IntelliSQL 集群化验证的统一 JDBC 测试架构。

该测试架构的核心目标，是让研发团队能够利用高度语义化的原生 SQL 注释文件（无需编写庞杂的 Java 样板代码），直接连接远端部署的 IntelliSQL 集群进行测试。同时，该框架深度集成并无缝连接原生的 MySQL 或 PostgreSQL 集群，通过自动发送对等 SQL 或执行手写验证 SQL 进行真实结果集校验，并支持智能感知排序语义（Order-Aware），以确保引擎在高度并发下的计算精确性。

### **IntelliSQL E2E SQL 模块整体架构拓扑**

整个 IntelliSQL E2E SQL 测试模块自顶向下划分为五大核心层，实现了测试用例定义、调度、执行与验证的彻底物理与逻辑解耦：

1. **测试入口与调度层 (Entry & Scheduling Layer)**：提供从日常快速验证到边缘协议探测的全方位入口。所有的 E2E 类统一实现 SQLE2E 接口，并基于 BaseDQLE2E、BaseDALE2E 等构建类继承体系，衍生出 General、Batch 与 Additional 多路入口。最重要的是，该层支持将整个测试平台打包为独立运行的 Executable Jar 包，实现测试执行逻辑与测试配置资源的彻底分离。  
2. **解析与路由层 (Parser & Routing Layer)**：负责接管动态环境与用例语义。通过自定义的 TDL (Test Description Language) 解析引擎，读取纯 .sql 文件中的 @Features, @Databases, @Params 等标准 Java 驼峰元指令，并由动态上下文桥接引擎自动路由连接到 IntelliSQL 待测集群与 MySQL/PG 基准参照库。  
3. **矩阵执行层 (Matrix Execution Layer)**：测试用例裂变与算力下推的核心引擎。通过参数化矩阵将单条 SQL 映射为多维度的正交测试树，并在 JDBC 驱动层实施智能拦截，将带有强类型的 PreparedStatement 自动降维转换成静态的 Statement 进行二次覆盖，确保验证所有 SQL 语法树解析链路。  
4. **验证与断言层 (Validation & Assertion Layer)**：双模结果校验中心。针对 DQL 提供原生目标数据库的 Default SQL 对等一致性比对；针对 DML 提供灵活的手写 SQL (sql="...") 和文件 (file="...") 期望状态断言。内置 Order-Aware Validator（感知 ORDER BY 的无序容错比对器），在应对集群乱序返回时提供极佳的健壮性。  
5. **沙箱环境层 (Sandbox Environment Layer)**：混合编排的生命周期守护者。提供 Native 直连与 Docker (Testcontainers) 容器动态拉起双模式。底层监听测试用例类型，对 DQL（查询）实施“一次构建、持续复用”策略，对 DML（变更）实施“执行即焚、实时重建”的绝对沙箱隔离机制。

以下对该架构中的具体模块实现机制进行详细论述：

### **1\. 基于统一接口与抽象基类的层次化测试引擎拓扑 (Hierarchical Entry Points and Topology)**

为了更好地组织庞大复杂的 JDBC 测试矩阵，同时满足日常快速验证、结构变更、权限控制与深度协议兼容性测试的差异化需求，架构在底层 Java 引擎实现上构建了一套具备高度扩展性的类拓扑抽象：

* **顶层标准化测试接口 (SQLE2E)：**  
  参考 ShardingSphere 的实现规范，所有的 E2E 测试类均统一实现顶层的 SQLE2E 接口。该接口定义了端到端测试的标准执行契约（如参数注入、断言触发等），确保调度引擎在处理不同维度的 SQL 语句时，能够以多态的方式统一驱动。  
* **全局核心基类 (BaseDQLE2E / BaseDMLE2E / BaseDDLE2E / BaseDALE2E / BaseDCLE2E 等)：**  
  作为所有测试入口的顶层抽象父类，基类负责统筹全局的底层状态机管理。它包含了解析外部 SQL 脚本、初始化矩阵调度引擎、拉起目标集群连接池以及执行环境生命周期（TearDown 与 SetUp 数据重建）重置的所有公共逻辑。任何具体的测试执行器只需继承该基类，便能自动获取“读取配置-路由连接-验证比对”的全链路能力。  
* **默认常规测试入口 (GeneralDQLE2E / GeneralDMLE2E / GeneralDDLE2E / GeneralDALE2E / GeneralDCLE2E)：** 这是框架在日常 CI 流水线中默认启动的标准执行引擎入口 2。除了常规的 DQL (查询)、DML (变更) 与 DDL (结构定义) 外，框架额外引入了针对数据访问语言的 GeneralDALE2E (如各类 SHOW、DESCRIBE 命令) 以及针对数据控制语言的 GeneralDCLE2E (如 GRANT、REVOKE 权限指令)。该引擎直接读取测试用例池中的 SQL，通过最通用的 Statement.execute() 或 PreparedStatement.executeQuery() / executeUpdate() 接口将指令发送至集群，以最快、最平级的方式完成所有业务逻辑的批量结果集断言校验。  
* **批量执行测试入口 (BatchDMLE2E)：** 专门针对海量数据写入与更新场景设计。当解析层捕获到 SQL 脚本中的 @Batch 元注解时，该用例将被自动路由至此入口执行 3。 该入口强制拦截 DML 测试用例，通过调用 JDBC 原生的 addBatch() 与 executeBatch() 接口循环加载预定义的数据集参数，从而精准验证分布式引擎在处理批量网络数据包（如 PostgreSQL 的 Bind/Execute 批量协议）时的路由准确性与性能边界。  
* **深度 JDBC API 兼容性附加入口 (AdditionalDQLE2E / AdditionalDMLE2E)：** 鉴于现代 ORM 框架及复杂业务系统会频繁调用驱动底层的特殊高级 API（如 ResultSet 的滚动/并发更新特性、获取多结果集等），仅仅测试常规的“一发一收”是远远不够的。为此，框架设计了独立的 Additional 附加扩展执行入口 2。通过注入特定的环境开关（例如 \--run.additional.cases=true），框架会在复用相同 .sql 测试用例文件的前提下，切换进入这套边缘测试执行树中。它将使用极端且繁复的 JDBC 原生接口来操作游标并请求数据，从而确保 IntelliSQL 的客户端驱动和协议交互层在面临各类苛刻应用场景时依然具备绝对的健壮性。

### **2\. 独立执行 Jar 包部署与配置/逻辑深度解耦 (Standalone Execution & Configuration Decoupling)**

为了大幅提升测试框架在 CI/CD 流水线以及各类异构物理节点上的灵活部署能力，系统在构建工程上被设计为可被打包成完全独立的 Executable Jar 包（独立运行程序）。

该架构强制实施了**测试配置与底层验证逻辑的物理分离**。测试引擎的调度核心、连接池路由与断言策略被固化在独立 Jar 包内，而所有的测试用例（纯 .sql 文件）、预期的断言规则以及集群拓扑等环境配置均作为外部输入资源动态加载。这意味着，测试工程师和研发人员只需要新增或修改外部的配置文件，即可执行所有的新增测试和断言动作，全程无需触碰或重新编译任何 Java 核心逻辑代码 25。这种“引擎轻量化+数据配置驱动”的设计，不仅剥离了多余的外部依赖，而且保障了框架极高的复用率与灵活性。

### **3\. 基于功能场景（Feature/Scenario）的独立目录化配置与路由引擎 (Scenario-based Configuration Routing Layer)**

为了避免全局单点配置造成的耦合与臃肿，JDBC 连接池资源管理与路由引擎彻底废弃了全局的 cluster-env.yaml 设计。取而代之的是，系统采用**强关联功能场景（Feature/Scenario）的独立目录配置模式**。

测试工程师为每一个被测 Feature 构建独立的资源目录。例如，在 src/test/resources/env/${SCENARIO\_NAME}/ 路径下隔离各自的规则与数据源 1：

YAML

\# 路径示例: src/test/resources/env/federated\_query/scenario-env.yaml  
\# 针对 'federated\_query' (联邦查询) 场景的专属配置  
execution\_profiles:  
  intellisql\_cluster:  
    driver\_class: "com.intellisql.Driver"  
    jdbc\_url: "jdbc:intellisql://${CLUSTER\_IP}:5432/federated\_db"  
  target\_mysql:  
    driver\_class: "com.mysql.cj.jdbc.Driver"  
    jdbc\_url: "jdbc:mysql://${MYSQL\_IP}:3306/expected\_db"  
      
\# 路径示例: src/test/resources/env/federated\_query/rules.yaml  
\# 仅在该场景下生效的引擎规则 (如联邦下推策略、特定开关等)  
rules:  
  \-\!FEDERATION  
    push\_down\_enabled: true

当解析与路由层读取到 SQL 脚本头部的 @Features({"federated\_query"}) 注解时，**动态上下文桥接引擎会自动进入 env/federated\_query/ 目录加载对应的 scenario-env.yaml 以及该场景独有的规则配置**。这种按 Feature 划分目录的沙箱化设计，确保了在并行测试数百个复杂高级特性时，数据源连接池、初始化表结构以及内核行为参数互不干扰。

### **4\. 基于 SQL 文件的声明式测试描述驱动语言与智能断言机制**

系统**统一采用纯 .sql 文件进行 Case 编写**。测试人员利用双横线 SQL 注释（TDL 元指令）精准声明用例的环境、参数以及最为核心的期望结果验证逻辑。

TDL 全面引入了 **Java 驼峰命名规范**，针对支持复数配置的集合属性统一采用复数命名（如 @Features, @Databases）。考虑到底层 JDBC 驱动构建 PreparedStatement 时必须具备明确的类型上下文，@Params 注解创新性地集成了 **值与类型的显式绑定** 2。

此外，为了实现针对批量更新等场景的无缝兼容，TDL 设计了 **@Batch 专用元注解**。当 SQL 块中显式包含 @Batch 声明时，测试引擎将预期该语句伴随多行 @Params 数据，并在底层调用 PreparedStatement.addBatch() 循环注入这批参数，最后以 executeBatch() 统一提交数据库 3。

为了提升脚本编写的紧凑度与灵活性，**所有的常规行级注解既支持独立单行书写，也支持在同一行内书写多个指令**。解析引擎会自动识别并分离在一行中用空格隔开的多个 @ 标记。唯一的例外是：@ParallelBegin 和 @ParallelEnd 属于跨行的代码块标记指令，它们必须独立成行，以明确圈定并发执行的 SQL 作用域。

SQL

\-- 紧凑写法：同一行内书写多个 @ 注解，系统将自动解析分离出对应的配置与数据源  
\-- @Features({"federated\_query", "dml\_update"}) @Databases({"MySQL", "PostgreSQL"}) 

\-- 1\. 查询语句：无需显式声明 @AssertExpected，默认使用当前相同的 SELECT SQL 在基准库对等执行并比对  
\-- @Params({"100:int", "'ACTIVE':String"})  
SELECT order\_id, status FROM t\_order WHERE user\_id \=? AND status \=?;

\-- @ParallelBegin  
\-- 2\. 批量执行语句 (Batch DML)：通过 @Batch 注解明确标识，并提供多行 @Params 作为批处理参数集  
\-- 变更语句对于 UPDATE 执行后，支持 file 断言或手写 sql 断言  
\-- @Batch  
\-- @Params({"101:int", "'FINISHED':String"})  
\-- @Params({"102:int", "'FINISHED':String"})  
\-- @AssertExpected(sql="SELECT status FROM t\_order WHERE user\_id IN (101, 102)")  
UPDATE t\_order SET status \=? WHERE user\_id \=?;  
\-- @ParallelEnd

**针对不同语义的 SQL 语句，框架设计了极其灵活且高度自动化的智能断言机制：**

* **对于查询类语句（SELECT / DQL）：**  
  框架遵循“默认对等执行”的原则。对于任何 SELECT 语句，框架默认将其同时发送至 intellisql\_cluster 和 Target DB 执行。直接将目标库（如原生 MySQL）返回的真实结果集作为断言基线，两者进行集合对比。因此，开发者无需手写庞大的 XML 期望数据文件，极大地降低了维护成本。  
* **对于变更类语句（UPDATE, INSERT, DELETE / DML）：**  
  执行 DML 或批处理 DML 后，需要验证数据库状态是否符合预期。此时可以通过 @AssertExpected 注解进行灵活配置：  
  * **基于手写 SQL 的断言（SQL Assertion）：** 编写一条额外的查询语句，该语句将被自动在双边集群中执行以校验更新后的数据状态。  
  * **基于文件的断言（File Assertion）：** 指定一个 XML 或 JSON 文件进行静态结果集校验 2。

### **5\. 参数化矩阵覆盖与 PreparedStatement 自动降维引擎**

在读取 SQL 文件后，框架利用参数化矩阵执行引擎（Matrix Executor）构造测试树 6。对于预编译语句，框架实现了一项自动降维逻辑以全面覆盖解析链路。

系统在拦截到带有 \-- @Params 标记的参数化查询语句时，会首先将其作为标准的 PreparedStatement 发送。由于 @Params({"100:int", "'ACTIVE':String"}) 中精确声明了 int 与 String 等类型 2，测试引擎可以直接调用强类型的 API（如 setInt(1, 100\) 和 setString(2, "ACTIVE")）进行安全绑定，彻底避免了因驱动底层弱类型推断（如一律使用 setObject）而造成的性能与解析差异。

随后，**框架会自动将 SQL 文本中的占位符（?）替换为注解中声明的实际字面量参数**，生成拼接后的完整静态 SQL 文本，进而使用普通的 Statement 再次发送测试 6。这从根本上保障了无论客户端使用哪种 JDBC API，优化器和路由协议层均不会产生任何执行计划异常。

### **6\. 结果集比对与 Order By 语义感知断言 (Order-Aware Assertion)**

由于在纯集群分布式环境中，网络分区与计算节点的算子下推往往会导致结果集乱序。系统在对 IntelliSQL 结果与目标库（或文件）结果进行交叉验证（Cross-Validation）时，引入了深度的语义感知比对机制：

* **Order-Independent (无序比对) 容错机制**：参考 Calcite 框架对无序集合校验的处理思想，如果原始 SQL 查询**不包含**明确的 ORDER BY 子句，由于数据库引擎（如 Postgres、MySQL 等）在没有指定排序时的返回顺序本质上是不确定的，测试引擎会自动放宽限制。在对比两个结果集前，框架会在内存中利用哈希集合计数或统一排序（Sort beforehand）将双方的记录对齐。只要行数据和类型匹配，哪怕物理顺序颠倒，均判定为执行通过。  
* **Strict Order (严格顺序) 断言**：仅当检测到 SQL 中包含明确的 ORDER BY 子句时，测试框架才会启动包含索引号的游标，逐行比对。任何一条记录的位置错乱都会立即引发 Assertion Error 并暴露排序下推算子的缺陷。

### **7\. 混合编排：Native 直连与 Docker (Testcontainers) 沙箱双模式环境管理**

系统将外部数据库依赖（如基准 MySQL、PostgreSQL 以及集群节点）的拉起与连接策略抽象为 **Native** 与 **Docker** 两种可无缝切换的模式：

* **Native 模式 (基础设施外置直连)**：在此模式下，测试框架假定所有的第三方服务和目标数据库均已由开发者或运维团队在外部提前部署完毕。系统会读取当前 Feature 目录（如 env/${SCENARIO\_NAME}/）下配置的真实物理网络 JDBC URL（例如 jdbc:mysql://192.168.1.100:3306/expected\_db）并建立直连 1。Native 模式去除了所有容器调度的开销，不仅具有极低的启动延迟，还极大地方便了研发人员在本地排查复杂集群拓扑时的单步断点跟踪调试 4。  
* **Docker 模式 (基于 Testcontainers 的沙箱编排)**：当系统运行于自动化集成流水线（如 GitHub Actions）或是需要执行干净回归测试时，可通过环境变量（如 \-Denvironment.type=DOCKER）一键切换至 Docker 模式 4。在此模式下，框架深度集成了 Testcontainers 容器编排机制。JDBC 驱动路由层会劫持底层的连接获取逻辑，利用类似 jdbc:tc:postgresql:14:///test\_db 的特殊协议前缀，在后台自动调起 Docker Daemon，动态拉取镜像并瞬间启动完全隔离的数据库容器 2。结合框架的用例生命周期，整个执行环境成为了一个“阅后即焚”的临时沙箱，用例执行完毕后 Ryuk 机制会自动销毁所有容器与数据卷，彻底根除脏数据残留。

### **8\. 读写分离生命周期下的环境隔离与重建机制**

为避免自动化测试出现数据污染，框架针对不同操作施加了严格的生命周期管控：

* **查询类语句（SELECT）的持续测试**：遵循“一次初始化，持续复用”原则。集群拉起并调用当前 Feature 目录下的 initdb.sql 准备好存量数据后 1，所有只读检索用例可无状态地在集群上高频并发运行，节约大量重置开销。  
* **变更类语句（DML 与 DDL）的沙箱重建**：对于插入、更新、删除操作，或表结构变更。程序每次均会拦截其上下文，在用例执行前触发 TearDown & SetUp 挂钩。通过 Testcontainers 的沙箱机制或重新清理表空间，确保每次 DML 或 DDL 验证都在完全一致、绝对隔离的初始表中进行，防止执行状态对后续测试产生雪崩式的脏读影响。

## **结论**

随着数据应用场景向高实时性与云原生异构平台深水区不断渗透，底层执行环境的不确定性使得依赖单一单机状态机断言的测试工具变得极为脆弱。Apache ShardingSphere 构建了一个正交化的笛卡尔积执行矩阵与 XML 预期集合解耦生态；Apache Calcite 利用自动容错快照对比（DiffRepository）消解了优化器状态空间验证的沉重负担；而 openGauss 的自动化框架 YAT 则通过对 SQL 方言的前沿扩展、物理并发状态的强隔离及调度，展现了对关系型集群系统稳定性的掌控。

本文设计的纯集群 JDBC 统一测试架构，摒弃了冗余的架构负担，将核心竞争力集中在了基于 SQL 注释的智能断言上。尤其是针对查询语句的默认双向比对、变更语句（DML）的灵活手写 SQL 验证拓展，以及感知 ORDER BY 语义的无序容错匹配机制。并且，通过实现测试配置与执行引擎代码的彻底解耦并提供单 Jar 包分发模式，再辅以由统一 SQLE2E 接口驱动的 General（涵盖 DQL、DML、DDL、DAL、DCL）、Batch（借助 @Batch 元指令触发 addBatch 执行） 及 Additional 层次化引擎入口设计，结合按功能场景划分的目录级 Native/Docker 沙箱隔离，此技术范式将极大程度释放研发在跨平台验证和数据集维护上的心智成本，成为保障下一代 IntelliSQL 分布式引擎高质量交付的核心基石。

#### **Works cited**

1. Apache ShardingSphere document, accessed April 2, 2026, [https://shardingsphere.apache.org/pdf/shardingsphere\_docs\_en.pdf](https://shardingsphere.apache.org/pdf/shardingsphere_docs_en.pdf)  
2. Integration Test \- Apache ShardingSphere, accessed April 2, 2026, [https://shardingsphere.apache.org/document/current/en/test-manual/integration-test/](https://shardingsphere.apache.org/document/current/en/test-manual/integration-test/)  
3. Integration Test \- Apache ShardingSphere, accessed April 2, 2026, [https://shardingsphere.apache.org/document/5.3.1/en/test-manual/integration-test/](https://shardingsphere.apache.org/document/5.3.1/en/test-manual/integration-test/)  
4. Integration Test \- Apache ShardingSphere, accessed April 2, 2026, [https://shardingsphere.apache.org/document/5.3.0/en/test-manual/integration-test/](https://shardingsphere.apache.org/document/5.3.0/en/test-manual/integration-test/)  
5. Integration Test \- Apache ShardingSphere, accessed April 2, 2026, [https://shardingsphere.apache.org/document/5.5.0/en/test-manual/integration-test/](https://shardingsphere.apache.org/document/5.5.0/en/test-manual/integration-test/)  
6. Integration Test \- Apache ShardingSphere, accessed April 2, 2026, [https://shardingsphere.apache.org/document/5.1.1/en/reference/test/integration-test/](https://shardingsphere.apache.org/document/5.1.1/en/reference/test/integration-test/)  
7. Pipeline E2E Test \- Apache ShardingSphere, accessed April 2, 2026, [https://shardingsphere.apache.org/document/current/en/test-manual/pipeline-e2e-test/](https://shardingsphere.apache.org/document/current/en/test-manual/pipeline-e2e-test/)  
8. Background \- Apache Calcite, accessed April 2, 2026, [https://calcite.apache.org/docs/](https://calcite.apache.org/docs/)  
9. Apache Calcite \- GitHub, accessed April 2, 2026, [https://github.com/apache/calcite](https://github.com/apache/calcite)  
10. Apache Calcite: A Foundational Framework for Optimized Query Processing Over Heterogeneous Data Sources, accessed April 2, 2026, [https://15799.courses.cs.cmu.edu/spring2025/papers/20-calcite/p221-begoli.pdf](https://15799.courses.cs.cmu.edu/spring2025/papers/20-calcite/p221-begoli.pdf)  
11. DiffRepository (Apache Calcite API), accessed April 2, 2026, [https://calcite.apache.org/javadocAggregate/org/apache/calcite/test/DiffRepository.html](https://calcite.apache.org/javadocAggregate/org/apache/calcite/test/DiffRepository.html)  
12. hydromatic/quidem: Idempotent query executor \- GitHub, accessed April 2, 2026, [https://github.com/hydromatic/quidem](https://github.com/hydromatic/quidem)  
13. Tutorial \- Apache Calcite, accessed April 2, 2026, [https://calcite.apache.org/docs/tutorial.html](https://calcite.apache.org/docs/tutorial.html)  
14. Where is the data of the files end with .iq of Calcite? \- Stack Overflow, accessed April 2, 2026, [https://stackoverflow.com/questions/76225712/where-is-the-data-of-the-files-end-with-iq-of-calcite](https://stackoverflow.com/questions/76225712/where-is-the-data-of-the-files-end-with-iq-of-calcite)  
15. SQL language \- Apache Calcite, accessed April 2, 2026, [https://calcite.apache.org/docs/reference.html](https://calcite.apache.org/docs/reference.html)  
16. History \- Apache Calcite, accessed April 2, 2026, [https://calcite.apache.org/docs/history.html](https://calcite.apache.org/docs/history.html)  
17. System Architecture | openGauss documentation, accessed April 2, 2026, [https://docs.opengauss.org/en/docs/5.0.0/docs/AboutopenGauss/system-architecture.html](https://docs.opengauss.org/en/docs/5.0.0/docs/AboutopenGauss/system-architecture.html)  
18. Mot Optimistic Concurrency Control Occ | openGauss documentation, accessed April 2, 2026, [https://docs.opengauss.org/en/docs/3.0.0-lite/docs/Developerguide/mot-optimistic-concurrency-control-occ.html](https://docs.opengauss.org/en/docs/3.0.0-lite/docs/Developerguide/mot-optimistic-concurrency-control-occ.html)  
19. Mot Optimistic Concurrency Control | openGauss documentation, accessed April 2, 2026, [https://docs.opengauss.org/en/docs/6.0.0/docs/DatabaseAdministrationGuide/mot-optimistic-concurrency-control.html](https://docs.opengauss.org/en/docs/6.0.0/docs/DatabaseAdministrationGuide/mot-optimistic-concurrency-control.html)  
20. Automatic Test Framework YAT | openGauss, accessed April 2, 2026, [https://opengauss.org/en/blogs/2022/Automatic-Test-Framework-YAT.html](https://opengauss.org/en/blogs/2022/Automatic-Test-Framework-YAT.html)  
21. openGauss/Yat \- Gitee, accessed April 2, 2026, [https://gitee.com/opengauss/Yat](https://gitee.com/opengauss/Yat)  
22. Concepts | Docs | openGauss, accessed April 2, 2026, [https://docs.opengauss.org/en/docs/latest/database\_administration\_guide/database\_concepts.html](https://docs.opengauss.org/en/docs/latest/database_administration_guide/database_concepts.html)  
23. Apache ShardingSphere & openGauss: Breaking the Distributed Database Performance Record with 10 Million tpmC \- SphereEx, accessed April 2, 2026, [https://sphere-ex.com/case-studies/27/](https://sphere-ex.com/case-studies/27/)  
24. Apache ShardingSphere & openGauss: Breaking the Distributed Database Performance Record with 10 Million tpmC \- Medium, accessed April 2, 2026, [https://medium.com/codex/apache-shardingsphere-opengauss-breaking-the-distributed-database-performance-record-with-10-b8ced05daa37](https://medium.com/codex/apache-shardingsphere-opengauss-breaking-the-distributed-database-performance-record-with-10-b8ced05daa37)  
25. Test Manual \- Apache ShardingSphere, accessed April 2, 2026, [https://shardingsphere.apache.org/document/5.5.2/en/test-manual/](https://shardingsphere.apache.org/document/5.5.2/en/test-manual/)