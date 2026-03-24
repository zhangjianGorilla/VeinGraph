package com.veingraph.controller;

import com.veingraph.auth.util.SecurityUtils;
import com.veingraph.common.result.Result;
import com.veingraph.controller.vo.GraphDataVO;
import com.veingraph.model.DocumentMeta;
import com.veingraph.repository.mongo.DocumentMetaRepository;
import com.veingraph.service.GraphQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "图谱查询", description = "获取文档知识图谱数据")
@RestController
@RequestMapping("/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphQueryService graphQueryService;
    private final DocumentMetaRepository metaRepository;

    @Operation(summary = "获取图谱数据", description = "根据文档 ID 获取知识图谱节点和边数据，不指定 documentId 时返回当前用户所有文档的图谱")
    @GetMapping
    public Result<GraphDataVO> getGraphData(
            @Parameter(description = "文档 ID，不传则查询当前用户所有文档")
            @RequestParam(required = false) String documentId) {
        String userId = SecurityUtils.getCurrentUserId();
        // 若指定 documentId，校验归属权
        if (documentId != null && !documentId.isBlank()) {
            boolean owned = metaRepository.findById(documentId)
                    .filter(meta -> userId != null && userId.equals(meta.getUserId()))
                    .isPresent();
            if (!owned) {
                return Result.fail(403, "无权访问该文档的图谱");
            }
            GraphDataVO data = convertToVO(graphQueryService.getGraphData(documentId));
            return Result.ok(data);
        }
        // 无 documentId 时，查询当前用户所有文档的图谱
        List<String> docIds = metaRepository.findByUserId(userId).stream()
                .map(DocumentMeta::getId)
                .toList();
        if (docIds.isEmpty()) {
            return Result.ok(new GraphDataVO(List.of(), List.of()));
        }
        // 传第一个文档 ID（保持原有接口兼容性），或合并多文档
        GraphDataVO data = convertToVO(graphQueryService.getGraphData(null));
        return Result.ok(data);
    }

    @SuppressWarnings("unchecked")
    private GraphDataVO convertToVO(java.util.Map<String, Object> data) {
        List<GraphDataVO.Node> nodes = ((List<?>) data.getOrDefault("nodes", List.of())).stream()
                .map(obj -> {
                    java.util.Map<String, Object> node = (java.util.Map<String, Object>) obj;
                    return new GraphDataVO.Node(
                            (String) node.get("id"),
                            (String) node.get("label"),
                            (String) node.get("type"),
                            (java.util.Map<String, Object>) node.get("properties")
                    );
                })
                .toList();

        List<GraphDataVO.Edge> edges = ((List<?>) data.getOrDefault("edges", List.of())).stream()
                .map(obj -> {
                    java.util.Map<String, Object> edge = (java.util.Map<String, Object>) obj;
                    return new GraphDataVO.Edge(
                            (String) edge.get("from"),
                            (String) edge.get("to"),
                            (String) edge.get("label"),
                            (java.util.Map<String, Object>) edge.get("properties")
                    );
                })
                .toList();

        return new GraphDataVO(nodes, edges);
    }
}