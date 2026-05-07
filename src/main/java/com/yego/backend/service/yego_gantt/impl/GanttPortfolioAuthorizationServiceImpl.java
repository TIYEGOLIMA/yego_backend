package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_principal.AreaRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_gantt.GanttPortfolioAuthorizationService;
import com.yego.backend.service.yego_gantt.GanttReadableAreasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GanttPortfolioAuthorizationServiceImpl implements GanttPortfolioAuthorizationService {

    private final UserRepository userRepository;
    private final AreaRepository areaRepository;
    private final GanttReadableAreasService ganttReadableAreasService;

    @Override
    public void requirePortfolioManager(Long requesterId, String forbiddenMessage) {
        User user = userRepository.findByIdWithRole(requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (ganttReadableAreasService.isPlatformAdmin(user)) {
            return;
        }
        if (!areaRepository.findByManagerId(user.getId()).isEmpty()) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
    }

    @Override
    public void requireWorkspacePrivileged(Long requesterId, String forbiddenMessage) {
        User user = userRepository.findByIdWithRole(requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (canManageWorkspacesByRole(user)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
    }

    /**
     * Alineado con WorkOS/front: ADMIN, SUPERADMIN, SUPERVISOR, SUPERVISOR_LEAD, y roles tipo supervisor de lealtad.
     */
    public static boolean canManageWorkspacesByRole(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String raw = user.getRole().getName();
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String r = raw.toUpperCase(Locale.ROOT).trim().replaceAll("[\\s-]+", "_");
        if ("ADMIN".equals(r) || "SUPERADMIN".equals(r)) {
            return true;
        }
        if ("SUPERVISOR".equals(r) || "SUPERVISOR_LEAD".equals(r)) {
            return true;
        }
        return r.contains("SUPERVISOR") && r.contains("LEALTAD");
    }

    @Override
    public void requirePlatformAdmin(Long requesterId, String forbiddenMessage) {
        User user = userRepository.findByIdWithRole(requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (ganttReadableAreasService.isPlatformAdmin(user)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
    }

    @Override
    public boolean readsFullWorkspaceCatalog(User user) {
        return ganttReadableAreasService.isPlatformAdmin(user) || canManageWorkspacesByRole(user);
    }
}
