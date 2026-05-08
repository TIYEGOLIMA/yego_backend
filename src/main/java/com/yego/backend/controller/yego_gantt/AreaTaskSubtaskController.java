package com.yego.backend.controller.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.CreateAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.request.MoveAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.request.ReorderAreaTaskSubtasksDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskSubtaskResponseDto;
import com.yego.backend.service.yego_gantt.AreaTaskSubtaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/yego-gantt/tasks/{taskId}/subtasks", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@RequiredArgsConstructor
public class AreaTaskSubtaskController {

    private final AreaTaskSubtaskService subtaskService;

    private static long requesterId(Authentication authentication) {
        return GanttControllerAuth.userId(authentication);
    }

    @GetMapping
    public ResponseEntity<List<AreaTaskSubtaskResponseDto>> list(Authentication authentication,
                                                                  @PathVariable Long taskId) {
        return ResponseEntity.ok(subtaskService.list(requesterId(authentication), taskId));
    }

    @PutMapping(value = "/order", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AreaTaskSubtaskResponseDto>> reorder(
            Authentication authentication,
            @PathVariable Long taskId,
            @Valid @RequestBody ReorderAreaTaskSubtasksDto dto) {
        return ResponseEntity.ok(
                subtaskService.reorder(
                        requesterId(authentication),
                        taskId,
                        dto.getOrderedSubtaskIds()));
    }

    @GetMapping("/{subtaskId}")
    public ResponseEntity<AreaTaskSubtaskResponseDto> get(Authentication authentication,
                                                          @PathVariable Long taskId,
                                                          @PathVariable Long subtaskId) {
        return ResponseEntity.ok(
                subtaskService.get(requesterId(authentication), taskId, subtaskId));
    }

    @PostMapping
    public ResponseEntity<AreaTaskSubtaskResponseDto> create(Authentication authentication,
                                                          @PathVariable Long taskId,
                                                          @Valid @RequestBody CreateAreaTaskSubtaskDto dto) {
        return ResponseEntity.status(201).body(subtaskService.create(requesterId(authentication), taskId, dto));
    }

    @PutMapping("/{subtaskId}")
    public ResponseEntity<AreaTaskSubtaskResponseDto> update(Authentication authentication,
                                                            @PathVariable Long taskId,
                                                            @PathVariable Long subtaskId,
                                                            @Valid @RequestBody UpdateAreaTaskSubtaskDto dto) {
        return ResponseEntity.ok(subtaskService.update(requesterId(authentication), taskId, subtaskId, dto));
    }

    @DeleteMapping("/{subtaskId}")
    public ResponseEntity<Void> delete(Authentication authentication,
                                      @PathVariable Long taskId,
                                      @PathVariable Long subtaskId) {
        subtaskService.delete(requesterId(authentication), taskId, subtaskId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{subtaskId}/move")
    public ResponseEntity<AreaTaskSubtaskResponseDto> moveToParent(
            Authentication authentication,
            @PathVariable Long taskId,
            @PathVariable Long subtaskId,
            @Valid @RequestBody MoveAreaTaskSubtaskDto dto) {
        return ResponseEntity.ok(
                subtaskService.moveToParent(
                        requesterId(authentication),
                        taskId,
                        subtaskId,
                        dto));
    }
}
