package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.CreateSprintDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateSprintDto;
import com.yego.backend.entity.yego_gantt.api.response.SprintResponseDto;

import java.util.List;

public interface SprintService {

    SprintResponseDto create(Long requesterId, CreateSprintDto dto);

    List<SprintResponseDto> findByProject(Long projectId);

    SprintResponseDto update(Long requesterId, Long id, UpdateSprintDto dto);

    void delete(Long requesterId, Long id);
}
