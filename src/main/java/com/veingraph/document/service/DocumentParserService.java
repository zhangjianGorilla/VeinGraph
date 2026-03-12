package com.veingraph.document.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * 核心文档解析服务
 * 职责：纯本地文档格式转换（PDF/Word/TXT → 纯文本），不涉及任何网络 IO
 */
@Slf4j
@Service
public class DocumentParserService {

    private final DocumentParser documentParser = new ApacheTikaDocumentParser();

    /**
     * 解析任意格式的文档流为纯文本 Document 对象
     *
     * @param inputStream 原始文件流 (PDF, Word, TXT 等)
     * @return 解析后的纯文本对象
     */
    public Document parse(InputStream inputStream) {
        log.info("开始使用 Apache Tika 本地解析文档...");
        return documentParser.parse(inputStream);
    }
}
