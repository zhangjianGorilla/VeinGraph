package com.veingraph.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文档切分服务
 * 职责：将超长文本切分为适合 LLM Context Window 的较小文本块（Chunk）
 */
@Slf4j
@Service
public class DocumentChunkingService {

    /** 单个 Chunk 最大 Token 数（中文约等于字符数） */
    @Value("${veingraph.chunking.max-size:1000}")
    private int maxChunkSize;

    /** 相邻 Chunk 最小字符数（控制切块粒度下限，保证上下文连贯） */
    @Value("${veingraph.chunking.max-overlap:200}")
    private int minChunkSizeChars;

    /**
     * 使用配置参数进行文本切块
     */
    public List<Document> split(Document document) {
        return split(document, maxChunkSize, minChunkSizeChars);
    }

    /**
     * Token 级文本切块（TokenTextSplitter）
     *
     * @param document         原始长文档
     * @param chunkSize        单块最大 Token 数
     * @param minChunkChars    切块最小字符数下限
     * @return 切分后的文本块列表
     */
    public List<Document> split(Document document, int chunkSize, int minChunkChars) {
        log.info("TokenTextSplitter 切块: chunkSize={}, minChunkChars={}", chunkSize, minChunkChars);
        TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, minChunkChars, 5, 10000, true);
        return splitter.split(document);
    }
}
