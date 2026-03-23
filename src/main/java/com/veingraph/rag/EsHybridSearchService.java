package com.veingraph.rag;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.veingraph.model.ChunkVectorDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ES 混合检索服务
 * 支持纯关键词检索和关键词 + 向量混合检索两种模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EsHybridSearchService {

    private final ElasticsearchClient esClient;

    private static final String INDEX_NAME = "veingraph-chunks";

    /**
     * 关键词检索（BM25）
     * 当向量化组件尚未就绪时使用此降级模式
     *
     * @param question 用户问题文本
     * @param topK     返回数量上限
     * @return 相关文本块列表
     */
    public List<String> keywordSearch(String question, int topK) {
        try {
            Query matchQuery = MatchQuery.of(m -> m
                    .field("text")
                    .query(question))._toQuery();

            SearchRequest request = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(matchQuery)
                    .size(topK));

            SearchResponse<ChunkVectorDocument> response =
                    esClient.search(request, ChunkVectorDocument.class);

            List<String> results = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(doc -> doc != null)
                    .map(ChunkVectorDocument::getText)
                    .collect(Collectors.toList());

            log.info("ES 关键词检索: query='{}', 命中 {} 条", question, results.size());
            return results;

        } catch (IOException e) {
            log.error("ES 关键词检索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 混合检索：BM25 关键词 + Dense Vector KNN 联合排序
     * Phase 5 完整版本（需要 Embedding 模型生成 queryVector）
     *
     * @param question    用户问题文本
     * @param queryVector 问题的稠密向量表示
     * @param topK        返回数量上限
     * @return 相关文本块列表
     */
    public List<String> hybridSearch(String question, float[] queryVector, int topK) {
        try {
            // BM25 关键词匹配
            Query matchQuery = MatchQuery.of(m -> m
                    .field("text")
                    .query(question)
                    .boost(0.3f))._toQuery();

            // 构建混合查询: BM25 + KNN
            SearchRequest request = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(BoolQuery.of(b -> b.should(matchQuery))._toQuery())
                    .knn(k -> k
                            .field("vector")
                            .queryVector(toList(queryVector))
                            .k(topK)
                            .numCandidates(topK * 2)
                            .boost(0.7f))
                    .size(topK));

            SearchResponse<ChunkVectorDocument> response =
                    esClient.search(request, ChunkVectorDocument.class);

            List<String> results = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(doc -> doc != null)
                    .map(ChunkVectorDocument::getText)
                    .collect(Collectors.toList());

            log.info("ES 混合检索: query='{}', 命中 {} 条", question, results.size());
            return results;

        } catch (IOException e) {
            log.error("ES 混合检索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** float[] 转 List<Float> */
    private List<Float> toList(float[] arr) {
        List<Float> list = new java.util.ArrayList<>(arr.length);
        for (float f : arr) {
            list.add(f);
        }
        return list;
    }
}
