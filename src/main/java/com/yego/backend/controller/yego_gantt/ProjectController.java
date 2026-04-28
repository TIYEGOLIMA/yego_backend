package com.yego.backend.controller.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.CreateProjectDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateProjectDto;
import com.yego.backend.entity.yego_gantt.api.response.ProjectResponseDto;
import com.yego.backend.service.yego_gantt.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/yego-gantt/projects", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponseDto> create(Authentication authentication,
                                                     @Valid @RequestBody CreateProjectDto dto) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(201).body(projectService.create(userId, dto));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponseDto>> findAllActive(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(projectService.findAllActiveForUser(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDto> update(Authentication authentication,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody UpdateProjectDto dto) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(projectService.update(userId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        Long userId = Long.parseLong(authentication.getName());
        projectService.delete(userId, id);
        return ResponseEntity.ok().build();
    }
}
