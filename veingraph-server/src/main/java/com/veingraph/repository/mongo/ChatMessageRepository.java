package com.veingraph.repository.mongo;

import com.veingraph.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /**
     * 按会话 ID 查询全部消息，按时间升序
     */
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * 按会话 ID 查询最近 N 条消息（用于热缓存加载）
     */
    List<ChatMessage> findTop20BySessionIdOrderByCreatedAtDesc(String sessionId);
}
