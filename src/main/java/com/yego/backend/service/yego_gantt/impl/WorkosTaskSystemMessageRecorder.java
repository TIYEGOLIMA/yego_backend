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
import java.util.List;

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
            String from = describeAssignees(before);
            String to = describeAssignees(after);
            out.add(actor + " cambió los asignados de «" + from + "» a «" + to + "».");
        }
        return out;
    }

    private String describeAssignees(AreaTaskFieldSnapshot s) {
        if (s.assigneeIdsSorted().isEmpty()) {
            return "sin asignar";
        }
        List<String> names = new ArrayList<>();
        for (Long id : s.assigneeIdsSorted()) {
            names.add(formatUserId(id));
        }
        return String.join(", ", names);
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
