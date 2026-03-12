package com.veingraph.config;

import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/**
 * Neo4j 配置类
 * Neo4j 作为"关系大脑"，存储实体 (Person 等) 和边 (关系)
 * 使用 Spring Data Neo4j (SDN) 进行图对象的 OGM 映射
 *
 */
 @org.springframework.context.annotation.Configuration
 @EnableNeo4jRepositories(basePackages = "com.veingraph.repository.neo4j")
public class Neo4jConfig {

    /**
     * 配置 Cypher DSL 使用 Neo4j 5 方言
     */
     @Bean
     public Configuration cypherDslConfiguration() {
         return Configuration.newConfig()
                 .withDialect(Dialect.NEO4J_5)
                 .build();
     }
}
