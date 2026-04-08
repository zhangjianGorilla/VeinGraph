package com.veingraph.agent.tool;

import com.veingraph.rag.EsHybridSearchService;
import com.veingraph.rag.MilvusVectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Phase 2 工具：书籍原文语义检索
 *
 * <p>底层执行 Milvus 向量检索（语义）+ Elasticsearch BM25 关键词检索的混合召回，
 * 专门用于回答"书中原话是怎么描述某段设定"、"某段剧情发生在哪里"等原文相关问题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookTextSearchTool {

    private final MilvusVectorSearchService milvusVectorSearchService;
    private final EsHybridSearchService esHybridSearchService;

    @Value("${veingraph.rag.es-top-k:5}")
    private int esTopK;

    @Value("${veingraph.rag.milvus-top-k:5}")
    private int milvusTopK;

    /**
     * 混合检索书籍原文内容。
     * 适用于查找：原文描述、人物台词、情节细节、场景描写等。
     *
     * @param query      搜索关键词或语义查询，尽量包含核心实体和事件
     * @param documentId 限定搜索范围的文档 ID，不限定时传空字符串
     * @return 检索到的原文片段，或"未找到"提示
     */
    @Tool(description = "混合检索书籍原文内容（向量语义 + 关键词）。" +
            "适用于：查找原文描述、人物台词、情节细节、场景描写等。" +
            "不适用于：人物关系网络查询（请使用 queryCharacterRelations）。")
    public String searchBookContent(
            @ToolParam(description = "搜索关键词或查询语句，尽量包含核心实体和事件关键词") String query,
            @ToolParam(description = "限定搜索范围的文档 ID；不需要限定范围时传空字符串") String documentId) {

        log.info("[BookTextSearchTool] query={}, documentId={}", query, documentId);

        String docFilter = (documentId == null || documentId.isBlank()) ? null : documentId;

        try {
            List<String> esResults = esHybridSearchService.keywordSearch(docFilter, query, esTopK);
            List<String> milvusResults = milvusVectorSearchService.vectorSearch(docFilter, query, milvusTopK);

            Set<String> merged = new LinkedHashSet<>(esResults);
            merged.addAll(milvusResults);

            if (merged.isEmpty()) {
                return "未在书籍原文中找到与「" + query + "」相关的内容。";
            }

            return "【书籍原文检索结果】\n" + String.join("\n---\n", merged);
        } catch (Exception e) {
            log.error("[BookTextSearchTool] 检索失败: {}", e.getMessage());
            return "书籍原文检索暂时不可用，请稍后重试。";
        }
    }
}
