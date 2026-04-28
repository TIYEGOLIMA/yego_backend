package com.yego.backend.entity.yego_gantt.api.response;

import com.yego.backend.entity.yego_gantt.entities.AreaTaskPriority;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskStatus;
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
public class AreaTaskResponseDto {
    private Long id;
    private Long areaId;
    private String areaName;
    private Long projectId;
    private Long sprintId;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private AreaTaskStatus status;
    private AreaTaskPriority priority;
    private Integer progressPercent;
    private Long assignedUserId;
    private List<Long> assignedUserIds;
    private List<String> tags;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
