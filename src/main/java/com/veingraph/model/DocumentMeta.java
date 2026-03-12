package com.veingraph.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 文档元信息
 */
@Data
@Document("document_meta")
@Schema(description = "文档元信息")
public class DocumentMeta {

    @Id
    @Schema(description = "文档 ID", example = "665a1b2c3d4e5f6789012345")
    private String id;

    @Schema(description = "原始文件名", example = "合同.pdf")
    private String fileName;

    @Schema(description = "MIME 类型", example = "application/pdf")
    private String contentType;

    @Schema(description = "文件大小 (bytes)", example = "102400")
    private long fileSize;

    @Schema(description = "切块总数", example = "5")
    private int totalChunks;

    @Schema(description = "处理状态: PENDING / EXTRACTING / COMPLETED / PARTIAL_FAILED", example = "COMPLETED")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_EXTRACTING = "EXTRACTING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_PARTIAL_FAILED = "PARTIAL_FAILED";
}
