package com.veingraph.llm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 完整结构化抽取结果
 */
@Data
@Schema(description = "LLM 结构化抽取结果")
public class ExtractionResult {

    @JsonProperty("records")
    @Schema(description = "关系记录列表")
    private List<RelationRecord> records;
}
