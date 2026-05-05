package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.WorkosTaskMessageCreateRequest;
import com.yego.backend.entity.yego_gantt.api.request.WorkosTaskMessageUpdateRequest;
import com.yego.backend.entity.yego_gantt.api.response.WorkosTaskMessageResponseDto;

import java.util.List;

public interface WorkosTaskMessageService {

    List<WorkosTaskMessageResponseDto> list(long userId, long taskId, Long subtaskIdFilter);

    WorkosTaskMessageResponseDto create(long userId, long taskId, WorkosTaskMessageCreateRequest request);

    void softDelete(long userId, long taskId, long messageId);

    WorkosTaskMessageResponseDto update(long userId, long taskId, long messageId, WorkosTaskMessageUpdateRequest request);
}
