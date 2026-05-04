package com.yego.backend.entity.yego_gantt.api.request;

import com.yego.backend.entity.yego_gantt.entities.enums.WorkosMeetingItemStatus;
import com.yego.backend.entity.yego_gantt.entities.enums.WorkosMeetingItemType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMeetingMinuteItemRequest {

    private Long areaId;

    private String areaNameSnapshot;

    private Long projectId;

    private Long sprintId;

    private WorkosMeetingItemType itemType;

    private String situation;

    private String decision;

    @Size(max = 255)
    private String taskTitle;

    private String taskDescription;

    private Long responsibleUserId;

    private String responsibleNameSnapshot;

    private LocalDate startDate;

    private LocalDate deadline;

    private String priority;

    private WorkosMeetingItemStatus status;
}
