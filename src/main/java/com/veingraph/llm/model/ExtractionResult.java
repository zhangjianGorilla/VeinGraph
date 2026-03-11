package com.veingraph.llm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 完整结构化抽取结果反馈对象
 * LLM 最终需按照这个格式被强制转化
 */
@Data
public class ExtractionResult {
    
    @JsonProperty("records")
    private List<RelationRecord> records;
}
