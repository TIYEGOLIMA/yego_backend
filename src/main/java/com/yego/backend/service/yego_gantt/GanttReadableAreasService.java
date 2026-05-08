package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_principal.entities.User;

import java.util.Set;

/**
 * Ámbito ADMIN/SUPERADMIN y lectura de equipos activos para WorkOS/Gantt.
 * El alcance por usuario incluye todas las áreas activas (colaboración entre equipos).
 */
public interface GanttReadableAreasService {

    boolean isPlatformAdmin(User user);

    /** Admin/SUPERVISOR y variantes: alcance todas las áreas para operaciones WorkOS/Gantt (igual que {@code ganttHasFullTabAccess} en front). */
    boolean canOperateAllAreasInGantt(User user);

    /** Ids de áreas activas con visibilidad Gantt para el usuario autenticado (alcance equipos cargados). */
    Set<Long> readableAreaIdsForUser(User user);
}
