package com.veingraph.rag;


import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Milvus 向量检索服务
 * 基于 ANN 近似最近邻搜索，返回语义最相关的文本块
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusVectorSearchService {

    private final MilvusClientV2 milvusClient;

    @Value("${veingraph.milvus.collection-name:veingraph_chunks}")
    private String collectionName;

    /**
     * 向量相似度检索
     *
     * @param documentId  限定的文档 ID（可为空，为空则全局搜）
     * @param queryVector 问题的稠密向量表示
     * @param topK        返回数量上限
     * @return 相关文本块列表
     */
    public List<String> vectorSearch(String documentId, float[] queryVector, int topK) {
        try {
            // 构建过滤表达式
            String filter = null;
            if (documentId != null && !documentId.isBlank()) {
                filter = "document_id == \"" + documentId + "\"";
            }

            SearchReq.SearchReqBuilder builder = SearchReq.builder()
                    .collectionName(collectionName)
                    .data(Collections.singletonList(new FloatVec(queryVector)))
                    .annsField("vector")
                    .topK(topK)
                    .outputFields(List.of("text"));

            if (filter != null) {
                builder.filter(filter);
            }

            SearchResp response = milvusClient.search(builder.build());

            List<List<SearchResp.SearchResult>> results = response.getSearchResults();
            if (results.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> texts = results.get(0).stream()
                    .map(result -> {
                        Object textObj = result.getEntity().get("text");
                        return textObj != null ? textObj.toString() : "";
                    })
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.toList());

            log.info("Milvus 向量检索: 命中 {} 条", texts.size());
            return texts;

        } catch (Exception e) {
            log.error("Milvus 向量检索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
