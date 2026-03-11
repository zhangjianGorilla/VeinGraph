package com.veingraph.llm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 结构化关系提取记录模型
 * 用于约束大模型输出的 JSON Schema 模板
 */
@Data
public class RelationRecord {
    
    @JsonProperty(value = "source", required = true)
    private String source;
    
    @JsonProperty(value = "target", required = true)
    private String target;
    
    @JsonProperty(value = "relation", required = true)
    private String relation;
    
    @JsonProperty(value = "evidence", required = true)
    private String evidence;
}
