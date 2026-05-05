package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_gantt.AreaTaskPrivateAccessService;
import com.yego.backend.service.yego_gantt.AreaTaskVisibilityService;
import com.yego.backend.service.yego_gantt.GanttTaskScope;
import com.yego.backend.service.yego_gantt.GanttTaskScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lectura/mutación de tareas WorkOS/Gantt coherente con {@link AreaTaskServiceImpl}.
 */
@Component
@RequiredArgsConstructor
public class AreaTaskAccessHelper {

    private final AreaTaskRepository areaTaskRepository;
    private final UserRepository userRepository;
    private final GanttTaskScopeService ganttTaskScopeService;
    private final AreaTaskVisibilityService areaTaskVisibilityService;
    private final AreaTaskPrivateAccessService areaTaskPrivateAccessService;

    public User requireUser(Long userId) {
        return userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    public GanttTaskScope resolveScope(User user) {
        return ganttTaskScopeService.resolve(user);
    }

    public void assertCanManage(GanttTaskScope scope, Long areaId) {
        if (scope.canAccessArea(areaId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede crear o editar tareas en esta área");
    }

    public void assertCanMutateTask(User user, GanttTaskScope scope, AreaTask task) {
        assertCanManage(scope, task.getAreaId());
        areaTaskPrivateAccessService.assertCanMutatePrivateTask(user, task);
    }

    public AreaTask requireReadableTask(User user, GanttTaskScope scope, Long taskId) {
        AreaTask task = areaTaskRepository.findWithAssigneesById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
        if (!areaTaskPrivateAccessService.canSeeTaskContent(user, task)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta tarea");
        }
        if (!areaTaskVisibilityService.canReadTaskByScopeAndAssignment(user, scope, task)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta tarea");
        }
        return task;
    }

    /** Carga usuario, scope y tarea legible; útil para servicios que no son {@code AreaTaskService}. */
    public AreaTask requireReadableTask(long userId, long taskId) {
        User user = requireUser(userId);
        GanttTaskScope scope = resolveScope(user);
        return requireReadableTask(user, scope, taskId);
    }

    public void assertCanMutateTask(long userId, AreaTask task) {
        User user = requireUser(userId);
        GanttTaskScope scope = resolveScope(user);
        assertCanMutateTask(user, scope, task);
    }
}
