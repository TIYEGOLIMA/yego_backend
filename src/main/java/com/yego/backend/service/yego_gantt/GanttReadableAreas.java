package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_principal.entities.Area;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_principal.AreaRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ámbito de áreas para WorkOS/Gantt: mismo criterio en listado de proyectos y tareas.
 */
public final class GanttReadableAreas {

    private GanttReadableAreas() {
    }

    public static boolean isPlatformAdmin(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String name = user.getRole().getName();
        return "ADMIN".equalsIgnoreCase(name) || "SUPERADMIN".equalsIgnoreCase(name);
    }

    /** Jefe de al menos un área → esas áreas; si no, el área del usuario colaborador. */
    public static Set<Long> forUser(User user, AreaRepository areaRepository) {
        if (user == null) {
            return Set.of();
        }
        List<Area> managed = areaRepository.findByManagerId(user.getId());
        if (managed != null && !managed.isEmpty()) {
            return managed.stream().map(Area::getId).collect(Collectors.toSet());
        }
        if (user.getAreaId() != null) {
            return Set.of(user.getAreaId());
        }
        return Set.of();
    }
}
