package com.yego.backend.controller.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.CreateAreaTaskSubtaskDto;
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

    @GetMapping
    public ResponseEntity<List<AreaTaskSubtaskResponseDto>> list(Authentication authentication,
                                                                  @PathVariable Long taskId) {
        return ResponseEntity.ok(subtaskService.list(Long.parseLong(authentication.getName()), taskId));
    }

    @PostMapping
    public ResponseEntity<AreaTaskSubtaskResponseDto> create(Authentication authentication,
                                                          @PathVariable Long taskId,
                                                          @Valid @RequestBody CreateAreaTaskSubtaskDto dto) {
        return ResponseEntity.status(201).body(subtaskService.create(Long.parseLong(authentication.getName()), taskId, dto));
    }

    @PutMapping("/{subtaskId}")
    public ResponseEntity<AreaTaskSubtaskResponseDto> update(Authentication authentication,
                                                            @PathVariable Long taskId,
                                                            @PathVariable Long subtaskId,
                                                            @Valid @RequestBody UpdateAreaTaskSubtaskDto dto) {
        return ResponseEntity.ok(subtaskService.update(Long.parseLong(authentication.getName()), taskId, subtaskId, dto));
    }

    @DeleteMapping("/{subtaskId}")
    public ResponseEntity<Void> delete(Authentication authentication,
                                      @PathVariable Long taskId,
                                      @PathVariable Long subtaskId) {
        subtaskService.delete(Long.parseLong(authentication.getName()), taskId, subtaskId);
        return ResponseEntity.noContent().build();
    }
}
