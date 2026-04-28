package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.request.CreateProjectDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateProjectDto;
import com.yego.backend.entity.yego_gantt.api.response.ProjectResponseDto;
import com.yego.backend.entity.yego_gantt.entities.Project;
import com.yego.backend.entity.yego_gantt.entities.ProjectMember;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_gantt.ProjectMemberRepository;
import com.yego.backend.repository.yego_gantt.ProjectRepository;
import com.yego.backend.repository.yego_principal.AreaRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_gantt.GanttPortfolioAuthorizations;
import com.yego.backend.service.yego_gantt.GanttReadableAreas;
import com.yego.backend.service.yego_gantt.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private static final Set<String> ALLOWED_PROJECT_ICON_KEYS = Set.of(
            "folder", "folder-kanban", "rocket", "briefcase", "layers", "cpu", "sparkles",
            "target", "globe", "zap", "building", "compass", "lightbulb", "box", "heart", "palette");

    private final ProjectRepository projectRepo;
    private final ProjectMemberRepository memberRepo;
    private final AreaTaskRepository areaTaskRepository;
    private final UserRepository userRepo;
    private final AreaRepository areaRepository;

    @Override
    @Transactional
    public ProjectResponseDto create(Long requesterId, CreateProjectDto dto) {
        GanttPortfolioAuthorizations.requirePortfolioManager(userRepo, areaRepository, requesterId,
                "Sin permiso para gestionar proyectos");
        Project p = Project.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .iconKey(normalizeIconKey(dto.getIconKey()))
                .build();
        p = projectRepo.save(p);

        if (dto.getMemberUserIds() != null) {
            for (Long userId : dto.getMemberUserIds()) {
                memberRepo.save(ProjectMember.builder()
                        .projectId(p.getId())
                        .userId(userId)
                        .build());
            }
        }
        return toDto(p);
    }

    @Override
    public List<ProjectResponseDto> findAllActiveForUser(Long requesterId) {
        User user = userRepo.findByIdWithRole(requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (GanttReadableAreas.isPlatformAdmin(user)) {
            return projectRepo.findByActivoTrueOrderByNameAsc()
                    .stream()
                    .map(this::toDto)
                    .toList();
        }

        Set<Long> visibleIds = memberRepo.findByUserId(requesterId).stream()
                .map(ProjectMember::getProjectId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> readableAreas = GanttReadableAreas.forUser(user, areaRepository);
        if (!readableAreas.isEmpty()) {
            visibleIds.addAll(areaTaskRepository.findDistinctProjectIdsByAreaIdIn(readableAreas));
        }

        if (visibleIds.isEmpty()) {
            return List.of();
        }
        return projectRepo.findAllById(visibleIds).stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ProjectResponseDto update(Long requesterId, Long id, UpdateProjectDto dto) {
        GanttPortfolioAuthorizations.requirePortfolioManager(userRepo, areaRepository, requesterId,
                "Sin permiso para gestionar proyectos");
        Project p = projectRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado: " + id));

        if (dto.getName() != null) p.setName(dto.getName().trim());
        if (dto.getDescription() != null) p.setDescription(dto.getDescription());
        if (dto.getIconKey() != null) p.setIconKey(normalizeIconKey(dto.getIconKey()));

        p = projectRepo.save(p);

        if (dto.getMemberUserIds() != null) {
            memberRepo.deleteByProjectId(id);
            for (Long userId : dto.getMemberUserIds()) {
                memberRepo.save(ProjectMember.builder()
                        .projectId(id)
                        .userId(userId)
                        .build());
            }
        }
        return toDto(p);
    }

    @Override
    @Transactional
    public void delete(Long requesterId, Long id) {
        GanttPortfolioAuthorizations.requirePortfolioManager(userRepo, areaRepository, requesterId,
                "Sin permiso para gestionar proyectos");
        memberRepo.deleteByProjectId(id);
        projectRepo.deleteById(id);
    }

    private ProjectResponseDto toDto(Project p) {
        List<Long> memberIds = memberRepo.findByProjectId(p.getId())
                .stream()
                .map(ProjectMember::getUserId)
                .toList();

        return ProjectResponseDto.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .activo(p.getActivo())
                .iconKey(p.getIconKey() != null ? p.getIconKey() : "folder")
                .memberUserIds(memberIds)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private static String normalizeIconKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "folder";
        }
        String k = raw.trim().toLowerCase();
        return ALLOWED_PROJECT_ICON_KEYS.contains(k) ? k : "folder";
    }
}
