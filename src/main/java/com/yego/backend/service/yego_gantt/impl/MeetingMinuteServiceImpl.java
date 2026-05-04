package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.request.*;
import com.yego.backend.entity.yego_gantt.api.response.*;
import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.WorkosMeetingMinute;
import com.yego.backend.entity.yego_gantt.entities.WorkosMeetingMinuteItem;
import com.yego.backend.entity.yego_gantt.entities.enums.*;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_gantt.ProjectRepository;
import com.yego.backend.repository.yego_gantt.SprintRepository;
import com.yego.backend.repository.yego_gantt.WorkosMeetingMinuteItemRepository;
import com.yego.backend.repository.yego_gantt.WorkosMeetingMinuteRepository;
import com.yego.backend.repository.yego_principal.AreaRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_gantt.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MeetingMinuteServiceImpl implements MeetingMinuteService {

    private final WorkosMeetingMinuteRepository minuteRepository;
    private final WorkosMeetingMinuteItemRepository itemRepository;
    private final AreaTaskRepository areaTaskRepository;
    private final AreaTaskService areaTaskService;
    private final UserRepository userRepository;
    private final AreaRepository areaRepository;
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final SprintService sprintService;

    private User requireUser(long userId) {
        return userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private GanttTaskScope scope(User user) {
        return GanttTaskScope.resolve(user, areaRepository);
    }

    private void assertHasScope(GanttTaskScope scope, User user) {
        if (GanttReadableAreas.isPlatformAdmin(user)) {
            return;
        }
        if (!scope.allAreas() && scope.areaIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin ámbito WorkOS para actas");
        }
    }

    /** Acta completa (todos los ítems y KPIs): admin, quien la creó o el dueño. */
    private boolean hasFullActaVisibility(User user, WorkosMeetingMinute m) {
        if (GanttReadableAreas.isPlatformAdmin(user)) {
            return true;
        }
        return Objects.equals(m.getCreatedByUserId(), user.getId())
                || Objects.equals(m.getOwnerUserId(), user.getId());
    }

    /** Ítem visible para quien no tiene vista completa: es su responsable o el ítem cae en su(s) área(s). */
    private boolean itemVisibleToUser(User user, GanttTaskScope sc, WorkosMeetingMinute minute, WorkosMeetingMinuteItem it) {
        if (hasFullActaVisibility(user, minute)) {
            return true;
        }
        if (Objects.equals(it.getResponsibleUserId(), user.getId())) {
            return true;
        }
        return it.getAreaId() != null && sc.canAccessArea(it.getAreaId());
    }

    private boolean canAccessMeeting(User user, GanttTaskScope sc, WorkosMeetingMinute m) {
        if (hasFullActaVisibility(user, m)) {
            return true;
        }
        List<WorkosMeetingMinuteItem> items = itemRepository.findByMeetingMinute_IdOrderByItemOrderAsc(m.getId());
        for (WorkosMeetingMinuteItem it : items) {
            if (itemVisibleToUser(user, sc, m, it)) {
                return true;
            }
        }
        return false;
    }

    private void assertMinuteReadable(User user, GanttTaskScope sc, WorkosMeetingMinute m) {
        if (!canAccessMeeting(user, sc, m)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta acta");
        }
    }

    /** Cabecera del acta, borrado, alta masiva de ítems y borrado de filas: solo admin, creador o dueño. */
    private boolean canWriteWholeMinute(User user, WorkosMeetingMinute m) {
        if (GanttReadableAreas.isPlatformAdmin(user)) {
            return true;
        }
        return Objects.equals(m.getCreatedByUserId(), user.getId())
                || Objects.equals(m.getOwnerUserId(), user.getId());
    }

    private void assertMinuteWritable(User user, WorkosMeetingMinute m) {
        if (!canWriteWholeMinute(user, m)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede editar esta acta");
        }
    }

    /** Edición de una fila o conversión a tarea: dueño de acta o el ítem es visible al usuario. */
    private void assertItemRowMutable(User user, GanttTaskScope sc, WorkosMeetingMinute m, WorkosMeetingMinuteItem it) {
        if (canWriteWholeMinute(user, m)) {
            return;
        }
        if (!itemVisibleToUser(user, sc, m, it)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede editar este ítem");
        }
    }

    private WorkosMeetingMinute requireMinute(long id, boolean withItems) {
        WorkosMeetingMinute m = withItems
                ? minuteRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acta no encontrada"))
                : minuteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acta no encontrada"));
        if (m.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Acta no encontrada");
        }
        return m;
    }

    private static AreaTaskPriority mapPriority(String p) {
        if (p == null || p.isBlank()) {
            return AreaTaskPriority.MEDIUM;
        }
        String u = p.replace(" ", "").trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case "BAJA", "LOW" -> AreaTaskPriority.LOW;
            case "MEDIA", "MEDIUM" -> AreaTaskPriority.MEDIUM;
            case "ALTA", "HIGH" -> AreaTaskPriority.HIGH;
            case "URGENTE", "URGENT", "CRITICA", "CRITICAL" -> AreaTaskPriority.URGENT;
            default -> AreaTaskPriority.MEDIUM;
        };
    }

    private static AreaTaskStatus mapItemStatusToTask(WorkosMeetingItemStatus s) {
        if (s == null) {
            return AreaTaskStatus.PENDING;
        }
        return switch (s) {
            case PENDIENTE, CANCELADA -> AreaTaskStatus.PENDING;
            case EN_PROGRESO -> AreaTaskStatus.IN_PROGRESS;
            case BLOQUEADA -> AreaTaskStatus.BLOCKED;
            case COMPLETADA -> AreaTaskStatus.DONE;
        };
    }

    private Map<Long, AreaTask> loadTasksById(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<AreaTask> list = areaTaskRepository.findAllById(ids.stream().filter(Objects::nonNull).distinct().toList());
        return list.stream().collect(Collectors.toMap(AreaTask::getId, t -> t));
    }

    private Map<Long, String> projectNames(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return projectRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p.getName() != null ? p.getName() : ""));
    }

    private Map<Long, String> sprintNames(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return sprintRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(s -> s.getId(), s -> s.getName() != null ? s.getName() : ""));
    }

    private MeetingMinuteItemResponse toItemDto(
            WorkosMeetingMinuteItem it,
            Map<Long, AreaTask> tasks,
            Map<Long, String> projectNames,
            Map<Long, String> sprintNames) {
        Long tid = it.getConvertedTaskId();
        AreaTask task = tid != null ? tasks.get(tid) : null;
        LocalDate today = LocalDate.now();
        boolean overdue = false;
        if (task != null) {
            overdue = task.getStatus() != AreaTaskStatus.DONE
                    && task.getEndDate() != null
                    && task.getEndDate().isBefore(today);
        }
        return MeetingMinuteItemResponse.builder()
                .id(it.getId())
                .meetingMinuteId(it.getMeetingMinute().getId())
                .itemOrder(it.getItemOrder())
                .areaId(it.getAreaId())
                .areaNameSnapshot(it.getAreaNameSnapshot())
                .projectId(it.getProjectId())
                .projectName(it.getProjectId() != null ? projectNames.get(it.getProjectId()) : null)
                .sprintId(it.getSprintId())
                .sprintName(it.getSprintId() != null ? sprintNames.get(it.getSprintId()) : null)
                .itemType(it.getItemType())
                .situation(it.getSituation())
                .decision(it.getDecision())
                .taskTitle(it.getTaskTitle())
                .taskDescription(it.getTaskDescription())
                .responsibleUserId(it.getResponsibleUserId())
                .responsibleNameSnapshot(it.getResponsibleNameSnapshot())
                .startDate(it.getStartDate())
                .deadline(it.getDeadline())
                .priority(it.getPriority())
                .status(it.getStatus())
                .converted(tid != null)
                .convertedTaskId(tid)
                .convertedAt(it.getConvertedAt())
                .convertedByUserId(it.getConvertedByUserId())
                .createdAt(it.getCreatedAt())
                .updatedAt(it.getUpdatedAt())
                .taskStatus(task != null ? task.getStatus() : null)
                .taskProgress(task != null ? task.getProgressPercent() : null)
                .taskEndDate(task != null ? task.getEndDate() : null)
                .taskIsOverdue(task != null ? overdue : null)
                .taskAssigneeIds(task != null ? new ArrayList<>(task.getAssignedUserIds()) : null)
                .build();
    }

    private MeetingMinuteResponse.SummaryKpis buildKpis(List<WorkosMeetingMinuteItem> items, Map<Long, AreaTask> tasks) {
        int total = items.size();
        int converted = (int) items.stream().filter(i -> i.getConvertedTaskId() != null).count();
        int unconverted = total - converted;
        int completed = 0;
        int inProgress = 0;
        int blocked = 0;
        int overdue = 0;
        for (WorkosMeetingMinuteItem it : items) {
            if (it.getConvertedTaskId() == null) {
                continue;
            }
            AreaTask t = tasks.get(it.getConvertedTaskId());
            if (t == null) {
                continue;
            }
            if (t.getStatus() == AreaTaskStatus.DONE) {
                completed++;
            } else if (t.getStatus() == AreaTaskStatus.IN_PROGRESS) {
                inProgress++;
            } else if (t.getStatus() == AreaTaskStatus.BLOCKED) {
                blocked++;
            }
            if (t.getStatus() != AreaTaskStatus.DONE && t.getEndDate() != null && t.getEndDate().isBefore(LocalDate.now())) {
                overdue++;
            }
        }
        int pendingNoTask = unconverted;
        // Avance solo sobre ítems ya llevados a tarea Gantt (sprint). Sin conversiones → 0 %. No mezcla "COMPLETADA"
        // en el acta sin tarea vinculada (eran acuerdos de reunión aún no ejecutados como tarea).
        double pct = converted == 0 ? 0.0 : (completed * 100.0 / converted);
        return MeetingMinuteResponse.SummaryKpis.builder()
                .totalItems(total)
                .convertedItems(converted)
                .unconvertedItems(unconverted)
                .completedTasks(completed)
                .inProgressTasks(inProgress)
                .blockedTasks(blocked)
                .overdueTasks(overdue)
                .pendingWithoutTask(pendingNoTask)
                .completionPercentage(Math.round(pct * 10.0) / 10.0)
                .build();
    }

    private MeetingMinuteResponse toResponse(WorkosMeetingMinute m, boolean includeItems, User user, GanttTaskScope sc) {
        List<WorkosMeetingMinuteItem> items = includeItems
                ? new ArrayList<>(m.getItems())
                : List.of();
        if (includeItems && items.isEmpty() && m.getId() != null) {
            items = new ArrayList<>(itemRepository.findByMeetingMinute_IdOrderByItemOrderAsc(m.getId()));
        }
        boolean partialItemsView = false;
        if (includeItems && !hasFullActaVisibility(user, m)) {
            partialItemsView = true;
            items = items.stream()
                    .filter(it -> itemVisibleToUser(user, sc, m, it))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        Set<Long> pids = items.stream().map(WorkosMeetingMinuteItem::getProjectId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> sids = items.stream().map(WorkosMeetingMinuteItem::getSprintId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> pn = projectNames(pids);
        Map<Long, String> sn = sprintNames(sids);
        Set<Long> tids = items.stream().map(WorkosMeetingMinuteItem::getConvertedTaskId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AreaTask> taskMap = loadTasksById(tids);
        List<MeetingMinuteItemResponse> itemDtos = items.stream()
                .map(i -> toItemDto(i, taskMap, pn, sn))
                .toList();
        MeetingMinuteResponse.SummaryKpis kpis = includeItems ? buildKpis(items, taskMap) : null;
        return MeetingMinuteResponse.builder()
                .id(m.getId())
                .title(m.getTitle())
                .meetingDate(m.getMeetingDate())
                .meetingType(m.getMeetingType())
                .summary(m.getSummary())
                .createdByUserId(m.getCreatedByUserId())
                .ownerUserId(m.getOwnerUserId())
                .status(m.getStatus())
                .nextMeetingDate(m.getNextMeetingDate())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .items(includeItems ? itemDtos : null)
                .kpis(kpis)
                .partialItemsView(includeItems && partialItemsView)
                .build();
    }

    private void fillAreaSnapshot(WorkosMeetingMinuteItem item) {
        if (item.getAreaId() == null) {
            return;
        }
        if (item.getAreaNameSnapshot() != null && !item.getAreaNameSnapshot().isBlank()) {
            return;
        }
        areaRepository.findById(item.getAreaId()).ifPresent(a ->
                item.setAreaNameSnapshot(a.getName()));
    }

    private void fillUserSnapshot(WorkosMeetingMinuteItem item) {
        if (item.getResponsibleUserId() == null) {
            return;
        }
        if (item.getResponsibleNameSnapshot() != null && !item.getResponsibleNameSnapshot().isBlank()) {
            return;
        }
        userRepository.findById(item.getResponsibleUserId()).ifPresent(u ->
                item.setResponsibleNameSnapshot(u.getName()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MeetingMinuteResponse> list(
            long userId,
            MeetingMinuteStatus status,
            MeetingMinuteType meetingType,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long ownerUserId,
            Long projectId,
            Long areaId,
            Pageable pageable) {
        User user = requireUser(userId);
        GanttTaskScope sc = scope(user);
        assertHasScope(sc, user);
        boolean admin = GanttReadableAreas.isPlatformAdmin(user);
        Specification<WorkosMeetingMinute> spec = Specification.allOf(
                MeetingMinuteSpecifications.notDeleted(),
                MeetingMinuteSpecifications.visibleTo(sc, userId, admin),
                MeetingMinuteSpecifications.statusOptional(status),
                MeetingMinuteSpecifications.meetingTypeOptional(meetingType),
                MeetingMinuteSpecifications.meetingDateFrom(dateFrom),
                MeetingMinuteSpecifications.meetingDateTo(dateTo),
                MeetingMinuteSpecifications.ownerOptional(ownerUserId),
                MeetingMinuteSpecifications.hasItemWithProject(projectId),
                MeetingMinuteSpecifications.hasItemWithArea(areaId));
        return minuteRepository.findAll(spec, pageable).map(m -> toResponse(m, true, user, sc));
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingMinuteResponse getById(long userId, long minuteId) {
        User user = requireUser(userId);
        GanttTaskScope sc = scope(user);
        WorkosMeetingMinute m = requireMinute(minuteId, true);
        assertMinuteReadable(user, sc, m);
        return toResponse(m, true, user, sc);
    }

    @Override
    @Transactional
    public MeetingMinuteResponse create(long userId, CreateMeetingMinuteRequest req) {
        User user = requireUser(userId);
        GanttTaskScope sc = scope(user);
        assertHasScope(sc, user);
        MeetingMinuteStatus st = req.getStatus() != null ? req.getStatus() : MeetingMinuteStatus.ABIERTA;
        WorkosMeetingMinute m = WorkosMeetingMinute.builder()
                .title(req.getTitle().trim())
                .meetingDate(req.getMeetingDate())
                .meetingType(req.getMeetingType())
                .summary(req.getSummary())
                .createdByUserId(userId)
                .ownerUserId(req.getOwnerUserId() != null ? req.getOwnerUserId() : userId)
                .status(st)
                .nextMeetingDate(req.getNextMeetingDate())
                .deleted(false)
                .items(new ArrayList<>())
                .build();
        m = minuteRepository.save(m);
        return toResponse(requireMinute(m.getId(), true), true, user, sc);
    }

    @Override
    @Transactional
    public MeetingMinuteResponse update(long userId, long minuteId, UpdateMeetingMinuteRequest req) {
        User user = requireUser(userId);
        GanttTaskScope sc = scope(user);
        WorkosMeetingMinute m = requireMinute(minuteId, false);
        assertMinuteWritable(user, m);
        if (req.getTitle() != null) {
            m.setTitle(req.getTitle().trim());
        }
        if (req.getMeetingDate() != null) {
            m.setMeetingDate(req.getMeetingDate());
        }
        if (req.getMeetingType() != null) {
            m.setMeetingType(req.getMeetingType());
        }
        if (req.getSummary() != null) {
            m.setSummary(req.getSummary());
        }
        if (req.getOwnerUserId() != null) {
            m.setOwnerUserId(req.getOwnerUserId());
        }
        if (req.getNextMeetingDate() != null) {
            m.setNextMeetingDate(req.getNextMeetingDate());
        }
        if (req.getStatus() != null) {
            m.setStatus(req.getStatus());
        }
        minuteRepository.save(m);
        return toResponse(requireMinute(minuteId, true), true, user, sc);
    }

    @Override
    @Transactional
    public void patchStatus(long userId, long minuteId, PatchMeetingMinuteStatusRequest req) {
        User user = requireUser(userId);
        GanttTaskScope sc = scope(user);
        WorkosMeetingMinute m = requireMinute(minuteId, false);
        assertMinuteWritable(user, m);
        m.setStatus(req.getStatus());
        minuteRepository.save(m);
    }

    @Override
    @Transactional
    public void softDelete(long userId, long minuteId) {
        User user = requireUser(userId);
        GanttTaskScope sc = scope(user);
        WorkosMeetingMinute m = requireMinute(minuteId, false);
        assertMinuteWritable(user, m);
        m.setDeleted(true);
        minuteRepository.save(m);
    }

    @Override
    @Transactional
    public MeetingMinuteResponse addItems(long userId, long minuteId, List<CreateMeetingMinuteItemRequest> reqs) {
        User user = requireUser(userId);
        GanttTaskScope sc = scope(user);
        WorkosMeetingMinute m = requireMinute(minuteId, false);
        assertMinuteWritable(user, m);
        int order = itemRepository.maxItemOrder(minuteId);
        for (CreateMeetingMinuteItemRequest r : reqs) {
            order++;
            int effectiveOrder = r.getItemOrder() != null ? r.getItemOrder() : order;
            if (r.getAreaId() != null && !GanttReadableAreas.isPlatformAdmin(user) && !sc.canAccessArea(r.getAreaId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Área no permitida en el ítem");
            }
            WorkosMeetingMinuteItem it = WorkosMeetingMinuteItem.builder()
                    .meetingMinute(m)
                    .itemOrder(effectiveOrder)
                    .areaId(r.getAreaId())
                    .areaNameSnapshot(r.getAreaNameSnapshot())
                    .projectId(r.getProjectId())
                    .sprintId(r.getSprintId())
                    .itemType(r.getItemType() != null ? r.getItemType() : WorkosMeetingItemType.ACCION)
                    .situation(r.getSituation())
                    .decision(r.getDecision())
                    .taskTitle(r.getTaskTitle())
                    .taskDescription(r.getTaskDescription())
                    .responsibleUserId(r.getResponsibleUserId())
                    .responsibleNameSnapshot(r.getResponsibleNameSnapshot())
                    .startDate(r.getStartDate())
                    .deadline(r.getDeadline())
                    .priority(r.getPriority())
                    .status(r.getStatus() != null ? r.getStatus() : WorkosMeetingItemStatus.PENDIENTE)
                    .build();
            fillAreaSnapshot(it);
            fillUserSnapshot(it);
            itemRepository.save(it);
        }
        return toResponse(requireMinute(minuteId, true), true, user, sc);
    }

    @Override
    @Transactional
    public MeetingMinuteResponse updateItem(long userId, long minuteId, long itemId, UpdateMeetingMinuteItemRequest req) {
        User user = requireUser(userId);
        GanttTaskScope sc = scope(user);
        WorkosMeetingMinute m = requireMinute(minuteId, false);
        assertMinuteReadable(user, sc, m);
        WorkosMeetingMinuteItem it = itemRepository.findByIdAndMeetingMinute_Id(itemId, minuteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ítem no encontrado"));
        assertItemRowMutable(user, sc, m, it);
        if (it.getConvertedTaskId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ítem ya convertido; no editable aquí");
        }
        if (req.getAreaId() != null) {
            if (!GanttReadableAreas.isPlatformAdmin(user) && !sc.canAccessArea(req.getAreaId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Área no permitida");
            }
            it.setAreaId(req.getAreaId());
        }
        if (req.getAreaNameSnapshot() != null) {
            it.setAreaNameSnapshot(req.getAreaNameSnapshot());
        }
        if (req.getProjectId() != null) {
            it.setProjectId(req.getProjectId());
        }
        if (req.getSprintId() != null) {
            it.setSprintId(req.getSprintId());
        }
        if (req.getItemType() != null) {
            it.setItemType(req.getItemType());
        }
        if (req.getSituation() != null) {
            it.setSituation(req.getSituation());
        }
        if (req.getDecision() != null) {
            it.setDecision(req.getDecision());
        }
        if (req.getTaskTitle() != null) {
            it.setTaskTitle(req.getTaskTitle());
        }
        if (req.getTaskDescription() != null) {
            it.setTaskDescription(req.getTaskDescription());
        }
        if (req.getResponsibleUserId() != null) {
            it.setResponsibleUserId(req.getResponsibleUserId());
        }
        if (req.getResponsibleNameSnapshot() != null) {
            it.setResponsibleNameSnapshot(req.getResponsibleNameSnapshot());
        }
        if (req.getStartDate() != null) {
            it.setStartDate(req.getStartDate());
        }
        if (req.getDeadline() != null) {
            it.setDeadline(req.getDeadline());
        }
        if (req.getPriority() != null) {
            it.setPriority(req.getPriority());
        }
        if (req.getStatus() != null) {
            it.setStatus(req.getStatus());
        }
        fillAreaSnapshot(it);
        fillUserSnapshot(it);
        itemRepository.save(it);
        return toResponse(requireMinute(minuteId, true), true, user, sc);
    }

    @Override
    @Transactional
    public void deleteItem(long userId, long minuteId, long itemId) {
        User user = requireUser(userId);
        GanttTaskScope sc = scope(user);
        WorkosMeetingMinute m = requireMinute(minuteId, false);
        assertMinuteWritable(user, m);
        WorkosMeetingMinuteItem it = itemRepository.findByIdAndMeetingMinute_Id(itemId, minuteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ítem no encontrado"));
        if (it.getConvertedTaskId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede borrar un ítem convertido");
        }
        itemRepository.delete(it);
    }

    @Override
    @Transactional
    public ConvertMeetingItemToTaskResponse convertItemToTask(
            long userId,
            long minuteId,
            long itemId,
            ConvertMeetingItemToTaskRequest req) {
        User user = requireUser(userId);
        GanttTaskScope sc = scope(user);
        WorkosMeetingMinute m = requireMinute(minuteId, false);
        assertMinuteReadable(user, sc, m);
        WorkosMeetingMinuteItem it = itemRepository.findByIdAndMeetingMinute_Id(itemId, minuteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ítem no encontrado"));
        assertItemRowMutable(user, sc, m, it);
        if (it.getConvertedTaskId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El ítem ya fue convertido");
        }
        String title = req.getTitle() != null && !req.getTitle().isBlank()
                ? req.getTitle().trim()
                : (it.getTaskTitle() != null && !it.getTaskTitle().isBlank() ? it.getTaskTitle().trim() : "Tarea desde acta");
        String desc = req.getDescription();
        if (desc == null || desc.isBlank()) {
            desc = Stream.of(it.getTaskDescription(), it.getSituation(), it.getDecision())
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining("\n\n"));
        }
        Long areaId = req.getAreaId() != null ? req.getAreaId() : it.getAreaId();
        if (areaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requiere área para crear la tarea");
        }
        LocalDate start = req.getStartDate() != null ? req.getStartDate()
                : (it.getStartDate() != null ? it.getStartDate() : LocalDate.now());
        LocalDate end = req.getEndDate() != null ? req.getEndDate()
                : (it.getDeadline() != null ? it.getDeadline() : start);
        if (end.isBefore(start)) {
            end = start;
        }
        AreaTaskPriority pr = req.getPriority() != null ? req.getPriority() : mapPriority(it.getPriority());
        AreaTaskStatus st = req.getStatus() != null ? req.getStatus() : mapItemStatusToTask(it.getStatus());
        List<Long> assignees = req.getAssignedUserIds();
        if (assignees == null || assignees.isEmpty()) {
            if (it.getResponsibleUserId() != null) {
                assignees = new ArrayList<>(List.of(it.getResponsibleUserId()));
            } else {
                assignees = new ArrayList<>();
            }
        }
        Long ownerAssignee = req.getAssignedUserId() != null
                ? req.getAssignedUserId()
                : (!assignees.isEmpty() ? assignees.get(0) : null);
        Long sprintIdForTask = req.getSprintId() != null ? req.getSprintId() : it.getSprintId();
        sprintService.assertSprintOpenForNewTasks(sprintIdForTask);
        CreateAreaTaskDto dto = CreateAreaTaskDto.builder()
                .areaId(areaId)
                .workspaceId(req.getWorkspaceId() != null ? req.getWorkspaceId() : it.getProjectId())
                .sprintId(sprintIdForTask)
                .title(title)
                .description(desc != null && !desc.isBlank() ? desc : null)
                .startDate(start)
                .endDate(end)
                .status(st)
                .priority(pr)
                .assignedUserId(ownerAssignee)
                .assignedUserIds(assignees.isEmpty() ? null : assignees)
                .privateTask(req.getPrivateTask())
                .tags(req.getTags())
                .build();
        AreaTaskResponseDto created = areaTaskService.create(userId, dto);
        it.setConvertedTaskId(created.getId());
        it.setConvertedAt(LocalDateTime.now());
        it.setConvertedByUserId(userId);
        itemRepository.save(it);
        WorkosMeetingMinute full = requireMinute(minuteId, true);
        WorkosMeetingMinuteItem savedItem = full.getItems().stream()
                .filter(x -> x.getId().equals(itemId))
                .findFirst()
                .orElse(it);
        Map<Long, AreaTask> tm = loadTasksById(List.of(created.getId()));
        Set<Long> pids = savedItem.getProjectId() != null ? Set.of(savedItem.getProjectId()) : Set.of();
        Set<Long> sids = savedItem.getSprintId() != null ? Set.of(savedItem.getSprintId()) : Set.of();
        MeetingMinuteItemResponse itemDto = toItemDto(savedItem, tm, projectNames(pids), sprintNames(sids));
        return ConvertMeetingItemToTaskResponse.builder()
                .item(itemDto)
                .task(created)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingMinuteItemResponse> listUnconverted(long userId) {
        User user = requireUser(userId);
        GanttTaskScope sc = scope(user);
        List<WorkosMeetingMinuteItem> raw = itemRepository.findAllUnconverted(MeetingMinuteStatus.CANCELADA);
        List<WorkosMeetingMinuteItem> filtered = raw.stream()
                .filter(i -> itemVisibleToUser(user, sc, i.getMeetingMinute(), i))
                .toList();
        Set<Long> pids = filtered.stream().map(WorkosMeetingMinuteItem::getProjectId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> sids = filtered.stream().map(WorkosMeetingMinuteItem::getSprintId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> pn = projectNames(pids);
        Map<Long, String> sn = sprintNames(sids);
        return filtered.stream().map(i -> toItemDto(i, Map.of(), pn, sn)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingMinutesDashboardKpisResponse dashboardKpis(long userId) {
        User user = requireUser(userId);
        GanttTaskScope sc = scope(user);
        boolean admin = GanttReadableAreas.isPlatformAdmin(user);
        Specification<WorkosMeetingMinute> spec = Specification.allOf(
                MeetingMinuteSpecifications.notDeleted(),
                MeetingMinuteSpecifications.visibleTo(sc, userId, admin));
        List<WorkosMeetingMinute> all = minuteRepository.findAll(spec);
        long open = all.stream().filter(m -> m.getStatus() == MeetingMinuteStatus.ABIERTA).count();
        long follow = all.stream().filter(m -> m.getStatus() == MeetingMinuteStatus.EN_SEGUIMIENTO).count();
        List<MeetingMinuteItemResponse> unc = listUnconverted(userId);
        Set<Long> convIds = new HashSet<>();
        Map<String, Long> respPending = new HashMap<>();
        List<WorkosMeetingMinuteItem> convertedItems = new ArrayList<>();
        for (WorkosMeetingMinute m : all) {
            List<WorkosMeetingMinuteItem> items = itemRepository.findByMeetingMinute_IdOrderByItemOrderAsc(m.getId());
            for (WorkosMeetingMinuteItem it : items) {
                if (!itemVisibleToUser(user, sc, m, it)) {
                    continue;
                }
                Long tid = it.getConvertedTaskId();
                if (tid != null) {
                    convIds.add(tid);
                    convertedItems.add(it);
                } else if (it.getStatus() != WorkosMeetingItemStatus.COMPLETADA) {
                    String nm = it.getResponsibleNameSnapshot() != null ? it.getResponsibleNameSnapshot() : "—";
                    respPending.merge(nm, 1L, Long::sum);
                }
            }
        }
        Map<Long, AreaTask> tmap = loadTasksById(convIds);
        Map<String, Long> areaBlock = new HashMap<>();
        for (WorkosMeetingMinuteItem it : convertedItems) {
            AreaTask t = tmap.get(it.getConvertedTaskId());
            if (t != null && t.getStatus() == AreaTaskStatus.BLOCKED) {
                String an = it.getAreaNameSnapshot() != null ? it.getAreaNameSnapshot() : "—";
                areaBlock.merge(an, 1L, Long::sum);
            }
        }
        long overdue = tmap.values().stream()
                .filter(t -> t.getStatus() != AreaTaskStatus.DONE && t.getEndDate() != null && t.getEndDate().isBefore(LocalDate.now()))
                .count();
        long done = tmap.values().stream().filter(t -> t.getStatus() == AreaTaskStatus.DONE).count();
        double pct = convIds.isEmpty() ? 0.0 : done * 100.0 / convIds.size();
        List<NamedCountDto> topR = respPending.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> NamedCountDto.builder().name(e.getKey()).count(e.getValue()).build())
                .toList();
        List<NamedCountDto> topA = areaBlock.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> NamedCountDto.builder().name(e.getKey()).count(e.getValue()).build())
                .toList();
        return MeetingMinutesDashboardKpisResponse.builder()
                .openMinutes(open)
                .inFollowUpMinutes(follow)
                .unconvertedItemsGlobal(unc.size())
                .tasksBornFromMinutes(convIds.size())
                .overdueTasksFromMinutes(overdue)
                .completionPercentFromMinutes(Math.round(pct * 10.0) / 10.0)
                .topResponsiblesPending(topR)
                .topAreasBlocked(topA)
                .build();
    }
}
