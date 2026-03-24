package com.veingraph.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * LLM 抽取结果记录
 */
@Data
@Document("extraction_record")
@Schema(description = "实体关系抽取记录")
public class ExtractionRecord {

    @Id
    @Schema(description = "记录 ID")
    private String id;

    @Indexed
    @Schema(description = "关联的文档 ID")
    private String documentId;

    @Indexed
    @Schema(description = "关联的文本块 ID")
    private String chunkId;

    @Schema(description = "源实体", example = "王局长")
    private String source;

    @Schema(description = "目标实体", example = "李主任")
    private String target;

    @Schema(description = "关系类型", example = "会见")
    private String relation;

    @Schema(description = "原文依据", example = "王局长昨天在会议室会见了李主任")
    private String evidence;

    @Indexed
    @Schema(description = "同步状态: UNSYNCED / SYNCED / FAILED", example = "UNSYNCED")
    private String syncStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    public static final String SYNC_UNSYNCED = "UNSYNCED";
    public static final String SYNC_SYNCED = "SYNCED";
    public static final String SYNC_FAILED = "FAILED";
}
