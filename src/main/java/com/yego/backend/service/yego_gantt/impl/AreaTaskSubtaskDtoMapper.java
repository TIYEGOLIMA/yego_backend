package com.yego.backend.service.yego_gantt.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_gantt.api.AreaTaskSubtaskChecklistItemDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskSubtaskResponseDto;
import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskSubtask;
import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskStatus;

import java.util.Collections;
import java.util.List;

/**
 * Conversión entidad → DTO de subtarea (misma forma en servicio de subtareas y en conversión desde tarea padre).
 */
final class AreaTaskSubtaskDtoMapper {

    private static final TypeReference<List<AreaTaskSubtaskChecklistItemDto>> CHECKLIST_TYPEREF =
            new TypeReference<>() {};

    private AreaTaskSubtaskDtoMapper() {}

    static List<AreaTaskSubtaskChecklistItemDto> readChecklist(String json, ObjectMapper om) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<AreaTaskSubtaskChecklistItemDto> list = om.readValue(json, CHECKLIST_TYPEREF);
            return list != null ? list : Collections.emptyList();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    static AreaTaskSubtaskResponseDto toDto(AreaTaskSubtask s, AreaTask parent, ObjectMapper om) {
        Long aid = s.getAreaId() != null ? s.getAreaId() : parent.getAreaId();
        Long ws = s.getWorkspaceId() != null ? s.getWorkspaceId() : parent.getWorkspaceId();
        List<AreaTaskSubtaskChecklistItemDto> checklist = readChecklist(s.getChecklistJson(), om);
        AreaTaskStatus st = s.getKanbanStatus() != null ? s.getKanbanStatus() : AreaTaskStatus.PENDING;
        return AreaTaskSubtaskResponseDto.builder()
                .id(s.getId())
                .parentTaskId(s.getParentTaskId())
                .title(s.getTitle())
                .description(s.getDescription())
                .objectives(s.getObjectives())
                .sortOrder(s.getSortOrder())
                .status(st)
                .done(s.getDone())
                .weight(s.getWeight())
                .assignedUserId(s.getAssignedUserId())
                .dueDate(s.getDueDate())
                .createdByUserId(s.getCreatedByUserId())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .areaId(aid)
                .workspaceId(ws)
                .checklist(checklist)
                .build();
    }
}
