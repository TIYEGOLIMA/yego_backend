package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.AreaTaskPriority;
import com.yego.backend.entity.yego_gantt.api.request.CreateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskKpisResponseDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskResponseDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTasksSummaryResponseDto;

import java.util.List;

public interface AreaTaskService {

    List<AreaTaskResponseDto> list(Long userId, Long areaIdFilter, Long projectIdFilter, AreaTaskPriority priorityFilter);

    AreaTaskKpisResponseDto kpis(Long userId, Long areaIdFilter, Long projectIdFilter, AreaTaskPriority priorityFilter);

    /** Carga tareas y KPIs en una sola pasada (un viaje a BD para filas de tareas). */
    AreaTasksSummaryResponseDto summary(Long userId, Long areaIdFilter, Long projectIdFilter, AreaTaskPriority priorityFilter);

    AreaTaskResponseDto create(Long userId, CreateAreaTaskDto dto);

    AreaTaskResponseDto update(Long userId, Long taskId, UpdateAreaTaskDto dto);

    void delete(Long userId, Long taskId);
}
