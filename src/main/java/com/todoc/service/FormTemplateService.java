package com.todoc.service;

import com.todoc.dto.response.FormTemplateDetailResponse;
import com.todoc.dto.response.FormTemplateDetailResponse.QuestionResponse;
import com.todoc.dto.response.FormTemplateDetailResponse.SectionResponse;
import com.todoc.dto.response.FormTemplateResponse;
import com.todoc.dto.response.PermitDashboardResponse;
import com.todoc.exception.NotFoundException;
import com.todoc.repository.ChecklistItemTemplateRepository;
import com.todoc.repository.FormQuestionRepository;
import com.todoc.repository.FormSectionRepository;
import com.todoc.repository.FormTemplateRepository;
import com.todoc.repository.TimelineTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FormTemplateService {

    private final FormTemplateRepository templateRepository;
    private final FormSectionRepository sectionRepository;
    private final FormQuestionRepository questionRepository;
    private final TimelineTemplateRepository timelineTemplateRepository;
    private final ChecklistItemTemplateRepository checklistItemTemplateRepository;

    public List<FormTemplateResponse> listActive(List<String> templateCodes) {
        var templates = (templateCodes == null || templateCodes.isEmpty())
                ? templateRepository.findAllByActiveTrue()
                : templateRepository.findAllByActiveTrueAndTemplateCodeIn(templateCodes);
        return templates.stream()
                .map(FormTemplateResponse::from)
                .toList();
    }

    public FormTemplateDetailResponse getDetail(String templateCode) {
        var template = templateRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> new NotFoundException("템플릿을 찾을 수 없습니다: " + templateCode));

        List<SectionResponse> sections = sectionRepository
                .findAllByTemplateIdOrderByOrderNo(template.getId()).stream()
                .map(section -> {
                    List<QuestionResponse> questions = questionRepository
                            .findAllBySectionIdOrderByOrderNo(section.getId()).stream()
                            .map(QuestionResponse::from)
                            .toList();
                    return SectionResponse.from(section, questions);
                })
                .toList();

        return FormTemplateDetailResponse.from(template, sections);
    }

    public PermitDashboardResponse getDashboard(String templateCode) {
        var template = templateRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> new NotFoundException("템플릿을 찾을 수 없습니다: " + templateCode));

        var timeline = timelineTemplateRepository.findByTemplate_TemplateCodeOrderByOrderNoAsc(templateCode);
        var checklist = checklistItemTemplateRepository.findByTemplate_TemplateCodeOrderByOrderNoAsc(templateCode);

        return PermitDashboardResponse.of(template, timeline, checklist);
    }
}
