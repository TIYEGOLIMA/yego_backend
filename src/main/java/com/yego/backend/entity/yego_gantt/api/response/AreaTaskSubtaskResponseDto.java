package com.yego.backend.entity.yego_gantt.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskStatus;

import com.yego.backend.entity.yego_gantt.api.AreaTaskSubtaskChecklistItemDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    private String objectives;

    /** Estado Kanban (columna independiente del proyecto padre). */
    private AreaTaskStatus status;

    /** Checklist persistido como JSON en BD; en API como lista materializada. */
    private List<AreaTaskSubtaskChecklistItemDto> checklist;
}
