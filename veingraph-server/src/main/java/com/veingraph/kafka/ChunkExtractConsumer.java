package com.veingraph.kafka;

import com.veingraph.llm.model.ExtractionResult;
import com.veingraph.llm.model.RelationRecord;
import com.veingraph.llm.service.EntityExtractionService;
import com.veingraph.model.ExtractionRecord;
import com.veingraph.repository.mongo.ExtractionRecordRepository;
import com.veingraph.repository.mongo.DocumentChunkRepository;
import com.veingraph.resolution.EntityResolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Kafka 消费者：异步消费文本块，调用 LLM 进行实体关系抽取
 * 包含后处理校验（代词残留拦截 + 自环过滤）和消歧触发
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkExtractConsumer {

    private final EntityExtractionService extractionService;
    private final ExtractionRecordRepository recordRepository;
    private final DocumentChunkRepository chunkRepository;
    private final EntityResolutionService entityResolutionService;

    /** 代词黑名单正则：匹配常见中文代词和泛指代词 */
    private static final Pattern PRONOUN_PATTERN = Pattern.compile(
            "^(他|她|它|他们|她们|它们|该人|此人|对方|其|这个人|那个人|某人|本人|自己|我|你|您)$"
    );

    /** 需要拦截的职务性泛指称呼 */
    private static final Set<String> TITLE_ONLY_NAMES = Set.of(
            "局长", "主任", "医生", "老师", "教授", "经理", "总裁", "董事长",
            "书记", "市长", "县长", "校长", "院长", "所长", "处长", "科长"
    );

    @KafkaListener(
            topics = ChunkMessage.TOPIC,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ChunkMessage message) {
        log.info("消费 Kafka 消息: documentId={}, chunkId={}", message.getDocumentId(), message.getChunkId());

        try {
            ExtractionResult result = extractionService.extract(message.getText());

            if (result != null && result.getRecords() != null) {
                int savedCount = 0;
                int filteredCount = 0;

                for (RelationRecord record : result.getRecords()) {
                    // === 后处理校验层 ===

                    // 校验 1：代词残留拦截
                    if (isPronounOrTitleOnly(record.getSource()) || isPronounOrTitleOnly(record.getTarget())) {
                        log.warn("[后处理拦截] 代词残留: source='{}', target='{}', 已丢弃",
                                record.getSource(), record.getTarget());
                        filteredCount++;
                        continue;
                    }

                    // 校验 2：自环关系过滤（source == target）
                    if (record.getSource().equals(record.getTarget())) {
                        log.warn("[后处理拦截] 自环关系: '{}' -[{}]-> '{}', 已丢弃",
                                record.getSource(), record.getRelation(), record.getTarget());
                        filteredCount++;
                        continue;
                    }

                    // 校验 3：空值过滤
                    if (record.getSource() == null || record.getSource().isBlank()
                            || record.getTarget() == null || record.getTarget().isBlank()) {
                        filteredCount++;
                        continue;
                    }

                    // 通过校验，写入 MongoDB
                    ExtractionRecord er = new ExtractionRecord();
                    er.setDocumentId(message.getDocumentId());
                    er.setChunkId(message.getChunkId());
                    er.setSource(record.getSource().trim());
                    er.setTarget(record.getTarget().trim());
                    er.setRelation(record.getRelation());
                    er.setEvidence(record.getEvidence());
                    er.setSyncStatus(ExtractionRecord.SYNC_UNSYNCED);
                    er.setCreatedAt(LocalDateTime.now());
                    recordRepository.save(er);
                    savedCount++;
                }
                
                // 将 chunk 标记为已抽取
                chunkRepository.findById(message.getChunkId()).ifPresent(chunk -> {
                    chunk.setExtracted(true);
                    chunkRepository.save(chunk);
                });

                log.info("Chunk[{}] 异步抽取成功, 提取 {} 条关系 (过滤 {} 条)",
                        message.getChunkId(), savedCount, filteredCount);
            } else {
                 // 即使没有抽取出关系，也算处理过了
                 chunkRepository.findById(message.getChunkId()).ifPresent(chunk -> {
                     chunk.setExtracted(true);
                     chunkRepository.save(chunk);
                 });
                 log.info("Chunk[{}] 异步抽取完成, 未提取出关系", message.getChunkId());
            }

            // === 检查是否全部 Chunk 抽取完毕，触发实体消歧 ===
            triggerResolutionIfComplete(message.getDocumentId());

        } catch (Exception e) {
            log.error("Chunk[{}] 异步抽取失败: {}", message.getChunkId(), e.getMessage());
            // 失败消息由 Kafka 的重试机制或死信队列处理
        }
    }

    /**
     * 判断实体名称是否为代词或纯职务泛指
     */
    private boolean isPronounOrTitleOnly(String name) {
        if (name == null || name.isBlank()) return true;
        String trimmed = name.trim();
        return PRONOUN_PATTERN.matcher(trimmed).matches() || TITLE_ONLY_NAMES.contains(trimmed);
    }

    /**
     * 检查文档的全部 Chunk 是否都已抽取完毕，若完毕则异步触发实体消歧
     */
    private void triggerResolutionIfComplete(String documentId) {
        long totalChunks = chunkRepository.countByDocumentId(documentId);
        long extractedChunks = chunkRepository.countByDocumentIdAndExtractedTrue(documentId);

        if (totalChunks > 0 && totalChunks == extractedChunks) {
            log.info("[消歧触发] 文档 {} 全部 {} 个 Chunk 已抽取完毕，触发实体消歧...",
                    documentId, totalChunks);
            // 异步执行消歧，不阻塞当前消费线程
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    entityResolutionService.resolveEntitiesForDocument(documentId);
                } catch (Exception e) {
                    log.error("[消歧触发] 文档 {} 消歧执行失败: {}", documentId, e.getMessage());
                }
            });
        }
    }
}
