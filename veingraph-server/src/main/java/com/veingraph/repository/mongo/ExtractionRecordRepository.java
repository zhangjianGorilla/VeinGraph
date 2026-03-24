package com.veingraph.repository.mongo;

import com.veingraph.model.ExtractionRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ExtractionRecordRepository extends MongoRepository<ExtractionRecord, String> {

    List<ExtractionRecord> findByDocumentId(String documentId);

    List<ExtractionRecord> findByChunkId(String chunkId);

    List<ExtractionRecord> findBySyncStatus(String syncStatus);

    long countByDocumentId(String documentId);

    /**
     * 按 syncStatus 查询，限制数量，按创建时间升序（先进先出）
     * 供 OutboxScheduler 批量分发使用
     */
    @org.springframework.data.mongodb.repository.Query("{'syncStatus': ?0}")
    List<ExtractionRecord> findTopBySyncStatusOrderByCreatedAtAsc(String syncStatus, int limit);
}
