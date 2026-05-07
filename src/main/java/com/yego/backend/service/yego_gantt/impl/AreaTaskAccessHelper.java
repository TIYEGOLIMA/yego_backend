package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_gantt.AreaTaskSubtaskRepository;
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
    private final AreaTaskSubtaskRepository areaTaskSubtaskRepository;
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
        if (!scope.canAccessArea(areaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede crear o editar tareas en esta área");
        }
    }

    public void assertCanMutateTask(User user, GanttTaskScope scope, AreaTask task) {
        if (!task.isPrivateTask()) {
            assertCanManage(scope, task.getAreaId());
        }
        areaTaskPrivateAccessService.assertCanMutatePrivateTask(user, task);
    }

    public void assertCanMutateTask(long userId, AreaTask task) {
        User user = requireUser(userId);
        GanttTaskScope scope = resolveScope(user);
        assertCanMutateTask(user, scope, task);
    }

    /**
     * Tarea cargada + validación privacidad + lectura según alcance/asignación al padre.
     *
     * @param includeSubtaskAssignee si {@code true}, también puede leer quien es responsable de alguna subtarea
     *                               aunque no esté en los asignados del padre.
     */
    public AreaTask requireReadableTask(User user, GanttTaskScope scope, Long taskId, boolean includeSubtaskAssignee) {
        AreaTask task = areaTaskRepository.findWithAssigneesById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
        if (!areaTaskPrivateAccessService.canSeeTaskContent(user, task)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta tarea");
        }
        if (areaTaskVisibilityService.canReadTaskByScopeAndAssignment(user, scope, task)) {
            return task;
        }
        if (includeSubtaskAssignee) {
            Long uid = user.getId();
            if (uid != null && areaTaskSubtaskRepository.existsByParentTaskIdAndAssignedUserId(taskId, uid)) {
                return task;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta tarea");
    }

    public AreaTask requireReadableTask(User user, GanttTaskScope scope, Long taskId) {
        return requireReadableTask(user, scope, taskId, false);
    }

    public AreaTask requireReadableTask(long userId, long taskId) {
        User user = requireUser(userId);
        GanttTaskScope scope = resolveScope(user);
        return requireReadableTask(user, scope, taskId, false);
    }
}
