package com.veingraph.llm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 结构化关系提取记录模型
 */
@Data
@Schema(description = "LLM 输出的单条关系记录")
public class RelationRecord {

    @JsonProperty(value = "source", required = true)
    @Schema(description = "源实体", example = "王局长")
    private String source;

    @JsonProperty(value = "target", required = true)
    @Schema(description = "目标实体", example = "李主任")
    private String target;

    @JsonProperty(value = "relation", required = true)
    @Schema(description = "关系类型", example = "会见")
    private String relation;

    @JsonProperty(value = "evidence", required = true)
    @Schema(description = "原文依据", example = "王局长昨天在会议室会见了李主任")
    private String evidence;
}
