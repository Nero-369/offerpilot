package com.offerpilot.service.memory;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class LightweightAgentMemoryService implements AgentMemoryService {
    private static final int HISTORY_LIMIT = 12;
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public LightweightAgentMemoryService(JdbcTemplate jdbc, StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.redis = redis;
    }

    @Override
    public MemoryContext recall(String userId, UUID conversationId, UUID offerId, String question) {
        String cacheKey = cacheKey(userId, conversationId);
        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) return new MemoryContext(cached, mode());
        } catch (RuntimeException ignored) { }

        List<String> messages = jdbc.query("""
                SELECT role || ': ' || content FROM (
                  SELECT role,content,created_at FROM chat_messages
                  WHERE conversation_id=? AND EXISTS (
                    SELECT 1 FROM chat_conversations c WHERE c.id=chat_messages.conversation_id AND c.user_id=?
                  ) ORDER BY created_at DESC LIMIT ?
                ) recent ORDER BY created_at
                """, (rs, row) -> rs.getString(1), conversationId, normalizedUser(userId), HISTORY_LIMIT);
        String context = String.join("\n", messages);
        cache(cacheKey, context);
        return new MemoryContext(context, mode());
    }

    @Override
    public void append(String userId, UUID conversationId, UUID offerId, String role, String content) {
        jdbc.update("""
                INSERT INTO chat_conversations(id,user_id,offer_id,title) VALUES (?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET offer_id=COALESCE(EXCLUDED.offer_id,chat_conversations.offer_id),updated_at=NOW()
                WHERE chat_conversations.user_id=EXCLUDED.user_id
                """, conversationId, normalizedUser(userId), offerId, title(content));
        Integer owned = jdbc.queryForObject("SELECT COUNT(*) FROM chat_conversations WHERE id=? AND user_id=?", Integer.class, conversationId, normalizedUser(userId));
        if (owned == null || owned == 0) throw new IllegalArgumentException("会话不属于当前用户");
        jdbc.update("INSERT INTO chat_messages(id,conversation_id,role,content) VALUES (?,?,?,?)",
                UUID.randomUUID(), conversationId, role, content);
        try { redis.delete(cacheKey(userId, conversationId)); } catch (RuntimeException ignored) { }
    }

    private void cache(String key, String value) {
        if (value.isBlank()) return;
        try { redis.opsForValue().set(key, value, Duration.ofHours(12)); } catch (RuntimeException ignored) { }
    }
    private String cacheKey(String userId, UUID id) { return "offerpilot:memory:" + normalizedUser(userId) + ":" + id; }
    private String normalizedUser(String userId) { return userId == null || userId.isBlank() ? "anonymous" : userId; }
    private String title(String content) { return content.substring(0, Math.min(content.length(), 80)); }
    @Override public String mode() { return "lightweight"; }
}
