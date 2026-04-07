package com.yego.backend.controller.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.CreateProjectDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateProjectDto;
import com.yego.backend.entity.yego_gantt.api.response.ProjectResponseDto;
import com.yego.backend.service.yego_gantt.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(value = "/api/yego-gantt/projects", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponseDto> create(@Valid @RequestBody CreateProjectDto dto) {
        return ResponseEntity.status(201).body(projectService.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponseDto>> findAllActive() {
        return ResponseEntity.ok(projectService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDto> findOne(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.findOne(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDto> update(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateProjectDto dto) {
        return ResponseEntity.ok(projectService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.ok().build();
    }
}
