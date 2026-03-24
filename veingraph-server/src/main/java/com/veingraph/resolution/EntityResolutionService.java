package com.veingraph.resolution;

import com.veingraph.model.ExtractionRecord;
import com.veingraph.repository.mongo.ExtractionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 实体消歧与统一服务
 * 职责：在文档全部 Chunk 抽取完毕后，扫描同一文档内的所有 ExtractionRecord，
 * 基于编辑距离聚类 + LLM 二次确认，将别名统一为标准名称，同步更新 MongoDB 和 Neo4j。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityResolutionService {

    private final ExtractionRecordRepository recordRepository;
    private final ChatClient chatClient;
    private final Driver neo4jDriver;

    @Value("classpath:prompts/resolution-confirm.st")
    private Resource confirmPromptResource;

    /** 编辑距离阈值：低于此值的实体对将被视为候选合并对 */
    @Value("${veingraph.resolution.edit-distance-threshold:3}")
    private int editDistanceThreshold;

    /** LLM 确认置信度阈值：高于此值才执行合并 */
    @Value("${veingraph.resolution.confidence-threshold:0.7}")
    private double confidenceThreshold;

    private final BeanOutputConverter<ResolutionResult> outputConverter =
            new BeanOutputConverter<>(ResolutionResult.class);

    /**
     * 对指定文档执行实体消歧
     */
    public void resolveEntitiesForDocument(String documentId) {
        log.info("[实体消歧] 开始对文档 {} 执行实体消歧...", documentId);
        long start = System.currentTimeMillis();

        // 1. 查询该文档的全部抽取记录
        List<ExtractionRecord> records = recordRepository.findByDocumentId(documentId);
        if (records.isEmpty()) {
            log.info("[实体消歧] 文档 {} 无抽取记录，跳过消歧", documentId);
            return;
        }

        // 2. 收集所有实体名称及其出现的证据
        Map<String, List<String>> entityEvidenceMap = new HashMap<>();
        for (ExtractionRecord r : records) {
            entityEvidenceMap.computeIfAbsent(r.getSource(), k -> new ArrayList<>()).add(r.getEvidence());
            entityEvidenceMap.computeIfAbsent(r.getTarget(), k -> new ArrayList<>()).add(r.getEvidence());
        }

        List<String> entities = new ArrayList<>(entityEvidenceMap.keySet());
        log.info("[实体消歧] 文档 {} 共发现 {} 个不同实体", documentId, entities.size());

        // 3. 基于编辑距离找出候选合并对
        Map<String, String> mergeMap = new HashMap<>(); // oldName -> canonicalName
        Set<String> processed = new HashSet<>();

        for (int i = 0; i < entities.size(); i++) {
            String entityA = entities.get(i);
            if (processed.contains(entityA)) continue;

            for (int j = i + 1; j < entities.size(); j++) {
                String entityB = entities.get(j);
                if (processed.contains(entityB)) continue;

                int distance = levenshteinDistance(entityA, entityB);
                // 仅当编辑距离在阈值内，且不超过较短名称长度的一半时，才进入 LLM 确认
                int maxAllowed = Math.min(editDistanceThreshold, Math.max(entityA.length(), entityB.length()) / 2);
                if (distance > 0 && distance <= maxAllowed) {
                    log.info("[实体消歧] 候选合并对: '{}' <-> '{}', 编辑距离={}", entityA, entityB, distance);

                    // 4. LLM 二次确认
                    String evidenceA = entityEvidenceMap.get(entityA).stream()
                            .limit(3).collect(Collectors.joining("\n"));
                    String evidenceB = entityEvidenceMap.get(entityB).stream()
                            .limit(3).collect(Collectors.joining("\n"));

                    ResolutionResult result = confirmWithLLM(entityA, entityB, evidenceA, evidenceB);
                    if (result != null && result.isSame() && result.getConfidence() >= confidenceThreshold) {
                        String canonical = result.getCanonicalName();
                        String alias = canonical.equals(entityA) ? entityB : entityA;
                        mergeMap.put(alias, canonical);
                        processed.add(alias);
                        log.info("[实体消歧] 确认合并: '{}' -> '{}' (置信度: {})",
                                alias, canonical, result.getConfidence());
                    }
                }
            }
        }

        if (mergeMap.isEmpty()) {
            log.info("[实体消歧] 文档 {} 无需合并的实体", documentId);
            return;
        }

        // 5. 批量更新 MongoDB
        int updatedCount = 0;
        for (ExtractionRecord r : records) {
            boolean changed = false;
            if (mergeMap.containsKey(r.getSource())) {
                r.setSource(mergeMap.get(r.getSource()));
                changed = true;
            }
            if (mergeMap.containsKey(r.getTarget())) {
                r.setTarget(mergeMap.get(r.getTarget()));
                changed = true;
            }
            if (changed) {
                recordRepository.save(r);
                updatedCount++;
            }
        }
        log.info("[实体消歧] MongoDB 已更新 {} 条记录", updatedCount);

        // 6. 在 Neo4j 中合并节点
        for (Map.Entry<String, String> entry : mergeMap.entrySet()) {
            mergeNodesInNeo4j(entry.getKey(), entry.getValue(), documentId);
        }

        log.info("[实体消歧] 文档 {} 消歧完成，共合并 {} 个实体别名，耗时 {} ms",
                documentId, mergeMap.size(), System.currentTimeMillis() - start);
    }

    /**
     * 调用 LLM 二次确认两个实体是否相同
     */
    private ResolutionResult confirmWithLLM(String entityA, String entityB,
                                            String evidenceA, String evidenceB) {
        try {
            PromptTemplate template = new PromptTemplate(confirmPromptResource);
            String prompt = template.render(Map.of(
                    "entityA", entityA,
                    "entityB", entityB,
                    "evidenceA", evidenceA,
                    "evidenceB", evidenceB,
                    "format", outputConverter.getFormat()
            ));

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return outputConverter.convert(response);
        } catch (Exception e) {
            log.warn("[实体消歧] LLM 确认失败 ({}↔{}): {}", entityA, entityB, e.getMessage());
            return null;
        }
    }

    /**
     * 在 Neo4j 中将别名节点合并到标准节点
     * 策略：将别名节点的所有关系转移到标准节点，然后删除别名节点
     */
    private void mergeNodesInNeo4j(String aliasName, String canonicalName, String documentId) {
        String cypher = """
                // 将别名节点的出边转移到标准节点
                MATCH (alias:Entity {name: $aliasName})-[r]->(t)
                WHERE r.documentId = $documentId
                MERGE (canonical:Entity {name: $canonicalName})
                MERGE (canonical)-[newR:RELATION {type: r.type}]->(t)
                  ON CREATE SET newR = properties(r)
                DELETE r
                WITH alias
                // 将别名节点的入边转移到标准节点
                MATCH (s)-[r]->(alias)
                WHERE r.documentId = $documentId
                MERGE (canonical:Entity {name: $canonicalName})
                MERGE (s)-[newR:RELATION {type: r.type}]->(canonical)
                  ON CREATE SET newR = properties(r)
                DELETE r
                WITH alias
                // 如果别名节点已无关系，则删除
                OPTIONAL MATCH (alias)-[remaining]-()
                WITH alias, count(remaining) AS relCount
                WHERE relCount = 0
                DELETE alias
                """;

        try (Session session = neo4jDriver.session(SessionConfig.defaultConfig())) {
            session.run(cypher, Map.of(
                    "aliasName", aliasName,
                    "canonicalName", canonicalName,
                    "documentId", documentId
            )).consume();
            log.info("[实体消歧] Neo4j 节点合并完成: '{}' -> '{}'", aliasName, canonicalName);
        } catch (Exception e) {
            log.error("[实体消歧] Neo4j 合并失败 ({}->{}): {}", aliasName, canonicalName, e.getMessage());
        }
    }

    /**
     * Levenshtein 编辑距离算法
     */
    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(dp[i - 1][j] + 1,
                        Math.min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost));
            }
        }
        return dp[a.length()][b.length()];
    }
}
