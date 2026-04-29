package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_principal.AreaRepository;

import java.util.Set;

/**
 * Ámbito de áreas para operaciones sobre tareas WorkOS/Gantt (lectura y gestión comparten el mismo conjunto).
 */
public record GanttTaskScope(boolean allAreas, Set<Long> areaIds) {

    public static GanttTaskScope resolve(User user, AreaRepository areaRepository) {
        if (GanttReadableAreas.isPlatformAdmin(user)) {
            return new GanttTaskScope(true, Set.of());
        }
        Set<Long> ids = GanttReadableAreas.forUser(user, areaRepository);
        if (ids.isEmpty()) {
            return new GanttTaskScope(false, Set.of());
        }
        return new GanttTaskScope(false, ids);
    }

    public boolean canAccessArea(long areaId) {
        return allAreas || areaIds.contains(areaId);
    }

    /** Si no hay alcance global, {@code areaId} nulo no es accesible. */
    public boolean canAccessArea(Long areaId) {
        if (allAreas) {
            return true;
        }
        return areaId != null && areaIds.contains(areaId);
    }
}
