package com.yego.backend.service.yego_gantt;

/**
 * ADMIN/SUPERADMIN o gestor de al menos un área (portfolio WorkOS/Gantt).
 */
public interface GanttPortfolioAuthorizationService {

    void requirePortfolioManager(Long requesterId, String forbiddenMessage);

    /** Solo ADMIN / SUPERADMIN (p. ej. eliminar sprints). */
    void requirePlatformAdmin(Long requesterId, String forbiddenMessage);
}
