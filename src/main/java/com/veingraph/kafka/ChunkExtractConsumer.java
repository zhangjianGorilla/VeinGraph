package com.veingraph.kafka;

import com.veingraph.llm.model.ExtractionResult;
import com.veingraph.llm.model.RelationRecord;
import com.veingraph.llm.service.EntityExtractionService;
import com.veingraph.model.ExtractionRecord;
import com.veingraph.repository.mongo.ExtractionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import com.veingraph.repository.mongo.DocumentChunkRepository; // <-- Import here if not already imported

/**
 * Kafka 消费者：异步消费文本块，调用 LLM 进行实体关系抽取
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkExtractConsumer {

    private final EntityExtractionService extractionService;
    private final ExtractionRecordRepository recordRepository;
    private final DocumentChunkRepository chunkRepository;

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
                for (RelationRecord record : result.getRecords()) {
                    ExtractionRecord er = new ExtractionRecord();
                    er.setDocumentId(message.getDocumentId());
                    er.setChunkId(message.getChunkId());
                    er.setSource(record.getSource());
                    er.setTarget(record.getTarget());
                    er.setRelation(record.getRelation());
                    er.setEvidence(record.getEvidence());
                    er.setSyncStatus(ExtractionRecord.SYNC_UNSYNCED);
                    er.setCreatedAt(LocalDateTime.now());
                    recordRepository.save(er);
                }
                
                // 将 chunk 标记为已抽取
                chunkRepository.findById(message.getChunkId()).ifPresent(chunk -> {
                    chunk.setExtracted(true);
                    chunkRepository.save(chunk);
                });

                log.info("Chunk[{}] 异步抽取成功, 提取 {} 条关系",
                        message.getChunkId(), result.getRecords().size());
            } else {
                 // 即使没有抽取出关系，也算处理过了
                 chunkRepository.findById(message.getChunkId()).ifPresent(chunk -> {
                     chunk.setExtracted(true);
                     chunkRepository.save(chunk);
                 });
                 log.info("Chunk[{}] 异步抽取完成, 未提取出关系", message.getChunkId());
            }
        } catch (Exception e) {
            log.error("Chunk[{}] 异步抽取失败: {}", message.getChunkId(), e.getMessage());
            // 失败消息由 Kafka 的重试机制或死信队列处理
        }
    }
}
