package com.veingraph.agent;

import com.veingraph.agent.tool.BookTextSearchTool;
import com.veingraph.agent.tool.CharacterGraphQueryTool;
import com.veingraph.chat.ChatHistoryService;
import com.veingraph.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Phase 2：在线智能问答 —— ReactAgent 服务
 *
 * <p>使用阿里云百炼 DashScope 模型（工具调用能力）驱动 ReAct 循环：
 * <ol>
 *   <li>Agent 分析用户问题，自主决定调用哪些工具、以何种顺序调用。</li>
 *   <li>{@link BookTextSearchTool}：混合检索书籍原文（Milvus 向量 + ES 关键词）。</li>
 *   <li>{@link CharacterGraphQueryTool}：查询 Neo4j 人物关系图谱。</li>
 *   <li>Agent 综合所有工具返回结果，生成最终人类友好的回答。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentChatService {

    private final ChatClient chatClient;

    /**
     * 同步问答：Agent 完成所有工具调用后一次性返回完整回答。
     */
    public String ask(String sessionId, String documentId, String question) {
        chatHistoryService.saveMessage(sessionId, ChatMessage.ROLE_USER, question);

        try {
            String answer = chatClient.prompt()
                    .system(buildSystemPrompt(sessionId, documentId))
                    .user(question)
                    .tools(bookTextSearchTool, characterGraphQueryTool)
                    .call()
                    .content();

            chatHistoryService.saveMessage(sessionId, ChatMessage.ROLE_ASSISTANT, answer);
            return answer;
        } catch (Exception e) {
            log.error("[AgentChatService] ReactAgent 问答失败: {}", e.getMessage());
            throw new RuntimeException("ReactAgent 问答失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式问答：Agent 工具调用完成后，将最终答案以 SSE 流式输出。
     */
    public Flux<String> askStream(String sessionId, String documentId, String question) {
        chatHistoryService.saveMessage(sessionId, ChatMessage.ROLE_USER, question);

        StringBuilder fullAnswer = new StringBuilder();

        return chatClient.prompt()
                .system(buildSystemPrompt(sessionId, documentId))
                .user(question)
                .tools(bookTextSearchTool, characterGraphQueryTool)
                .stream()
                .content()
                .doOnNext(fullAnswer::append)
                .doOnComplete(() ->
                        chatHistoryService.saveMessage(
                                sessionId, ChatMessage.ROLE_ASSISTANT, fullAnswer.toString()))
                .doOnError(e -> log.error("[AgentChatService] 流式问答失败: {}", e.getMessage()));
    }

    /**
     * 构建 Agent 系统提示词，注入文档范围约束和历史对话上下文。
     */
    private String buildSystemPrompt(String sessionId, String documentId) {
        String docInstruction = (documentId != null && !documentId.isBlank())
                ? "用户的问题限定在文档ID为 [" + documentId + "] 的书籍范围内。"
                + "调用工具时，documentId 参数必须传入该值。"
                : "用户的问题不限定特定文档，调用工具时 documentId 参数传空字符串。";

        String chatHistory = chatHistoryService.getContextHistory(sessionId);
        String historySection = (chatHistory == null || chatHistory.isBlank())
                ? "" : "\n\n## 历史对话上下文\n" + chatHistory;

        return """
                你是一个专业的书籍知识问答助手，擅长解答关于书籍内容和人物关系的问题。

                ## 可用工具
                - **searchBookContent**：混合检索书籍原文（向量语义 + 关键词）。
                  适用场景：查找原文描述、人物台词、情节细节、场景描写等。
                - **queryCharacterRelations**：查询人物关系图谱（Neo4j 知识图谱）。
                  适用场景：梳理人物关系网络、查询两人之间的关联、找某人的师傅/徒弟/同伴等。

                ## 工作策略
                1. 仔细分析用户问题，判断需要调用哪些工具（可多次调用、组合调用）。
                2. 对于复杂问题（如"张三的师傅在光明顶说了什么"），先调用图谱工具查人物关系，
                   再用原文工具检索具体情节——分步推理，逐步获取答案。
                3. 综合所有工具返回的信息，生成准确、有条理的最终回答。
                4. 若工具返回结果不足，可再次调用工具补充检索，直到信息充分。
                5. 回答使用中文，语言流畅自然，引用原文时注明来源。

                """ + docInstruction + historySection;
    }
}
