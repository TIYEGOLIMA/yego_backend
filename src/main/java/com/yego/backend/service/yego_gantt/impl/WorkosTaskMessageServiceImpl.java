package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.request.WorkosTaskMessageCreateRequest;
import com.yego.backend.entity.yego_gantt.api.request.WorkosTaskMessageUpdateRequest;
import com.yego.backend.entity.yego_gantt.api.response.WorkosTaskMessageResponseDto;
import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.WorkosTaskMessage;
import com.yego.backend.entity.yego_gantt.entities.enums.WorkosTaskMessageType;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_gantt.AreaTaskSubtaskRepository;
import com.yego.backend.repository.yego_gantt.WorkosTaskMessageListRow;
import com.yego.backend.repository.yego_gantt.WorkosTaskMessageRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_gantt.GanttReadableAreasService;
import com.yego.backend.service.yego_gantt.WorkosTaskMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkosTaskMessageServiceImpl implements WorkosTaskMessageService {

    private static final int MAX_CONTENT = 5000;

    private final WorkosTaskMessageRepository workosTaskMessageRepository;
    private final AreaTaskAccessHelper areaTaskAccessHelper;
    private final AreaTaskSubtaskRepository areaTaskSubtaskRepository;
    private final UserRepository userRepository;
    private final GanttReadableAreasService ganttReadableAreasService;

    @Override
    @Transactional(readOnly = true)
    public List<WorkosTaskMessageResponseDto> list(long userId, long taskId, Long subtaskIdFilter) {
        areaTaskAccessHelper.requireReadableTask(userId, taskId);
        List<WorkosTaskMessageListRow> rows = subtaskIdFilter != null
                ? workosTaskMessageRepository.findVisibleByTaskAndSubtaskWithAuthors(taskId, subtaskIdFilter)
                : workosTaskMessageRepository.findVisibleByTaskWithAuthors(taskId);
        return rows.stream().map(WorkosTaskMessageServiceImpl::fromListRow).toList();
    }

    @Override
    @Transactional
    public WorkosTaskMessageResponseDto create(long userId, long taskId, WorkosTaskMessageCreateRequest request) {
        areaTaskAccessHelper.requireReadableTask(userId, taskId);
        Long subtaskId = request.getSubtaskId();
        if (subtaskId != null) {
            validateSubtaskBelongs(taskId, subtaskId);
        }
        String content = normalizeContent(request.getContent());
        WorkosTaskMessage saved = workosTaskMessageRepository.save(
                WorkosTaskMessage.builder()
                        .taskId(taskId)
                        .subtaskId(subtaskId)
                        .authorUserId(userId)
                        .messageType(WorkosTaskMessageType.USER)
                        .content(content)
                        .isDeleted(false)
                        .build());
        return toDto(saved, lookupAuthorNames(List.of(saved)));
    }

    @Override
    @Transactional
    public void softDelete(long userId, long taskId, long messageId) {
        WorkosTaskMessage msg = workosTaskMessageRepository
                .findVisibleByIdAndTaskId(messageId, taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado"));
        User viewer = areaTaskAccessHelper.requireUser(userId);
        AreaTask task = areaTaskAccessHelper.requireReadableTask(userId, taskId);
        boolean author = Objects.equals(userId, msg.getAuthorUserId());
        boolean admin = ganttReadableAreasService.isPlatformAdmin(viewer);
        if (!author && !admin) {
            areaTaskAccessHelper.assertCanMutateTask(userId, task);
        }
        if (!msg.isDeleted()) {
            msg.setDeleted(true);
            msg.setDeletedAt(LocalDateTime.now());
            workosTaskMessageRepository.save(msg);
        }
    }

    @Override
    @Transactional
    public WorkosTaskMessageResponseDto update(long userId, long taskId, long messageId, WorkosTaskMessageUpdateRequest request) {
        WorkosTaskMessage msg = workosTaskMessageRepository
                .findVisibleByIdAndTaskId(messageId, taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado"));
        if (msg.getMessageType() != WorkosTaskMessageType.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pueden editar mensajes de sistema o resolución");
        }
        if (!Objects.equals(userId, msg.getAuthorUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el autor puede editar este mensaje");
        }
        areaTaskAccessHelper.requireReadableTask(userId, taskId);
        msg.setContent(normalizeContent(request.getContent()));
        return toDto(workosTaskMessageRepository.save(msg), lookupAuthorNames(List.of(msg)));
    }

    private void validateSubtaskBelongs(long taskId, long subtaskId) {
        if (!areaTaskSubtaskRepository.existsByIdAndParentTaskId(subtaskId, taskId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La subtarea no pertenece a esta tarea");
        }
    }

    private static String normalizeContent(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El mensaje no puede estar vacío");
        }
        String t = raw.strip();
        if (t.length() > MAX_CONTENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El mensaje supera los " + MAX_CONTENT + " caracteres");
        }
        return t;
    }

    private static WorkosTaskMessageResponseDto fromListRow(WorkosTaskMessageListRow row) {
        boolean del = Boolean.TRUE.equals(row.getDeleted());
        return WorkosTaskMessageResponseDto.builder()
                .id(row.getId())
                .taskId(row.getTaskId())
                .subtaskId(row.getSubtaskId())
                .authorUserId(row.getAuthorUserId())
                .authorName(row.getAuthorName() != null ? row.getAuthorName() : "")
                .messageType(WorkosTaskMessageType.valueOf(row.getMessageType()))
                .content(row.getContent())
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .deleted(del)
                .build();
    }

    private WorkosTaskMessageResponseDto toDto(WorkosTaskMessage m, Map<Long, String> authorNames) {
        String authorName = m.getAuthorUserId() != null ? authorNames.get(m.getAuthorUserId()) : null;
        return WorkosTaskMessageResponseDto.builder()
                .id(m.getId())
                .taskId(m.getTaskId())
                .subtaskId(m.getSubtaskId())
                .authorUserId(m.getAuthorUserId())
                .authorName(authorName != null ? authorName : "")
                .messageType(m.getMessageType())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .deleted(m.isDeleted())
                .build();
    }

    private Map<Long, String> lookupAuthorNames(List<WorkosTaskMessage> rows) {
        Set<Long> ids = rows.stream()
                .map(WorkosTaskMessage::getAuthorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<User> users = userRepository.findAllById(ids);
        Map<Long, String> out = new LinkedHashMap<>();
        for (User u : users) {
            String name = fullName(u);
            out.put(u.getId(), name != null ? name : "");
        }
        for (Long id : ids) {
            out.putIfAbsent(id, "Usuario #" + id);
        }
        return out;
    }

    private static String fullName(User u) {
        String raw = ((u.getName() != null ? u.getName() : "") + " "
                + (u.getLastName() != null ? u.getLastName() : "")).trim();
        if (!raw.isEmpty()) {
            return raw;
        }
        return u.getUsername();
    }
}
