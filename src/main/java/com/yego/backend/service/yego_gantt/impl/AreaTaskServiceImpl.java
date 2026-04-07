package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.request.CreateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskKpisResponseDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskResponseDto;
import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskPriority;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskStatus;
import com.yego.backend.entity.yego_principal.entities.Area;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_principal.AreaRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_gantt.AreaTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
        String roleName = user.getRole() != null ? user.getRole().getName() : "";
        if ("ADMIN".equalsIgnoreCase(roleName) || "SUPERADMIN".equalsIgnoreCase(roleName)) {
            return TaskScope.fullAccess();
        }
        List<Area> managed = areaRepository.findByManagerId(user.getId());
        if (isAdministracionJefe(roleName, managed)) {
            return TaskScope.fullAccess();
        }
        if (managed != null && !managed.isEmpty()) {
            Set<Long> ids = managed.stream().map(Area::getId).collect(Collectors.toSet());
            return new TaskScope(false, ids, ids);
        }
        if (user.getAreaId() != null) {
            return new TaskScope(false, Set.of(user.getAreaId()), Set.of());
        }
        return TaskScope.empty();
    }

    private boolean isAdministracionJefe(String userRole, List<Area> areasComoJefe) {
        if (areasComoJefe == null || areasComoJefe.isEmpty()) {
            return false;
        }
        boolean rolEsAdministracion = userRole != null
                && ("Administración".equalsIgnoreCase(userRole.trim()) || "Administracion".equalsIgnoreCase(userRole.trim()));
        boolean algunaAreaEsAdministracion = areasComoJefe.stream().anyMatch(a ->
                a.getName() != null && ("Administración".equalsIgnoreCase(a.getName().trim())
                        || "Administracion".equalsIgnoreCase(a.getName().trim())));
        return rolEsAdministracion && algunaAreaEsAdministracion;
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

    private List<AreaTask> loadVisibleTasks(TaskScope scope, Long areaIdFilter) {
        assertAreaFilterAllowed(scope, areaIdFilter);
        List<AreaTask> list;
        if (scope.allAreas) {
            list = areaTaskRepository.findAll();
        } else if (scope.readableAreaIds.isEmpty()) {
            return List.of();
        } else {
            list = areaTaskRepository.findByAreaIdInOrderByAreaIdAscSortOrderAscIdAsc(scope.readableAreaIds);
        }
        if (areaIdFilter != null) {
            return list.stream().filter(t -> areaIdFilter.equals(t.getAreaId())).toList();
        }
        return list;
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


    private List<AreaTask> applyPriorityFilter(List<AreaTask> tasks, AreaTaskPriority priorityFilter) {
        if (priorityFilter == null) {
            return tasks;
        }
        return tasks.stream().filter(t -> priorityFilter.equals(t.getPriority())).toList();
    }

    private Map<Long, String> areaNamesForIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return areaRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Area::getId, a -> a.getName() != null ? a.getName() : ""));
    }

    private String idsToString(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Long> stringToIds(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::valueOf).toList();
    }

    private String tagsToString(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return tags.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining(","));
    }

    private List<String> stringToTags(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private AreaTaskResponseDto toDto(AreaTask t, Map<Long, String> names) {
        return AreaTaskResponseDto.builder()
                .id(t.getId())
                .areaId(t.getAreaId())
                .areaName(names.get(t.getAreaId()))
                .title(t.getTitle())
                .description(t.getDescription())
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .status(t.getStatus())
                .priority(t.getPriority())
                .progressPercent(t.getProgressPercent())
                .assignedUserId(t.getAssignedUserId())
                .assignedUserIds(stringToIds(t.getAssignedUserIds()))
                .tags(stringToTags(t.getTags()))
                .sortOrder(t.getSortOrder())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaTaskResponseDto> list(Long userId, Long areaIdFilter, AreaTaskPriority priorityFilter) {
        User user = requireUser(userId);
        TaskScope scope = resolveScope(user);
        List<AreaTask> tasks = applyPriorityFilter(loadVisibleTasks(scope, areaIdFilter), priorityFilter);
        Set<Long> ids = tasks.stream().map(AreaTask::getAreaId).collect(Collectors.toSet());
        Map<Long, String> names = areaNamesForIds(ids);
        return tasks.stream().map(t -> toDto(t, names)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AreaTaskKpisResponseDto kpis(Long userId, Long areaIdFilter, AreaTaskPriority priorityFilter) {
        User user = requireUser(userId);
        TaskScope scope = resolveScope(user);
        List<AreaTask> tasks = applyPriorityFilter(loadVisibleTasks(scope, areaIdFilter), priorityFilter);
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
    public AreaTaskResponseDto getById(Long userId, Long taskId) {
        User user = requireUser(userId);
        TaskScope scope = resolveScope(user);
        AreaTask task = requireReadableTask(scope, taskId);
        Map<Long, String> names = areaNamesForIds(Set.of(task.getAreaId()));
        return toDto(task, names);
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
                .title(dto.getTitle().trim())
                .description(dto.getDescription())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(dto.getStatus() != null ? dto.getStatus() : AreaTaskStatus.PENDING)
                .priority(dto.getPriority() != null ? dto.getPriority() : AreaTaskPriority.MEDIUM)
                .progressPercent(dto.getProgressPercent() != null ? dto.getProgressPercent() : 0)
                .assignedUserId(dto.getAssignedUserId())
                .assignedUserIds(idsToString(dto.getAssignedUserIds()))
                .tags(tagsToString(dto.getTags()))
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
        if (dto.getAssignedUserId() != null) {
            task.setAssignedUserId(dto.getAssignedUserId());
        }
        if (dto.getAssignedUserIds() != null) {
            task.setAssignedUserIds(idsToString(dto.getAssignedUserIds()));
        }
        if (dto.getTags() != null) {
            task.setTags(tagsToString(dto.getTags()));
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
