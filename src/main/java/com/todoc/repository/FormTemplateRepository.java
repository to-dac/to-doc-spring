package com.todoc.repository;

import com.todoc.domain.FormTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FormTemplateRepository extends JpaRepository<FormTemplate, Long> {
    Optional<FormTemplate> findByTemplateCode(String templateCode);
    List<FormTemplate> findAllByActiveTrue();
    List<FormTemplate> findAllByActiveTrueAndTemplateCodeIn(List<String> templateCodes);
    boolean existsByTemplateCode(String templateCode);
}
