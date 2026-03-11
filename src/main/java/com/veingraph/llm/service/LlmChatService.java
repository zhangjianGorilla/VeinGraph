package com.veingraph.llm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 基础 LLM 对话服务
 * 职责：封装基于 Spring AI Alibaba 的远程大模型 (DashScope) 网络通信
 */
@Slf4j
@Service
public class LlmChatService {

    private final ChatClient chatClient;

    public LlmChatService(ChatClient.Builder builder) {
        // 使用 Spring AI 提供的 Builder 构建 ChatClient 实例
        this.chatClient = builder.build();
    }

    /**
     * 简单的非流式问答测试接口
     * @param message 用户提问的消息
     * @return 大模型的回答文本
     */
    public String chat(String message) {
        log.info("调用 Spring AI Alibaba (DashScope) 请求模型响应...");
        return this.chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
    
    /**
     * 系统级指令下的多轮对话测试
     */
    public String systemChat(String systemPrompt, String userMessage) {
        log.info("携带 SystemPrompt 发起模型调用...");
        return this.chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }
}
