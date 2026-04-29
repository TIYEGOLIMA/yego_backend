package com.yego.backend.controller.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.response.AreaTaskAttachmentResponseDto;
import com.yego.backend.service.yego_gantt.AreaTaskAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(value = "/api/yego-gantt/tasks/{taskId}/attachments")
@RequiredArgsConstructor
public class AreaTaskAttachmentController {

    private final AreaTaskAttachmentService attachmentService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<List<AreaTaskAttachmentResponseDto>> list(Authentication authentication,
                                                                   @PathVariable Long taskId) {
        return ResponseEntity.ok(attachmentService.list(Long.parseLong(authentication.getName()), taskId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<AreaTaskAttachmentResponseDto> upload(Authentication authentication,
                                                                  @PathVariable Long taskId,
                                                                  @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(201).body(attachmentService.upload(Long.parseLong(authentication.getName()), taskId, file));
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(Authentication authentication,
                                         @PathVariable Long taskId,
                                         @PathVariable Long attachmentId) {
        attachmentService.delete(Long.parseLong(authentication.getName()), taskId, attachmentId);
        return ResponseEntity.noContent().build();
    }
}
