package com.veingraph.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 生产者：将文本块抽取任务投递到消息队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkExtractProducer {

    private final KafkaTemplate<String, ChunkMessage> kafkaTemplate;

    /**
     * 投递一个文本块抽取任务
     *
     * @param message 文本块消息（含 documentId, chunkId, text）
     */
    public void send(ChunkMessage message) {
        log.info("投递 Kafka 消息: topic={}, documentId={}, chunkId={}",
                ChunkMessage.TOPIC, message.getDocumentId(), message.getChunkId());
        kafkaTemplate.send(ChunkMessage.TOPIC, message.getDocumentId(), message);
    }
}
