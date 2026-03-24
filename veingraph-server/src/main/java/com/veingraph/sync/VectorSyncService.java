package com.veingraph.sync;

import com.veingraph.model.ChunkVectorDocument;
import com.veingraph.model.DocumentChunk;
import com.veingraph.repository.mongo.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

/**
 * 向量化同步服务
 * 将 DocumentChunk 通过 EmbeddingModel 转为稠密向量，写入 ES
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSyncService {

    private final EmbeddingModel embeddingModel;
    private final ElasticsearchOperations esOperations;
    private final DocumentChunkRepository chunkRepository;

    /**
     * 对单个 Chunk 进行向量化并写入 ES
     */
    public void vectorizeAndSync(DocumentChunk chunk) {
        // 1. 调用 Embedding 模型生成向量
        float[] vector = embeddingModel.embed(chunk.getText());
        log.debug("Embedding 完成: chunkId={}, 维度={}", chunk.getId(), vector.length);

        // 2. 构建 ES 文档
        ChunkVectorDocument doc = new ChunkVectorDocument();
        doc.setId(chunk.getId());
        doc.setDocumentId(chunk.getDocumentId());
        doc.setText(chunk.getText());
        doc.setVector(vector);

        // 3. 写入 ES（save 支持 upsert 语义）
        esOperations.save(doc);

        // 4. 更新 MongoDB 标记
        chunk.setVectorized(true);
        chunkRepository.save(chunk);

        log.debug("Chunk 向量化同步完成: chunkId={}", chunk.getId());
    }
}
