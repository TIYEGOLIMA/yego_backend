package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.request.CreateSprintDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateSprintDto;
import com.yego.backend.entity.yego_gantt.api.response.SprintResponseDto;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskStatus;
import com.yego.backend.entity.yego_gantt.entities.Sprint;
import com.yego.backend.entity.yego_gantt.entities.SprintStatus;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_gantt.ProjectRepository;
import com.yego.backend.repository.yego_gantt.SprintRepository;
import com.yego.backend.repository.yego_principal.AreaRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_gantt.GanttPortfolioAuthorizations;
import com.yego.backend.service.yego_gantt.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SprintServiceImpl implements SprintService {

    private final SprintRepository sprintRepo;
    private final AreaTaskRepository taskRepo;
    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;
    private final AreaRepository areaRepository;

    @Override
    @Transactional
    public SprintResponseDto create(Long requesterId, CreateSprintDto dto) {
        GanttPortfolioAuthorizations.requirePortfolioManager(userRepo, areaRepository, requesterId,
                "Sin permiso para gestionar sprints");
        projectRepo.findById(dto.getProjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Proyecto no encontrado: " + dto.getProjectId()));
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha fin no puede ser anterior al inicio");
        }
        Sprint sprint = Sprint.builder()
                .projectId(dto.getProjectId())
                .name(dto.getName().trim())
                .goal(dto.getGoal())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(dto.getStatus() != null ? dto.getStatus() : SprintStatus.PLANNED)
                .build();
        return toDto(sprintRepo.save(sprint));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SprintResponseDto> findByProject(Long projectId) {
        return sprintRepo.findByProjectIdOrderByStartDateAsc(projectId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public SprintResponseDto update(Long requesterId, Long id, UpdateSprintDto dto) {
        GanttPortfolioAuthorizations.requirePortfolioManager(userRepo, areaRepository, requesterId,
                "Sin permiso para gestionar sprints");
        Sprint sprint = sprintRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint no encontrado: " + id));
        if (dto.getName() != null) sprint.setName(dto.getName().trim());
        if (dto.getGoal() != null) sprint.setGoal(dto.getGoal());
        if (dto.getStartDate() != null) sprint.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) sprint.setEndDate(dto.getEndDate());
        if (dto.getStatus() != null) sprint.setStatus(dto.getStatus());
        if (sprint.getEndDate().isBefore(sprint.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha fin no puede ser anterior al inicio");
        }
        return toDto(sprintRepo.save(sprint));
    }

    @Override
    @Transactional
    public void delete(Long requesterId, Long id) {
        GanttPortfolioAuthorizations.requirePortfolioManager(userRepo, areaRepository, requesterId,
                "Sin permiso para gestionar sprints");
        sprintRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint no encontrado: " + id));
        sprintRepo.deleteById(id);
    }

    private SprintResponseDto toDto(Sprint s) {
        long taskCount = taskRepo.countBySprintId(s.getId());
        long doneCount = taskRepo.countBySprintIdAndStatus(s.getId(), AreaTaskStatus.DONE);

        return SprintResponseDto.builder()
                .id(s.getId())
                .projectId(s.getProjectId())
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
