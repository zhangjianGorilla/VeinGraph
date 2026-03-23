# Phase 3 实体消歧与统一 (Entity Resolution) 实现计划

## 目标

在现有的文档抽取链路中补充**实体消歧**能力，解决以下两个核心问题：
1. **同名/别名合并**：跨 Chunk 出现的同一实体因名称变体（如「莫里斯」「老莫」「奥古斯特」）而被创建为多个 Neo4j 节点的问题
2. **代词指代消解**：增强 Prompt 层指令，确保 LLM 在抽取时主动将「他」「她」「该人」等代词还原为全名；并在后处理阶段校验残余代词

## 设计思路

采用**两阶段策略**：
- **阶段 A（Prompt 增强 + 后处理校验）**：强化 LLM 端的代词消解能力，并在 Consumer 侧增加正则校验拦截
- **阶段 B（批量实体合并服务）**：在文档所有 Chunk 抽取完毕后，扫描同一文档内的全部 [ExtractionRecord](file:///c:/Code/VeinGraph/src/main/java/com/veingraph/model/ExtractionRecord.java#14-54)，基于编辑距离 + LLM 二次确认进行实体合并

---

## Proposed Changes

### 组件 1：Prompt 模板增强（代词消解）

#### [MODIFY] [extraction-system.st](file:///c:/Code/VeinGraph/src/main/resources/prompts/extraction-system.st)

在 System Prompt 中追加更精确的代词消解指令和别名统一规则：
- 要求 LLM 对文本中的代词必须回溯上文替换为实体全名
- 要求 LLM 对同一实体的不同称呼统一使用首次出现的全名
- 增加 Few-Shot 示例，展示正确的代词消解结果

---

### 组件 2：后处理校验（代词残留拦截）

#### [MODIFY] [ChunkExtractConsumer.java](file:///c:/Code/VeinGraph/src/main/java/com/veingraph/kafka/ChunkExtractConsumer.java)

在 Consumer 将 [RelationRecord](file:///c:/Code/VeinGraph/src/main/java/com/veingraph/llm/model/RelationRecord.java#10-30) 写入 MongoDB 前，增加一道**后处理校验层**：
- 用正则检测 `source` / `target` 是否仍为代词（如「他」「她」「该人」「此人」等）
- 若检测到代词残留，将该条记录标记为 `NEEDS_HUMAN`，跳过写入，打印警告日志
- 同时过滤掉 source == target 的自环关系

---

### 组件 3：实体消歧合并服务（同名合并）

#### [NEW] [EntityResolutionService.java](file:///c:/Code/VeinGraph/src/main/java/com/veingraph/resolution/EntityResolutionService.java)

核心消歧服务，主方法 `resolveEntitiesForDocument(documentId)`：
1. 从 MongoDB 查询该文档的全部 [ExtractionRecord](file:///c:/Code/VeinGraph/src/main/java/com/veingraph/model/ExtractionRecord.java#14-54)
2. 收集所有出现过的实体名称，按编辑距离（Levenshtein）进行分组聚类（阈值可配置）
3. 对编辑距离相近的实体对，调用 LLM 进行二次确认：「A 和 B 是否指同一实体？」
4. 确认后批量更新 MongoDB 中 [ExtractionRecord](file:///c:/Code/VeinGraph/src/main/java/com/veingraph/model/ExtractionRecord.java#14-54) 的 `source`/`target` 字段为**标准名**（canonicalName）
5. 同时在 Neo4j 中执行 `MERGE` 合并旧节点到标准节点

#### [NEW] [resolution-confirm.st](file:///c:/Code/VeinGraph/src/main/resources/prompts/resolution-confirm.st)

LLM 二次确认 Prompt 模板：给定两个实体名称和上下文证据，判断是否为同一实体

---

### 组件 4：消歧触发时机

#### [MODIFY] [ChunkExtractConsumer.java](file:///c:/Code/VeinGraph/src/main/java/com/veingraph/kafka/ChunkExtractConsumer.java)

在文档全部 Chunk 抽取完毕后（通过检查 [DocumentChunk](file:///c:/Code/VeinGraph/src/main/java/com/veingraph/model/DocumentChunk.java#14-44) 是否全部 `extracted=true`），自动触发 `EntityResolutionService.resolveEntitiesForDocument(documentId)`

#### [MODIFY] [ExtractionRecordRepository.java](file:///c:/Code/VeinGraph/src/main/java/com/veingraph/repository/mongo/ExtractionRecordRepository.java)

新增查询方法：
- `findDistinctEntitiesByDocumentId(documentId)` — 获取文档内所有不重复的实体名称
- `updateEntityName(documentId, oldName, newName)` — 批量替换实体名

---

## Verification Plan

### 编译验证
```bash
mvn compile -q
```

### 功能验证
1. 上传一篇含有大量代词指代的测试文档
2. 观察控制台日志，确认代词残留被正确拦截
3. 确认文档抽取完毕后，消歧服务自动触发
4. 查看 Neo4j Browser 检查是否还存在明显的重复节点
