package com.veingraph.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB 配置类
 * MongoDB 同时承担业务数据库和数据湖的角色：
 * - 业务数据：用户体系、权限、任务流水
 * - 数据湖：原始长文、LLM 原始 JSON 结果、运行日志
 */
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.veingraph.repository.mongo")
public class MongoConfig {
    // Spring Data MongoDB 通过 application.yml 自动配置
    // 此处开启审计功能用于自动填充 createdAt / updatedAt
}
