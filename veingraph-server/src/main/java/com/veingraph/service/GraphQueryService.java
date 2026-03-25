package com.veingraph.service;

import com.veingraph.controller.vo.GraphDataVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphQueryService {

    private final Driver neo4jDriver;

    /**
     * 获取指定文档的隔离图谱数据
     * @param documentId 文档ID。如果为空，则默认返回全局的全量（或限制一定数量）图谱。
     */
    public GraphDataVO getGraphData(String documentId) {
        String cypher;
        Map<String, Object> params = new HashMap<>();

        if (documentId != null && !documentId.isBlank()) {
            cypher = """
                MATCH (s:Entity)-[r:RELATION]->(t:Entity)
                WHERE r.documentId = $documentId
                RETURN s.name AS source, t.name AS target, r.type AS relation
                LIMIT 500
                """;
            params.put("documentId", documentId);
        } else {
            // 全局漫游图谱，为防止数据量过大导致前端卡死，限制返回 300 条关系
            cypher = """
                MATCH (s:Entity)-[r:RELATION]->(t:Entity)
                RETURN s.name AS source, t.name AS target, r.type AS relation
                LIMIT 300
                """;
        }

        List<GraphDataVO.Node> nodes = new ArrayList<>();
        List<GraphDataVO.Edge> edges = new ArrayList<>();
        Set<String> nodeNames = new HashSet<>();

        try (Session session = neo4jDriver.session(SessionConfig.defaultConfig())) {
            Result result = session.run(cypher, params);
            while (result.hasNext()) {
                Record record = result.next();
                String source = record.get("source").asString();
                String target = record.get("target").asString();
                String relation = record.get("relation").asString();

                if (nodeNames.add(source)) {
                    nodes.add(new GraphDataVO.Node(source, source, null, null));
                }
                if (nodeNames.add(target)) {
                    nodes.add(new GraphDataVO.Node(target, target, null, null));
                }

                edges.add(new GraphDataVO.Edge(source, target, relation, null));
            }
        } catch (Exception e) {
            log.error("查询图谱数据失败: {}", e.getMessage(), e);
        }

        return new GraphDataVO(nodes, edges);
    }
}
