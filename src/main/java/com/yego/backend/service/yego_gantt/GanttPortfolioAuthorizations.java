package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_principal.AreaRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * ADMIN/SUPERADMIN o gestor de al menos un área (portfolio WorkOS/Gantt).
 */
public final class GanttPortfolioAuthorizations {

    private GanttPortfolioAuthorizations() {
    }

    public static void requirePortfolioManager(UserRepository userRepo,
                                              AreaRepository areaRepository,
                                              Long requesterId,
                                              String forbiddenMessage) {
        User user = userRepo.findByIdWithRole(requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (GanttReadableAreas.isPlatformAdmin(user)) {
            return;
        }
        if (!areaRepository.findByManagerId(user.getId()).isEmpty()) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
    }
}
