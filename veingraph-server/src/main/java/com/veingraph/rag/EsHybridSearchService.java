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
     *
     * @param documentId 限定的文档 ID（可为空，为空则全局搜）
     * @param question 用户问题文本
     * @param topK     返回数量上限
     * @return 相关文本块列表
     */
    public List<String> keywordSearch(String documentId, String question, int topK) {
        try {
            Query matchQuery = MatchQuery.of(m -> m
                    .field("text")
                    .query(question))._toQuery();

            // 构建最终查询（带或不带文档过滤）
            final Query searchQuery;
            if (documentId != null && !documentId.isBlank()) {
                Query termQuery = co.elastic.clients.elasticsearch._types.query_dsl.TermQuery.of(t -> t
                        .field("documentId")
                        .value(documentId))._toQuery();
                searchQuery = BoolQuery.of(b -> b.must(matchQuery).filter(termQuery))._toQuery();
            } else {
                searchQuery = matchQuery;
            }

            SearchRequest request = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(searchQuery)
                    .size(topK));

            SearchResponse<com.fasterxml.jackson.databind.node.ObjectNode> response =
                    esClient.search(request, com.fasterxml.jackson.databind.node.ObjectNode.class);

            List<String> results = response.hits().hits().stream()
                    .map(co.elastic.clients.elasticsearch.core.search.Hit::source)
                    .filter(doc -> doc != null && doc.has("text"))
                    .map(doc -> doc.get("text").asText())
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
     * 
     * @param documentId  限定的文档 ID（可为空）
     * @param question    用户问题文本
     * @param queryVector 问题的稠密向量表示
     * @param topK        返回数量上限
     * @return 相关文本块列表
     */
    public List<String> hybridSearch(String documentId, String question, float[] queryVector, int topK) {
        try {
            // BM25 关键词匹配
            Query matchQuery = MatchQuery.of(m -> m
                    .field("text")
                    .query(question)
                    .boost(0.3f))._toQuery();

            // 构建最终查询（带或不带文档过滤）
            final Query searchQuery;
            if (documentId != null && !documentId.isBlank()) {
                Query termQuery = co.elastic.clients.elasticsearch._types.query_dsl.TermQuery.of(t -> t
                        .field("documentId")
                        .value(documentId))._toQuery();
                searchQuery = BoolQuery.of(b -> b.should(matchQuery).filter(termQuery))._toQuery();
            } else {
                searchQuery = BoolQuery.of(b -> b.should(matchQuery))._toQuery();
            }

            // KNN 预过滤查询（可选）
            final Query knnFilter;
            if (documentId != null && !documentId.isBlank()) {
                knnFilter = co.elastic.clients.elasticsearch._types.query_dsl.TermQuery.of(t -> t
                        .field("documentId")
                        .value(documentId))._toQuery();
            } else {
                knnFilter = null;
            }

            SearchRequest request = SearchRequest.of(s -> {
                s.index(INDEX_NAME)
                 .query(searchQuery)
                 .size(topK);
                s.knn(k -> {
                    k.field("vector")
                     .queryVector(toList(queryVector))
                     .k(topK)
                     .numCandidates(topK * 2)
                     .boost(0.7f);
                    if (knnFilter != null) {
                        k.filter(knnFilter);
                    }
                    return k;
                });
                return s;
            });

            SearchResponse<com.fasterxml.jackson.databind.node.ObjectNode> response =
                    esClient.search(request, com.fasterxml.jackson.databind.node.ObjectNode.class);

            List<String> results = response.hits().hits().stream()
                    .map(co.elastic.clients.elasticsearch.core.search.Hit::source)
                    .filter(doc -> doc != null && doc.has("text"))
                    .map(doc -> doc.get("text").asText())
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
