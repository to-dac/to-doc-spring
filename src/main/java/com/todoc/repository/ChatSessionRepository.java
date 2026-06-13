package com.todoc.repository;

import com.todoc.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
