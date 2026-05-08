package com.yego.backend.controller.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.*;
import com.yego.backend.entity.yego_gantt.api.response.*;
import com.yego.backend.entity.yego_gantt.entities.enums.MeetingMinuteStatus;
import com.yego.backend.entity.yego_gantt.entities.enums.MeetingMinuteType;
import com.yego.backend.service.yego_gantt.MeetingMinuteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(value = "/api/yego-gantt/meeting-minutes", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@RequiredArgsConstructor
public class MeetingMinuteController {

    private final MeetingMinuteService meetingMinuteService;

    @GetMapping
    public ResponseEntity<Page<MeetingMinuteResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) MeetingMinuteStatus status,
            @RequestParam(required = false) MeetingMinuteType meetingType,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) Long ownerUserId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long areaId,
            @PageableDefault(size = 20, sort = "meetingDate", direction = Sort.Direction.DESC) Pageable pageable) {
        long uid = GanttControllerAuth.userId(authentication);
        return ResponseEntity.ok(meetingMinuteService.list(
                uid, status, meetingType, dateFrom, dateTo, ownerUserId, projectId, areaId, pageable));
    }

    @GetMapping("/dashboard-kpis")
    public ResponseEntity<MeetingMinutesDashboardKpisResponse> dashboardKpis(Authentication authentication) {
        long uid = GanttControllerAuth.userId(authentication);
        return ResponseEntity.ok(meetingMinuteService.dashboardKpis(uid));
    }

    @GetMapping("/unconverted-items")
    public ResponseEntity<List<MeetingMinuteItemResponse>> unconverted(Authentication authentication) {
        long uid = GanttControllerAuth.userId(authentication);
        return ResponseEntity.ok(meetingMinuteService.listUnconverted(uid));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeetingMinuteResponse> getById(Authentication authentication, @PathVariable Long id) {
        long uid = GanttControllerAuth.userId(authentication);
        return ResponseEntity.ok(meetingMinuteService.getById(uid, id));
    }

    @PostMapping
    public ResponseEntity<MeetingMinuteResponse> create(Authentication authentication,
                                                        @Valid @RequestBody CreateMeetingMinuteRequest req) {
        long uid = GanttControllerAuth.userId(authentication);
        return ResponseEntity.status(201).body(meetingMinuteService.create(uid, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MeetingMinuteResponse> update(Authentication authentication,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody UpdateMeetingMinuteRequest req) {
        long uid = GanttControllerAuth.userId(authentication);
        return ResponseEntity.ok(meetingMinuteService.update(uid, id, req));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> patchStatus(Authentication authentication,
                                            @PathVariable Long id,
                                            @Valid @RequestBody PatchMeetingMinuteStatusRequest req) {
        long uid = GanttControllerAuth.userId(authentication);
        meetingMinuteService.patchStatus(uid, id, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(Authentication authentication, @PathVariable Long id) {
        long uid = GanttControllerAuth.userId(authentication);
        meetingMinuteService.softDelete(uid, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<MeetingMinuteResponse> addItems(Authentication authentication,
                                                          @PathVariable Long id,
                                                          @Valid @RequestBody List<CreateMeetingMinuteItemRequest> items) {
        long uid = GanttControllerAuth.userId(authentication);
        return ResponseEntity.status(201).body(meetingMinuteService.addItems(uid, id, items));
    }

    @PutMapping("/{id}/items/{itemId}")
    public ResponseEntity<MeetingMinuteResponse> updateItem(Authentication authentication,
                                                            @PathVariable Long id,
                                                            @PathVariable Long itemId,
                                                            @Valid @RequestBody UpdateMeetingMinuteItemRequest req) {
        long uid = GanttControllerAuth.userId(authentication);
        return ResponseEntity.ok(meetingMinuteService.updateItem(uid, id, itemId, req));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(Authentication authentication,
                                           @PathVariable Long id,
                                           @PathVariable Long itemId) {
        long uid = GanttControllerAuth.userId(authentication);
        meetingMinuteService.deleteItem(uid, id, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/items/{itemId}/convert-to-task")
    public ResponseEntity<ConvertMeetingItemToTaskResponse> convertToTask(Authentication authentication,
                                                                          @PathVariable Long id,
                                                                          @PathVariable Long itemId,
                                                                          @Valid @RequestBody ConvertMeetingItemToTaskRequest req) {
        long uid = GanttControllerAuth.userId(authentication);
        return ResponseEntity.ok(meetingMinuteService.convertItemToTask(uid, id, itemId, req));
    }
}
