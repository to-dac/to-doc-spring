package com.todoc.controller;

import com.todoc.dto.request.SendChatMessageRequest;
import com.todoc.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/permit")
@RequiredArgsConstructor
public class PermitChatController {

    private final ChatService chatService;

    public record PermitChatRequest(String message, String thread_id) {}

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody PermitChatRequest request) {
        Long sessionId = Long.parseLong(request.thread_id());
        return chatService.sendStreamingMessage(sessionId, new SendChatMessageRequest(request.message()));
    }
}
