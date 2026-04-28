package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.CreateProjectDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateProjectDto;
import com.yego.backend.entity.yego_gantt.api.response.ProjectResponseDto;

import java.util.List;

public interface ProjectService {

    ProjectResponseDto create(Long requesterId, CreateProjectDto dto);

    List<ProjectResponseDto> findAllActiveForUser(Long requesterId);

    ProjectResponseDto update(Long requesterId, Long id, UpdateProjectDto dto);

    void delete(Long requesterId, Long id);
}
