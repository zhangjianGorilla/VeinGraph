package com.veingraph.sync;

import com.veingraph.model.ExtractionRecord;
import com.veingraph.repository.mongo.ExtractionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 发件箱定时扫描器
 * 定期扫描 MongoDB 中 syncStatus=UNSYNCED 的记录，批量分发到 Neo4j
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final GraphSyncService graphSyncService;
    private final ExtractionRecordRepository recordRepository;

    /** 每次扫描的最大记录数 */
    @Value("${veingraph.outbox.batch-size:100}")
    private int batchSize;

    /**
     * 定时扫描并同步
     * fixedDelayString 从配置读取，默认 10 秒
     */
    @Scheduled(fixedDelayString = "${veingraph.outbox.scan-interval:10000}")
    public void scanAndSync() {
        List<ExtractionRecord> unsyncedRecords =
                recordRepository.findTopBySyncStatusOrderByCreatedAtAsc(
                        ExtractionRecord.SYNC_UNSYNCED, batchSize);

        if (unsyncedRecords.isEmpty()) {
            return;
        }

        log.info("发件箱扫描: 发现 {} 条 UNSYNCED 记录，开始同步...", unsyncedRecords.size());

        int successCount = 0;
        int failCount = 0;

        for (ExtractionRecord record : unsyncedRecords) {
            try {
                graphSyncService.syncRecord(record);
                successCount++;
            } catch (Exception e) {
                failCount++;
                record.setSyncStatus(ExtractionRecord.SYNC_FAILED);
                recordRepository.save(record);
                log.error("同步失败 [recordId={}]: {}", record.getId(), e.getMessage());
            }
        }

        log.info("发件箱同步完成: 成功={}, 失败={}", successCount, failCount);
    }
}
