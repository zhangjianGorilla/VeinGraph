package com.veingraph.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka 配置类
 * Kafka 作为异步解耦层：
 * - 前端上传长文后 LangChain4j 切分好的 Chunk 推入 Topic
 * - 后端消费者按批次拉取并调用 Spring AI Alibaba 进行抽取
 */
@Configuration
public class KafkaConfig {

    /**
     * 核心提取 Topic: 接收文档切块后的文本
     * 后端 Kafka Consumer 异步消费并调用 LLM 进行实体/关系抽取
     */
    @Bean
    public NewTopic docChunkExtractTopic() {
        return TopicBuilder.name("doc-chunk-extract-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
