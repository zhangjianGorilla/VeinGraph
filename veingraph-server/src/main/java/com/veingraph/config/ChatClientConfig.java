package com.veingraph.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 全局 ChatClient 配置
 * 通过统一的 Bean 管理 ChatClient 实例，避免各 Service 重复构建
 */
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        // 设置连接超时为 30 秒，读取超时（等待大模型推理响应）为 120 秒
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(30))
                .withReadTimeout(Duration.ofSeconds(6000000));

        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings));
    }
}
