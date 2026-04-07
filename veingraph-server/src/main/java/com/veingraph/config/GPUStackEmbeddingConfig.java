package com.veingraph.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GPUStack Embedding 配置
 * GPUStack 返回的响应格式与 OpenAI 标准不同：
 * - OpenAI: data[].embedding
 * - GPUStack: data[].dense + data[].sparse (混合向量)
 *
 * 此配置创建自定义 EmbeddingModel 来解析 GPUStack 的 dense 向量
 */
@Slf4j
@Configuration
public class GPUStackEmbeddingConfig {

    @Value("${spring.ai.openai.embedding.base-url:http://localhost:19980}")
    private String baseUrl;

    @Value("${spring.ai.openai.embedding.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.embedding.options.model:bge-m3}")
    private String model;

    @Value("${spring.ai.openai.embedding.options.dimensions:1024}")
    private int dimensions;

    /**
     * 创建自定义 GPUStack EmbeddingModel Bean，覆盖 Spring AI 默认的 OpenAI EmbeddingModel
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel(RestClient.Builder restClientBuilder) {
        RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
        return new GPUStackEmbeddingModel(restClient, apiKey, model, dimensions);
    }

    /**
     * 自定义 GPUStack EmbeddingModel 实现
     * 直接调用 GPUStack API 并解析其特有的 dense 字段响应格式
     */
    @Slf4j
    static class GPUStackEmbeddingModel implements EmbeddingModel {

        private final RestClient restClient;
        private final String apiKey;
        private final String model;
        private final int dimensions;
        private final ObjectMapper objectMapper;

        public GPUStackEmbeddingModel(RestClient restClient, String apiKey, String model, int dimensions) {
            this.restClient = restClient;
            this.apiKey = apiKey;
            this.model = model;
            this.dimensions = dimensions;
            this.objectMapper = new ObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            // 从 EmbeddingRequest 中提取文本输入
            // EmbeddingRequest 的 inputs 是 Document 列表
            List<String> inputs = new ArrayList<>();
            if (request.getInput() != null) {
                for (Object input : request.getInput()) {
                    if (input instanceof String) {
                        inputs.add((String) input);
                    } else if (input instanceof Document) {
                        inputs.add(((Document) input).getText());
                    }
                }
            }

            if (inputs.isEmpty()) {
                return new EmbeddingResponse(new ArrayList<>());
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", inputs.size() == 1 ? inputs.get(0) : inputs);
            requestBody.put("model", model);
            requestBody.put("dimensions", dimensions);

            try {
                String responseJson = restClient.post()
                        .uri("/v1/embeddings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                List<Embedding> embeddings = parseEmbeddings(responseJson);
                return new EmbeddingResponse(embeddings);

            } catch (Exception e) {
                log.error("GPUStack embedding API call failed: {}", e.getMessage());
                throw new RuntimeException("GPUStack embedding failed", e);
            }
        }

        @Override
        public float[] embed(String text) {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", text);
            requestBody.put("model", model);
            requestBody.put("dimensions", dimensions);

            try {
                String responseJson = restClient.post()
                        .uri("/v1/embeddings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                return parseDenseVector(responseJson);

            } catch (Exception e) {
                log.error("GPUStack embedding API call failed: {}", e.getMessage());
                throw new RuntimeException("GPUStack embedding failed", e);
            }
        }

        @Override
        public float[] embed(Document document) {
            return embed(document.getText());
        }

        private float[] parseDenseVector(String json) {
            try {
                Map<String, Object> responseMap = objectMapper.readValue(json, Map.class);
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) responseMap.get("data");

                if (dataList == null || dataList.isEmpty()) {
                    log.warn("GPUStack embedding response data is empty");
                    return new float[dimensions];
                }

                List<Double> denseList = (List<Double>) dataList.get(0).get("dense");
                if (denseList == null) {
                    log.warn("GPUStack embedding response missing dense field");
                    return new float[dimensions];
                }

                float[] vector = new float[denseList.size()];
                for (int i = 0; i < denseList.size(); i++) {
                    vector[i] = denseList.get(i).floatValue();
                }

                return vector;

            } catch (Exception e) {
                log.error("Failed to parse GPUStack embedding response: {}", e.getMessage());
                throw new RuntimeException("Failed to parse GPUStack embedding response", e);
            }
        }

        private List<Embedding> parseEmbeddings(String json) {
            try {
                Map<String, Object> responseMap = objectMapper.readValue(json, Map.class);
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) responseMap.get("data");

                List<Embedding> embeddings = new ArrayList<>();
                if (dataList == null) {
                    return embeddings;
                }

                for (Map<String, Object> data : dataList) {
                    List<Double> denseList = (List<Double>) data.get("dense");
                    if (denseList != null) {
                        float[] vector = new float[denseList.size()];
                        for (int i = 0; i < denseList.size(); i++) {
                            vector[i] = denseList.get(i).floatValue();
                        }
                        Integer index = (Integer) data.get("index");
                        embeddings.add(new Embedding(vector, index != null ? index : 0));
                    }
                }

                return embeddings;

            } catch (Exception e) {
                log.error("Failed to parse GPUStack embedding response: {}", e.getMessage());
                throw new RuntimeException("Failed to parse GPUStack embedding response", e);
            }
        }
    }
}