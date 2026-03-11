# 脉络 (VeinGraph) 项目实现计划

本文档用于追踪 VeinGraph 项目的具体开发进度。开发过程分为 5 个主要阶段（Phase）。

## Phase 1: 基础设施搭建 (Infrastructure Bootstrapping)

- [x] 初始化 Spring Boot 3 工程并在 [pom.xml](file:///c:/Code/VeinGraph/pom.xml) 中引入核心依赖（Spring Web, Spring Actuator 等）
- [x] 搭建并验证本地开发依赖栈 (Docker Compose): 
  - [x] Redis (缓存 / Session)
  - [x] MongoDB (统一数据湖 / 业务库)
  - [x] Neo4j (关系大脑)
  - [x] Elasticsearch (>= 8.x) (全文/向量检索)
  - [x] Kafka & Zookeeper (异步消息总线)
- [x] 配置 Spring Boot 多数据源连接 (Spring Data MongoDB, Spring Data Neo4j, Elasticsearch Client)

## Phase 2: 核心文档处理与 LLM 集成 (Document & Foundation LLM)

- [x] 集成 LangChain4j: 
  - [x] 实现文档解析功能（支持 Tika/PDF 解析）
  - [x] 实现长文本的 `RecursiveCharacterTextSplitter` 分块逻辑
- [x] 集成 Spring AI Alibaba:
  - [x] 配置通义千问 (DashScope) 的 API Key 和客户端
  - [x] 实现基础的多轮对话调用测试接口
- [x] 构建底层 LLM Prompt 策略模板 (Few-Shot / CoT 模板化)
- [x] 实现并测试基础的实体抽取与结构化 JSON 解析逻辑 (Function Calling / Schema 约束)

## Phase 3: 异步异步解耦流与多模态存储打通 (Async & Polyglot Storage)

- [ ] 构建 Kafka 生产者与消费者框架：
  - [ ] 创建 Topic: `doc-chunk-extract-topic`
  - [ ] 前端上传文件的接收接口 -> LangChain4j 分块 -> Kafka 投递
  - [ ] Kafka 消费端（配置限流）-> 调用 Spring AI Alibaba 抽取
- [ ] 实现实体消歧与统一 (Entity Resolution) 逻辑 (基于 MatchScore 和上下文)
- [ ] 实现提取结果的发件箱投递 (Outbox Pattern & Change Streams):
  - [ ] 主单写：大模型关系提取后仅带有 `status="UNSYNCED"` 单写回 MongoDB（原始长文/运行状态归档）
  - [ ] 同步构建流：监听 MongoDB Collection 的变更流 (Change Streams) 或起后台扫表任务
  - [ ] **执行向量化转化**：调用 `Spring AI Alibaba EmbeddingModel`，将当前文档块转化为稠密向量 (Dense Vector) 放入 Payload
  - [ ] 扇出写 (Fan-out Upsert)：向 Elasticsearch >= 8.x 推送带向量 Chunk；通过 SDN 向 Neo4j 合并结构关系，全量 ACK 后改状态为 `SYNCED`

## Phase 4: GraphRAG 与 Agent 引擎构建

- [ ] 实现 Text2Cypher 提示工程：根据自然语言自动生成 Cypher 语句查询关系路径
- [ ] 构建高并发虚拟线程召回架构 (Parallel Retrieval Agent):
  - [ ] 实现并发赛道 A：启动 Virtual Task 执行 Text2Cypher 并查 Neo4j
  - [ ] 实现并发赛道 B：调用 EmbeddingModel 将用户问题向量化，并行执行 ES 混合查文档块
  - [ ] 主线程屏障拦截 (Barrier) 等待并汇总“图谱强关系 + 原始文本细节”
  - [ ] 大模型一次性统合检索成果并作为超级 Prompt 生成用户回答

## Phase 5: MVP前端层接入与全链路联调

- [ ] 搭建 Vue 3 + Element Plus 前端脚手架
- [ ] 实现基于 D3.js / vis-network / Neo4j Visualization 的关系力导向图可视化组件
- [ ] 实现对话界面 (Chat UI) 支持流式 SSE 响应
- [ ] 实现文档长传管理界面及抽取实时进度监控
- [ ] 端到端大文件全链路测试与优化
