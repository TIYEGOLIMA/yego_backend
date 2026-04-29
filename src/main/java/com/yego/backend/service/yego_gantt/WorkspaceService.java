package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.CreateWorkspaceDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateWorkspaceDto;
import com.yego.backend.entity.yego_gantt.api.response.WorkspaceResponseDto;

import java.util.List;

public interface WorkspaceService {

    WorkspaceResponseDto create(Long requesterId, CreateWorkspaceDto dto);

    List<WorkspaceResponseDto> findAllActiveForUser(Long requesterId);

    WorkspaceResponseDto update(Long requesterId, Long id, UpdateWorkspaceDto dto);

    void delete(Long requesterId, Long id);
}
