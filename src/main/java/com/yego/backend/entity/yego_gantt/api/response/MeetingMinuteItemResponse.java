package com.yego.backend.entity.yego_gantt.api.response;

import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskStatus;
import com.yego.backend.entity.yego_gantt.entities.enums.WorkosMeetingItemStatus;
import com.yego.backend.entity.yego_gantt.entities.enums.WorkosMeetingItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingMinuteItemResponse {
    private Long id;
    private Long meetingMinuteId;
    private Integer itemOrder;
    private Long areaId;
    private String areaNameSnapshot;
    private Long projectId;
    private String projectName;
    private Long sprintId;
    private String sprintName;
    private WorkosMeetingItemType itemType;
    private String situation;
    private String decision;
    private String taskTitle;
    private String taskDescription;
    private Long responsibleUserId;
    private String responsibleNameSnapshot;
    private LocalDate startDate;
    private LocalDate deadline;
    private String priority;
    private WorkosMeetingItemStatus status;
    private boolean converted;
    private Long convertedTaskId;
    private LocalDateTime convertedAt;
    private Long convertedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private AreaTaskStatus taskStatus;
    private Integer taskProgress;
    private LocalDate taskEndDate;
    private Boolean taskIsOverdue;
    private List<Long> taskAssigneeIds;
}
