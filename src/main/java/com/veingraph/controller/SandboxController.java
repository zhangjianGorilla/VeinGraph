package com.veingraph.controller;

import com.veingraph.common.result.Result;
import com.veingraph.document.service.DocumentChunkingService;
import com.veingraph.document.service.DocumentParserService;
import com.veingraph.llm.model.ExtractionResult;
import com.veingraph.llm.service.EntityExtractionService;
import com.veingraph.llm.service.LlmChatService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 2 测试控制器
 * 提供供外部验证文档解析与基础对话的接口
 */
@RestController
@RequestMapping("/api/sandbox")
public class SandboxController {

    @Autowired
    private DocumentParserService documentParserService;

    @Autowired
    private DocumentChunkingService documentChunkingService;

    @Autowired
    private LlmChatService llmChatService;

    @Autowired
    private EntityExtractionService entityExtractionService;

    /**
     * 测试：大模型基础闲聊通信
     */
    @GetMapping("/chat")
    public Result<String> testLlmChat(@RequestParam(defaultValue = "你好，请自我介绍并说明你背后的模型是什么？") String msg) {
        String reply = llmChatService.chat(msg);
        return Result.ok(reply);
    }

    /**
     * 测试：LangChain4j 文本解析与分段分块策略
     */
    @GetMapping("/chunking")
    public Result<Map<String, Object>> testDocumentChunking() {
        String mockLongText = "这是第一段极其冗长的测试文本。".repeat(20) + 
                              "\n\n\n接下来进入第二章的测试内容。".repeat(20) + 
                              "\n\n这段文字用于测试LangChain4j引擎切分时的Overlap重叠机制是否正常运作。";
        
        // 1. Mock InputStream
        ByteArrayInputStream is = new ByteArrayInputStream(mockLongText.getBytes(StandardCharsets.UTF_8));
        
        // 2. Parse (Tika)
        Document document = documentParserService.parse(is);
        
        // 3. Split (RecursiveCharacterTextSplitter)
        List<TextSegment> segments = documentChunkingService.split(document);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("originalLength", mockLongText.length());
        result.put("chunkCount", segments.size());
        result.put("chunks", segments.stream().map(TextSegment::text).toList());

        return Result.ok(result);
    }

    /**
     * 测试：大语言模型结构化实体与关系抽取 (Function Calling / Schema 约束)
     */
    @GetMapping("/extract")
    public Result<ExtractionResult> testEntityExtraction(@RequestParam(defaultValue = "王局长昨天在会议室会见了李主任，两人讨论了部门预算事宜。") String text) {
        ExtractionResult result = entityExtractionService.extract(text);
        return Result.ok(result);
    }
}
