package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_gantt.AreaTaskSubtaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Mantiene coherentes el % ponderado del padre y su fecha fin frente al máximo {@code due_date} de subtareas.
 */
@Component
@RequiredArgsConstructor
public class AreaTaskSubtaskParentFieldsReconciler {

    private final AreaTaskSubtaskRepository subtaskRepo;
    private final AreaTaskRepository taskRepo;

    private void syncParentEndDateFromSubtaskDues(Long parentTaskId) {
        Optional<LocalDate> maxDueOpt = subtaskRepo.findMaxDueDateByParentTaskId(parentTaskId);
        if (maxDueOpt.isEmpty()) {
            return;
        }
        AreaTask parent = taskRepo.findById(parentTaskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea padre no encontrada"));
        LocalDate maxDue = maxDueOpt.get();
        LocalDate newEnd = maxDue.isBefore(parent.getStartDate()) ? parent.getStartDate() : maxDue;
        if (!newEnd.equals(parent.getEndDate())) {
            int n = taskRepo.updateEndDateById(parentTaskId, newEnd);
            if (n == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea padre no encontrada");
            }
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

    /**
     * Tras cualquier cambio en subtareas: opcionalmente recalcula % del padre y siempre revisa fin por due dates.
     */
    private void maybeRecalcParentProgressThenSyncEnd(Long parentTaskId, boolean recalcProgressPercent) {
        if (recalcProgressPercent) {
            recalcAndPersistParentProgress(parentTaskId);
        }
        syncParentEndDateFromSubtaskDues(parentTaskId);
    }

    /** Crear / borrar / mover / reorder: padre siempre consistente con el listado actual. */
    public void reconcileFully(Long parentTaskId) {
        maybeRecalcParentProgressThenSyncEnd(parentTaskId, true);
    }

    /** Tras PATCH puntual sobre una subtarea. */
    public void reconcileAfterSubtaskPatch(Long parentTaskId, boolean progressShaped) {
        maybeRecalcParentProgressThenSyncEnd(parentTaskId, progressShaped);
    }
}
