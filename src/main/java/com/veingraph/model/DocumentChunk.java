package com.veingraph.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 切分后的文本块
 */
@Data
@Document("document_chunk")
@Schema(description = "文档文本块")
public class DocumentChunk {

    @Id
    @Schema(description = "块 ID")
    private String id;

    @Indexed
    @Schema(description = "关联的文档 ID")
    private String documentId;

    @Schema(description = "块序号 (0-based)", example = "0")
    private int chunkIndex;

    @Schema(description = "纯文本内容")
    private String text;

    @Indexed
    @Schema(description = "是否已向量化并同步至 ES")
    private boolean vectorized;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
