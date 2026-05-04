package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.request.CreateAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskSubtaskResponseDto;
import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskSubtask;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_gantt.AreaTaskSubtaskRepository;
import com.yego.backend.repository.yego_principal.AreaRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_gantt.AreaTaskSubtaskService;
import com.yego.backend.service.yego_gantt.AreaTaskPrivateAccess;
import com.yego.backend.service.yego_gantt.AreaTaskVisibility;
import com.yego.backend.service.yego_gantt.GanttTaskScope;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaTaskSubtaskServiceImpl implements AreaTaskSubtaskService {

    private final AreaTaskSubtaskRepository subtaskRepo;
    private final AreaTaskRepository taskRepo;
    private final UserRepository userRepository;
    private final AreaRepository areaRepository;

    private GanttTaskScope resolveScope(User user) {
        return GanttTaskScope.resolve(user, areaRepository);
    }

    private User requireUser(Long userId) {
        return userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private AreaTask requireReadableParent(User user, GanttTaskScope scope, Long parentTaskId) {
        AreaTask task = taskRepo.findById(parentTaskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
        if (!AreaTaskPrivateAccess.canSeeTaskContent(user, task)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta tarea");
        }
        if (!AreaTaskVisibility.canReadTaskByScopeAndAssignment(user, scope, task)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta tarea");
        }
        return task;
    }

    private void assertCanManageParent(User user, GanttTaskScope scope, AreaTask parent) {
        if (!scope.canAccessArea(parent.getAreaId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede editar subtareas en esta área");
        }
        AreaTaskPrivateAccess.assertCanMutatePrivateTask(user, parent);
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
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private void recalcAndPersistParentProgress(Long parentTaskId) {
        Integer computed = subtaskRepo.computeWeightedProgressPercent(parentTaskId);
        int pct = computed != null ? computed : 0;
        int updated = taskRepo.updateProgressPercentById(parentTaskId, pct);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea padre no encontrada");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaTaskSubtaskResponseDto> list(Long requesterId, Long parentTaskId) {
        User user = requireUser(requesterId);
        GanttTaskScope scope = resolveScope(user);
        requireReadableParent(user, scope, parentTaskId);
        return subtaskRepo.findByParentTaskIdOrderBySortOrderAscIdAsc(parentTaskId).stream()
                .map(AreaTaskSubtaskServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AreaTaskSubtaskResponseDto create(Long requesterId, Long parentTaskId, CreateAreaTaskSubtaskDto dto) {
        User user = requireUser(requesterId);
        GanttTaskScope scope = resolveScope(user);
        AreaTask parent = requireReadableParent(user, scope, parentTaskId);
        assertCanManageParent(user, scope, parent);
        int nextOrder = subtaskRepo.findMaxSortOrderByParentTaskId(parentTaskId) + 1;
        BigDecimal w = dto.getWeight() != null ? dto.getWeight() : BigDecimal.ONE;
        AreaTaskSubtask s = AreaTaskSubtask.builder()
                .parentTaskId(parentTaskId)
                .title(dto.getTitle().trim())
                .weight(w)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : nextOrder)
                .done(dto.getDone() != null ? dto.getDone() : false)
                .build();
        AreaTaskSubtask saved = subtaskRepo.save(s);
        recalcAndPersistParentProgress(parentTaskId);
        return toDto(saved);
    }

    @Override
    @Transactional
    public AreaTaskSubtaskResponseDto update(Long requesterId, Long parentTaskId, Long subtaskId, UpdateAreaTaskSubtaskDto dto) {
        User user = requireUser(requesterId);
        GanttTaskScope scope = resolveScope(user);
        AreaTask parent = requireReadableParent(user, scope, parentTaskId);
        assertCanManageParent(user, scope, parent);
        AreaTaskSubtask s = requireSubtask(parentTaskId, subtaskId);
        if (dto.getTitle() != null) {
            s.setTitle(dto.getTitle().trim());
        }
        if (dto.getWeight() != null) {
            s.setWeight(dto.getWeight());
        }
        if (dto.getSortOrder() != null) {
            s.setSortOrder(dto.getSortOrder());
        }
        if (dto.getDone() != null) {
            s.setDone(dto.getDone());
        }
        AreaTaskSubtask saved = subtaskRepo.save(s);
        boolean progressRelevant = dto.getDone() != null || dto.getWeight() != null;
        if (progressRelevant) {
            recalcAndPersistParentProgress(parentTaskId);
        }
        return toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long requesterId, Long parentTaskId, Long subtaskId) {
        User user = requireUser(requesterId);
        GanttTaskScope scope = resolveScope(user);
        AreaTask parent = requireReadableParent(user, scope, parentTaskId);
        assertCanManageParent(user, scope, parent);
        int removed = subtaskRepo.deleteByIdAndParentTaskId(subtaskId, parentTaskId);
        if (removed == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subtarea no encontrada");
        }
        recalcAndPersistParentProgress(parentTaskId);
    }
}
