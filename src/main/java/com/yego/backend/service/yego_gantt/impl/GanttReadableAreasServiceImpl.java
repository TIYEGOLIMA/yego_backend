package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_principal.entities.Area;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_principal.AreaRepository;
import com.yego.backend.service.yego_gantt.GanttReadableAreasService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GanttReadableAreasServiceImpl implements GanttReadableAreasService {

    private final AreaRepository areaRepository;

    @Override
    public boolean isPlatformAdmin(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String name = user.getRole().getName();
        return "ADMIN".equalsIgnoreCase(name) || "SUPERADMIN".equalsIgnoreCase(name);
    }

    @Override
    public boolean canOperateAllAreasInGantt(User user) {
        return isPlatformAdmin(user) || GanttPortfolioAuthorizationServiceImpl.canManageWorkspacesByRole(user);
    }

    @Override
    public Set<Long> readableAreaIdsForUser(User user) {
        if (user == null) {
            return Set.of();
        }
        // Gantt/colaboración: cualquier usuario con acceso puede ver y operar tareas por equipo
        // en todas las áreas activas; el equipo de la persona sigue mostrándose en la UI.
        List<Area> activas = areaRepository.findAllActivas();
        if (activas != null && !activas.isEmpty()) {
            return activas.stream().map(Area::getId).collect(Collectors.toSet());
        }
        return Set.of();
    }
}
