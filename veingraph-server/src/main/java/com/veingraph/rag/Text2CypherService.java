package com.veingraph.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Text2Cypher 服务
 * 将自然语言转化为 Cypher 查询语句，执行并返回结果
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Text2CypherService {

    private final ChatClient chatClient;
    private final Driver neo4jDriver;

    @Value("classpath:prompts/text2cypher-system.st")
    private Resource systemPromptResource;

    /**
     * 将用户问题转换为 Cypher 并执行，返回可读的文本摘要
     *
     * @param documentId 限定的文档 ID (可选)
     * @param question   用户自然语言问题
     * @return Neo4j 查询结果的文本摘要，查询失败或无结果时返回空字符串
     */
    public String queryCypher(String documentId, String question) {
        try {
            // 1. LLM 生成 Cypher
            String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
            if (documentId != null && !documentId.isBlank()) {
                systemPrompt += "\n\nCRITICAL INSTRUCTION: You MUST filter the relationship by the specific document ID given. " 
                             + "Always append `WHERE r.documentId = '" + documentId + "'` to your MATCH clause. "
                             + "Do NOT try to match the filename using CONTAINS.";
            }

            String cypher = chatClient.prompt()
                    .system(systemPrompt)
                    .user(question)
                    .call()
                    .content();

            if (cypher == null || cypher.isBlank()) {
                log.info("Text2Cypher: 无法将问题映射为 Cypher 查询");
                return "";
            }

            // 清理可能的 markdown 代码块标记
            cypher = cypher.replaceAll("```cypher\\s*", "").replaceAll("```\\s*", "").trim();
            log.info("Text2Cypher 生成 Cypher:\n{}", cypher);

            // 2. 执行 Cypher
            try (Session session = neo4jDriver.session(SessionConfig.defaultConfig())) {
                List<Record> records = session.run(cypher).list();
                if (records.isEmpty()) {
                    log.info("Text2Cypher: Cypher 执行成功但结果为空");
                    return "";
                }

                // 3. 格式化结果为可读文本
                String result = records.stream()
                        .map(record -> record.values().stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(" | ")))
                        .collect(Collectors.joining("\n"));

                log.info("Text2Cypher: 查询返回 {} 条结果", records.size());
                return result;
            }
        } catch (Exception e) {
            log.error("Text2Cypher 执行失败: {}", e.getMessage());
            return "";
        }
    }
}
