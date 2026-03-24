package com.veingraph.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 图谱数据响应
 */
@Schema(description = "图谱数据")
public record GraphDataVO(
        @Schema(description = "节点列表")
        List<Node> nodes,
        @Schema(description = "边列表")
        List<Edge> edges
) {
    @Schema(description = "图谱节点")
    public record Node(
            @Schema(description = "节点ID")
            String id,
            @Schema(description = "节点标签")
            String label,
            @Schema(description = "节点类型")
            String type,
            @Schema(description = "节点属性")
            java.util.Map<String, Object> properties
    ) {}

    @Schema(description = "图谱边")
    public record Edge(
            @Schema(description = "起始节点ID")
            String from,
            @Schema(description = "目标节点ID")
            String to,
            @Schema(description = "关系类型")
            String label,
            @Schema(description = "边属性")
            java.util.Map<String, Object> properties
    ) {}
}