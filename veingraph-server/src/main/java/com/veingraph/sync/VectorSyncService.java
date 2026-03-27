package com.veingraph.sync;

import com.google.gson.JsonObject;
import com.veingraph.model.ChunkVectorDocument;
import com.veingraph.model.DocumentChunk;
import com.veingraph.repository.mongo.DocumentChunkRepository;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.UpsertReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 向量化同步服务
 * 将 DocumentChunk 通过 EmbeddingModel 转为稠密向量，写入 Milvus
 * 同时将文本写入 ES（用于 BM25 关键词检索）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSyncService {

    private final EmbeddingModel embeddingModel;
    private final ElasticsearchOperations esOperations;
    private final MilvusClientV2 milvusClient;
    private final DocumentChunkRepository chunkRepository;

    @Value("${veingraph.milvus.collection-name:veingraph_chunks}")
    private String collectionName;

    /**
     * 对单个 Chunk 进行向量化并写入 Milvus + ES
     */
    public void vectorizeAndSync(DocumentChunk chunk) {
        // 1. 调用 Embedding 模型生成向量
        float[] vector = embeddingModel.embed(chunk.getText());
        log.debug("Embedding 完成: chunkId={}, 维度={}", chunk.getId(), vector.length);

        // 2. 写入 Milvus（向量 + 元数据）
        JsonObject row = new JsonObject();
        row.addProperty("id", chunk.getId());
        row.addProperty("document_id", chunk.getDocumentId());
        row.addProperty("text", chunk.getText());
        com.google.gson.JsonArray vectorArray = new com.google.gson.JsonArray();
        for (float v : vector) {
            vectorArray.add(v);
        }
        row.add("vector", vectorArray);

        milvusClient.upsert(UpsertReq.builder()
                .collectionName(collectionName)
                .data(Collections.singletonList(row))
                .build());

        // 3. 写入 ES（仅文本，用于 BM25 关键词检索）
        ChunkVectorDocument doc = new ChunkVectorDocument();
        doc.setId(chunk.getId());
        doc.setDocumentId(chunk.getDocumentId());
        doc.setText(chunk.getText());
        esOperations.save(doc);

        // 4. 更新 MongoDB 标记
        chunk.setVectorized(true);
        chunkRepository.save(chunk);

        log.debug("Chunk 向量化同步完成: chunkId={}", chunk.getId());
    }
}
