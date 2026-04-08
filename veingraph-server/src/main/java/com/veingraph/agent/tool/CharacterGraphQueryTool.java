package com.veingraph.agent.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Phase 2 工具：人物关系图谱查询
 *
 * <p>底层查询 Neo4j 图数据库，专门用于回答"张三和李四是什么关系"、
 * "帮我梳理一下王五的社会关系网"等图谱结构性问题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CharacterGraphQueryTool {

    private final Driver neo4jDriver;

    /**
     * 查询指定人物/实体的关系网络。
     *
     * @param characterName 要查询的人物姓名或实体名称（支持模糊匹配）
     * @param documentId    限定查询范围的文档 ID，不限定时传空字符串
     * @return 格式化的关系图谱文本，或"未找到"提示
     */
    @Tool(description = "查询人物/实体关系图谱。" +
            "适用于：查询某人物的所有关系、两人之间的关系、人物社交网络等结构性问题。" +
            "不适用于：原文内容、台词、场景描写（请使用 searchBookContent）。")
    public String queryCharacterRelations(
            @ToolParam(description = "要查询的人物姓名或实体名称，支持模糊匹配") String characterName,
            @ToolParam(description = "限定查询范围的文档 ID；不需要限定范围时传空字符串") String documentId) {

        log.info("[CharacterGraphQueryTool] character={}, documentId={}", characterName, documentId);

        Map<String, Object> params = new HashMap<>();
        params.put("name", characterName);

        String cypher;
        if (documentId != null && !documentId.isBlank()) {
            cypher = """
                    MATCH (s:Entity)-[r:RELATION]->(t:Entity)
                    WHERE (s.name CONTAINS $name OR t.name CONTAINS $name)
                      AND r.documentId = $documentId
                    RETURN s.name AS source, r.type AS relation, t.name AS target, r.evidence AS evidence
                    LIMIT 50
                    """;
            params.put("documentId", documentId);
        } else {
            cypher = """
                    MATCH (s:Entity)-[r:RELATION]->(t:Entity)
                    WHERE s.name CONTAINS $name OR t.name CONTAINS $name
                    RETURN s.name AS source, r.type AS relation, t.name AS target, r.evidence AS evidence
                    LIMIT 50
                    """;
        }

        try (Session session = neo4jDriver.session(SessionConfig.defaultConfig())) {
            List<Record> records = session.run(cypher, params).list();

            if (records.isEmpty()) {
                return "图谱中未找到与「" + characterName + "」相关的关系记录。";
            }

            String relations = records.stream()
                    .map(r -> String.format("  %s -[%s]-> %s（依据：%s）",
                            r.get("source").asString(),
                            r.get("relation").asString(),
                            r.get("target").asString(),
                            r.get("evidence").asString("无")))
                    .collect(Collectors.joining("\n"));

            return "【人物关系图谱】「" + characterName + "」的关系网络（共 "
                    + records.size() + " 条）：\n" + relations;
        } catch (Exception e) {
            log.error("[CharacterGraphQueryTool] 图谱查询失败: {}", e.getMessage());
            return "人物关系图谱查询暂时不可用，请稍后重试。";
        }
    }
}
