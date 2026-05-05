package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.service.yego_gantt.AreaTaskVisibilityService;
import com.yego.backend.service.yego_gantt.GanttReadableAreasService;
import com.yego.backend.service.yego_gantt.GanttTaskScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaTaskVisibilityServiceImpl implements AreaTaskVisibilityService {

    private final GanttReadableAreasService ganttReadableAreasService;

    @Override
    public boolean isAssignee(Long userId, AreaTask task) {
        if (userId == null || task == null) {
            return false;
        }
        if (task.getAssignedUserId() != null && task.getAssignedUserId().equals(userId)) {
            return true;
        }
        List<Long> ids = task.getAssignedUserIds();
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (Long id : ids) {
            if (id != null && id.equals(userId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canReadTaskByScopeAndAssignment(User user, GanttTaskScope scope, AreaTask task) {
        if (ganttReadableAreasService.isPlatformAdmin(user)) {
            return true;
        }
        if (scope.canAccessArea(task.getAreaId())) {
            return true;
        }
        return isAssignee(user.getId(), task);
    }
}
