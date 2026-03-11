package com.veingraph.llm.service;

import com.veingraph.llm.model.ExtractionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 实体和关系抽取服务
 * 职责：结合 Prompt 工程，强制模型解析段落并输出结构化的 ExtractionResult 对象
 */
@Slf4j
@Service
public class EntityExtractionService {

    private final ChatClient chatClient;
    
    // 用于将大模型返回的 JSON 解析成强类型 Bean
    private final BeanOutputConverter<ExtractionResult> outputConverter;

    public EntityExtractionService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
        this.outputConverter = new BeanOutputConverter<>(ExtractionResult.class);
    }

    /**
     * 核心抽取方法：将纯文本块送入 LLM 进行实体和关系的 JSON 结构化提取
     * @param documentChunk 被切分好的单一文本子块
     * @return 强类型提取结果
     */
    public ExtractionResult extract(String documentChunk) {
        log.info("开始抽取实体与关系，文本长度: {}", documentChunk.length());

        // 使用 Few-Shot 和系统指令设定工作模式
        String systemText = """
                你是一个严谨的结构化知识抽取专家。你的任务是从文本中抽取实体及其关系。
                核心规则：
                1. 绝对不要捏造文本中未提及的人物或关系。
                2. 先识别核心实体，再分析他们的行为产生什么关系。
                3. 如果代词（如“他”、“局长”）明显指代上文提及的角色，请直接将其替换为全名。
                4. {format}
                """;

        String userText = """
                分析以下文本并提取关系知识。
                文本内容:
                {chunk}
                """;

        // 构造 System Prompt，并注入 JSON Schema 输出约束指导语
        PromptTemplate systemTemplate = new PromptTemplate(systemText);
        String systemMsg = systemTemplate.render(Map.of("format", outputConverter.getFormat()));

        // 构造 User Prompt
        PromptTemplate userTemplate = new PromptTemplate(userText);
        String userMsg = userTemplate.render(Map.of("chunk", documentChunk));

        log.debug("生成 System Prompt: \n{}", systemMsg);

        // 调用大模型
        String responseContent = this.chatClient.prompt()
                .system(systemMsg)
                .user(userMsg)
                // 通过 modelOptions 也可以强制启动百炼 JSON 格式限制，
                // 但这里依靠 OutputConverter 注入指令进行转换也是标准做法
                .call()
                .content();

        // 框架层尝试反序列化 JSON，如果不符合格式会抛出解析异常
        return outputConverter.convert(responseContent);
    }
}
