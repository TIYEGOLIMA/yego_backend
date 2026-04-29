package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_principal.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Visibilidad de tareas marcadas como privadas: solo el creador o administrador de plataforma.
 */
public final class AreaTaskPrivateAccess {

    private AreaTaskPrivateAccess() {
    }

    /** Tras comprobar acceso por área: ¿puede el usuario ver el contenido de esta tarea? */
    public static boolean canSeeTaskContent(User viewer, AreaTask task) {
        if (!task.isPrivateTask()) {
            return true;
        }
        if (GanttReadableAreas.isPlatformAdmin(viewer)) {
            return true;
        }
        return task.getCreatedByUserId() != null && task.getCreatedByUserId().equals(viewer.getId());
    }

    /** Solo creador o admin de plataforma pueden modificar una tarea privada. */
    public static void assertCanMutatePrivateTask(User user, AreaTask task) {
        if (!task.isPrivateTask()) {
            return;
        }
        if (GanttReadableAreas.isPlatformAdmin(user)) {
            return;
        }
        if (task.getCreatedByUserId() != null && task.getCreatedByUserId().equals(user.getId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el creador puede modificar esta tarea privada");
    }
}
