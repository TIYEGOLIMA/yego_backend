package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.request.CreateSprintDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateSprintDto;
import com.yego.backend.entity.yego_gantt.api.response.SprintResponseDto;
import com.yego.backend.entity.yego_gantt.entities.Sprint;
import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskStatus;
import com.yego.backend.entity.yego_gantt.entities.enums.SprintStatus;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_gantt.ProjectRepository;
import com.yego.backend.repository.yego_gantt.SprintRepository;
import com.yego.backend.service.yego_gantt.GanttPortfolioAuthorizationService;
import com.yego.backend.service.yego_gantt.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SprintServiceImpl implements SprintService {

    private final SprintRepository sprintRepo;
    private final AreaTaskRepository taskRepo;
    private final ProjectRepository projectRepo;
    private final GanttPortfolioAuthorizationService ganttPortfolioAuthorizationService;

    @Override
    @Transactional
    public SprintResponseDto create(Long requesterId, CreateSprintDto dto) {
        ganttPortfolioAuthorizationService.requirePortfolioManager(requesterId,
                "Sin permiso para gestionar sprints");
        projectRepo.findById(dto.getWorkspaceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Espacio de trabajo no encontrado: " + dto.getWorkspaceId()));
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha fin no puede ser anterior al inicio");
        }
        SprintStatus initialStatus = dto.getStatus() != null ? dto.getStatus() : SprintStatus.PLANNED;
        if (initialStatus == SprintStatus.COMPLETED && LocalDate.now().isBefore(dto.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede cerrar el sprint antes de su fecha de fin");
        }
        Sprint sprint = Sprint.builder()
                .workspaceId(dto.getWorkspaceId())
                .name(dto.getName().trim())
                .goal(dto.getGoal())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(initialStatus)
                .build();
        return toDto(sprintRepo.save(sprint));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SprintResponseDto> findByWorkspace(Long workspaceId, boolean assignableOnly) {
        List<Sprint> list = assignableOnly
                ? sprintRepo.findByWorkspaceIdAndStatusInOrderByStartDateAsc(
                        workspaceId, List.of(SprintStatus.PLANNED, SprintStatus.ACTIVE))
                : sprintRepo.findByWorkspaceIdOrderByStartDateAsc(workspaceId);
        return list.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void assertSprintOpenForNewTasks(Long sprintId) {
        if (sprintId == null) {
            return;
        }
        Sprint sprint = sprintRepo.findById(sprintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sprint no encontrado"));
        if (!sprint.getStatus().isOpenForAssignment()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El sprint está completado o cancelado; elige uno planificado o activo para asignar la tarea.");
        }
    }

    @Override
    @Transactional
    public SprintResponseDto update(Long requesterId, Long id, UpdateSprintDto dto) {
        ganttPortfolioAuthorizationService.requirePortfolioManager(requesterId,
                "Sin permiso para gestionar sprints");
        Sprint sprint = sprintRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint no encontrado: " + id));
        SprintStatus previousStatus = sprint.getStatus();
        if (dto.getName() != null) sprint.setName(dto.getName().trim());
        if (dto.getGoal() != null) sprint.setGoal(dto.getGoal());
        if (dto.getStartDate() != null) sprint.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) sprint.setEndDate(dto.getEndDate());
        if (dto.getStatus() != null) sprint.setStatus(dto.getStatus());
        if (sprint.getEndDate().isBefore(sprint.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha fin no puede ser anterior al inicio");
        }
        if (sprint.getStatus() == SprintStatus.COMPLETED && previousStatus != SprintStatus.COMPLETED) {
            if (LocalDate.now().isBefore(sprint.getEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No se puede cerrar el sprint antes de su fecha de fin");
            }
        }
        return toDto(sprintRepo.save(sprint));
    }

    @Override
    @Transactional
    public void delete(Long requesterId, Long id) {
        ganttPortfolioAuthorizationService.requirePlatformAdmin(requesterId,
                "Solo administradores pueden eliminar sprints");
        sprintRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint no encontrado: " + id));
        sprintRepo.deleteById(id);
    }

    private SprintResponseDto toDto(Sprint s) {
        long taskCount = taskRepo.countBySprintId(s.getId());
        long doneCount = taskRepo.countBySprintIdAndStatus(s.getId(), AreaTaskStatus.DONE);

        return SprintResponseDto.builder()
                .id(s.getId())
                .workspaceId(s.getWorkspaceId())
                .name(s.getName())
                .goal(s.getGoal())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .status(s.getStatus())
                .taskCount((int) taskCount)
                .doneCount((int) doneCount)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
