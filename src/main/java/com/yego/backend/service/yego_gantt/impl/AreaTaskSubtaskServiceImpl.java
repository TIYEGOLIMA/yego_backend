package com.yego.backend.service.yego_gantt.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_gantt.api.request.CreateAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.request.MoveAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskSubtaskResponseDto;
import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskSubtask;
import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskStatus;
import com.yego.backend.entity.yego_gantt.entities.Project;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_gantt.AreaTaskSubtaskRepository;
import com.yego.backend.repository.yego_gantt.ProjectRepository;
import com.yego.backend.repository.yego_gantt.WorkosTaskMessageRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_gantt.AreaTaskPrivateAccessService;
import com.yego.backend.service.yego_gantt.AreaTaskSubtaskService;
import com.yego.backend.service.yego_gantt.GanttTaskScope;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaTaskSubtaskServiceImpl implements AreaTaskSubtaskService {

    private final AreaTaskSubtaskRepository subtaskRepo;
    private final AreaTaskRepository taskRepo;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final AreaTaskAccessHelper areaTaskAccessHelper;
    private final AreaTaskPrivateAccessService areaTaskPrivateAccessService;
    private final WorkosTaskMessageRepository workosTaskMessageRepository;
    private final ObjectMapper objectMapper;
    private final AreaTaskSubtaskChecklistCodec checklistCodec;
    private final AreaTaskSubtaskParentFieldsReconciler parentFieldsReconciler;

    private record ParentContext(User user, GanttTaskScope scope, AreaTask parent) {}

    private ParentContext requireReadableParent(Long requesterId, Long parentTaskId) {
        User user = areaTaskAccessHelper.requireUser(requesterId);
        GanttTaskScope scope = areaTaskAccessHelper.resolveScope(user);
        AreaTask parent = areaTaskAccessHelper.requireReadableTask(user, scope, parentTaskId, true);
        return new ParentContext(user, scope, parent);
    }

    private void assertCanManageParent(ParentContext ctx) {
        if (!ctx.scope().canAccessArea(ctx.parent().getAreaId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede editar subtareas en esta área");
        }
        areaTaskPrivateAccessService.assertCanMutatePrivateTask(ctx.user(), ctx.parent());
    }

    /** Padre legible + permiso de mutación (crear / borrar / mover). */
    private ParentContext requireReadableAndManageableParent(Long requesterId, Long parentTaskId) {
        ParentContext ctx = requireReadableParent(requesterId, parentTaskId);
        assertCanManageParent(ctx);
        return ctx;
    }

    private AreaTaskSubtask requireSubtask(Long parentTaskId, Long subtaskId) {
        AreaTaskSubtask s = subtaskRepo.findById(subtaskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subtarea no encontrada"));
        if (!parentTaskId.equals(s.getParentTaskId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subtarea no pertenece a la tarea");
        }
        return s;
    }

    private void assertWorkspaceIfPresent(Long workspaceId) {
        if (workspaceId == null) {
            return;
        }
        Project p = projectRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Espacio de trabajo inválido"));
        if (Boolean.FALSE.equals(p.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Espacio de trabajo inactivo");
        }
    }

    /**
     * Valida área/proyecto efectivos (herencia desde el padre si la subtarea no los fija).
     */
    private void assertSubtaskAreaAndWorkspace(ParentContext ctx, AreaTask parent, Long areaIdDb, Long workspaceIdDb) {
        Long effArea = areaIdDb != null ? areaIdDb : parent.getAreaId();
        Long effWs = workspaceIdDb != null ? workspaceIdDb : parent.getWorkspaceId();
        areaTaskAccessHelper.requireAreaActiva(effArea);
        assertWorkspaceIfPresent(effWs);
        if (!parent.isPrivateTask()) {
            areaTaskAccessHelper.assertCanManage(ctx.scope(), effArea);
        }
    }

    private void assertAssignableUserExists(Long userId) {
        if (userId == null) {
            return;
        }
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario responsable inválido");
        }
    }

    /** La fecha límite no puede preceder al inicio del padre (el fin del padre puede ampliarse automáticamente). */
    private static void assertSubtaskDueNotBeforeParentStart(AreaTask parent, LocalDate dueDate) {
        if (dueDate == null) {
            return;
        }
        if (dueDate.isBefore(parent.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha límite de la subtarea no puede ser anterior al inicio de la tarea padre");
        }
    }

    private int nextSubtaskSortOrder(Long parentTaskId) {
        return subtaskRepo.findMaxSortOrderByParentTaskId(parentTaskId) + 1;
    }

    private void assertAuthorizedForUpdate(ParentContext ctx, AreaTaskSubtask entity, UpdateAreaTaskSubtaskDto dto) {
        boolean heavyStructural = AreaTaskSubtaskPolicies.updateTouchesHeavyStructuralFields(dto);
        boolean declaresDone = dto.getDone() != null;
        boolean declaresStatus = dto.getStatus() != null;
        boolean declaresChecklist = dto.getChecklist() != null;
        if (heavyStructural) {
            assertCanManageParent(ctx);
            return;
        }
        if (declaresDone || declaresStatus || declaresChecklist) {
            Long uid = ctx.user().getId();
            if (!AreaTaskSubtaskPolicies.mayToggleDone(uid, ctx.parent(), entity)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No tiene permiso para actualizar esta subtarea (solo responsables pueden marcar estado o checklist ligero)");
            }
            return;
        }
        assertCanManageParent(ctx);
    }

    /** Aplica campos mutables validando responsable/fecha donde corresponde. */
    private void applyUpdateDto(UpdateAreaTaskSubtaskDto dto, AreaTaskSubtask entity, ParentContext ctx) {
        AreaTask parent = ctx.parent();
        if (dto.getTitle() != null) {
            entity.setTitle(dto.getTitle().trim());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(AreaTaskSubtaskScalars.normalizeDescriptionOrNull(dto.getDescription()));
        }
        if (dto.getWeight() != null) {
            entity.setWeight(dto.getWeight());
        }
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }
        if (dto.getStatus() != null) {
            entity.setKanbanStatus(dto.getStatus());
            entity.setDone(dto.getStatus() == AreaTaskStatus.DONE);
        } else if (dto.getDone() != null) {
            entity.setDone(dto.getDone());
            if (Boolean.TRUE.equals(dto.getDone())) {
                entity.setKanbanStatus(AreaTaskStatus.DONE);
            } else if (entity.getKanbanStatus() == AreaTaskStatus.DONE) {
                entity.setKanbanStatus(AreaTaskStatus.PENDING);
            }
        }
        if (Boolean.TRUE.equals(dto.getUnassignUser())) {
            entity.setAssignedUserId(null);
        } else if (dto.getAssignedUserId() != null) {
            assertAssignableUserExists(dto.getAssignedUserId());
            entity.setAssignedUserId(dto.getAssignedUserId());
        }
        if (Boolean.TRUE.equals(dto.getClearDueDate())) {
            entity.setDueDate(null);
        } else if (dto.getDueDate() != null) {
            assertSubtaskDueNotBeforeParentStart(parent, dto.getDueDate());
            entity.setDueDate(dto.getDueDate());
        }
        if (dto.getAreaId() != null) {
            areaTaskAccessHelper.requireAreaActiva(dto.getAreaId());
            if (!parent.isPrivateTask()) {
                areaTaskAccessHelper.assertCanManage(ctx.scope(), dto.getAreaId());
            }
            entity.setAreaId(Objects.equals(dto.getAreaId(), parent.getAreaId()) ? null : dto.getAreaId());
        }
        if (Boolean.TRUE.equals(dto.getClearWorkspace())) {
            entity.setWorkspaceId(null);
        } else if (dto.getWorkspaceId() != null) {
            assertWorkspaceIfPresent(dto.getWorkspaceId());
            entity.setWorkspaceId(
                    Objects.equals(dto.getWorkspaceId(), parent.getWorkspaceId())
                            ? null
                            : dto.getWorkspaceId());
        }
        if (dto.getObjectives() != null) {
            entity.setObjectives(AreaTaskSubtaskScalars.objectivesOrNull(dto.getObjectives()));
        }
        if (dto.getChecklist() != null) {
            entity.setChecklistJson(checklistCodec.checklistJsonOrNull(dto.getChecklist()));
        }
        boolean touchedAreaWorkspace =
                dto.getAreaId() != null
                        || Boolean.TRUE.equals(dto.getClearWorkspace())
                        || dto.getWorkspaceId() != null;
        if (touchedAreaWorkspace) {
            assertSubtaskAreaAndWorkspace(ctx, parent, entity.getAreaId(), entity.getWorkspaceId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaTaskSubtaskResponseDto> list(Long requesterId, Long parentTaskId) {
        ParentContext ctx = requireReadableParent(requesterId, parentTaskId);
        return subtaskRepo.findByParentTaskIdOrderBySortOrderAscIdAsc(parentTaskId).stream()
                .map(s -> AreaTaskSubtaskDtoMapper.toDto(s, ctx.parent(), objectMapper))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AreaTaskSubtaskResponseDto get(Long requesterId, Long parentTaskId, Long subtaskId) {
        ParentContext ctx = requireReadableParent(requesterId, parentTaskId);
        AreaTaskSubtask entity = requireSubtask(parentTaskId, subtaskId);
        return AreaTaskSubtaskDtoMapper.toDto(entity, ctx.parent(), objectMapper);
    }

    @Override
    @Transactional
    public AreaTaskSubtaskResponseDto create(Long requesterId, Long parentTaskId, CreateAreaTaskSubtaskDto dto) {
        ParentContext ctx = requireReadableAndManageableParent(requesterId, parentTaskId);

        AreaTask parent = ctx.parent();
        Long effectiveAssignee =
                dto.getAssignedUserId() != null ? dto.getAssignedUserId() : AreaTaskSubtaskPolicies.principalAssigneeId(parent);
        assertAssignableUserExists(effectiveAssignee);
        assertSubtaskDueNotBeforeParentStart(parent, dto.getDueDate());

        Long areaDb = dto.getAreaId() != null && !Objects.equals(dto.getAreaId(), parent.getAreaId())
                ? dto.getAreaId()
                : null;
        Long wsDb = dto.getWorkspaceId() != null && !Objects.equals(dto.getWorkspaceId(), parent.getWorkspaceId())
                ? dto.getWorkspaceId()
                : null;
        assertSubtaskAreaAndWorkspace(ctx, parent, areaDb, wsDb);

        int nextOrder = nextSubtaskSortOrder(parentTaskId);
        BigDecimal w = dto.getWeight() != null ? dto.getWeight() : BigDecimal.ONE;

        AreaTaskStatus ks = dto.getStatus() != null ? dto.getStatus() : AreaTaskStatus.PENDING;
        Boolean doneInDto = dto.getDone();
        boolean doneFlag;
        if (doneInDto != null) {
            doneFlag = Boolean.TRUE.equals(doneInDto);
            if (doneFlag) {
                ks = AreaTaskStatus.DONE;
            } else if (ks == AreaTaskStatus.DONE) {
                ks = AreaTaskStatus.PENDING;
            }
        } else {
            doneFlag = ks == AreaTaskStatus.DONE;
        }

        AreaTaskSubtask entity = AreaTaskSubtask.builder()
                .parentTaskId(parentTaskId)
                .title(dto.getTitle().trim())
                .weight(w)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : nextOrder)
                .done(doneFlag)
                .kanbanStatus(ks)
                .assignedUserId(effectiveAssignee)
                .dueDate(dto.getDueDate())
                .description(AreaTaskSubtaskScalars.normalizeDescriptionOrNull(dto.getDescription()))
                .objectives(AreaTaskSubtaskScalars.objectivesOrNull(dto.getObjectives()))
                .checklistJson(checklistCodec.checklistJsonOrNull(dto.getChecklist()))
                .createdByUserId(requesterId)
                .areaId(areaDb)
                .workspaceId(wsDb)
                .build();

        AreaTaskSubtask saved = subtaskRepo.save(entity);
        parentFieldsReconciler.reconcileFully(parentTaskId);
        return AreaTaskSubtaskDtoMapper.toDto(saved, parent, objectMapper);
    }

    @Override
    @Transactional
    public AreaTaskSubtaskResponseDto update(Long requesterId, Long parentTaskId, Long subtaskId, UpdateAreaTaskSubtaskDto dto) {
        ParentContext ctx = requireReadableParent(requesterId, parentTaskId);
        AreaTaskSubtask entity = requireSubtask(parentTaskId, subtaskId);
        assertAuthorizedForUpdate(ctx, entity, dto);
        applyUpdateDto(dto, entity, ctx);

        AreaTaskSubtask saved = subtaskRepo.save(entity);
        boolean progressShaped =
                dto.getDone() != null || dto.getWeight() != null || dto.getStatus() != null;
        parentFieldsReconciler.reconcileAfterSubtaskPatch(parentTaskId, progressShaped);
        return AreaTaskSubtaskDtoMapper.toDto(saved, ctx.parent(), objectMapper);
    }

    @Override
    @Transactional
    public void delete(Long requesterId, Long parentTaskId, Long subtaskId) {
        requireReadableAndManageableParent(requesterId, parentTaskId);
        int removed = subtaskRepo.deleteByIdAndParentTaskId(subtaskId, parentTaskId);
        if (removed == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subtarea no encontrada");
        }
        parentFieldsReconciler.reconcileFully(parentTaskId);
    }

    @Override
    @Transactional
    public AreaTaskSubtaskResponseDto moveToParent(
            Long requesterId, Long fromParentTaskId, Long subtaskId, MoveAreaTaskSubtaskDto dto) {
        Long toParentTaskId = dto.getTargetParentTaskId();
        if (toParentTaskId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tarea destino requerida");
        }
        ParentContext fromCtx = requireReadableAndManageableParent(requesterId, fromParentTaskId);
        ParentContext toCtx = requireReadableAndManageableParent(requesterId, toParentTaskId);

        if (fromParentTaskId.equals(toParentTaskId)) {
            AreaTaskSubtask entity = requireSubtask(fromParentTaskId, subtaskId);
            return AreaTaskSubtaskDtoMapper.toDto(entity, fromCtx.parent(), objectMapper);
        }

        AreaTaskSubtask entity = requireSubtask(fromParentTaskId, subtaskId);
        AreaTask newParent = toCtx.parent();
        assertSubtaskDueNotBeforeParentStart(newParent, entity.getDueDate());

        int nextOrder = nextSubtaskSortOrder(toParentTaskId);
        entity.setParentTaskId(toParentTaskId);
        entity.setSortOrder(nextOrder);
        entity.setAreaId(null);
        entity.setWorkspaceId(null);

        AreaTaskSubtask saved = subtaskRepo.save(entity);
        workosTaskMessageRepository.reassignSubtaskThreadToParent(
                fromParentTaskId, subtaskId, toParentTaskId);

        parentFieldsReconciler.reconcileFully(fromParentTaskId);
        parentFieldsReconciler.reconcileFully(toParentTaskId);
        return AreaTaskSubtaskDtoMapper.toDto(saved, newParent, objectMapper);
    }

    @Override
    @Transactional
    public List<AreaTaskSubtaskResponseDto> reorder(
            Long requesterId, Long parentTaskId, List<Long> orderedSubtaskIds) {
        ParentContext ctx = requireReadableAndManageableParent(requesterId, parentTaskId);
        AreaTask parent = ctx.parent();
        List<AreaTaskSubtask> existing =
                subtaskRepo.findByParentTaskIdOrderBySortOrderAscIdAsc(parentTaskId);
        AreaTaskSubtaskReorderValidator.assertValidReorderPayload(orderedSubtaskIds, existing);
        Map<Long, AreaTaskSubtask> byId =
                existing.stream().collect(Collectors.toMap(AreaTaskSubtask::getId, s -> s));
        int i = 0;
        for (Long sid : orderedSubtaskIds) {
            Objects.requireNonNull(byId.get(sid)).setSortOrder(i++);
        }
        subtaskRepo.saveAll(existing);
        parentFieldsReconciler.reconcileFully(parentTaskId);
        return subtaskRepo.findByParentTaskIdOrderBySortOrderAscIdAsc(parentTaskId).stream()
                .map(s -> AreaTaskSubtaskDtoMapper.toDto(s, parent, objectMapper))
                .toList();
    }

    @Override
    @Transactional
    public void reconcileParentDerivedFields(Long parentTaskId) {
        parentFieldsReconciler.reconcileFully(parentTaskId);
    }
}
