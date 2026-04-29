package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskPriority;

/**
 * Filtros opcionales al cargar tareas WorkOS/Gantt (misma consulta acotada en BD que antes en {@code summary}).
 */
public record AreaTaskListParams(
        Long areaId,
        Long workspaceId,
        AreaTaskPriority priority,
        Long ownerUserId
) {
}
