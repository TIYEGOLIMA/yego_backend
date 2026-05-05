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
    public void requirePlatformAdmin(Long requesterId, String forbiddenMessage) {
        User user = userRepository.findByIdWithRole(requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (ganttReadableAreasService.isPlatformAdmin(user)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
    }
}
