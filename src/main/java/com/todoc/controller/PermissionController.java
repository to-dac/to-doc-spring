package com.todoc.controller;

import com.todoc.dto.request.UpdatePermissionRequest;
import com.todoc.dto.response.PermissionResponse;
import com.todoc.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public List<PermissionResponse> getPermission(@RequestParam Long sessionId) {
        return permissionService.getBySessionId(sessionId);
    }

    @PatchMapping("/{sessionId}")
    public PermissionResponse updateTemplateId(
            @PathVariable Long sessionId,
            @RequestBody UpdatePermissionRequest request) {
        return permissionService.updateTemplateId(sessionId, request.templateId());
    }
}
