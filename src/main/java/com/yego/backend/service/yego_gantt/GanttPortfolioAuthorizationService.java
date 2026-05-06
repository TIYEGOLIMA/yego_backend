package com.yego.backend.service.yego_gantt;

/**
 * ADMIN/SUPERADMIN o gestor de al menos un área (portfolio WorkOS/Gantt).
 */
public interface GanttPortfolioAuthorizationService {

    void requirePortfolioManager(Long requesterId, String forbiddenMessage);

    /**
     * Solo roles operativos pueden crear/editar/eliminar espacios de trabajo (tabla {@code gantt_projects}):
     * ADMIN, SUPERADMIN, SUPERVISOR, SUPERVISOR_LEAD y variantes con SUPERVISOR + LEALTAD en el nombre del rol.
     */
    void requireWorkspacePrivileged(Long requesterId, String forbiddenMessage);

    /** Solo ADMIN / SUPERADMIN (p. ej. eliminar sprints). */
    void requirePlatformAdmin(Long requesterId, String forbiddenMessage);
}
