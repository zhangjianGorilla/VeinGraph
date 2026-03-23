# 脉络 (VeinGraph) 项目实现计划

本文档用于追踪 VeinGraph 项目的具体开发进度。开发过程分为 6 个主要阶段（Phase）。

## Phase 1: 基础设施搭建 (Infrastructure Bootstrapping)

- [x] 初始化 Spring Boot 3 工程并在 `pom.xml` 中引入核心依赖
- [x] 搭建并验证本地开发依赖栈 (Docker Compose): Redis, MongoDB, Neo4j, ES, Kafka
- [x] 配置 Spring Boot 多数据源连接 (Spring Data MongoDB, Elasticsearch Client)
- [x] 添加全局异常处理器与统一响应封装 (`GlobalExceptionHandler` / `Result`)

## Phase 2: 核心文档处理与 LLM 集成

- [x] 集成 LangChain4j 本地 NLP 工具: Tika 文档解析 + RecursiveCharacterTextSplitter 分块
- [x] 集成 Spring AI (智谱 ZhiPu GLM): 通过 `spring-ai-starter-model-zhipuai` 对接智谱大模型
- [x] 构建底层 Prompt 策略模板 (Few-Shot / CoT) 与结构化 JSON 抽取 (`EntityExtractionService`)

## Phase 3: 文档上传全链路 + MongoDB 数据建模

> **优化说明**：原计划此阶段一次性打通 Kafka + 发件箱 + ES + Neo4j，跨度过大。
> 拆分原则：先让"上传 → 解析 → 切块 → 抽取 → 存 MongoDB"的主干链路跑通，再在 Phase 4 接入异构分发。

- [x] 设计 MongoDB 核心 Document 模型：
  - [x] `DocumentMeta`：文档元信息（文件名、上传时间、状态、来源等）
  - [x] `DocumentChunk`：切分后的文本块（包含原文、块序号、来源文档 ID）
  - [x] `ExtractionRecord`：LLM 抽取结果（source, target, relation, evidence, 状态字段 `syncStatus`）
- [x] 实现文件上传 REST 接口 (`POST /api/documents/upload`)：
  - [x] 接收文件 → LangChain4j 解析 → 分块 → 持久化 `DocumentMeta` + `DocumentChunk` 至 MongoDB
- [x] 实现同步抽取模式（MVP 优先）：
  - [x] 遍历 Chunk 调用 `EntityExtractionService` → 将结果写入 `ExtractionRecord`（`syncStatus=UNSYNCED`）
  - [x] 添加重试与降级逻辑：失败标记 `NEEDS_HUMAN` 不阻塞后续 Chunk
- [ ] 实现实体消歧与统一 (Entity Resolution) 基础逻辑
  - [ ] 同名同姓合并策略（基于上下文 MatchScore）
  - [ ] 代词指代消解（Prompt 层指令 + 后处理校验）

## Phase 4: 异构数据分发 + 向量化 + 异步解耦

> **优化说明**：Phase 3 主干跑通后，此阶段专注于将 MongoDB 中的 `UNSYNCED` 数据分发至 ES 和 Neo4j。
> Kafka 异步解耦也在此阶段引入，替换 Phase 3 中的同步调用模式。

- [x] 向量化流水线：
  - [x] 调用 Spring AI `EmbeddingModel` 将 `DocumentChunk` 转化为稠密向量 (`VectorSyncService`)
  - [x] 设计 ES 索引映射（含 `dense_vector` 字段 + 原文 `text` 字段）(`ChunkVectorDocument`)
  - [x] 定时扫描未向量化 Chunk 并批量处理 (`VectorSyncScheduler`)
- [x] 发件箱分发 (Outbox Pattern)：
  - [x] 启动定时扫描任务，捕获 `syncStatus=UNSYNCED` 记录 (`OutboxScheduler`)
  - [x] 扇出写 ES：Embedding 向量化后推送至 ES (`VectorSyncService`)
  - [x] 扇出写 Neo4j：通过 Cypher 客户端执行 `MERGE` 合并实体和关系 (`GraphSyncService`)
  - [x] 同步成功后更新 MongoDB 状态为 `SYNCED`
- [x] Kafka 异步抽取模式（替换 Phase 3 同步模式）：
  - [x] 创建 Topic: `doc-chunk-extract` (`ChunkMessage`)
  - [x] 上传接口改为：分块后投递 Kafka → Consumer 异步消费调用 LLM 抽取 (`ChunkExtractProducer` & `Consumer`)
  - [x] Consumer 限流配置（`max-poll-records`=5）与异常处理降级机制

## Phase 5: GraphRAG Agent 引擎

- [x] 实现 Text2Cypher 提示工程：自然语言 → Cypher 语句生成 → Neo4j 查询 (`Text2CypherService`)
- [x] 实现 ES 混合检索 (Hybrid Search)：关键词 BM25 + Dense Vector KNN 联合排序 (`EsHybridSearchService`)
- [x] 构建并发上下文召回架构 (Parallel Retrieval)：(`GraphRagService`)
  - [x] 赛道 A：CompletableFuture 执行 Text2Cypher 查 Neo4j
  - [x] 赛道 B：CompletableFuture 执行 ES 关键词/混合查询
  - [x] Barrier 汇总 → 组装 Super Prompt → LLM 一次性融合生成回答
- [x] 实现对话历史管理（MongoDB 存储 + Redis 热缓存）(`ChatHistoryService`)
- [x] 实现流式 SSE 响应 (Server-Sent Events) (`ChatController`)

## Phase 6: 前端 MVP 与全链路联调

- [ ] 搭建 Vue 3 + Element Plus 前端脚手架
- [ ] 实现基础布局结构 (左、中、右三栏)，并支持左右面板的收缩与展开
- [ ] 实现关系力导向图可视化组件 (D3.js / vis-network)
- [ ] 实现对话界面 (Chat UI) 支持流式 SSE 展示
- [ ] 实现文档上传管理界面及抽取进度监控
- [ ] 端到端大文件全链路测试与性能优化
