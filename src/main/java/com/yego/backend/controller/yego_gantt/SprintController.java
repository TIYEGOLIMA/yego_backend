package com.yego.backend.controller.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.CreateSprintDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateSprintDto;
import com.yego.backend.entity.yego_gantt.api.response.SprintResponseDto;
import com.yego.backend.service.yego_gantt.SprintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/yego-gantt/sprints", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    @PostMapping
    public ResponseEntity<SprintResponseDto> create(Authentication authentication,
                                                    @Valid @RequestBody CreateSprintDto dto) {
        return ResponseEntity.status(201).body(sprintService.create(GanttControllerAuth.userId(authentication), dto));
    }

    @GetMapping("/by-workspace/{workspaceId}")
    public ResponseEntity<List<SprintResponseDto>> findByWorkspace(
            @PathVariable Long workspaceId,
            @RequestParam(required = false, defaultValue = "false") boolean assignableOnly) {
        return ResponseEntity.ok(sprintService.findByWorkspace(workspaceId, assignableOnly));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SprintResponseDto> update(Authentication authentication,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody UpdateSprintDto dto) {
        return ResponseEntity.ok(sprintService.update(GanttControllerAuth.userId(authentication), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        sprintService.delete(GanttControllerAuth.userId(authentication), id);
        return ResponseEntity.noContent().build();
    }
}
