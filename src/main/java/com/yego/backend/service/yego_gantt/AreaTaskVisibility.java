package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_principal.entities.User;

import java.util.List;

/**
 * Visibilidad por ámbito y por asignación: un colaborador puede ver una tarea en su proyecto
 * aunque el {@code areaId} de la tarea no sea de sus áreas gestionadas.
 */
public final class AreaTaskVisibility {

    private AreaTaskVisibility() {
    }

    public static boolean isAssignee(Long userId, AreaTask task) {
        if (userId == null || task == null) {
            return false;
        }
        if (task.getAssignedUserId() != null && task.getAssignedUserId().equals(userId)) {
            return true;
        }
        List<Long> ids = task.getAssignedUserIds();
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (Long id : ids) {
            if (id != null && id.equals(userId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lectura de tarea: admin global, o área permitida, o asignado (con reglas de privacidad ya aplicadas aparte).
     */
    public static boolean canReadTaskByScopeAndAssignment(User user, GanttTaskScope scope, AreaTask task) {
        if (GanttReadableAreas.isPlatformAdmin(user)) {
            return true;
        }
        if (scope.canAccessArea(task.getAreaId())) {
            return true;
        }
        return isAssignee(user.getId(), task);
    }
}
