package com.offerpilot.service.memory;

import java.util.UUID;

public interface AgentMemoryService {
    MemoryContext recall(String userId, UUID conversationId, UUID offerId, String question);
    void append(String userId, UUID conversationId, UUID offerId, String role, String content);
    String mode();

    record MemoryContext(String text, String mode) {
        public static MemoryContext empty(String mode) { return new MemoryContext("", mode); }
    }
}
