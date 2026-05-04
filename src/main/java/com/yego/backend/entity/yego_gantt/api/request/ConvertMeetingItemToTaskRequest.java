package com.yego.backend.entity.yego_gantt.api.request;

import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskPriority;
import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertMeetingItemToTaskRequest {

    /** Si se omite, se usa taskTitle del ítem. */
    @Size(max = 500)
    private String title;

    @Size(max = 4000)
    private String description;

    private Long areaId;

    private Long workspaceId;

    private Long sprintId;

    private LocalDate startDate;

    private LocalDate endDate;

    private AreaTaskStatus status;

    private AreaTaskPriority priority;

    private Long assignedUserId;

    private List<Long> assignedUserIds;

    private Boolean privateTask;

    private List<String> tags;
}
