package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.response.AreaTaskSubtaskResponseDto;
import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskSubtask;

/**
 * Conversión entidad → DTO de subtarea (misma forma en servicio de subtareas y en conversión desde tarea padre).
 */
final class AreaTaskSubtaskDtoMapper {

    private AreaTaskSubtaskDtoMapper() {}

    static AreaTaskSubtaskResponseDto toDto(AreaTaskSubtask s, AreaTask parent) {
        Long aid = s.getAreaId() != null ? s.getAreaId() : parent.getAreaId();
        Long ws = s.getWorkspaceId() != null ? s.getWorkspaceId() : parent.getWorkspaceId();
        return AreaTaskSubtaskResponseDto.builder()
                .id(s.getId())
                .parentTaskId(s.getParentTaskId())
                .title(s.getTitle())
                .description(s.getDescription())
                .sortOrder(s.getSortOrder())
                .done(s.getDone())
                .weight(s.getWeight())
                .assignedUserId(s.getAssignedUserId())
                .dueDate(s.getDueDate())
                .createdByUserId(s.getCreatedByUserId())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .areaId(aid)
                .workspaceId(ws)
                .build();
    }
}
