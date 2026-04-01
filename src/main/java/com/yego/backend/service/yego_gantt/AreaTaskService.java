package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.AreaTaskPriority;
import com.yego.backend.entity.yego_gantt.api.request.CreateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.request.UpdateAreaTaskDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskKpisResponseDto;
import com.yego.backend.entity.yego_gantt.api.response.AreaTaskResponseDto;

import java.util.List;

public interface AreaTaskService {

    List<AreaTaskResponseDto> list(Long userId, Long areaIdFilter, AreaTaskPriority priorityFilter);

    AreaTaskKpisResponseDto kpis(Long userId, Long areaIdFilter, AreaTaskPriority priorityFilter);

    AreaTaskResponseDto getById(Long userId, Long taskId);

    AreaTaskResponseDto create(Long userId, CreateAreaTaskDto dto);

    AreaTaskResponseDto update(Long userId, Long taskId, UpdateAreaTaskDto dto);

    void delete(Long userId, Long taskId);
}
