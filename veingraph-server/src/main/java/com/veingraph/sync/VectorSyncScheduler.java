package com.veingraph.sync;

import com.veingraph.model.DocumentChunk;
import com.veingraph.repository.mongo.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 向量化定时扫描器
 * 定期扫描 MongoDB 中未向量化的 Chunk，批量调用 VectorSyncService 处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorSyncScheduler {

    private final VectorSyncService vectorSyncService;
    private final DocumentChunkRepository chunkRepository;

    /**
     * 定时扫描未向量化的 Chunk
     * 每 15 秒执行一次，每次最多处理 50 条
     */
    @Scheduled(fixedDelayString = "${veingraph.vector.scan-interval:15000}")
    public void scanAndVectorize() {
        List<DocumentChunk> chunks = chunkRepository.findTop50ByVectorizedFalseOrderByCreatedAtAsc();

        if (chunks.isEmpty()) {
            return;
        }

        log.info("向量化扫描: 发现 {} 条未向量化 Chunk，开始处理...", chunks.size());

        int successCount = 0;
        int failCount = 0;

        for (DocumentChunk chunk : chunks) {
            try {
                vectorSyncService.vectorizeAndSync(chunk);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("向量化失败 [chunkId={}]: {}", chunk.getId(), e.getMessage());
            }
        }

        log.info("向量化同步完成: 成功={}, 失败={}", successCount, failCount);
    }
}
