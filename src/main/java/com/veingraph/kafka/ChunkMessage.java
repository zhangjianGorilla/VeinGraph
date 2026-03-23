package com.veingraph.kafka;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka 消息体：文本块抽取任务
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kafka 文本块抽取消息")
public class ChunkMessage {

    @Schema(description = "文档 ID")
    private String documentId;

    @Schema(description = "文本块 ID")
    private String chunkId;

    @Schema(description = "文本块内容")
    private String text;

    /** Kafka Topic 名称常量 */
    public static final String TOPIC = "doc-chunk-extract";
}
