package com.veingraph.repository.mongo;

import com.veingraph.model.ExtractionRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ExtractionRecordRepository extends MongoRepository<ExtractionRecord, String> {

    List<ExtractionRecord> findByDocumentId(String documentId);

    List<ExtractionRecord> findByChunkId(String chunkId);

    List<ExtractionRecord> findBySyncStatus(String syncStatus);

    long countByDocumentId(String documentId);
}
