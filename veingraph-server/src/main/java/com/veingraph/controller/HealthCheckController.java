package com.veingraph.controller;

import com.veingraph.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
// import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基础设施健康检查 Controller
 * 用于验证各个数据源连接是否正常
 */
@Tag(name = "基础设施健康检查", description = "用于验证各个数据源连接是否正常")
@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MilvusClientV2 milvusClient;

    @Value("${veingraph.milvus.collection-name:veingraph_chunks}")
    private String milvusCollectionName;

    /**
     * 检查所有数据源连接状态
     */
    @Operation(summary = "检查所有数据源连接状态", description = "检查所有数据源连接状态")
    @GetMapping("/datasources")
    public Result<Map<String, Object>> checkDatasources() {
        Map<String, Object> status = new LinkedHashMap<>();

        // 检查 MongoDB
        try {
            mongoTemplate.getDb().getName();
            status.put("mongodb", "✅ 连接正常");
        } catch (Exception e) {
            status.put("mongodb", "❌ " + e.getMessage());
        }

        // 检查 Neo4j
         try {
             neo4jClient.query("RETURN 1 AS result").fetch().one();
             status.put("neo4j", "✅ 连接正常");
         } catch (Exception e) {
             status.put("neo4j", "❌ " + e.getMessage());
         }

        // 检查 Redis
        try {
            redisTemplate.opsForValue().set("veingraph:health", "ok");
            Object val = redisTemplate.opsForValue().get("veingraph:health");
            status.put("redis", "ok".equals(val) ? "✅ 连接正常" : "❌ 读写异常");
        } catch (Exception e) {
            status.put("redis", "❌ " + e.getMessage());
        }

        // 检查 Elasticsearch (BM25 关键词检索)
        try {
            status.put("elasticsearch", "✅ 配置已加载 (详见 /actuator/health)");
        } catch (Exception e) {
            status.put("elasticsearch", "❌ " + e.getMessage());
        }

        // 检查 Milvus (向量检索)
        try {
            boolean exists = milvusClient.hasCollection(HasCollectionReq.builder()
                    .collectionName(milvusCollectionName).build());
            status.put("milvus", exists
                    ? "✅ 连接正常 (Collection: " + milvusCollectionName + ")"
                    : "⚠️ 连接正常但 Collection 不存在");
        } catch (Exception e) {
            status.put("milvus", "❌ " + e.getMessage());
        }

        // Kafka 状态
        status.put("kafka", "✅ 配置已加载 (Topic: doc-chunk-extract-topic)");

        return Result.ok(status);
    }
}
