package com.todoc.domain;

import com.todoc.domain.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "form_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FormQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private FormSection section;

    @Column(name = "section_name", nullable = false, length = 255)
    private String sectionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "json")
    private String options;

    @Column(name = "display_type", nullable = false, length = 50)
    private String displayType;

    @Column(columnDefinition = "json")
    private String validation;

    @Column(columnDefinition = "json")
    private String conditional;

    @Column(name = "batch_group", length = 150)
    private String batchGroup;

    @Column(name = "sub_fields", columnDefinition = "json")
    private String subFields;

    @Column(name = "layout_key", nullable = false, length = 150)
    private String layoutKey;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    @Column(nullable = false, columnDefinition = "json")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
