package com.veingraph.chat;

import com.veingraph.model.ChatMessage;
import com.veingraph.repository.mongo.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话历史管理服务
 * MongoDB 持久化全量历史 + Redis 热缓存最近 N 轮
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatMessageRepository messageRepository;
    private final StringRedisTemplate redisTemplate;

    /** Redis 缓存 Key 前缀 */
    private static final String CACHE_PREFIX = "chat:history:";
    /** 缓存过期时间 */
    private static final Duration CACHE_TTL = Duration.ofHours(2);
    /** 上下文窗口：最近 N 轮对话 */
    private static final int CONTEXT_WINDOW = 10;

    /**
     * 保存一条对话消息
     */
    public ChatMessage saveMessage(String sessionId, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        msg = messageRepository.save(msg);

        // 追加到 Redis 缓存
        String cacheKey = CACHE_PREFIX + sessionId;
        String entry = role + ": " + content;
        redisTemplate.opsForList().rightPush(cacheKey, entry);
        // 只保留最近 N*2 条（每轮包含 user + assistant）
        redisTemplate.opsForList().trim(cacheKey, -(CONTEXT_WINDOW * 2L), -1);
        redisTemplate.expire(cacheKey, CACHE_TTL);

        return msg;
    }

    /**
     * 获取上下文窗口内的对话历史（优先从 Redis 读取）
     *
     * @return 格式化的历史文本，适合直接放入 Prompt
     */
    public String getContextHistory(String sessionId) {
        String cacheKey = CACHE_PREFIX + sessionId;

        // 优先 Redis
        List<String> cached = redisTemplate.opsForList().range(cacheKey, 0, -1);
        if (cached != null && !cached.isEmpty()) {
            log.debug("从 Redis 加载对话历史: sessionId={}, 条数={}", sessionId, cached.size());
            return String.join("\n", cached);
        }

        // 降级到 MongoDB
        List<ChatMessage> messages = messageRepository.findTop20BySessionIdOrderByCreatedAtDesc(sessionId);
        if (messages.isEmpty()) {
            return "";
        }
        // 反转为时间正序
        Collections.reverse(messages);

        // 取最近 CONTEXT_WINDOW 轮
        List<String> entries = messages.stream()
                .limit(CONTEXT_WINDOW * 2L)
                .map(m -> m.getRole() + ": " + m.getContent())
                .collect(Collectors.toList());

        // 回填 Redis
        for (String entry : entries) {
            redisTemplate.opsForList().rightPush(cacheKey, entry);
        }
        redisTemplate.expire(cacheKey, CACHE_TTL);

        log.debug("从 MongoDB 加载并回填 Redis: sessionId={}, 条数={}", sessionId, entries.size());
        return String.join("\n", entries);
    }

    /**
     * 查询完整对话历史
     */
    public List<ChatMessage> getFullHistory(String sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
}
