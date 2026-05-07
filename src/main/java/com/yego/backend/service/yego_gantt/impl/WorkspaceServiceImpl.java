package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.request.CreateWorkspaceDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateWorkspaceDto;
import com.yego.backend.entity.yego_gantt.api.response.WorkspaceResponseDto;
import com.yego.backend.entity.yego_gantt.entities.Project;
import com.yego.backend.entity.yego_gantt.entities.ProjectMember;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_gantt.AreaTaskRepository;
import com.yego.backend.repository.yego_gantt.ProjectMemberRepository;
import com.yego.backend.repository.yego_gantt.ProjectRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_gantt.GanttPortfolioAuthorizationService;
import com.yego.backend.service.yego_gantt.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private static final Set<String> ALLOWED_WORKSPACE_ICON_KEYS = Set.of(
            "folder", "folder-kanban", "rocket", "briefcase", "layers", "cpu", "sparkles",
            "target", "globe", "zap", "building", "compass", "lightbulb", "box", "heart", "palette");

    private final ProjectRepository projectRepo;
    private final ProjectMemberRepository memberRepo;
    private final AreaTaskRepository areaTaskRepository;
    private final UserRepository userRepo;
    private final GanttPortfolioAuthorizationService ganttPortfolioAuthorizationService;

    @Override
    @Transactional
    public WorkspaceResponseDto create(Long requesterId, CreateWorkspaceDto dto) {
        ganttPortfolioAuthorizationService.requireWorkspacePrivileged(requesterId,
                "Sin permiso para crear espacios de trabajo");
        Project p = Project.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .iconKey(normalizeIconKey(dto.getIconKey()))
                .build();
        p = projectRepo.save(p);
        appendMembersIfPresent(p.getId(), dto.getMemberUserIds());
        return toDto(p);
    }

    @Override
    public List<WorkspaceResponseDto> findAllActiveForUser(Long requesterId) {
        User user = requireUser(requesterId);
        if (ganttPortfolioAuthorizationService.readsFullWorkspaceCatalog(user)) {
            return allActiveOrderedDtos();
        }
        Set<Long> ids = collectWorkspaceIdsFromTasksAndMembership(user.getId());
        if (ids.isEmpty()) {
            return allActiveOrderedDtos();
        }
        return activeProjectsSubsetToDtos(ids);
    }

    @Override
    @Transactional
    public WorkspaceResponseDto update(Long requesterId, Long id, UpdateWorkspaceDto dto) {
        ganttPortfolioAuthorizationService.requireWorkspacePrivileged(requesterId,
                "Sin permiso para editar espacios de trabajo");
        Project p = requireWorkspaceById(id);

        if (dto.getName() != null) p.setName(dto.getName().trim());
        if (dto.getDescription() != null) p.setDescription(dto.getDescription());
        if (dto.getIconKey() != null) p.setIconKey(normalizeIconKey(dto.getIconKey()));

        p = projectRepo.save(p);
        replaceMembersIfPresent(id, dto.getMemberUserIds());
        return toDto(p);
    }

    @Override
    @Transactional
    public void delete(Long requesterId, Long id) {
        ganttPortfolioAuthorizationService.requireWorkspacePrivileged(requesterId,
                "Sin permiso para eliminar espacios de trabajo");
        memberRepo.deleteByWorkspaceId(id);
        projectRepo.deleteById(id);
    }

    private User requireUser(Long userId) {
        return userRepo.findByIdWithRole(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private Project requireWorkspaceById(Long id) {
        return projectRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Espacio de trabajo no encontrado: " + id));
    }

    /** Si {@code memberUserIds} es {@code null}, no cambia membresías (el cliente no mandó la lista). */
    private void replaceMembersIfPresent(Long workspaceId, List<Long> memberUserIds) {
        if (memberUserIds == null) return;
        memberRepo.deleteByWorkspaceId(workspaceId);
        saveMembers(workspaceId, memberUserIds);
    }

    /** En alta de espacio: {@code null} → no hay miembros iniciales. */
    private void appendMembersIfPresent(Long workspaceId, List<Long> memberUserIds) {
        if (memberUserIds == null) return;
        saveMembers(workspaceId, memberUserIds);
    }

    private void saveMembers(Long workspaceId, Iterable<Long> memberUserIds) {
        for (Long userId : memberUserIds) {
            memberRepo.save(ProjectMember.builder()
                    .workspaceId(workspaceId)
                    .userId(userId)
                    .build());
        }
    }

    private Set<Long> collectWorkspaceIdsFromTasksAndMembership(long userId) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>(
                areaTaskRepository.findDistinctWorkspaceIdsWhereUserIsAssignee(userId));
        memberRepo.findByUserId(userId).forEach(pm -> ids.add(pm.getWorkspaceId()));
        return ids;
    }

    private List<WorkspaceResponseDto> activeProjectsSubsetToDtos(Set<Long> ids) {
        return projectRepo.findAllById(ids).stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .toList();
    }

    private List<WorkspaceResponseDto> allActiveOrderedDtos() {
        return projectRepo.findByActivoTrueOrderByNameAsc().stream().map(this::toDto).toList();
    }

    private WorkspaceResponseDto toDto(Project p) {
        List<Long> memberIds = memberUserIdsOfWorkspace(p.getId());
        return WorkspaceResponseDto.builder()
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

    private List<Long> memberUserIdsOfWorkspace(Long workspaceId) {
        return memberRepo.findByWorkspaceId(workspaceId).stream().map(ProjectMember::getUserId).toList();
    }

    private static String normalizeIconKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "folder";
        }
        String k = raw.trim().toLowerCase();
        return ALLOWED_WORKSPACE_ICON_KEYS.contains(k) ? k : "folder";
    }
}
