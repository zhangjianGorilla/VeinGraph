package com.veingraph.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * 核心文档解析服务
 * 职责：纯本地文档格式转换（PDF/Word/TXT → 纯文本），不涉及任何网络 IO
 */
@Slf4j
@Service
public class DocumentParserService {

    /**
     * 解析任意格式的文档流为 Spring AI Document 对象
     *
     * @param inputStream 原始文件流 (PDF, Word, TXT 等)
     * @return 解析后的纯文本 Document
     */
    public Document parse(InputStream inputStream) {
        log.info("开始使用 Apache Tika 本地解析文档...");
        TikaDocumentReader reader = new TikaDocumentReader(new InputStreamResource(inputStream));
        List<Document> docs = reader.get();
        return docs.isEmpty() ? new Document("") : docs.get(0);
    }
}
