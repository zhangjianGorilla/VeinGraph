package com.veingraph.rag;

import com.veingraph.chat.ChatHistoryService;
import com.veingraph.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * GraphRAG 编排服务
 * 并发执行 Neo4j 图查询 + ES 语义检索，融合上下文后调用 LLM 生成回答
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphRagService {

    private final ChatClient chatClient;
    private final Text2CypherService text2CypherService;
    private final EsHybridSearchService esHybridSearchService;
    private final ChatHistoryService chatHistoryService;

    @Value("classpath:prompts/graphrag-system.st")
    private Resource systemPromptResource;

    /** ES 检索返回 Top-K */
    @Value("${veingraph.rag.es-top-k:5}")
    private int esTopK;

    /** 并发召回执行器 */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 非流式问答：并发召回 → 融合 → LLM 一次性生成
     */
    public String ask(String sessionId, String documentId, String question) {
        // 保存用户消息
        chatHistoryService.saveMessage(sessionId, ChatMessage.ROLE_USER, question);

        // 构建 Super Prompt
        String systemPrompt = buildSuperPrompt(sessionId, documentId, question);

        // 调用 LLM 生成
        try {
            String answer = chatClient.prompt()
                    .system(systemPrompt)
                    .user(question)
                    .call()
                    .content();

            // 保存助手消息
            chatHistoryService.saveMessage(sessionId, ChatMessage.ROLE_ASSISTANT, answer);
            return answer;
        } catch (Exception e) {
            log.error("GraphRAG 问答失败: {}", e.getMessage());
            throw new RuntimeException("GraphRAG 问答失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式问答：并发召回 → 融合 → LLM SSE 流式输出
     */
    public Flux<String> askStream(String sessionId, String documentId, String question) {
        // 保存用户消息
        chatHistoryService.saveMessage(sessionId, ChatMessage.ROLE_USER, question);

        // 构建 Super Prompt
        String systemPrompt = buildSuperPrompt(sessionId, documentId, question);

        // 流式调用 LLM
        Flux<String> stream = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .stream()
                .content();

        // 收集完整回答并保存
        StringBuilder fullAnswer = new StringBuilder();
        return stream.doOnNext(fullAnswer::append)
                .doOnComplete(() -> {
                    chatHistoryService.saveMessage(
                            sessionId, ChatMessage.ROLE_ASSISTANT, fullAnswer.toString());
                    log.info("流式问答完成, sessionId={}, 回答长度={}", sessionId, fullAnswer.length());
                });
    }

    /**
     * 构建 Super Prompt：并发执行双路召回并融合
     */
    private String buildSuperPrompt(String sessionId, String documentId, String question) {
        // 并发执行两路召回
        CompletableFuture<String> graphFuture = CompletableFuture.supplyAsync(
                () -> text2CypherService.queryCypher(documentId, question), executor);

        CompletableFuture<List<String>> esFuture = CompletableFuture.supplyAsync(
                () -> esHybridSearchService.keywordSearch(documentId, question, esTopK), executor);

        CompletableFuture<String> historyFuture = CompletableFuture.supplyAsync(
                () -> chatHistoryService.getContextHistory(sessionId), executor);

        // Barrier 汇总
        String graphContext;
        String searchContext;
        String chatHistory;
        try {
            graphContext = graphFuture.join();
            List<String> esResults = esFuture.join();
            searchContext = esResults.isEmpty() ? "无相关结果" : String.join("\n---\n", esResults);
            chatHistory = historyFuture.join();
        } catch (Exception e) {
            log.warn("部分召回通道失败，使用降级数据: {}", e.getMessage());
            graphContext = "";
            searchContext = "无相关结果";
            chatHistory = "";
        }

        // 渲染 Super Prompt
        try {
            String template = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
            return template
                    .replace("{graphContext}", graphContext.isEmpty() ? "无相关图谱数据" : graphContext)
                    .replace("{searchContext}", searchContext)
                    .replace("{chatHistory}", chatHistory.isEmpty() ? "无历史对话" : chatHistory);
        } catch (Exception e) {
            log.error("加载 GraphRAG Prompt 模板失败: {}", e.getMessage());
            return "请根据你的知识回答用户问题。";
        }
    }
}
