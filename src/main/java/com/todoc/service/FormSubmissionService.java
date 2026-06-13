package com.todoc.service;

import com.todoc.domain.ChatSession;
import com.todoc.domain.FormAnswer;
import com.todoc.domain.FormSubmission;
import com.todoc.service.AiPermitClient;
import com.todoc.dto.request.SubmitFormRequest;
import com.todoc.dto.response.FormSubmissionResponse;
import com.todoc.dto.response.FormSubmissionResponse.AnswerResponse;
import com.todoc.exception.NotFoundException;
import com.todoc.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormSubmissionService {

    private final FormSubmissionRepository submissionRepository;
    private final FormAnswerRepository answerRepository;
    private final FormTemplateRepository templateRepository;
    private final FormQuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Transactional
    public FormSubmissionResponse submit(SubmitFormRequest request) {
        var template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new NotFoundException("템플릿을 찾을 수 없습니다: id=" + request.templateId()));
        var user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다: id=" + request.userId()));

        var submission = FormSubmission.builder()
                .template(template)
                .user(user)
                .build();
        submission = submissionRepository.save(submission);

        final var savedSubmission = submission;
        List<FormAnswer> answers = request.answers().stream()
                .map(item -> {
                    var question = questionRepository.findById(item.questionId())
                            .orElseThrow(() -> new NotFoundException("질문을 찾을 수 없습니다: id=" + item.questionId()));
                    return FormAnswer.builder()
                            .submission(savedSubmission)
                            .question(question)
                            .answerValue(item.answerValue())
                            .build();
                })
                .toList();

        answerRepository.saveAll(answers);
        submission.submit();

        List<AnswerResponse> answerResponses = answers.stream()
                .map(AnswerResponse::from)
                .toList();
        return FormSubmissionResponse.from(submission, answerResponses);
    }

    @Transactional
    public FormSubmission createPending(Long templateId, Long userId, ChatSession session) {
        var template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("템플릿을 찾을 수 없습니다: id=" + templateId));
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다: id=" + userId));
        var submission = FormSubmission.builder()
                .template(template)
                .user(user)
                .session(session)
                .build();
        return submissionRepository.save(submission);
    }

    @Transactional
    public FormSubmissionResponse fillAnswers(Long submissionId, List<AiPermitClient.AnswerItem> answers) {
        var submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("제출을 찾을 수 없습니다: id=" + submissionId));

        List<FormAnswer> formAnswers = answers.stream()
                .map(item -> {
                    var question = questionRepository.findById(item.question_id())
                            .orElseThrow(() -> new NotFoundException("질문을 찾을 수 없습니다: id=" + item.question_id()));
                    return FormAnswer.builder()
                            .submission(submission)
                            .question(question)
                            .answerValue(item.answer_value())
                            .build();
                })
                .toList();

        answerRepository.saveAll(formAnswers);
        submission.toDraft();

        List<FormSubmissionResponse.AnswerResponse> answerResponses = formAnswers.stream()
                .map(FormSubmissionResponse.AnswerResponse::from)
                .toList();
        return FormSubmissionResponse.from(submission, answerResponses);
    }

    @Transactional
    public FormSubmission submitAiGenerated(Long templateId, Long userId, ChatSession session,
            List<AiPermitClient.AnswerItem> answers) {
        var template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("템플릿을 찾을 수 없습니다: id=" + templateId));
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다: id=" + userId));

        var submission = FormSubmission.builder()
                .template(template)
                .user(user)
                .session(session)
                .build();
        submission = submissionRepository.save(submission);

        final var savedSubmission = submission;
        List<FormAnswer> formAnswers = answers.stream()
                .map(item -> {
                    var question = questionRepository.findById(item.question_id())
                            .orElseThrow(() -> new NotFoundException("질문을 찾을 수 없습니다: id=" + item.question_id()));
                    return FormAnswer.builder()
                            .submission(savedSubmission)
                            .question(question)
                            .answerValue(item.answer_value())
                            .build();
                })
                .toList();

        answerRepository.saveAll(formAnswers);
        return submission;
    }

    @Transactional
    public void applyChanges(Long submissionId, List<AiPermitClient.ChangeItem> changes) {
        for (AiPermitClient.ChangeItem change : changes) {
            answerRepository.findBySubmissionIdAndQuestionId(submissionId, change.questionId())
                    .ifPresent(answer -> {
                        String newValue = toJsonValue(change.current());
                        answer.updateAnswerValue(newValue);
                        answerRepository.save(answer);
                        log.info("답변 업데이트: questionId={}, {} → {}", change.questionId(), change.previous(), change.current());
                    });
        }
    }

    private String toJsonValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        String str = value.toString().trim();
        if (str.startsWith("{") || str.startsWith("[") || str.startsWith("\"")) return str;
        return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Transactional(readOnly = true)
    public FormSubmissionResponse getById(Long id) {
        var submission = submissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("제출을 찾을 수 없습니다: id=" + id));
        List<AnswerResponse> answers = answerRepository.findAllBySubmissionId(id).stream()
                .map(AnswerResponse::from)
                .toList();
        return FormSubmissionResponse.from(submission, answers);
    }

    @Transactional(readOnly = true)
    public List<FormSubmissionResponse> listByUser(Long userId) {
        return submissionRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(s -> {
                    List<AnswerResponse> answers = answerRepository.findAllBySubmissionId(s.getId()).stream()
                            .map(AnswerResponse::from)
                            .toList();
                    return FormSubmissionResponse.from(s, answers);
                })
                .toList();
    }
}
