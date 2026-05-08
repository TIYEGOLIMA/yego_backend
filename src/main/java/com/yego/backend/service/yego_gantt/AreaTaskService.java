package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.CreateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskResponseDto;
import com.yego.backend.entity.yego_gantt.api.request.ConvertTaskToSubtaskDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskSubtaskResponseDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTasksSummaryResponseDto;

public interface AreaTaskService {

    /** Tareas visibles + KPIs en una sola lectura acotada a BD. */
    AreaTasksSummaryResponseDto summary(long userId, AreaTaskListParams filters);

    AreaTaskResponseDto create(Long userId, CreateAreaTaskDto dto);

    AreaTaskResponseDto update(Long userId, Long taskId, UpdateAreaTaskDto dto);

    void delete(Long userId, Long taskId);

    AreaTaskSubtaskResponseDto convertTaskToSubtask(Long userId, Long taskId, ConvertTaskToSubtaskDto dto);
}
