package com.offerpilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.*;

@Component
public class QwenEmbeddingClient {
    private final RestClient http;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;
    private final int dimensions;

    public QwenEmbeddingClient(RestClient.Builder builder, ObjectMapper mapper,
        @Value("${offerpilot.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
        @Value("${offerpilot.qwen.api-key:}") String apiKey,
        @Value("${offerpilot.qwen.embedding-model:text-embedding-v4}") String model,
        @Value("${offerpilot.qwen.embedding-dimensions:1024}") int dimensions) {
        this.http = builder.baseUrl(baseUrl.replaceAll("/$", "")).build();
        this.mapper = mapper; this.apiKey = apiKey; this.model = model; this.dimensions = dimensions;
    }

    public boolean configured() { return apiKey != null && !apiKey.isBlank() && !apiKey.contains("not-used"); }

    public List<float[]> embed(List<String> input) {
        if (!configured()) throw new IllegalStateException("Qwen API Key 尚未配置");
        if (input.isEmpty() || input.size() > 10) throw new IllegalArgumentException("Embedding每批需要1-10个切片");
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("model", model); body.put("input", input); body.put("dimensions", dimensions); body.put("encoding_format", "float");
        String json = http.post().uri("/embeddings").contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + apiKey).body(body).retrieve().body(String.class);
        try {
            JsonNode data = mapper.readTree(json).path("data");
            List<float[]> vectors = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode embedding = item.path("embedding");
                float[] vector = new float[embedding.size()];
                for (int i=0;i<embedding.size();i++) vector[i]=(float)embedding.get(i).asDouble();
                if (vector.length != dimensions) throw new IllegalStateException("Embedding维度不匹配: " + vector.length);
                vectors.add(vector);
            }
            if (vectors.size()!=input.size()) throw new IllegalStateException("Embedding返回数量异常");
            return vectors;
        } catch (Exception e) { throw new IllegalStateException("解析Qwen Embedding响应失败", e); }
    }

    public float[] embedOne(String text) { return embed(List.of(text)).getFirst(); }
    public String model() { return model; }
}
