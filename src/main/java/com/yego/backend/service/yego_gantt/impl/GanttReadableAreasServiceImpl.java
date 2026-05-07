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
