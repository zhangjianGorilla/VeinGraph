package com.veingraph.document.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文档切分服务
 * 职责：将超长文本切分为适合 LLM Context Window 的较小文本块 (Chunk)
 */
@Slf4j
@Service
public class DocumentChunkingService {

    // 默认切片大小
    private static final int DEFAULT_MAX_CHUNK_SIZE = 1000;
    // 默认切片重叠字数（保证上下文连贯性）
    private static final int DEFAULT_MAX_OVERLAP = 200;

    /**
     * 递归字符切块 (Recursive Character Text Splitter)
     * 这是目前 NLP 领域最推荐的长文切块方式，它会按段落、句子、单词的优先级递归寻找切分点
     *
     * @param document  原始长文档
     * @return 切分后的文本段 (TextSegment) 列表
     */
    public List<TextSegment> split(Document document) {
        log.info("开始使用 RecursiveCharacterTextSplitter 进行长文切块...");
        return DocumentSplitters.recursive(
                DEFAULT_MAX_CHUNK_SIZE,
                DEFAULT_MAX_OVERLAP
        ).split(document);
    }
}
