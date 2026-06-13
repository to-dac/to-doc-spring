package com.todoc.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todoc.domain.FormQuestion;
import com.todoc.domain.FormSection;
import com.todoc.domain.FormTemplate;
import com.todoc.domain.User;
import com.todoc.domain.enums.QuestionType;
import com.todoc.repository.FormQuestionRepository;
import com.todoc.repository.FormSectionRepository;
import com.todoc.repository.FormTemplateRepository;
import com.todoc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final FormTemplateRepository templateRepository;
    private final FormSectionRepository sectionRepository;
    private final FormQuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        if (!userRepository.existsByEmail("test@test.com")) {
            userRepository.save(User.builder()
                    .email("test@test.com")
                    .userName("테스트 유저")
                    .password(passwordEncoder.encode("test1234"))
                    .build());
            log.info("Default test user seeded successfully.");
        }

        if (templateRepository.existsByTemplateCode("building_major_repair_use_change_permit")) {
            log.info("Building permit template already seeded, skipping.");
            return;
        }
        try {
            var resource = new ClassPathResource("data/building_permit_template.json");
            JsonNode root = objectMapper.readTree(resource.getInputStream());

            FormTemplate template = templateRepository.save(FormTemplate.builder()
                    .templateCode(root.get("templateCode").asText())
                    .name(root.get("name").asText())
                    .description(root.get("description").asText())
                    .version(root.get("version").asText())
                    .metadata(objectMapper.writeValueAsString(root.get("metadata")))
                    .build());

            for (JsonNode sNode : root.get("sections")) {
                FormSection section = sectionRepository.save(FormSection.builder()
                        .template(template)
                        .sectionCode(sNode.get("sectionCode").asText())
                        .name(sNode.get("name").asText())
                        .orderNo(sNode.get("orderNo").asInt())
                        .metadata("{}")
                        .build());

                List<FormQuestion> questions = new ArrayList<>();
                for (JsonNode qNode : sNode.get("questions")) {
                    questions.add(FormQuestion.builder()
                            .section(section)
                            .sectionName(section.getName())
                            .questionType(QuestionType.valueOf(qNode.get("questionType").asText()))
                            .name(qNode.get("name").asText())
                            .description(textOrNull(qNode, "description"))
                            .options(jsonOrNull(qNode, "options"))
                            .displayType(qNode.get("displayType").asText())
                            .validation(jsonOrNull(qNode, "validation"))
                            .subFields(jsonOrNull(qNode, "subFields"))
                            .layoutKey(qNode.get("layoutKey").asText())
                            .orderNo(qNode.get("orderNo").asInt())
                            .metadata(objectMapper.writeValueAsString(qNode.get("metadata")))
                            .build());
                }
                questionRepository.saveAll(questions);
            }
            log.info("Building permit template seeded successfully.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to seed building permit template", e);
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n == null || n.isNull()) ? null : n.asText();
    }

    private String jsonOrNull(JsonNode node, String field) throws JsonProcessingException {
        JsonNode n = node.get(field);
        return (n == null || n.isNull()) ? null : objectMapper.writeValueAsString(n);
    }
}
