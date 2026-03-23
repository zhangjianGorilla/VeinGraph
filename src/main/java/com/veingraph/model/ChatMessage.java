package com.veingraph.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 对话消息记录
 * 按 sessionId 分组管理多轮对话
 */
@Data
@Document("chat_message")
@Schema(description = "对话消息")
public class ChatMessage {

    @Id
    @Schema(description = "消息 ID")
    private String id;

    @Indexed
    @Schema(description = "会话 ID")
    private String sessionId;

    @Schema(description = "角色: user / assistant", example = "user")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
}
