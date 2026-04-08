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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * GraphRAG 编排服务
 * 并发执行 Neo4j 图查询 + ES 关键词检索 + Milvus 向量检索，融合上下文后调用 LLM 生成回答
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphRagService {

    private final ChatClient chatClient;
    private final Text2CypherService text2CypherService;
    private final EsHybridSearchService esHybridSearchService;
    private final MilvusVectorSearchService milvusVectorSearchService;
    private final ChatHistoryService chatHistoryService;

    @Value("classpath:prompts/graphrag-system.st")
    private Resource systemPromptResource;

    /** ES 检索返回 Top-K */
    @Value("${veingraph.rag.es-top-k:5}")
    private int esTopK;

    /** Milvus 向量检索返回 Top-K */
    @Value("${veingraph.rag.milvus-top-k:5}")
    private int milvusTopK;

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
        long totalStart = System.currentTimeMillis();

        // 保存用户消息
        chatHistoryService.saveMessage(sessionId, ChatMessage.ROLE_USER, question);

        // 构建 Super Prompt
        long promptStart = System.currentTimeMillis();
        String systemPrompt = buildSuperPrompt(sessionId, documentId, question);
        log.info("[耗时统计] 构造 SuperPrompt (多路并发等待结束) 耗时: {} ms", System.currentTimeMillis() - promptStart);

        // 流式调用 LLM
        Flux<String> stream = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .stream()
                .content();

        // 收集完整回答并保存
        StringBuilder fullAnswer = new StringBuilder();
        long[] firstTokenTime = new long[1];
        firstTokenTime[0] = 0;

        return stream.doOnNext(chunk -> {
                    if (firstTokenTime[0] == 0) {
                        firstTokenTime[0] = System.currentTimeMillis();
                        log.info("[耗时统计] LLM 首字出块耗时 (自进入接口起): {} ms", firstTokenTime[0] - totalStart);
                    }
                    fullAnswer.append(chunk);
                })
                .doOnComplete(() -> {
                    chatHistoryService.saveMessage(
                            sessionId, ChatMessage.ROLE_ASSISTANT, fullAnswer.toString());
                    log.info("[耗时统计] 流式问答全部回传完毕总耗时: {} ms, 最终回答长度={}", System.currentTimeMillis() - totalStart, fullAnswer.length());
                });
    }

    /**
     * 构建 Super Prompt：并发执行多路召回并融合
     */
    private String buildSuperPrompt(String sessionId, String documentId, String question) {
        // 并发执行四路召回
        CompletableFuture<String> graphFuture = CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            String res = text2CypherService.queryCypher(documentId, question);
            log.info("[耗时统计] Text2Cypher及Neo4j图谱查询 子任务耗时: {} ms", System.currentTimeMillis() - start);
            return res;
        }, executor);

        CompletableFuture<List<String>> esFuture = CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            List<String> res = esHybridSearchService.keywordSearch(documentId, question, esTopK);
            log.info("[耗时统计] ES关键词检索 子任务耗时: {} ms", System.currentTimeMillis() - start);
            return res;
        }, executor);

        CompletableFuture<List<String>> milvusFuture = CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            List<String> res = milvusVectorSearchService.vectorSearch(documentId, question, milvusTopK);
            log.info("[耗时统计] Milvus向量检索 子任务耗时: {} ms", System.currentTimeMillis() - start);
            return res;
        }, executor);

        CompletableFuture<String> historyFuture = CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            String res = chatHistoryService.getContextHistory(sessionId);
            log.info("[耗时统计] Redis/MongoDB历史上下文加载 子任务耗时: {} ms", System.currentTimeMillis() - start);
            return res;
        }, executor);

        // Barrier 汇总
        String graphContext;
        String searchContext;
        String chatHistory;
        try {
            graphContext = graphFuture.join();

            // 合并 ES 关键词 + Milvus 向量结果（去重）
            List<String> esResults = esFuture.join();
            List<String> milvusResults = milvusFuture.join();
            Set<String> merged = new LinkedHashSet<>(esResults);
            merged.addAll(milvusResults);

            searchContext = merged.isEmpty() ? "无相关结果" : String.join("\n---\n", merged);
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
