package com.veingraph.resolution;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * LLM 实体消歧确认结果
 */
@Data
public class ResolutionResult {

    @JsonProperty("isSame")
    private boolean isSame;

    @JsonProperty("canonicalName")
    private String canonicalName;

    @JsonProperty("confidence")
    private double confidence;
}
