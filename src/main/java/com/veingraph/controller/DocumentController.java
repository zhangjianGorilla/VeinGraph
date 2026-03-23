package com.veingraph.controller;

import com.veingraph.common.result.Result;
import com.veingraph.document.service.DocumentService;
import com.veingraph.model.DocumentMeta;
import com.veingraph.model.ExtractionRecord;
import com.veingraph.repository.mongo.DocumentMetaRepository;
import com.veingraph.repository.mongo.ExtractionRecordRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档管理控制器
 */
@Tag(name = "文档管理", description = "文件上传、文档查询、抽取结果查询")
@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentMetaRepository metaRepository;
    private final ExtractionRecordRepository recordRepository;

    @Operation(summary = "同步上传文件并抽取", description = "上传文档文件，同步执行 Tika 解析 → 切块 → LLM 实体关系抽取全链路（适合小文件）")
    @PostMapping("/upload")
    public Result<DocumentMeta> upload(
            @Parameter(description = "待上传的文档文件 (PDF/Word/TXT 等)")
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.fail("上传文件不能为空");
        }
        log.info("接收文件同步上传: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
        DocumentMeta meta = documentService.uploadAndExtract(file);
        return Result.ok(meta);
    }

    @Operation(summary = "异步上传文件并抽取", description = "上传文档文件，解析并切块后将抽取任务投递至 Kafka，由后台异步处理（推荐）")
    @PostMapping("/upload-async")
    public Result<DocumentMeta> uploadAsync(
            @Parameter(description = "待上传的文档文件 (PDF/Word/TXT 等)")
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.fail("上传文件不能为空");
        }
        log.info("接收文件异步上传: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
        DocumentMeta meta = documentService.uploadAsync(file);
        return Result.ok(meta);
    }

    @Operation(summary = "文档列表", description = "分页查询所有已上传文档，按创建时间倒序")
    @GetMapping
    public Result<List<DocumentMeta>> list(
            @Parameter(description = "页码 (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        Page<DocumentMeta> pageResult = metaRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return Result.ok(pageResult.getContent());
    }

    @Operation(summary = "文档详情", description = "查询指定文档的元信息及抽取统计")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(
            @Parameter(description = "文档 ID") @PathVariable String id) {
        return metaRepository.findById(id)
                .map(meta -> {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("meta", meta);
                    detail.put("extractionCount", recordRepository.countByDocumentId(id));
                    return Result.ok(detail);
                })
                .orElse(Result.fail(404, "文档不存在"));
    }

    @Operation(summary = "抽取结果", description = "查询指定文档的所有实体关系抽取记录")
    @GetMapping("/{id}/records")
    public Result<List<ExtractionRecord>> records(
            @Parameter(description = "文档 ID") @PathVariable String id) {
        return Result.ok(recordRepository.findByDocumentId(id));
    }
}
