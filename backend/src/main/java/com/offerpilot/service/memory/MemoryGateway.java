package com.offerpilot.service.memory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MemoryGateway {
    private final LightweightAgentMemoryService lightweight;
    private final TencentDbAgentMemoryService tencentdb;
    private final String configuredMode;

    public MemoryGateway(LightweightAgentMemoryService lightweight, TencentDbAgentMemoryService tencentdb,
            @Value("${offerpilot.memory.mode:lightweight}") String configuredMode) {
        this.lightweight = lightweight;
        this.tencentdb = tencentdb;
        this.configuredMode = configuredMode;
    }

    public AgentMemoryService.MemoryContext recall(String userId, UUID conversationId, UUID offerId, String question) {
        if (!"tencentdb".equalsIgnoreCase(configuredMode)) return lightweight.recall(userId, conversationId, offerId, question);
        try { return tencentdb.recall(userId, conversationId, offerId, question); }
        catch (RuntimeException ignored) { return lightweight.recall(userId, conversationId, offerId, question); }
    }

    public void append(String userId, UUID conversationId, UUID offerId, String role, String content) {
        lightweight.append(userId, conversationId, offerId, role, content);
        if ("tencentdb".equalsIgnoreCase(configuredMode)) {
            try { tencentdb.append(userId, conversationId, offerId, role, content); } catch (RuntimeException ignored) { }
        }
    }

    /**
     * Persist the local transcript immediately, but do not hold up the first answer token while
     * TencentDB performs remote embedding and indexing.
     */
    public void appendAsync(String userId, UUID conversationId, UUID offerId, String role, String content) {
        lightweight.append(userId, conversationId, offerId, role, content);
        if ("tencentdb".equalsIgnoreCase(configuredMode)) {
            Thread.startVirtualThread(() -> {
                try { tencentdb.append(userId, conversationId, offerId, role, content); }
                catch (RuntimeException ignored) { }
            });
        }
    }
}
