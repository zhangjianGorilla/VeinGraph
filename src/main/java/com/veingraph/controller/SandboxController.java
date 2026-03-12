package com.veingraph.controller;

import com.veingraph.common.result.Result;
import com.veingraph.document.service.DocumentChunkingService;
import com.veingraph.document.service.DocumentParserService;
import com.veingraph.llm.model.ExtractionResult;
import com.veingraph.llm.service.EntityExtractionService;
import com.veingraph.llm.service.LlmChatService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
 * Phase 2 测试沙箱控制器
 */
@Tag(name = "沙箱测试", description = "文档解析、LLM 对话、实体抽取的实验性验证接口")
@RestController
@RequestMapping("/sandbox")
@RequiredArgsConstructor
public class SandboxController {

    private final DocumentParserService documentParserService;
    private final DocumentChunkingService documentChunkingService;
    private final LlmChatService llmChatService;
    private final EntityExtractionService entityExtractionService;

    @Operation(summary = "大模型基础闲聊", description = "发送一条消息给智谱 GLM，测试基础通信是否正常")
    @GetMapping("/chat")
    public Result<String> testLlmChat(
            @Parameter(description = "用户消息")
            @RequestParam(defaultValue = "你好，请自我介绍并说明你背后的模型是什么？") String msg) {
        return Result.ok(llmChatService.chat(msg));
    }

    @Operation(summary = "文本切块测试", description = "使用 LangChain4j RecursiveCharacterTextSplitter 对 Mock 长文进行切块")
    @GetMapping("/chunking")
    public Result<Map<String, Object>> testDocumentChunking() {
        String mockLongText = "这是第一段极其冗长的测试文本。".repeat(20)
                + "\n\n\n接下来进入第二章的测试内容。".repeat(20)
                + "\n\n这段文字用于测试LangChain4j引擎切分时的Overlap重叠机制是否正常运作。";

        ByteArrayInputStream is = new ByteArrayInputStream(mockLongText.getBytes(StandardCharsets.UTF_8));
        Document document = documentParserService.parse(is);
        List<TextSegment> segments = documentChunkingService.split(document);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("originalLength", mockLongText.length());
        result.put("chunkCount", segments.size());
        result.put("chunks", segments.stream().map(TextSegment::text).toList());

        return Result.ok(result);
    }

    @Operation(summary = "实体关系抽取测试", description = "将文本送入 LLM 进行结构化实体和关系提取")
    @GetMapping("/extract")
    public Result<ExtractionResult> testEntityExtraction(
            @Parameter(description = "待抽取的文本内容")
            @RequestParam(defaultValue = "王局长昨天在会议室会见了李主任，两人讨论了部门预算事宜。") String text) {
        return Result.ok(entityExtractionService.extract(text));
    }
}
