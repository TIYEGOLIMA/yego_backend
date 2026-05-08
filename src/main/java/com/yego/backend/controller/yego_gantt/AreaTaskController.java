package com.yego.backend.controller.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.CreateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.request.ConvertTaskToSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskResponseDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskSubtaskResponseDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTasksSummaryResponseDto;
import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskPriority;
import com.yego.backend.service.yego_gantt.AreaTaskListParams;
import com.yego.backend.service.yego_gantt.AreaTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/yego-gantt/tasks",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@RequiredArgsConstructor
public class AreaTaskController {

    private final AreaTaskService areaTaskService;

    /**
     * Resumen de tareas. {@code mySpace=true}: lista agregada del usuario (sin workspace propias + privadas en proyectos).
     * Si {@code mySpace=true}, se ignora {@code onlyWithoutWorkspace} y el filtro por {@code workspaceId} concreto.
     */
    @GetMapping("/summary")
    public ResponseEntity<AreaTasksSummaryResponseDto> summary(Authentication authentication,
                                                               @RequestParam(required = false) Long areaId,
                                                               @RequestParam(required = false) Long workspaceId,
                                                               @RequestParam(required = false) Long ownerUserId,
                                                               @RequestParam(required = false) AreaTaskPriority priority,
                                                               @RequestParam(required = false) Boolean onlyWithoutWorkspace,
                                                               @RequestParam(required = false) Boolean mySpace) {
        Long userId = Long.parseLong(authentication.getName());
        boolean mySpaceFlag = Boolean.TRUE.equals(mySpace);
        boolean onlyNoWs = !mySpaceFlag && Boolean.TRUE.equals(onlyWithoutWorkspace);
        return ResponseEntity.ok(areaTaskService.summary(
                userId,
                new AreaTaskListParams(areaId, workspaceId, priority, ownerUserId, onlyNoWs, mySpaceFlag)));
    }

    @PostMapping
    public ResponseEntity<AreaTaskResponseDto> create(Authentication authentication,
                                                      @Valid @RequestBody CreateAreaTaskDto dto) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(201).body(areaTaskService.create(userId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AreaTaskResponseDto> update(Authentication authentication,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody UpdateAreaTaskDto dto) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(areaTaskService.update(userId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        Long userId = Long.parseLong(authentication.getName());
        areaTaskService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/convert-to-subtask")
    public ResponseEntity<AreaTaskSubtaskResponseDto> convertToSubtask(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ConvertTaskToSubtaskDto dto) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(areaTaskService.convertTaskToSubtask(userId, id, dto));
    }
}
