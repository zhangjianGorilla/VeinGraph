package com.veingraph.repository.mongo;

import com.veingraph.model.DocumentChunk;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DocumentChunkRepository extends MongoRepository<DocumentChunk, String> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(String documentId);

    long countByDocumentId(String documentId);

    /**
     * 查询未向量化的 Chunk，供 VectorSyncScheduler 批量处理
     */
    List<DocumentChunk> findTop50ByVectorizedFalseOrderByCreatedAtAsc();
}
