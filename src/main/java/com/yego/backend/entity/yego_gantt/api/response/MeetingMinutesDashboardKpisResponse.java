package com.yego.backend.entity.yego_gantt.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingMinutesDashboardKpisResponse {
    private long openMinutes;
    private long inFollowUpMinutes;
    private long unconvertedItemsGlobal;
    private long tasksBornFromMinutes;
    private long overdueTasksFromMinutes;
    private double completionPercentFromMinutes;
    private List<NamedCountDto> topResponsiblesPending;
    private List<NamedCountDto> topAreasBlocked;
}
