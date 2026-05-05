package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskPriority;
import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskStatus;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Copia inmutable de campos de tarea antes de un update, para mensajes SYSTEM */
public record AreaTaskFieldSnapshot(
        AreaTaskStatus status,
        AreaTaskPriority priority,
        LocalDate startDate,
        LocalDate endDate,
        int progressPercent,
        Long assignedUserId,
        List<Long> assigneeIdsSorted
) {
    public static AreaTaskFieldSnapshot from(AreaTask t) {
        List<Long> raw = t.getAssignedUserIds();
        Set<Long> uniq = new HashSet<>();
        if (t.getAssignedUserId() != null) {
            uniq.add(t.getAssignedUserId());
        }
        if (raw != null) {
            for (Long id : raw) {
                if (id != null) {
                    uniq.add(id);
                }
            }
        }
        List<Long> sorted = uniq.stream().sorted().toList();
        int pct = t.getProgressPercent() != null ? t.getProgressPercent() : 0;
        return new AreaTaskFieldSnapshot(
                t.getStatus(),
                t.getPriority(),
                t.getStartDate(),
                t.getEndDate(),
                pct,
                t.getAssignedUserId(),
                sorted
        );
    }
}
