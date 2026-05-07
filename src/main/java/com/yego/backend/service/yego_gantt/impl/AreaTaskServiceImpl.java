package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.request.CreateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskResponseDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTasksSummaryResponseDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTasksSummaryResponseDto.SummaryKpis;
import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskPriority;
import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskStatus;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_gantt.AreaTaskSubtaskRepository;
import com.yego.backend.repository.yego_gantt.WorkosMeetingMinuteItemRepository;
import com.yego.backend.repository.yego_gantt.WorkosTaskMessageRepository;
import com.yego.backend.repository.yego_principal.AreaRepository;
import com.yego.backend.service.yego_gantt.AreaTaskListParams;
import com.yego.backend.service.yego_gantt.AreaTaskService;
import com.yego.backend.service.yego_gantt.AreaTaskVisibilityService;
import com.yego.backend.service.yego_gantt.GanttReadableAreasService;
import com.yego.backend.service.yego_gantt.GanttTaskTagPrivacyService;
import com.yego.backend.service.yego_gantt.GanttTaskScope;
import com.yego.backend.service.yego_gantt.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaTaskServiceImpl implements AreaTaskService {

    private final AreaTaskRepository areaTaskRepository;
    private final AreaTaskSubtaskRepository areaTaskSubtaskRepository;
    private final WorkosTaskMessageRepository workosTaskMessageRepository;
    private final WorkosMeetingMinuteItemRepository workosMeetingMinuteItemRepository;
    private final AreaRepository areaRepository;
    private final AreaTaskAccessHelper areaTaskAccess;
    private final SprintService sprintService;
    private final WorkosTaskSystemMessageRecorder systemMessageRecorder;
    private final GanttReadableAreasService ganttReadableAreasService;
    private final GanttTaskTagPrivacyService ganttTaskTagPrivacyService;
    private final AreaTaskVisibilityService areaTaskVisibilityService;

    private static boolean isStatusOnlyUpdate(UpdateAreaTaskDto dto) {
        if (dto == null || dto.getStatus() == null) {
            return false;
        }
        return dto.getWorkspaceId() == null
                && dto.getSprintId() == null
                && dto.getAreaId() == null
                && dto.getTitle() == null
                && dto.getDescription() == null
                && dto.getStartDate() == null
                && dto.getEndDate() == null
                && dto.getPriority() == null
                && dto.getProgressPercent() == null
                && dto.getAssignedUserId() == null
                && dto.getAssignedUserIds() == null
                && dto.getPrivateTask() == null
                && dto.getTags() == null
                && dto.getSortOrder() == null;
    }

    /**
     * Puede enviar PUT solo con {@code status} sin ser gestor del área: asignado al padre, creador o responsable de subtarea.
     */
    private boolean canUpdateTaskStatusAsParticipant(User user, AreaTask task) {
        if (ganttReadableAreasService.isPlatformAdmin(user)) {
            return true;
        }
        Long uid = user.getId();
        if (uid == null) {
            return false;
        }
        if (areaTaskVisibilityService.isAssignee(uid, task)) {
            return true;
        }
        if (task.getCreatedByUserId() != null && task.getCreatedByUserId().equals(uid)) {
            return true;
        }
        Long tid = task.getId();
        return tid != null && areaTaskSubtaskRepository.existsByParentTaskIdAndAssignedUserId(tid, uid);
    }

    private void assertAreaFilterAllowed(GanttTaskScope scope, Long areaIdFilter) {
        if (areaIdFilter == null) {
            return;
        }
        if (!scope.canAccessArea(areaIdFilter)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta área");
        }
    }

    /** Una sola consulta acotada en BD (sin findAll). */
    /** Listado agregado «Mi espacio»: sin workspace del creador + privadas en proyectos del creador. */
    private List<AreaTask> loadMySpaceTasksFromDb(User viewer, GanttTaskScope scope, Long areaIdFilter,
                                                  AreaTaskPriority priorityFilter, Long ownerUserIdFilter) {
        assertAreaFilterAllowed(scope, areaIdFilter);
        if (!scope.allAreas() && scope.areaIds().isEmpty()) {
            return List.of();
        }
        long viewerId = viewer.getId();
        if (scope.allAreas()) {
            return areaTaskRepository.findAdminMySpaceFiltered(areaIdFilter, priorityFilter,
                    ownerUserIdFilter, viewerId);
        }
        return areaTaskRepository.findScopedMySpaceFiltered(scope.areaIds(), areaIdFilter, priorityFilter,
                ownerUserIdFilter, viewerId);
    }

    private List<AreaTask> loadVisibleTasksFromDb(User viewer, GanttTaskScope scope, Long areaIdFilter, Long workspaceIdFilter,
                                                  boolean onlyWithoutWorkspace,
                                                  AreaTaskPriority priorityFilter, Long ownerUserIdFilter) {
        assertAreaFilterAllowed(scope, areaIdFilter);
        if (!scope.allAreas() && scope.areaIds().isEmpty()) {
            return List.of();
        }
        boolean skipPrivate = ganttReadableAreasService.canOperateAllAreasInGantt(viewer);
        long viewerId = viewer.getId();
        if (scope.allAreas()) {
            return areaTaskRepository.findAdminFiltered(areaIdFilter, workspaceIdFilter, onlyWithoutWorkspace, priorityFilter,
                    ownerUserIdFilter, viewerId, skipPrivate);
        }
        return areaTaskRepository.findScopedFiltered(scope.areaIds(), areaIdFilter, workspaceIdFilter, onlyWithoutWorkspace, priorityFilter,
                ownerUserIdFilter, viewerId, skipPrivate);
    }

    /** Tareas privadas: sin sprint; asignaciones del DTO, con principal = primer elemento de la lista. */
    private void applyPrivateTaskRules(AreaTask task) {
        if (!task.isPrivateTask()) {
            return;
        }
        task.setSprintId(null);
        List<Long> rawList = safeIds(task.getAssignedUserIds());
        if (!rawList.isEmpty()) {
            rawList = dedupePreserveOrder(rawList);
            task.setAssignedUserIds(new ArrayList<>(rawList));
            task.setAssignedUserId(rawList.get(0));
            return;
        }
        Long only = task.getAssignedUserId();
        if (only != null) {
            task.setAssignedUserIds(new ArrayList<>(List.of(only)));
        } else {
            task.setAssignedUserId(null);
            task.setAssignedUserIds(new ArrayList<>());
        }
    }

    private static List<Long> dedupePreserveOrder(List<Long> ids) {
        List<Long> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Long id : ids) {
            if (id != null && seen.add(id)) {
                out.add(id);
            }
        }
        return out;
    }

    private void requireAreaActiva(Long areaId) {
        if (areaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar un área");
        }
        areaRepository.findByIdAndActivoTrue(areaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "El área no existe o está inactiva"));
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
        if (tags == null) {
            return new ArrayList<>();
        }
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

    private record SubtaskAgg(int done, int total) {
    }

    private Map<Long, SubtaskAgg> loadSubtaskAggregates(List<AreaTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Map.of();
        }
        List<Long> taskIds = tasks.stream().map(AreaTask::getId).filter(id -> id != null).distinct().toList();
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = areaTaskSubtaskRepository.summarizeByParentTaskIds(taskIds);
        Map<Long, SubtaskAgg> m = new HashMap<>();
        for (Object[] r : rows) {
            Long pid = ((Number) r[0]).longValue();
            int total = ((Number) r[1]).intValue();
            int done = ((Number) r[2]).intValue();
            m.put(pid, new SubtaskAgg(done, total));
        }
        return m;
    }

    private AreaTaskResponseDto toDto(AreaTask t, Map<Long, String> names, Map<Long, SubtaskAgg> subMap,
            Set<Long> viewerSubtaskParentIds) {
        SubtaskAgg sa = subMap != null ? subMap.get(t.getId()) : null;
        int done = sa == null ? 0 : sa.done();
        int total = sa == null ? 0 : sa.total();
        boolean subtaskMine = viewerSubtaskParentIds != null && t.getId() != null
                && viewerSubtaskParentIds.contains(t.getId());
        return AreaTaskResponseDto.builder()
                .id(t.getId())
                .areaId(t.getAreaId())
                .areaName(names.get(t.getAreaId()))
                .workspaceId(t.getWorkspaceId())
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
                .privateTask(t.isPrivateTask())
                .createdByUserId(t.getCreatedByUserId())
                .sortOrder(t.getSortOrder())
                .subtaskDone(done)
                .subtaskTotal(total)
                .subtaskAssignedToViewer(subtaskMine)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private SummaryKpis buildKpis(List<AreaTask> tasks) {
        if (tasks.isEmpty()) {
            return SummaryKpis.builder()
                    .equipos(0)
                    .tareas(0)
                    .progresoPromedioPct(0)
                    .completadas(0)
                    .bloqueadas(0)
                    .build();
        }
        long equipos = tasks.stream().map(AreaTask::getAreaId).distinct().count();
        int completadas = (int) tasks.stream().filter(t -> t.getStatus() == AreaTaskStatus.DONE).count();
        int bloqueadas = (int) tasks.stream().filter(t -> t.getStatus() == AreaTaskStatus.BLOCKED).count();
        double avg = tasks.stream().mapToInt(t -> t.getProgressPercent() != null ? t.getProgressPercent() : 0).average().orElse(0);
        return SummaryKpis.builder()
                .equipos((int) equipos)
                .tareas(tasks.size())
                .progresoPromedioPct(Math.round(avg * 10.0) / 10.0)
                .completadas(completadas)
                .bloqueadas(bloqueadas)
                .build();
    }

    private List<AreaTaskResponseDto> toDtos(List<AreaTask> tasks, Set<Long> viewerSubtaskParentIds) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = tasks.stream().map(AreaTask::getAreaId).collect(Collectors.toSet());
        Map<Long, String> names = areaNamesForIds(ids);
        Map<Long, SubtaskAgg> subMap = loadSubtaskAggregates(tasks);
        return tasks.stream().map(t -> toDto(t, names, subMap, viewerSubtaskParentIds)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AreaTasksSummaryResponseDto summary(long userId, AreaTaskListParams filters) {
        User user = areaTaskAccess.requireUser(userId);
        GanttTaskScope scope = areaTaskAccess.resolveScope(user);
        List<AreaTask> tasks = filters.mySpace()
                ? loadMySpaceTasksFromDb(user, scope, filters.areaId(), filters.priority(), filters.ownerUserId())
                : loadVisibleTasksFromDb(user, scope,
                        filters.areaId(),
                        filters.workspaceId(),
                        filters.onlyWithoutWorkspace(),
                        filters.priority(),
                        filters.ownerUserId());
        Set<Long> viewerSubtaskParents = new HashSet<>(
                areaTaskSubtaskRepository.findDistinctParentTaskIdsByAssignedUserId(userId));
        return AreaTasksSummaryResponseDto.builder()
                .tasks(toDtos(tasks, viewerSubtaskParents))
                .kpis(buildKpis(tasks))
                .build();
    }

    @Override
    @Transactional
    public AreaTaskResponseDto create(Long userId, CreateAreaTaskDto dto) {
        User user = areaTaskAccess.requireUser(userId);
        GanttTaskScope scope = areaTaskAccess.resolveScope(user);
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha fin no puede ser anterior al inicio");
        }
        List<String> tagList = safeTags(dto.getTags());
        boolean priv = dto.getPrivateTask() != null
                ? dto.getPrivateTask()
                : ganttTaskTagPrivacyService.tagsIndicatePrivate(tagList);
        Long effectiveAreaId = priv
                ? (dto.getAreaId() != null ? dto.getAreaId() : user.getAreaId())
                : dto.getAreaId();
        requireAreaActiva(effectiveAreaId);
        if (!priv) {
            areaTaskAccess.assertCanManage(scope, effectiveAreaId);
            sprintService.assertSprintOpenForNewTasks(dto.getSprintId());
        }
        List<String> tagsStored = ganttTaskTagPrivacyService.stripPrivacyTagLabels(tagList);
        AreaTask task = AreaTask.builder()
                .areaId(effectiveAreaId)
                .workspaceId(dto.getWorkspaceId())
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
                .tags(new ArrayList<>(tagsStored))
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .privateTask(priv)
                .createdByUserId(userId)
                .build();
        applyPrivateTaskRules(task);
        AreaTask saved = areaTaskRepository.save(task);
        return toDtos(List.of(saved), Set.of()).get(0);
    }

    @Override
    @Transactional
    public AreaTaskResponseDto update(Long userId, Long taskId, UpdateAreaTaskDto dto) {
        User user = areaTaskAccess.requireUser(userId);
        GanttTaskScope scope = areaTaskAccess.resolveScope(user);
        AreaTask task = areaTaskAccess.requireReadableTask(user, scope, taskId);
        boolean participantStatusOnly = isStatusOnlyUpdate(dto) && canUpdateTaskStatusAsParticipant(user, task);
        if (!participantStatusOnly) {
            areaTaskAccess.assertCanMutateTask(user, scope, task);
        }
        AreaTaskFieldSnapshot beforeSnapshot = AreaTaskFieldSnapshot.from(task);
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
            if (areaTaskSubtaskRepository.countByParentTaskId(taskId) == 0) {
                task.setProgressPercent(dto.getProgressPercent());
            }
        }
        if (dto.getWorkspaceId() != null) {
            task.setWorkspaceId(dto.getWorkspaceId());
        }
        if (dto.getSprintId() != null) {
            sprintService.assertSprintOpenForNewTasks(dto.getSprintId());
            task.setSprintId(dto.getSprintId());
        }
        if (dto.getAssignedUserId() != null) {
            task.setAssignedUserId(dto.getAssignedUserId());
        }
        if (dto.getAssignedUserIds() != null) {
            task.setAssignedUserIds(safeIds(dto.getAssignedUserIds()));
        }
        if (dto.getTags() != null) {
            List<String> raw = safeTags(dto.getTags());
            if (dto.getPrivateTask() == null) {
                task.setPrivateTask(ganttTaskTagPrivacyService.tagsIndicatePrivate(raw));
            }
            task.setTags(new ArrayList<>(ganttTaskTagPrivacyService.stripPrivacyTagLabels(raw)));
        }
        if (dto.getPrivateTask() != null) {
            task.setPrivateTask(dto.getPrivateTask());
        }
        if (dto.getSortOrder() != null) {
            task.setSortOrder(dto.getSortOrder());
        }
        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
        }
        if (dto.getAreaId() != null && !participantStatusOnly) {
            Long nid = dto.getAreaId();
            if (!Objects.equals(task.getAreaId(), nid)) {
                requireAreaActiva(nid);
                task.setAreaId(nid);
            }
        } else if (dto.getAreaId() == null
                && task.isPrivateTask()
                && Objects.equals(user.getId(), task.getCreatedByUserId())
                && user.getAreaId() != null) {
            requireAreaActiva(user.getAreaId());
            task.setAreaId(user.getAreaId());
        }
        requireAreaActiva(task.getAreaId());
        if (!participantStatusOnly && !task.isPrivateTask()) {
            areaTaskAccess.assertCanManage(scope, task.getAreaId());
        }
        applyPrivateTaskRules(task);
        if (task.getEndDate().isBefore(task.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha fin no puede ser anterior al inicio");
        }
        AreaTask saved = areaTaskRepository.save(task);
        systemMessageRecorder.recordAfterTaskUpdate(userId, taskId, beforeSnapshot, saved);
        Set<Long> subParents = new HashSet<>(
                areaTaskSubtaskRepository.findDistinctParentTaskIdsByAssignedUserId(userId));
        return toDtos(List.of(saved), subParents).get(0);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long taskId) {
        User user = areaTaskAccess.requireUser(userId);
        GanttTaskScope scope = areaTaskAccess.resolveScope(user);
        AreaTask task = areaTaskAccess.requireReadableTask(user, scope, taskId);
        areaTaskAccess.assertCanMutateTask(user, scope, task);
        workosMeetingMinuteItemRepository.clearConvertedTaskLinks(taskId);
        workosTaskMessageRepository.deleteAllByTaskId(taskId);
        areaTaskSubtaskRepository.deleteByParentTaskId(taskId);
        areaTaskRepository.delete(task);
    }
}
