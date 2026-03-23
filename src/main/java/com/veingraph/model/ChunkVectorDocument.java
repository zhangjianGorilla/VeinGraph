package com.veingraph.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * ES 索引实体：文本块 + 稠密向量
 * 用于混合检索（BM25 关键词 + Dense Vector KNN）
 */
@Data
@Document(indexName = "veingraph-chunks")
@Schema(description = "ES 文本块向量索引文档")
public class ChunkVectorDocument {

    @Id
    @Schema(description = "文档 ID (同 MongoDB DocumentChunk.id)")
    private String id;

    @Field(type = FieldType.Keyword)
    @Schema(description = "关联的 MongoDB 文档 ID")
    private String documentId;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    @Schema(description = "纯文本内容")
    private String text;

    @Field(type = FieldType.Dense_Vector, dims = 1024)
    @Schema(description = "文本稠密向量 (1024 维)")
    private float[] vector;
}
