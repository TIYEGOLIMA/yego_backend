package com.yego.backend.entity.yego_gantt.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertMeetingItemToTaskResponse {
    private MeetingMinuteItemResponse item;
    private AreaTaskResponseDto task;
}
