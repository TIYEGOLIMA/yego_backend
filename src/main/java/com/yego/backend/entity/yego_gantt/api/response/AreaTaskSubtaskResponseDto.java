package com.yego.backend.entity.yego_gantt.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaTaskSubtaskResponseDto {
    private Long id;
    private Long parentTaskId;
    private String title;
    private String description;
    private Integer sortOrder;
    private Boolean done;
    private BigDecimal weight;
    private Long assignedUserId;
    private LocalDate dueDate;
    private Long createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** Efectivos (nullable en BD ⇒ se resolvió desde el padre al serializar). */
    private Long areaId;
    private Long workspaceId;
}
