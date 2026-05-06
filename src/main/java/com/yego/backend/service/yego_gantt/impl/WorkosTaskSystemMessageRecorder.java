package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.entities.WorkosTaskMessage;
import com.yego.backend.entity.yego_gantt.entities.enums.WorkosTaskMessageType;
import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskPriority;
import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskStatus;
import com.yego.backend.repository.yego_gantt.WorkosTaskMessageRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_principal.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Inserta mensajes SYSTEM tras actualización de tarea. Sin dependencia de {@code AreaTaskService}.
 */
@Component
@RequiredArgsConstructor
public class WorkosTaskSystemMessageRecorder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final WorkosTaskMessageRepository workosTaskMessageRepository;
    private final UserRepository userRepository;

    @Transactional
    public void recordAfterTaskUpdate(long actorUserId, long taskId, AreaTaskFieldSnapshot before, AreaTask afterTask) {
        AreaTaskFieldSnapshot after = AreaTaskFieldSnapshot.from(afterTask);
        String actor = displayActor(actorUserId);
        List<String> lines = buildLines(actor, before, after);
        for (String content : lines) {
            WorkosTaskMessage msg = WorkosTaskMessage.builder()
                    .taskId(taskId)
                    .subtaskId(null)
                    .authorUserId(actorUserId)
                    .messageType(WorkosTaskMessageType.SYSTEM)
                    .content(content)
                    .isDeleted(false)
                    .build();
            workosTaskMessageRepository.save(msg);
        }
    }

    private String displayActor(Long userId) {
        if (userId == null) {
            return "Usuario";
        }
        return userRepository.findById(userId)
                .map(this::fullNameOrUsername)
                .filter(s -> !s.isBlank())
                .orElse("Usuario");
    }

    private String fullNameOrUsername(User u) {
        String name = ((u.getName() != null ? u.getName() : "") + " " + (u.getLastName() != null ? u.getLastName() : "")).trim();
        if (!name.isEmpty()) {
            return name;
        }
        return u.getUsername() != null ? u.getUsername() : "";
    }

    private List<String> buildLines(String actor, AreaTaskFieldSnapshot before, AreaTaskFieldSnapshot after) {
        List<String> out = new ArrayList<>();
        if (before.status() != after.status()) {
            out.add(actor + " cambió el estado de " + spanishStatus(before.status()) + " a " + spanishStatus(after.status()) + ".");
        }
        if (before.priority() != after.priority()) {
            out.add(actor + " cambió la prioridad de " + spanishPriority(before.priority()) + " a "
                    + spanishPriority(after.priority()) + ".");
        }
        if (!before.startDate().equals(after.startDate())) {
            out.add(actor + " cambió la fecha de inicio de " + before.startDate().format(DATE_FMT) + " a "
                    + after.startDate().format(DATE_FMT) + ".");
        }
        if (!before.endDate().equals(after.endDate())) {
            out.add(actor + " cambió la fecha de fin de " + before.endDate().format(DATE_FMT) + " a "
                    + after.endDate().format(DATE_FMT) + ".");
        }
        if (before.progressPercent() != after.progressPercent()) {
            out.add(actor + " actualizó el avance de " + before.progressPercent() + "% a " + after.progressPercent() + "%.");
        }
        boolean assigneesChanged = !before.assigneeIdsSorted().equals(after.assigneeIdsSorted());
        if (assigneesChanged) {
            out.add(compactAssigneeChangeMessage(actor, before, after));
        }
        return out;
    }

    /**
     * Historial corto: recuento (antes→después) + solo deltas (entraron/salieron), sin repetir todo el equipo.
     */
    private String compactAssigneeChangeMessage(String actor, AreaTaskFieldSnapshot before, AreaTaskFieldSnapshot after) {
        List<Long> oldIds = before.assigneeIdsSorted();
        List<Long> newIds = after.assigneeIdsSorted();
        Set<Long> oldSet = new LinkedHashSet<>(oldIds);
        Set<Long> newSet = new LinkedHashSet<>(newIds);
        List<Long> entered = new ArrayList<>();
        for (Long id : newIds) {
            if (!oldSet.contains(id)) {
                entered.add(id);
            }
        }
        List<Long> left = new ArrayList<>();
        for (Long id : oldIds) {
            if (!newSet.contains(id)) {
                left.add(id);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(actor)
                .append(" actualizó asignaciones (")
                .append(oldIds.size())
                .append(" → ")
                .append(newIds.size())
                .append(")");
        if (!left.isEmpty()) {
            sb.append(". Salieron: ").append(formatNamesPreview(left, 2));
        }
        if (!entered.isEmpty()) {
            sb.append(". Entraron: ").append(formatNamesPreview(entered, 2));
        }
        sb.append(".");
        return sb.toString();
    }

    private String formatNamesPreview(List<Long> userIds, int maxShown) {
        if (userIds.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>(userIds.size());
        for (Long id : userIds) {
            names.add(compactDisplayName(formatUserId(id)));
        }
        if (names.size() <= maxShown) {
            return String.join(", ", names);
        }
        return String.join(", ", names.subList(0, maxShown))
                + " (+" + (names.size() - maxShown) + ")";
    }

    /** Evita líneas gigantes cuando el nombre completo es muy largo. */
    private String compactDisplayName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String t = fullName.trim();
        final int maxChars = 32;
        if (t.length() <= maxChars) {
            return t;
        }
        return t.substring(0, maxChars - 1).trim() + "…";
    }

    private String formatUserId(long id) {
        return userRepository.findById(id)
                .map(this::fullNameOrUsername)
                .filter(str -> !str.isBlank())
                .orElse("Usuario #" + id);
    }

    private static String spanishStatus(AreaTaskStatus status) {
        if (status == null) {
            return "?";
        }
        return switch (status) {
            case PENDING -> "Pendiente";
            case IN_PROGRESS -> "En progreso";
            case DONE -> "Completada";
            case BLOCKED -> "Bloqueada";
        };
    }

    private static String spanishPriority(AreaTaskPriority priority) {
        if (priority == null) {
            return "?";
        }
        return switch (priority) {
            case LOW -> "Baja";
            case MEDIUM -> "Media";
            case HIGH -> "Alta";
            case URGENT -> "Urgente";
        };
    }
}
