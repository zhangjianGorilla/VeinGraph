package com.veingraph.llm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 基础 LLM 对话服务
 * 职责：封装基于 Spring AI (智谱 ZhiPu GLM) 的远程大模型网络通信
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmChatService {

    private final ChatClient chatClient;

    /**
     * 简单的非流式问答
     *
     * @param message 用户提问消息
     * @return 大模型回答文本
     */
    public String chat(String message) {
        log.info("调用智谱 GLM 模型, 消息: {}", message);
        try {
            String content = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();
            log.info("模型响应成功, 长度: {}", content != null ? content.length() : 0);
            return content;
        } catch (Exception e) {
            log.error("LLM 调用失败，请检查 API Key、模型名称和网络连接: ", e);
            throw new RuntimeException("大模型调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 携带系统角色指令的对话
     *
     * @param systemPrompt 系统级角色设定
     * @param userMessage  用户输入
     * @return 大模型回答文本
     */
    public String systemChat(String systemPrompt, String userMessage) {
        log.info("携带 SystemPrompt 发起模型调用...");
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("LLM 调用失败: ", e);
            throw new RuntimeException("大模型调用失败: " + e.getMessage(), e);
        }
    }
}
