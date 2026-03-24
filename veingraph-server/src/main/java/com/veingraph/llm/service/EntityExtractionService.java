package com.veingraph.llm.service;

import com.veingraph.llm.model.ExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 实体和关系抽取服务
 * 职责：结合 Prompt 工程，将文本块送入 LLM 并输出结构化的 ExtractionResult
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityExtractionService {

    private final ChatClient chatClient;

    /** System Prompt 模板 (从 classpath 加载，修改无需重编译) */
    @Value("classpath:prompts/extraction-system.st")
    private Resource systemPromptResource;

    /** User Prompt 模板 */
    @Value("classpath:prompts/extraction-user.st")
    private Resource userPromptResource;

    /** 强类型 JSON 输出转换器 */
    private final BeanOutputConverter<ExtractionResult> outputConverter =
            new BeanOutputConverter<>(ExtractionResult.class);

    /**
     * 核心抽取方法：将纯文本块送入 LLM 进行实体和关系的结构化提取
     *
     * @param documentChunk 被切分好的单一文本子块
     * @return 强类型提取结果
     */
    public ExtractionResult extract(String documentChunk) {
        log.info("开始抽取实体与关系，文本长度: {}", documentChunk.length());

        // 渲染 System Prompt（注入 JSON Schema 格式约束）
        PromptTemplate systemTemplate = new PromptTemplate(systemPromptResource);
        String systemMsg = systemTemplate.render(Map.of("format", outputConverter.getFormat()));

        // 渲染 User Prompt（注入待分析文本）
        PromptTemplate userTemplate = new PromptTemplate(userPromptResource);
        String userMsg = userTemplate.render(Map.of("chunk", documentChunk));

        log.debug("System Prompt:\n{}", systemMsg);

        try {
            // 调用大模型
            String responseContent = chatClient.prompt()
                    .system(systemMsg)
                    .user(userMsg)
                    .call()
                    .content();

            // 框架层反序列化 JSON → 强类型 Bean
            return outputConverter.convert(responseContent);
        } catch (Exception e) {
            log.error("实体抽取失败: ", e);
            throw new RuntimeException("实体抽取失败: " + e.getMessage(), e);
        }
    }
}
