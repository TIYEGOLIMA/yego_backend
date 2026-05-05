package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.request.CreateAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskSubtaskResponseDto;
import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskSubtask;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_gantt.AreaTaskSubtaskRepository;
import com.yego.backend.service.yego_gantt.AreaTaskPrivateAccessService;
import com.yego.backend.service.yego_gantt.AreaTaskSubtaskService;
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
    private final AreaTaskAccessHelper areaTaskAccessHelper;
    private final AreaTaskPrivateAccessService areaTaskPrivateAccessService;

    private void assertCanManageParent(User user, GanttTaskScope scope, AreaTask parent) {
        if (!scope.canAccessArea(parent.getAreaId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede editar subtareas en esta área");
        }
        areaTaskPrivateAccessService.assertCanMutatePrivateTask(user, parent);
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
        areaTaskAccessHelper.requireReadableTask(requesterId, parentTaskId);
        return subtaskRepo.findByParentTaskIdOrderBySortOrderAscIdAsc(parentTaskId).stream()
                .map(AreaTaskSubtaskServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AreaTaskSubtaskResponseDto create(Long requesterId, Long parentTaskId, CreateAreaTaskSubtaskDto dto) {
        User user = areaTaskAccessHelper.requireUser(requesterId);
        GanttTaskScope scope = areaTaskAccessHelper.resolveScope(user);
        AreaTask parent = areaTaskAccessHelper.requireReadableTask(user, scope, parentTaskId);
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
        User user = areaTaskAccessHelper.requireUser(requesterId);
        GanttTaskScope scope = areaTaskAccessHelper.resolveScope(user);
        AreaTask parent = areaTaskAccessHelper.requireReadableTask(user, scope, parentTaskId);
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
        User user = areaTaskAccessHelper.requireUser(requesterId);
        GanttTaskScope scope = areaTaskAccessHelper.resolveScope(user);
        AreaTask parent = areaTaskAccessHelper.requireReadableTask(user, scope, parentTaskId);
        assertCanManageParent(user, scope, parent);
        int removed = subtaskRepo.deleteByIdAndParentTaskId(subtaskId, parentTaskId);
        if (removed == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subtarea no encontrada");
        }
        recalcAndPersistParentProgress(parentTaskId);
    }
}
