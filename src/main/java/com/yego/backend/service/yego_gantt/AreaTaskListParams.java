package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskPriority;

/**
 * Filtros opcionales al cargar tareas WorkOS/Gantt (misma consulta acotada en BD que antes en {@code summary}).
 *
 * @param mySpace vista agregada «Mi espacio»: sin workspace propias del visor + privadas con workspace creadas por el visor.
 */
public record AreaTaskListParams(
        Long areaId,
        Long workspaceId,
        AreaTaskPriority priority,
        Long ownerUserId,
        boolean onlyWithoutWorkspace,
        boolean mySpace
) {
    public AreaTaskListParams(Long areaId, Long workspaceId, AreaTaskPriority priority, Long ownerUserId,
                              boolean onlyWithoutWorkspace) {
        this(areaId, workspaceId, priority, ownerUserId, onlyWithoutWorkspace, false);
    }

    /** Compatibilidad: sin filtro «solo sin proyecto» ni Mi espacio. */
    public AreaTaskListParams(Long areaId, Long workspaceId, AreaTaskPriority priority, Long ownerUserId) {
        this(areaId, workspaceId, priority, ownerUserId, false, false);
    }
}
