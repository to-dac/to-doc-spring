package com.todoc.controller;

import com.todoc.dto.response.FormTemplateDetailResponse;
import com.todoc.service.FormTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/form-templates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FormTemplateController {

    private final FormTemplateService formTemplateService;

    @GetMapping("/{templateCode}")
    public FormTemplateDetailResponse getDetail(@PathVariable String templateCode) {
        return formTemplateService.getDetail(templateCode);
    }
}
