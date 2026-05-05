package com.yego.backend.controller.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.WorkosTaskMessageCreateRequest;
import com.yego.backend.entity.yego_gantt.api.request.WorkosTaskMessageUpdateRequest;
import com.yego.backend.entity.yego_gantt.api.response.WorkosTaskMessageResponseDto;
import com.yego.backend.service.yego_gantt.WorkosTaskMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        value = "/api/yego-gantt/tasks/{taskId}/messages",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@RequiredArgsConstructor
public class WorkosTaskMessageController {

    private final WorkosTaskMessageService workosTaskMessageService;

    @GetMapping
    public ResponseEntity<List<WorkosTaskMessageResponseDto>> list(
            Authentication authentication,
            @PathVariable Long taskId,
            @RequestParam(required = false) Long subtaskId) {
        long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(workosTaskMessageService.list(userId, taskId, subtaskId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<WorkosTaskMessageResponseDto> create(
            Authentication authentication,
            @PathVariable Long taskId,
            @Valid @RequestBody WorkosTaskMessageCreateRequest request) {
        long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workosTaskMessageService.create(userId, taskId, request));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long taskId,
            @PathVariable Long messageId) {
        long userId = Long.parseLong(authentication.getName());
        workosTaskMessageService.softDelete(userId, taskId, messageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{messageId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkosTaskMessageResponseDto> update(
            Authentication authentication,
            @PathVariable Long taskId,
            @PathVariable Long messageId,
            @Valid @RequestBody WorkosTaskMessageUpdateRequest request) {
        long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(workosTaskMessageService.update(userId, taskId, messageId, request));
    }
}
