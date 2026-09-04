package com.offerpilot.service.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TencentDbAgentMemoryService implements AgentMemoryService {
    private final RestClient client;
    private final ObjectMapper json;
    private final String teamId;
    private final String agentId;
    private final String gatewayApiKey;

    public TencentDbAgentMemoryService(ObjectMapper json,
            @Value("${offerpilot.memory.tencentdb.base-url:http://localhost:8420}") String baseUrl,
            @Value("${offerpilot.memory.tencentdb.team-id:offerpilot}") String teamId,
            @Value("${offerpilot.memory.tencentdb.agent-id:offer-advisor}") String agentId,
            @Value("${offerpilot.memory.tencentdb.gateway-api-key:local}") String gatewayApiKey) {
        this.json = json;
        this.teamId = teamId;
        this.agentId = agentId;
        this.gatewayApiKey = gatewayApiKey;
        SimpleClientHttpRequestFactory http = new SimpleClientHttpRequestFactory();
        http.setConnectTimeout(600);
        http.setReadTimeout(1_800);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(http).build();
    }

    @Override
    public MemoryContext recall(String userId, UUID conversationId, UUID offerId, String question) {
        Map<String,Object> body = common(userId, conversationId, offerId);
        body.put("query", question);
        String raw = client.post().uri("/v3/conversation/query")
                .header("Authorization", "Bearer " + gatewayApiKey)
                .header("x-tdai-service-id", "default")
                .body(body).retrieve().body(String.class);
        try {
            JsonNode root = json.readTree(raw == null ? "{}" : raw);
            JsonNode data = root.has("data") ? root.path("data") : root;
            return new MemoryContext(data.toString(), mode());
        } catch (Exception exception) {
            throw new IllegalStateException("TencentDB记忆响应无法解析", exception);
        }
    }

    @Override
    public void append(String userId, UUID conversationId, UUID offerId, String role, String content) {
        Map<String,Object> body = common(userId, conversationId, offerId);
        body.put("messages", List.of(Map.of("role", role, "content", content)));
        client.post().uri("/v3/conversation/add")
                .header("Authorization", "Bearer " + gatewayApiKey)
                .header("x-tdai-service-id", "default")
                .body(body).retrieve().toBodilessEntity();
    }

    private Map<String,Object> common(String userId, UUID conversationId, UUID offerId) {
        Map<String,Object> body = new java.util.LinkedHashMap<>();
        body.put("session_id", conversationId.toString());
        body.put("team_id", teamId);
        body.put("agent_id", agentId);
        body.put("user_id", userId == null || userId.isBlank() ? "anonymous" : userId);
        if (offerId != null) body.put("task_id", offerId.toString());
        return body;
    }
    @Override public String mode() { return "tencentdb"; }
}
