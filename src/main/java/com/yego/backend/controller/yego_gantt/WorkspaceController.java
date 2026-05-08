package com.yego.backend.controller.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.CreateWorkspaceDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateWorkspaceDto;
import com.yego.backend.entity.yego_gantt.api.response.WorkspaceResponseDto;
import com.yego.backend.service.yego_gantt.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/yego-gantt/workspaces", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<WorkspaceResponseDto> create(Authentication authentication,
                                                       @Valid @RequestBody CreateWorkspaceDto dto) {
        return ResponseEntity.status(201).body(workspaceService.create(GanttControllerAuth.userId(authentication), dto));
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponseDto>> findAllActive(Authentication authentication) {
        return ResponseEntity.ok(workspaceService.findAllActiveForUser(GanttControllerAuth.userId(authentication)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceResponseDto> update(Authentication authentication,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody UpdateWorkspaceDto dto) {
        return ResponseEntity.ok(workspaceService.update(GanttControllerAuth.userId(authentication), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        workspaceService.delete(GanttControllerAuth.userId(authentication), id);
        return ResponseEntity.noContent().build();
    }
}
