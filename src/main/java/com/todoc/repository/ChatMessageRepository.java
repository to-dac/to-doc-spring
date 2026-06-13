package com.todoc.repository;

import com.todoc.domain.ChatMessage;
import com.todoc.domain.enums.MessageRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    void deleteBySessionId(Long sessionId);
    long countBySessionIdAndRole(Long sessionId, MessageRole role);
}
