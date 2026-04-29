package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.response.AreaTaskAttachmentResponseDto;
import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskAttachment;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_gantt.AreaTaskAttachmentRepository;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_principal.AreaRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.MinIOService;
import com.yego.backend.service.yego_gantt.AreaTaskAttachmentService;
import com.yego.backend.service.yego_gantt.AreaTaskPrivateAccess;
import com.yego.backend.service.yego_gantt.GanttTaskScope;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaTaskAttachmentServiceImpl implements AreaTaskAttachmentService {

    private final AreaTaskAttachmentRepository attachmentRepo;
    private final AreaTaskRepository taskRepo;
    private final UserRepository userRepository;
    private final AreaRepository areaRepository;
    private final MinIOService minIOService;

    private GanttTaskScope resolveScope(User user) {
        return GanttTaskScope.resolve(user, areaRepository);
    }

    private User requireUser(Long userId) {
        return userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private AreaTask requireReadableTask(User user, GanttTaskScope scope, Long taskId) {
        AreaTask task = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
        if (!scope.canAccessArea(task.getAreaId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta tarea");
        }
        if (!AreaTaskPrivateAccess.canSeeTaskContent(user, task)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta tarea");
        }
        return task;
    }

    private void assertCanManage(User user, GanttTaskScope scope, AreaTask task) {
        if (!scope.canAccessArea(task.getAreaId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede modificar adjuntos en esta área");
        }
        AreaTaskPrivateAccess.assertCanMutatePrivateTask(user, task);
    }

    private static AreaTaskAttachmentResponseDto toDto(AreaTaskAttachment a) {
        return AreaTaskAttachmentResponseDto.builder()
                .id(a.getId())
                .taskId(a.getTaskId())
                .objectKey(a.getObjectKey())
                .originalFilename(a.getOriginalFilename())
                .contentType(a.getContentType())
                .sizeBytes(a.getSizeBytes())
                .createdAt(a.getCreatedAt())
                .createdByUserId(a.getCreatedByUserId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaTaskAttachmentResponseDto> list(Long userId, Long taskId) {
        User user = requireUser(userId);
        GanttTaskScope scope = resolveScope(user);
        requireReadableTask(user, scope, taskId);
        return attachmentRepo.findByTaskIdOrderByCreatedAtDescIdDesc(taskId).stream()
                .map(AreaTaskAttachmentServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AreaTaskAttachmentResponseDto upload(Long userId, Long taskId, MultipartFile file) {
        User user = requireUser(userId);
        GanttTaskScope scope = resolveScope(user);
        AreaTask task = requireReadableTask(user, scope, taskId);
        assertCanManage(user, scope, task);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacío");
        }
        String url = minIOService.subirArchivo(file);
        if (url == null || url.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo subir el archivo al almacén");
        }
        AreaTaskAttachment saved = attachmentRepo.save(AreaTaskAttachment.builder()
                .taskId(taskId)
                .objectKey(url)
                .originalFilename(safeFilename(file.getOriginalFilename()))
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .createdByUserId(userId)
                .build());
        return toDto(saved);
    }

    private static String safeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "adjunto";
        }
        String n = name.replace('\0', '_').trim();
        if (n.isEmpty()) {
            return "adjunto";
        }
        return n.length() > 500 ? n.substring(0, 500) : n;
    }

    @Override
    @Transactional
    public void delete(Long userId, Long taskId, Long attachmentId) {
        User user = requireUser(userId);
        GanttTaskScope scope = resolveScope(user);
        AreaTask task = requireReadableTask(user, scope, taskId);
        assertCanManage(user, scope, task);
        AreaTaskAttachment a = attachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adjunto no encontrado"));
        if (!taskId.equals(a.getTaskId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El adjunto no pertenece a la tarea");
        }
        minIOService.eliminarArchivo(a.getObjectKey());
        attachmentRepo.delete(a);
    }
}
