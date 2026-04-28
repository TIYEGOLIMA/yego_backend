package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.request.CreateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskKpisResponseDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskResponseDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTasksSummaryResponseDto;
import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskPriority;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskStatus;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_principal.AreaRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_gantt.AreaTaskService;
import com.yego.backend.service.yego_gantt.GanttReadableAreas;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaTaskServiceImpl implements AreaTaskService {

    private final AreaTaskRepository areaTaskRepository;
    private final AreaRepository areaRepository;
    private final UserRepository userRepository;

    private record TaskScope(boolean allAreas, Set<Long> readableAreaIds, Set<Long> manageableAreaIds) {
        static TaskScope fullAccess() {
            return new TaskScope(true, Set.of(), Set.of());
        }

        static TaskScope empty() {
            return new TaskScope(false, Set.of(), Set.of());
        }
    }

    private TaskScope resolveScope(User user) {
        if (GanttReadableAreas.isPlatformAdmin(user)) {
            return TaskScope.fullAccess();
        }
        Set<Long> ids = GanttReadableAreas.forUser(user, areaRepository);
        if (ids.isEmpty()) {
            return TaskScope.empty();
        }
        return new TaskScope(false, ids, ids);
    }

    private User requireUser(Long userId) {
        return userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private void assertAreaFilterAllowed(TaskScope scope, Long areaIdFilter) {
        if (areaIdFilter == null) {
            return;
        }
        if (scope.allAreas) {
            return;
        }
        if (!scope.readableAreaIds.contains(areaIdFilter)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta área");
        }
    }

    /** Una sola consulta acotada en BD (sin findAll). */
    private List<AreaTask> loadVisibleTasksFromDb(TaskScope scope, Long areaIdFilter, Long projectIdFilter, AreaTaskPriority priorityFilter) {
        assertAreaFilterAllowed(scope, areaIdFilter);
        if (!scope.allAreas && scope.readableAreaIds.isEmpty()) {
            return List.of();
        }
        if (scope.allAreas) {
            return areaTaskRepository.findAdminFiltered(areaIdFilter, projectIdFilter, priorityFilter);
        }
        return areaTaskRepository.findScopedFiltered(scope.readableAreaIds, areaIdFilter, projectIdFilter, priorityFilter);
    }

    private void assertCanManage(TaskScope scope, Long areaId) {
        if (scope.allAreas) {
            return;
        }
        if (!scope.manageableAreaIds.contains(areaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede crear o editar tareas en esta área");
        }
    }

    private AreaTask requireReadableTask(TaskScope scope, Long taskId) {
        AreaTask task = areaTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
        if (scope.allAreas) {
            return task;
        }
        if (!scope.readableAreaIds.contains(task.getAreaId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta tarea");
        }
        return task;
    }

    private Map<Long, String> areaNamesForIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return areaRepository.findIdAndNameByIdIn(ids).stream()
                .collect(Collectors.toMap(AreaRepository.AreaIdNameRow::getId,
                        p -> p.getName() != null ? p.getName() : ""));
    }

    private List<Long> safeIds(List<Long> ids) {
        return ids != null ? ids : new ArrayList<>();
    }

    private List<String> safeTags(List<String> tags) {
        if (tags == null) return new ArrayList<>();
        return tags.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static List<Long> detachedUserIds(AreaTask t) {
        List<Long> raw = t.getAssignedUserIds();
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return List.copyOf(raw);
    }

    private static List<String> detachedTags(AreaTask t) {
        List<String> raw = t.getTags();
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return List.copyOf(raw);
    }

    private AreaTaskResponseDto toDto(AreaTask t, Map<Long, String> names) {
        return AreaTaskResponseDto.builder()
                .id(t.getId())
                .areaId(t.getAreaId())
                .areaName(names.get(t.getAreaId()))
                .projectId(t.getProjectId())
                .sprintId(t.getSprintId())
                .title(t.getTitle())
                .description(t.getDescription())
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .status(t.getStatus())
                .priority(t.getPriority())
                .progressPercent(t.getProgressPercent())
                .assignedUserId(t.getAssignedUserId())
                .assignedUserIds(detachedUserIds(t))
                .tags(detachedTags(t))
                .sortOrder(t.getSortOrder())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private AreaTaskKpisResponseDto buildKpis(List<AreaTask> tasks) {
        if (tasks.isEmpty()) {
            return AreaTaskKpisResponseDto.builder()
                    .equipos(0)
                    .tareas(0)
                    .progresoPromedioPct(0)
                    .completadas(0)
                    .enRiesgo(0)
                    .bloqueadas(0)
                    .build();
        }
        long equipos = tasks.stream().map(AreaTask::getAreaId).distinct().count();
        int completadas = (int) tasks.stream().filter(t -> t.getStatus() == AreaTaskStatus.DONE).count();
        int enRiesgo = (int) tasks.stream().filter(t -> t.getStatus() == AreaTaskStatus.AT_RISK).count();
        int bloqueadas = (int) tasks.stream().filter(t -> t.getStatus() == AreaTaskStatus.BLOCKED).count();
        double avg = tasks.stream().mapToInt(t -> t.getProgressPercent() != null ? t.getProgressPercent() : 0).average().orElse(0);
        return AreaTaskKpisResponseDto.builder()
                .equipos((int) equipos)
                .tareas(tasks.size())
                .progresoPromedioPct(Math.round(avg * 10.0) / 10.0)
                .completadas(completadas)
                .enRiesgo(enRiesgo)
                .bloqueadas(bloqueadas)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaTaskResponseDto> list(Long userId, Long areaIdFilter, Long projectIdFilter, AreaTaskPriority priorityFilter) {
        User user = requireUser(userId);
        TaskScope scope = resolveScope(user);
        List<AreaTask> tasks = loadVisibleTasksFromDb(scope, areaIdFilter, projectIdFilter, priorityFilter);
        Set<Long> ids = tasks.stream().map(AreaTask::getAreaId).collect(Collectors.toSet());
        Map<Long, String> names = areaNamesForIds(ids);
        return tasks.stream().map(t -> toDto(t, names)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AreaTaskKpisResponseDto kpis(Long userId, Long areaIdFilter, Long projectIdFilter, AreaTaskPriority priorityFilter) {
        User user = requireUser(userId);
        TaskScope scope = resolveScope(user);
        List<AreaTask> tasks = loadVisibleTasksFromDb(scope, areaIdFilter, projectIdFilter, priorityFilter);
        return buildKpis(tasks);
    }

    @Override
    @Transactional(readOnly = true)
    public AreaTasksSummaryResponseDto summary(Long userId, Long areaIdFilter, Long projectIdFilter, AreaTaskPriority priorityFilter) {
        User user = requireUser(userId);
        TaskScope scope = resolveScope(user);
        List<AreaTask> tasks = loadVisibleTasksFromDb(scope, areaIdFilter, projectIdFilter, priorityFilter);
        AreaTaskKpisResponseDto kpisDto = buildKpis(tasks);
        Set<Long> ids = tasks.stream().map(AreaTask::getAreaId).collect(Collectors.toSet());
        Map<Long, String> names = areaNamesForIds(ids);
        List<AreaTaskResponseDto> taskDtos = tasks.stream().map(t -> toDto(t, names)).toList();
        return AreaTasksSummaryResponseDto.builder()
                .tasks(taskDtos)
                .kpis(kpisDto)
                .build();
    }

    @Override
    @Transactional
    public AreaTaskResponseDto create(Long userId, CreateAreaTaskDto dto) {
        User user = requireUser(userId);
        TaskScope scope = resolveScope(user);
        assertCanManage(scope, dto.getAreaId());
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha fin no puede ser anterior al inicio");
        }
        areaRepository.findById(dto.getAreaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Área no existe"));
        AreaTask task = AreaTask.builder()
                .areaId(dto.getAreaId())
                .projectId(dto.getProjectId())
                .sprintId(dto.getSprintId())
                .title(dto.getTitle().trim())
                .description(dto.getDescription())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(dto.getStatus() != null ? dto.getStatus() : AreaTaskStatus.PENDING)
                .priority(dto.getPriority() != null ? dto.getPriority() : AreaTaskPriority.MEDIUM)
                .progressPercent(dto.getProgressPercent() != null ? dto.getProgressPercent() : 0)
                .assignedUserId(dto.getAssignedUserId())
                .assignedUserIds(safeIds(dto.getAssignedUserIds()))
                .tags(safeTags(dto.getTags()))
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .build();
        AreaTask saved = areaTaskRepository.save(task);
        Map<Long, String> names = areaNamesForIds(Set.of(saved.getAreaId()));
        return toDto(saved, names);
    }

    @Override
    @Transactional
    public AreaTaskResponseDto update(Long userId, Long taskId, UpdateAreaTaskDto dto) {
        User user = requireUser(userId);
        TaskScope scope = resolveScope(user);
        AreaTask task = requireReadableTask(scope, taskId);
        assertCanManage(scope, task.getAreaId());
        if (dto.getTitle() != null) {
            task.setTitle(dto.getTitle().trim());
        }
        if (dto.getDescription() != null) {
            task.setDescription(dto.getDescription());
        }
        if (dto.getStartDate() != null) {
            task.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            task.setEndDate(dto.getEndDate());
        }
        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus());
        }
        if (dto.getProgressPercent() != null) {
            task.setProgressPercent(dto.getProgressPercent());
        }
        if (dto.getProjectId() != null) {
            task.setProjectId(dto.getProjectId());
        }
        if (dto.getSprintId() != null) {
            task.setSprintId(dto.getSprintId());
        }
        if (dto.getAssignedUserId() != null) {
            task.setAssignedUserId(dto.getAssignedUserId());
        }
        if (dto.getAssignedUserIds() != null) {
            task.setAssignedUserIds(safeIds(dto.getAssignedUserIds()));
        }
        if (dto.getTags() != null) {
            task.setTags(safeTags(dto.getTags()));
        }
        if (dto.getSortOrder() != null) {
            task.setSortOrder(dto.getSortOrder());
        }
        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
        }
        if (task.getEndDate().isBefore(task.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha fin no puede ser anterior al inicio");
        }
        AreaTask saved = areaTaskRepository.save(task);
        Map<Long, String> names = areaNamesForIds(Set.of(saved.getAreaId()));
        return toDto(saved, names);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long taskId) {
        User user = requireUser(userId);
        TaskScope scope = resolveScope(user);
        AreaTask task = requireReadableTask(scope, taskId);
        assertCanManage(scope, task.getAreaId());
        areaTaskRepository.delete(task);
    }
}
