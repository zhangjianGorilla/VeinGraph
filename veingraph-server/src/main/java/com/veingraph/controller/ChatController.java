package com.veingraph.controller;

import com.veingraph.chat.ChatHistoryService;
import com.veingraph.common.result.Result;
import com.veingraph.model.ChatMessage;
import com.veingraph.rag.GraphRagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

/**
 * GraphRAG 对话控制器
 * 提供 SSE 流式问答和对话历史查询
 */
@Tag(name = "智能问答", description = "基于知识图谱的 GraphRAG 问答引擎")
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final GraphRagService graphRagService;
    private final ChatHistoryService chatHistoryService;

    /**
     * SSE 流式问答（推荐）
     */
    @Operation(summary = "流式问答", description = "基于 GraphRAG 的流式问答，通过 SSE 实时输出回答")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @Parameter(description = "会话 ID，不传则自动创建")
            @RequestParam(required = false) String sessionId,
            @Parameter(description = "指定限定范围的文档 ID")
            @RequestParam(required = false) String documentId,
            @Parameter(description = "用户问题")
            @RequestParam String question) {

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        log.info("流式问答: sessionId={}, documentId={}, question={}", sessionId, documentId, question);
        return graphRagService.askStream(sessionId, documentId, question);
    }

    /**
     * 非流式问答
     */
    @Operation(summary = "同步问答", description = "基于 GraphRAG 的同步问答，一次性返回完整回答")
    @GetMapping("/ask")
    public Result<String> ask(
            @Parameter(description = "会话 ID，不传则自动创建")
            @RequestParam(required = false) String sessionId,
            @Parameter(description = "指定限定范围的文档 ID")
            @RequestParam(required = false) String documentId,
            @Parameter(description = "用户问题")
            @RequestParam String question) {

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        log.info("同步问答: sessionId={}, documentId={}, question={}", sessionId, documentId, question);
        return Result.ok(graphRagService.ask(sessionId, documentId, question));
    }

    /**
     * 查询会话历史
     */
    @Operation(summary = "对话历史", description = "查询指定会话的全部对话记录")
    @GetMapping("/history")
    public Result<List<ChatMessage>> history(
            @Parameter(description = "会话 ID")
            @RequestParam String sessionId) {
        return Result.ok(chatHistoryService.getFullHistory(sessionId));
    }
}
