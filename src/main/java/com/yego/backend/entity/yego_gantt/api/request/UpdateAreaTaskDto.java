package com.yego.backend.entity.yego_gantt.api.request;

import com.yego.backend.entity.yego_gantt.entities.AreaTaskPriority;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class UpdateAreaTaskDto {

    private Long projectId;

    private Long sprintId;

    @Size(max = 500)
    private String title;

    @Size(max = 4000)
    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private AreaTaskStatus status;

    private AreaTaskPriority priority;

    @Min(0)
    @Max(100)
    private Integer progressPercent;

    private Long assignedUserId;

    private List<Long> assignedUserIds;

    private List<String> tags;

    private Integer sortOrder;
}
