package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.service.yego_gantt.GanttReadableAreasService;
import com.yego.backend.service.yego_gantt.GanttTaskScope;
import com.yego.backend.service.yego_gantt.GanttTaskScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class GanttTaskScopeServiceImpl implements GanttTaskScopeService {

    private final GanttReadableAreasService ganttReadableAreasService;

    @Override
    public GanttTaskScope resolve(User user) {
        if (ganttReadableAreasService.isPlatformAdmin(user)) {
            return new GanttTaskScope(true, Set.of());
        }
        Set<Long> ids = ganttReadableAreasService.readableAreaIdsForUser(user);
        if (ids.isEmpty()) {
            return new GanttTaskScope(false, Set.of());
        }
        return new GanttTaskScope(false, ids);
    }
}
