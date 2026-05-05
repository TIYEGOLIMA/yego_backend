package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_principal.entities.User;

/**
 * Visibilidad por ámbito y asignación para tareas WorkOS/Gantt.
 */
public interface AreaTaskVisibilityService {

    boolean isAssignee(Long userId, AreaTask task);

    /** Lectura de tarea: admin global, o área permitida, o asignado (privacidad revisada aparte). */
    boolean canReadTaskByScopeAndAssignment(User user, GanttTaskScope scope, AreaTask task);
}
