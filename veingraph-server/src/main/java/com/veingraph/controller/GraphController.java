package com.veingraph.controller;

import com.veingraph.auth.util.SecurityUtils;
import com.veingraph.common.result.Result;
import com.veingraph.model.DocumentMeta;
import com.veingraph.repository.mongo.DocumentMetaRepository;
import com.veingraph.service.GraphQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphQueryService graphQueryService;
    private final DocumentMetaRepository metaRepository;

    @GetMapping
    public Result<Map<String, Object>> getGraphData(
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
            Map<String, Object> data = graphQueryService.getGraphData(documentId);
            return Result.ok(data);
        }
        // 无 documentId 时，查询当前用户所有文档的图谱
        List<String> docIds = metaRepository.findByUserId(userId).stream()
                .map(DocumentMeta::getId)
                .collect(Collectors.toList());
        if (docIds.isEmpty()) {
            return Result.ok(Map.of("nodes", List.of(), "edges", List.of()));
        }
        // 传第一个文档 ID（保持原有接口兼容性），或合并多文档
        Map<String, Object> data = graphQueryService.getGraphData(null);
        return Result.ok(data);
    }
}
