package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.CreateAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.request.MoveAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateAreaTaskSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskSubtaskResponseDto;

import java.util.List;

public interface AreaTaskSubtaskService {

    List<AreaTaskSubtaskResponseDto> list(Long requesterId, Long parentTaskId);

    AreaTaskSubtaskResponseDto create(Long requesterId, Long parentTaskId, CreateAreaTaskSubtaskDto dto);

    AreaTaskSubtaskResponseDto update(Long requesterId, Long parentTaskId, Long subtaskId, UpdateAreaTaskSubtaskDto dto);

    void delete(Long requesterId, Long parentTaskId, Long subtaskId);

    /** Mueve la subtarea a otra tarea padre; recalcula progreso y fechas en ambos padres. */
    AreaTaskSubtaskResponseDto moveToParent(
            Long requesterId, Long fromParentTaskId, Long subtaskId, MoveAreaTaskSubtaskDto dto);
}
