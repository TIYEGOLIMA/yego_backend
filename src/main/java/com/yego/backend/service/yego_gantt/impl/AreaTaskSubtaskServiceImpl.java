package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.request.CreateAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskSubtaskResponseDto;
import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskSubtask;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_gantt.AreaTaskSubtaskRepository;
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

@Service
@RequiredArgsConstructor
public class AreaTaskSubtaskServiceImpl implements AreaTaskSubtaskService {

    private final AreaTaskSubtaskRepository subtaskRepo;
    private final AreaTaskRepository taskRepo;
    private final UserRepository userRepository;
    private final AreaTaskAccessHelper areaTaskAccessHelper;
    private final AreaTaskPrivateAccessService areaTaskPrivateAccessService;

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

    private AreaTaskSubtask requireSubtask(Long parentTaskId, Long subtaskId) {
        AreaTaskSubtask s = subtaskRepo.findById(subtaskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subtarea no encontrada"));
        if (!parentTaskId.equals(s.getParentTaskId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subtarea no pertenece a la tarea");
        }
        return s;
    }

    private static AreaTaskSubtaskResponseDto toDto(AreaTaskSubtask s) {
        return AreaTaskSubtaskResponseDto.builder()
                .id(s.getId())
                .parentTaskId(s.getParentTaskId())
                .title(s.getTitle())
                .sortOrder(s.getSortOrder())
                .done(s.getDone())
                .weight(s.getWeight())
                .assignedUserId(s.getAssignedUserId())
                .dueDate(s.getDueDate())
                .createdByUserId(s.getCreatedByUserId())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private void assertAssignableUserExists(Long userId) {
        if (userId == null) {
            return;
        }
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario responsable inválido");
        }
    }

    private void assertDueDateWithinParent(AreaTask parent, LocalDate dueDate) {
        if (dueDate == null) {
            return;
        }
        if (dueDate.isBefore(parent.getStartDate()) || dueDate.isAfter(parent.getEndDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha límite de la subtarea debe estar entre el inicio y el fin de la tarea padre");
        }
    }

    private void recalcAndPersistParentProgress(Long parentTaskId) {
        Integer computed = subtaskRepo.computeWeightedProgressPercent(parentTaskId);
        int pct = computed != null ? computed : 0;
        int updated = taskRepo.updateProgressPercentById(parentTaskId, pct);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea padre no encontrada");
        }
    }

    private void assertAuthorizedForUpdate(ParentContext ctx, AreaTaskSubtask entity, UpdateAreaTaskSubtaskDto dto) {
        boolean structural = AreaTaskSubtaskPolicies.updateTouchesNonDoneFields(dto);
        boolean declaresDone = dto.getDone() != null;
        if (structural) {
            assertCanManageParent(ctx);
            return;
        }
        if (declaresDone) {
            Long uid = ctx.user().getId();
            if (!AreaTaskSubtaskPolicies.mayToggleDone(uid, ctx.parent(), entity)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede marcar esta subtarea");
            }
            return;
        }
        assertCanManageParent(ctx);
    }

    /** Aplica campos mutables validando responsable/fecha donde corresponde. */
    private void applyUpdateDto(UpdateAreaTaskSubtaskDto dto, AreaTaskSubtask entity, AreaTask parent) {
        if (dto.getTitle() != null) {
            entity.setTitle(dto.getTitle().trim());
        }
        if (dto.getWeight() != null) {
            entity.setWeight(dto.getWeight());
        }
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }
        if (dto.getDone() != null) {
            entity.setDone(dto.getDone());
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
            assertDueDateWithinParent(parent, dto.getDueDate());
            entity.setDueDate(dto.getDueDate());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaTaskSubtaskResponseDto> list(Long requesterId, Long parentTaskId) {
        requireReadableParent(requesterId, parentTaskId);
        return subtaskRepo.findByParentTaskIdOrderBySortOrderAscIdAsc(parentTaskId).stream()
                .map(AreaTaskSubtaskServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AreaTaskSubtaskResponseDto create(Long requesterId, Long parentTaskId, CreateAreaTaskSubtaskDto dto) {
        ParentContext ctx = requireReadableParent(requesterId, parentTaskId);
        assertCanManageParent(ctx);

        AreaTask parent = ctx.parent();
        Long effectiveAssignee =
                dto.getAssignedUserId() != null ? dto.getAssignedUserId() : AreaTaskSubtaskPolicies.principalAssigneeId(parent);
        assertAssignableUserExists(effectiveAssignee);
        assertDueDateWithinParent(parent, dto.getDueDate());

        int nextOrder = subtaskRepo.findMaxSortOrderByParentTaskId(parentTaskId) + 1;
        BigDecimal w = dto.getWeight() != null ? dto.getWeight() : BigDecimal.ONE;

        AreaTaskSubtask entity = AreaTaskSubtask.builder()
                .parentTaskId(parentTaskId)
                .title(dto.getTitle().trim())
                .weight(w)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : nextOrder)
                .done(Boolean.TRUE.equals(dto.getDone()))
                .assignedUserId(effectiveAssignee)
                .dueDate(dto.getDueDate())
                .createdByUserId(requesterId)
                .build();

        AreaTaskSubtask saved = subtaskRepo.save(entity);
        recalcAndPersistParentProgress(parentTaskId);
        return toDto(saved);
    }

    @Override
    @Transactional
    public AreaTaskSubtaskResponseDto update(Long requesterId, Long parentTaskId, Long subtaskId, UpdateAreaTaskSubtaskDto dto) {
        ParentContext ctx = requireReadableParent(requesterId, parentTaskId);
        AreaTaskSubtask entity = requireSubtask(parentTaskId, subtaskId);
        assertAuthorizedForUpdate(ctx, entity, dto);
        applyUpdateDto(dto, entity, ctx.parent());

        AreaTaskSubtask saved = subtaskRepo.save(entity);
        if (dto.getDone() != null || dto.getWeight() != null) {
            recalcAndPersistParentProgress(parentTaskId);
        }
        return toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long requesterId, Long parentTaskId, Long subtaskId) {
        ParentContext ctx = requireReadableParent(requesterId, parentTaskId);
        assertCanManageParent(ctx);
        int removed = subtaskRepo.deleteByIdAndParentTaskId(subtaskId, parentTaskId);
        if (removed == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subtarea no encontrada");
        }
        recalcAndPersistParentProgress(parentTaskId);
    }
}
