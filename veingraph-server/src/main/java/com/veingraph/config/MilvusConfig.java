package com.veingraph.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Milvus 向量数据库配置
 * 自动创建客户端连接和 Collection
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "veingraph.milvus")
public class MilvusConfig {

    private String host;
    private int port;
    private String collectionName;
    private int dimension;

    @Bean
    public MilvusClientV2 milvusClient() {
        ConnectConfig config = ConnectConfig.builder()
                .uri("http://" + host + ":" + port)
                .build();
        return new MilvusClientV2(config);
    }

    /**
     * 应用启动后自动创建 Collection（延迟执行，避免循环依赖）
     */
    @Bean
    public CommandLineRunner milvusCollectionInitializer(MilvusClientV2 client) {
        return args -> {
            try {
                boolean exists = client.hasCollection(HasCollectionReq.builder()
                        .collectionName(collectionName)
                        .build());

                if (exists) {
                    log.info("Milvus Collection '{}' 已存在，跳过创建", collectionName);
                    return;
                }

                // 定义 Schema
                CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder().build();

                schema.addField(AddFieldReq.builder()
                        .fieldName("id")
                        .dataType(DataType.VarChar)
                        .maxLength(128)
                        .isPrimaryKey(true)
                        .build());

                schema.addField(AddFieldReq.builder()
                        .fieldName("document_id")
                        .dataType(DataType.VarChar)
                        .maxLength(128)
                        .build());

                schema.addField(AddFieldReq.builder()
                        .fieldName("text")
                        .dataType(DataType.VarChar)
                        .maxLength(32000)
                        .build());

                schema.addField(AddFieldReq.builder()
                        .fieldName("vector")
                        .dataType(DataType.FloatVector)
                        .dimension(dimension)
                        .build());

                // 向量索引：HNSW + COSINE
                IndexParam vectorIndex = IndexParam.builder()
                        .fieldName("vector")
                        .indexType(IndexParam.IndexType.HNSW)
                        .metricType(IndexParam.MetricType.COSINE)
                        .build();

                client.createCollection(CreateCollectionReq.builder()
                        .collectionName(collectionName)
                        .collectionSchema(schema)
                        .indexParams(List.of(vectorIndex))
                        .build());

                log.info("Milvus Collection '{}' 创建成功 (dim={})", collectionName, dimension);
            } catch (Exception e) {
                log.warn("Milvus Collection 初始化失败（Milvus 可能未启动）: {}", e.getMessage());
            }
        };
    }
}