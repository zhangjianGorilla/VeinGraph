package com.veingraph.controller;

import com.veingraph.common.result.Result;
import com.veingraph.service.GraphQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphQueryService graphQueryService;

    @GetMapping
    public Result<Map<String, Object>> getGraphData(
            @RequestParam(required = false) String documentId) {
        Map<String, Object> data = graphQueryService.getGraphData(documentId);
        return Result.ok(data);
    }
}
