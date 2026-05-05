package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.service.yego_gantt.AreaTaskPrivateAccessService;
import com.yego.backend.service.yego_gantt.GanttReadableAreasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AreaTaskPrivateAccessServiceImpl implements AreaTaskPrivateAccessService {

    private final GanttReadableAreasService ganttReadableAreasService;

    @Override
    public boolean canSeeTaskContent(User viewer, AreaTask task) {
        if (!task.isPrivateTask()) {
            return true;
        }
        if (ganttReadableAreasService.isPlatformAdmin(viewer)) {
            return true;
        }
        return task.getCreatedByUserId() != null && task.getCreatedByUserId().equals(viewer.getId());
    }

    @Override
    public void assertCanMutatePrivateTask(User user, AreaTask task) {
        if (!task.isPrivateTask()) {
            return;
        }
        if (ganttReadableAreasService.isPlatformAdmin(user)) {
            return;
        }
        if (task.getCreatedByUserId() != null && task.getCreatedByUserId().equals(user.getId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el creador puede modificar esta tarea privada");
    }
}
