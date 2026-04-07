package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.CreateProjectDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateProjectDto;
import com.yego.backend.entity.yego_gantt.api.response.ProjectResponseDto;

import java.util.List;

public interface ProjectService {

    ProjectResponseDto create(CreateProjectDto dto);

    List<ProjectResponseDto> findAllActive();

    ProjectResponseDto findOne(Long id);

    ProjectResponseDto update(Long id, UpdateProjectDto dto);

    void delete(Long id);
}
