package com.veingraph.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Milvus 向量检索服务
 * 基于 Spring AI VectorStore 抽象执行 ANN 近似最近邻搜索，返回语义最相关的文本块
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusVectorSearchService {

    private final VectorStore vectorStore;

    /**
     * 向量相似度检索
     *
     * @param documentId 限定的文档 ID（可为空，为空则全局搜）
     * @param query      查询文本（VectorStore 内部完成 Embedding）
     * @param topK       返回数量上限
     * @return 相关文本块列表
     */
    public List<String> vectorSearch(String documentId, String query, int topK) {
        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(topK);

            if (documentId != null && !documentId.isBlank()) {
                builder.filterExpression("document_id == '" + documentId + "'");
            }

            List<Document> results = vectorStore.similaritySearch(builder.build());

            List<String> texts = results.stream()
                    .map(Document::getText)
                    .filter(text -> text != null && !text.isEmpty())
                    .collect(Collectors.toList());

            log.info("Milvus 向量检索: 命中 {} 条", texts.size());
            return texts;

        } catch (Exception e) {
            log.error("Milvus 向量检索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
