package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_principal.entities.User;

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

    /**
     * Listado completo de espacios activos para UI (selector, combos); no concede alta/edición de proyectos.
     */
    boolean readsFullWorkspaceCatalog(User user);
}
