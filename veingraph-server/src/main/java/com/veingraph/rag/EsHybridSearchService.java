package com.veingraph.rag;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ES 关键词检索服务（BM25）
 * 向量检索已迁移至 Milvus，本类仅保留 BM25 全文检索
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
}
