package com.veingraph.document.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文档切分服务
 * 职责：将超长文本切分为适合 LLM Context Window 的较小文本块 (Chunk)
 */
@Slf4j
@Service
public class DocumentChunkingService {

    /** 单个 Chunk 最大字符数 */
    @Value("${veingraph.chunking.max-size:1000}")
    private int maxChunkSize;

    /** 相邻 Chunk 的重叠字符数（保证上下文连贯性） */
    @Value("${veingraph.chunking.max-overlap:200}")
    private int maxOverlap;

    /**
     * 使用默认参数进行递归字符切块
     */
    public List<TextSegment> split(Document document) {
        return split(document, maxChunkSize, maxOverlap);
    }

    /**
     * 递归字符切块 (Recursive Character Text Splitter)
     * 按段落 → 句子 → 单词的优先级递归寻找最优切分点
     *
     * @param document  原始长文档
     * @param chunkSize 单块最大字符数
     * @param overlap   相邻块重叠字符数
     * @return 切分后的文本段列表
     */
    public List<TextSegment> split(Document document, int chunkSize, int overlap) {
        log.info("RecursiveCharacterTextSplitter 切块: chunkSize={}, overlap={}", chunkSize, overlap);
        return DocumentSplitters.recursive(chunkSize, overlap).split(document);
    }
}
