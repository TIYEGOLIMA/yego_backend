package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.api.request.CreateProjectDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateProjectDto;
import com.yego.backend.entity.yego_gantt.api.response.ProjectResponseDto;
import com.yego.backend.entity.yego_gantt.entities.Project;
import com.yego.backend.entity.yego_gantt.entities.ProjectMember;
import com.yego.backend.repository.yego_gantt.ProjectMemberRepository;
import com.yego.backend.repository.yego_gantt.ProjectRepository;
import com.yego.backend.service.yego_gantt.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepo;
    private final ProjectMemberRepository memberRepo;

    @Override
    @Transactional
    public ProjectResponseDto create(CreateProjectDto dto) {
        Project p = Project.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription())
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
    public List<ProjectResponseDto> findAllActive() {
        return projectRepo.findByActivoTrueOrderByNameAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectResponseDto findOne(Long id) {
        Project p = projectRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado: " + id));
        return toDto(p);
    }

    @Override
    @Transactional
    public ProjectResponseDto update(Long id, UpdateProjectDto dto) {
        Project p = projectRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado: " + id));

        if (dto.getName() != null) p.setName(dto.getName().trim());
        if (dto.getDescription() != null) p.setDescription(dto.getDescription());

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
    public void delete(Long id) {
        memberRepo.deleteByProjectId(id);
        projectRepo.deleteById(id);
    }

    private ProjectResponseDto toDto(Project p) {
        List<Long> memberIds = memberRepo.findByProjectId(p.getId())
                .stream()
                .map(ProjectMember::getUserId)
                .collect(Collectors.toList());

        return ProjectResponseDto.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .activo(p.getActivo())
                .memberUserIds(memberIds)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
