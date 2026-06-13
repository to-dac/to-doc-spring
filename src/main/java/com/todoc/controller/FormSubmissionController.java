package com.todoc.controller;

import com.todoc.dto.request.SubmitFormRequest;
import com.todoc.dto.response.FormSubmissionResponse;
import com.todoc.service.FormSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/form-submissions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FormSubmissionController {

    private final FormSubmissionService formSubmissionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FormSubmissionResponse submit(@RequestBody SubmitFormRequest request) {
        return formSubmissionService.submit(request);
    }

    @GetMapping("/{id}")
    public FormSubmissionResponse getById(@PathVariable Long id) {
        return formSubmissionService.getById(id);
    }

    @GetMapping
    public List<FormSubmissionResponse> listByUser(@RequestParam Long userId) {
        return formSubmissionService.listByUser(userId);
    }
}
