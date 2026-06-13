package com.todoc.dto.response;

import com.todoc.domain.FormAnswer;
import com.todoc.domain.FormSubmission;

import java.time.LocalDateTime;
import java.util.List;

public record FormSubmissionResponse(
        Long id,
        Long templateId,
        String templateCode,
        String templateName,
        Long userId,
        String status,
        LocalDateTime submittedAt,
        LocalDateTime createdAt,
        List<AnswerResponse> answers
) {
    public static FormSubmissionResponse from(FormSubmission s, List<AnswerResponse> answers) {
        return new FormSubmissionResponse(
                s.getId(),
                s.getTemplate().getId(),
                s.getTemplate().getTemplateCode(),
                s.getTemplate().getName(),
                s.getUser().getId(),
                s.getStatus().name(),
                s.getSubmittedAt(),
                s.getCreatedAt(),
                answers);
    }

    public record AnswerResponse(
            Long id,
            Long questionId,
            String layoutKey,
            String answerValue
    ) {
        public static AnswerResponse from(FormAnswer a) {
            return new AnswerResponse(
                    a.getId(),
                    a.getQuestion().getId(),
                    a.getQuestion().getLayoutKey(),
                    a.getAnswerValue());
        }
    }
}
