package com.veingraph.document.service;

import com.veingraph.llm.model.ExtractionResult;
import com.veingraph.llm.model.RelationRecord;
import com.veingraph.llm.service.EntityExtractionService;
import com.veingraph.model.DocumentChunk;
import com.veingraph.model.DocumentMeta;
import com.veingraph.model.ExtractionRecord;
import com.veingraph.repository.mongo.DocumentChunkRepository;
import com.veingraph.repository.mongo.DocumentMetaRepository;
import com.veingraph.repository.mongo.ExtractionRecordRepository;
import com.veingraph.kafka.ChunkExtractProducer;
import com.veingraph.kafka.ChunkMessage;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档上传全链路编排服务
 * 串联：上传 → Tika 解析 → 切块 → LLM 抽取 → 存 MongoDB
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentParserService parserService;
    private final DocumentChunkingService chunkingService;
    private final EntityExtractionService extractionService;
    private final DocumentMetaRepository metaRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ExtractionRecordRepository recordRepository;
    private final ChunkExtractProducer chunkExtractProducer;

    /**
     * 同步全链路抽取（供小文件测试用）
     */
    public DocumentMeta uploadAndExtract(MultipartFile file, String userId) throws IOException {
        // 1. 保存文档元信息
        DocumentMeta meta = new DocumentMeta();
        meta.setFileName(file.getOriginalFilename());
        meta.setContentType(file.getContentType());
        meta.setFileSize(file.getSize());
        meta.setStatus(DocumentMeta.STATUS_PENDING);
        meta.setUserId(userId);
        meta.setCreatedAt(LocalDateTime.now());
        meta.setUpdatedAt(LocalDateTime.now());
        meta = metaRepository.save(meta);
        log.info("文档元信息已保存: id={}, fileName={}", meta.getId(), meta.getFileName());

        // 2. Tika 解析
        Document document = parserService.parse(file.getInputStream());
        log.info("Tika 解析完成, 文本长度: {}", document.text().length());

        // 3. 切块
        List<TextSegment> segments = chunkingService.split(document);
        log.info("切块完成, 共 {} 块", segments.size());

        // 4. 持久化所有 Chunk
        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(meta.getId());
            chunk.setChunkIndex(i);
            chunk.setText(segments.get(i).text());
            chunk.setCreatedAt(LocalDateTime.now());
            chunks.add(chunk);
        }
        chunks = chunkRepository.saveAll(chunks);

        // 5. 更新 Meta 状态
        meta.setTotalChunks(chunks.size());
        meta.setStatus(DocumentMeta.STATUS_EXTRACTING);
        meta.setUpdatedAt(LocalDateTime.now());
        metaRepository.save(meta);

        // 6. 逐 Chunk 调用 LLM 抽取
        int failedCount = 0;
        for (DocumentChunk chunk : chunks) {
            try {
                ExtractionResult result = extractionService.extract(chunk.getText());
                if (result != null && result.getRecords() != null) {
                    for (RelationRecord record : result.getRecords()) {
                        ExtractionRecord er = new ExtractionRecord();
                        er.setDocumentId(meta.getId());
                        er.setChunkId(chunk.getId());
                        er.setSource(record.getSource());
                        er.setTarget(record.getTarget());
                        er.setRelation(record.getRelation());
                        er.setEvidence(record.getEvidence());
                        er.setSyncStatus(ExtractionRecord.SYNC_UNSYNCED);
                        er.setCreatedAt(LocalDateTime.now());
                        recordRepository.save(er);
                    }
                }
                log.info("Chunk[{}] 抽取成功, 提取 {} 条关系",
                        chunk.getChunkIndex(),
                        result != null && result.getRecords() != null ? result.getRecords().size() : 0);
            } catch (Exception e) {
                failedCount++;
                log.error("Chunk[{}] 抽取失败，跳过: {}", chunk.getChunkIndex(), e.getMessage());
            }
        }

        // 7. 最终更新 Meta 状态
        meta.setStatus(failedCount == 0 ? DocumentMeta.STATUS_COMPLETED : DocumentMeta.STATUS_PARTIAL_FAILED);
        meta.setUpdatedAt(LocalDateTime.now());
        metaRepository.save(meta);

        log.info("文档 [{}] 全链路处理完成, 状态: {}, 失败块数: {}",
                meta.getFileName(), meta.getStatus(), failedCount);
        return meta;
    }

    /**
     * 核心方法：异步上传文件，切块后投递至 Kafka
     */
    public DocumentMeta uploadAsync(MultipartFile file, String userId) throws IOException {
        // 1. 保存文档元信息
        DocumentMeta meta = new DocumentMeta();
        meta.setFileName(file.getOriginalFilename());
        meta.setContentType(file.getContentType());
        meta.setFileSize(file.getSize());
        meta.setStatus(DocumentMeta.STATUS_PENDING);
        meta.setUserId(userId);
        meta.setCreatedAt(LocalDateTime.now());
        meta.setUpdatedAt(LocalDateTime.now());
        meta = metaRepository.save(meta);
        log.info("异步上传: 文档元信息已保存: id={}, fileName={}", meta.getId(), meta.getFileName());

        // 2. Tika 解析
        Document document = parserService.parse(file.getInputStream());
        log.info("Tika 解析完成, 文本长度: {}", document.text().length());

        // 3. 切块
        List<TextSegment> segments = chunkingService.split(document);
        log.info("切块完成, 共 {} 块", segments.size());

        // 4. 持久化所有 Chunk
        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(meta.getId());
            chunk.setChunkIndex(i);
            chunk.setText(segments.get(i).text());
            chunk.setCreatedAt(LocalDateTime.now());
            chunks.add(chunk);
        }
        chunks = chunkRepository.saveAll(chunks);

        // 5. 更新 Meta 状态为 EXTRACTING
        meta.setTotalChunks(chunks.size());
        meta.setStatus(DocumentMeta.STATUS_EXTRACTING);
        meta.setUpdatedAt(LocalDateTime.now());
        metaRepository.save(meta);

        // 6. 遍历 Chunk，投递 Kafka 消息
        for (DocumentChunk chunk : chunks) {
            ChunkMessage message = new ChunkMessage(meta.getId(), chunk.getId(), chunk.getText());
            chunkExtractProducer.send(message);
        }

        log.info("文档 [{}] 异步处理流派发完成, 已投递 {} 条切块任务至 Kafka", meta.getFileName(), chunks.size());
        // 实际的完成状态依赖于消费者执行，这里暂标记为 EXTRACTING，
        // 后续可通过定时任务或在所有 Chunk 处理完毕后最终修改为 COMPLETED
        return meta;
    }
}
