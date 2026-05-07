package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_principal.entities.User;

import java.util.Set;

/**
 * Ámbito ADMIN/SUPERADMIN y áreas legibles por usuario para WorkOS/Gantt.
 */
public interface GanttReadableAreasService {

    boolean isPlatformAdmin(User user);

    /** Admin/SUPERVISOR y variantes: alcance todas las áreas para operaciones WorkOS/Gantt (igual que {@code ganttHasFullTabAccess} en front). */
    boolean canOperateAllAreasInGantt(User user);

    /** Jefe de al menos un área → esas áreas; si no, el área del usuario colaborador. */
    Set<Long> readableAreaIdsForUser(User user);
}
