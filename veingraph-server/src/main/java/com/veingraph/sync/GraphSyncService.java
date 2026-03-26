package com.veingraph.sync;

import com.veingraph.model.ExtractionRecord;
import com.veingraph.repository.mongo.ExtractionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Neo4j 图同步服务
 * 将 ExtractionRecord 中的实体和关系通过 Cypher MERGE 写入 Neo4j
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphSyncService {

    private final Driver neo4jDriver;
    private final ExtractionRecordRepository recordRepository;

    /**
     * 将单条抽取记录同步到 Neo4j
     * 使用 MERGE 保证幂等性（重复执行不会产生重复节点/边）
     */
    public void syncRecord(ExtractionRecord record) {
        String cypher = """
                MERGE (s:Entity {name: $source})
                SET s.documentId = $documentId
                MERGE (t:Entity {name: $target})
                SET t.documentId = $documentId
                MERGE (s)-[r:RELATION {type: $relation, documentId: $documentId}]->(t)
                  ON CREATE SET r.evidence = $evidence,
                                r.chunkId = $chunkId,
                                r.createdAt = datetime()
                  ON MATCH SET  r.evidence = $evidence,
                                r.chunkId = $chunkId
                """;

        Map<String, Object> params = Map.of(
                "source", record.getSource(),
                "target", record.getTarget(),
                "relation", record.getRelation(),
                "evidence", record.getEvidence(),
                "documentId", record.getDocumentId(),
                "chunkId", record.getChunkId()
        );

        try (Session session = neo4jDriver.session(SessionConfig.defaultConfig())) {
            session.run(cypher, params).consume();
        }

        // 更新 MongoDB 同步状态
        record.setSyncStatus(ExtractionRecord.SYNC_SYNCED);
        recordRepository.save(record);

        log.debug("已同步到 Neo4j: ({}) -[{}]-> ({})",
                record.getSource(), record.getRelation(), record.getTarget());
    }
}
