package com.veingraph.document.schedule;

import com.veingraph.model.DocumentMeta;
import com.veingraph.repository.mongo.DocumentChunkRepository;
import com.veingraph.repository.mongo.DocumentMetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 轮询检查处于 EXTRACTING (抽取中) 状态的文档，
 * 若该文档全部 Chunk 都已经通过 Kafka 处理完成，则更新状态为 COMPLETED。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentStatusScheduler {

    private final DocumentMetaRepository metaRepository;
    private final DocumentChunkRepository chunkRepository;

    @Scheduled(fixedDelayString = "5000") // 每 5 秒扫描一次
    public void scanAndCompleteDocuments() {
        List<DocumentMeta> extractingDocs = metaRepository.findByStatus(DocumentMeta.STATUS_EXTRACTING);
        if (extractingDocs.isEmpty()) {
            return;
        }

        for (DocumentMeta doc : extractingDocs) {
            long extractedCount = chunkRepository.countByDocumentIdAndExtractedTrue(doc.getId());
            if (extractedCount >= doc.getTotalChunks()) {
                // 全部处理完成
                doc.setStatus(DocumentMeta.STATUS_COMPLETED);
                doc.setUpdatedAt(LocalDateTime.now());
                metaRepository.save(doc);
                log.info("文档 [{}] 所有切片已抽取完成, 状态更新为 COMPLETED", doc.getFileName());
            } else {
                log.debug("文档 [{}] 抽取进度: {}/{}", doc.getFileName(), extractedCount, doc.getTotalChunks());
            }
        }
    }
}
