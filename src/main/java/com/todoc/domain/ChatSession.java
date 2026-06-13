package com.todoc.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    @Builder.Default
    private String title = "새 채팅";

    @Column(length = 500)
    private String address;

    @Column(length = 19)
    private String pnu;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "land_info", columnDefinition = "TEXT")
    private String landInfo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateAddress(String address) {
        this.address = address;
    }

    public void updatePnu(String pnu) {
        this.pnu = pnu;
    }

    public void updateTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public void updateLandInfo(String landInfo) {
        this.landInfo = landInfo;
    }
}
