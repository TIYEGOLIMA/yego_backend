package com.yego.backend.service.yego_gantt;

import java.util.Set;

/**
 * Ámbito de áreas para WorkOS/Gantt: admin ve todo; gestor/colaborador solo sus áreas legibles.
 */
public record GanttTaskScope(boolean allAreas, Set<Long> areaIds) {

    /** Si no hay alcance global, {@code areaId} nulo no es accesible. */
    public boolean canAccessArea(Long areaId) {
        if (allAreas) {
            return true;
        }
        return areaId != null && areaIds.contains(areaId);
    }
}
