package com.todoc.service;

import com.todoc.domain.Attachment;
import com.todoc.domain.ChatMessage;
import com.todoc.dto.response.AttachmentDetailResponse;
import com.todoc.exception.NotFoundException;
import com.todoc.repository.AttachmentRepository;
import com.todoc.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Transactional
    public AttachmentDetailResponse upload(Long messageId, MultipartFile file) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("메시지를 찾을 수 없습니다: id=" + messageId));

        Long userId = message.getSession().getUser().getId();
        Long sessionId = message.getSession().getId();

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;
        String relativePath = userId + "/" + sessionId + "/" + storedFilename;

        Path targetDir = Paths.get(uploadDir, String.valueOf(userId), String.valueOf(sessionId));
        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetDir.resolve(storedFilename));
        } catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }

        Attachment attachment = Attachment.builder()
                .message(message)
                .name(originalFilename)
                .url(relativePath)
                .build();

        return AttachmentDetailResponse.from(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public AttachmentDetailResponse findById(Long id) {
        return attachmentRepository.findById(id)
                .map(AttachmentDetailResponse::from)
                .orElseThrow(() -> new NotFoundException("첨부파일을 찾을 수 없습니다: id=" + id));
    }

    @Transactional(readOnly = true)
    public Resource loadFile(Long id) {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("첨부파일을 찾을 수 없습니다: id=" + id));

        Path filePath = Paths.get(uploadDir).resolve(attachment.getUrl()).normalize();
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new NotFoundException("파일이 존재하지 않습니다: id=" + id);
            }
            return resource;
        } catch (IOException e) {
            throw new RuntimeException("파일 로드에 실패했습니다.", e);
        }
    }
}
