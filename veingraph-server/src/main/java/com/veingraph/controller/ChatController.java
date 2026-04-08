package com.veingraph.controller;

import com.veingraph.agent.AgentChatService;
import com.veingraph.chat.ChatHistoryService;
import com.veingraph.common.result.Result;
import com.veingraph.model.ChatMessage;
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
 * Phase 2：在线智能问答控制器
 *
 * <p>所有问答请求均由 {@link AgentChatService}（ReactAgent）处理。
 * Agent 持有 BookTextSearchTool 和 CharacterGraphQueryTool，
 * 根据用户问题自主规划检索路径后生成回答。
 */
@Tag(name = "智能问答", description = "基于 ReactAgent 的 GraphRAG 多工具问答引擎")
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AgentChatService agentChatService;
    private final ChatHistoryService chatHistoryService;

    /**
     * SSE 流式问答（推荐）
     */
    @Operation(summary = "流式问答",
            description = "ReactAgent 自主规划工具调用，完成检索后以 SSE 流式输出回答")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @Parameter(description = "会话 ID，不传则自动创建")
            @RequestParam(required = false) String sessionId,
            @Parameter(description = "限定范围的文档 ID，不传则全局检索")
            @RequestParam(required = false) String documentId,
            @Parameter(description = "用户问题")
            @RequestParam String question) {

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        log.info("[ChatController] 流式问答: sessionId={}, documentId={}, question={}", sessionId, documentId, question);
        return agentChatService.askStream(sessionId, documentId, question);
    }

    /**
     * 非流式问答
     */
    @Operation(summary = "同步问答",
            description = "ReactAgent 完成所有工具调用后一次性返回完整回答")
    @GetMapping("/ask")
    public Result<String> ask(
            @Parameter(description = "会话 ID，不传则自动创建")
            @RequestParam(required = false) String sessionId,
            @Parameter(description = "限定范围的文档 ID，不传则全局检索")
            @RequestParam(required = false) String documentId,
            @Parameter(description = "用户问题")
            @RequestParam String question) {

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        log.info("[ChatController] 同步问答: sessionId={}, documentId={}, question={}", sessionId, documentId, question);
        return Result.ok(agentChatService.ask(sessionId, documentId, question));
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
