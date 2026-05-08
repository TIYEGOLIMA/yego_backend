package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.request.UpdateAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskSubtask;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Reglas puras de subtareas (sin acceso a BD): responsable principal del padre,
 * mutaciones del DTO distintas de {@code done}, y quién puede togglear hecho.
 */
final class AreaTaskSubtaskPolicies {

    private AreaTaskSubtaskPolicies() {}

    static Long principalAssigneeId(AreaTask parent) {
        List<Long> ids = parent.getAssignedUserIds() != null ? parent.getAssignedUserIds() : Collections.emptyList();
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        return parent.getAssignedUserId();
    }

    /** {@code true} si el DTO solicita cambiar algo más que solo el flag {@code done}. */
    static boolean updateTouchesNonDoneFields(UpdateAreaTaskSubtaskDto dto) {
        return dto.getTitle() != null
                || dto.getDescription() != null
                || dto.getWeight() != null
                || dto.getSortOrder() != null
                || dto.getAssignedUserId() != null
                || Boolean.TRUE.equals(dto.getUnassignUser())
                || dto.getDueDate() != null
                || Boolean.TRUE.equals(dto.getClearDueDate())
                || dto.getAreaId() != null
                || dto.getWorkspaceId() != null
                || Boolean.TRUE.equals(dto.getClearWorkspace());
    }

    /** Quién puede enviar un PATCH solo con {@code done} sin ser gestor del padre. */
    static boolean mayToggleDone(Long requesterUserId, AreaTask parent, AreaTaskSubtask subtask) {
        if (requesterUserId == null) {
            return false;
        }
        if (Objects.equals(subtask.getAssignedUserId(), requesterUserId)) {
            return true;
        }
        if (Objects.equals(subtask.getCreatedByUserId(), requesterUserId)) {
            return true;
        }
        if (Objects.equals(parent.getCreatedByUserId(), requesterUserId)) {
            return true;
        }
        return Objects.equals(principalAssigneeId(parent), requesterUserId);
    }
}
