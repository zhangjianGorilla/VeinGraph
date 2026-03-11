package com.veingraph.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch 配置类
 * ES >= 8.x 作为"语义与防幻觉引擎"：
 * - 存储文本块原始文字和稠密向量 (Dense Vector)
 * - 提供 Hybrid Search (关键词 + 向量相似度混合检索)
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.veingraph.repository.es")
public class ElasticsearchConfig {
    // Spring Data Elasticsearch 通过 application.yml 自动配置连接
    // 向量字段 (dense_vector) 的映射在对应的 @Document 实体上通过注解定义
}
