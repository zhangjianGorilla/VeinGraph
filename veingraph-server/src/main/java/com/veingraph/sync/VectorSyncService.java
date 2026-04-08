package com.veingraph.sync;

import com.veingraph.model.DocumentChunk;
import com.veingraph.repository.mongo.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import com.veingraph.model.ChunkVectorDocument;

import java.util.List;
import java.util.Map;

/**
 * 向量化同步服务
 * 将 DocumentChunk 通过 EmbeddingModel 转为稠密向量，写入 Milvus（由 VectorStore 封装）
 * 同时将文本写入 ES（用于 BM25 关键词检索）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSyncService {

    private final VectorStore vectorStore;
    private final ElasticsearchOperations esOperations;
    private final DocumentChunkRepository chunkRepository;

    /**
     * 对单个 Chunk 进行向量化并写入 Milvus + ES
     */
    public void vectorizeAndSync(DocumentChunk chunk) {
        // 1. 写入 Milvus（VectorStore 内部完成 Embedding + upsert）
        Document doc = Document.builder()
                .id(chunk.getId())
                .text(chunk.getText())
                .metadata(Map.of("document_id", chunk.getDocumentId()))
                .build();
        vectorStore.add(List.of(doc));
        log.debug("Milvus upsert 完成: chunkId={}", chunk.getId());

        // 2. 写入 ES（仅文本，用于 BM25 关键词检索）
        ChunkVectorDocument esDoc = new ChunkVectorDocument();
        esDoc.setId(chunk.getId());
        esDoc.setDocumentId(chunk.getDocumentId());
        esDoc.setText(chunk.getText());
        esOperations.save(esDoc);

        // 3. 更新 MongoDB 标记
        chunk.setVectorized(true);
        chunkRepository.save(chunk);

        log.debug("Chunk 向量化同步完成: chunkId={}", chunk.getId());
    }
}
