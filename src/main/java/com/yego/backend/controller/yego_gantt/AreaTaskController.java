package com.yego.backend.controller.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.CreateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskKpisResponseDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskResponseDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTasksSummaryResponseDto;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskPriority;
import com.yego.backend.service.yego_gantt.AreaTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@RestController
@RequestMapping(value = "/api/yego-gantt/tasks")
@RequiredArgsConstructor
public class AreaTaskController {

    private final AreaTaskService areaTaskService;

    @GetMapping("/kpis")
    public ResponseEntity<AreaTaskKpisResponseDto> kpis(Authentication authentication,
                                                        @RequestParam(required = false) Long areaId,
                                                        @RequestParam(required = false) Long projectId,
                                                        @RequestParam(required = false) AreaTaskPriority priority) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(areaTaskService.kpis(userId, areaId, projectId, priority));
    }

    @GetMapping("/summary")
    public ResponseEntity<AreaTasksSummaryResponseDto> summary(Authentication authentication,
                                                               @RequestParam(required = false) Long areaId,
                                                               @RequestParam(required = false) Long projectId,
                                                               @RequestParam(required = false) AreaTaskPriority priority) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(areaTaskService.summary(userId, areaId, projectId, priority));
    }

    @GetMapping
    public ResponseEntity<List<AreaTaskResponseDto>> list(Authentication authentication,
                                                          @RequestParam(required = false) Long areaId,
                                                          @RequestParam(required = false) Long projectId,
                                                          @RequestParam(required = false) AreaTaskPriority priority) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(areaTaskService.list(userId, areaId, projectId, priority));
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
}
