package com.yego.backend.entity.yego_gantt.api.response;

import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskPriority;
import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskStatus;
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
    private Long workspaceId;
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
    private boolean privateTask;
    private Long createdByUserId;
    private Integer sortOrder;
    private Integer subtaskDone;
    private Integer subtaskTotal;
    /** Vista actual: el usuario tiene al menos una subtarea asignada (tablero / estado sin rol de gestión). */
    private boolean subtaskAssignedToViewer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
