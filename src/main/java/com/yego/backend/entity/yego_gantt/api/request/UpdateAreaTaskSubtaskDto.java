package com.yego.backend.entity.yego_gantt.api.request;

import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskStatus;

import com.yego.backend.entity.yego_gantt.api.AreaTaskSubtaskChecklistItemDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAreaTaskSubtaskDto {

    @Size(max = 500)
    private String title;

    @Size(max = 4000)
    private String description;

    @DecimalMin(value = "0.0001", inclusive = true)
    private BigDecimal weight;

    private Integer sortOrder;

    private Boolean done;

    /** Columna/agrupación independiente del padre en tableros (PENDING | IN_PROGRESS | BLOCKED | DONE). */
    private AreaTaskStatus status;

    private Long assignedUserId;

    private Boolean unassignUser;

    private LocalDate dueDate;

    private Boolean clearDueDate;

    private Long areaId;

    private Long workspaceId;

    /** Quitar proyecto explícito; la subtarea hereda del padre. */
    private Boolean clearWorkspace;

    @Size(max = 4000)
    private String objectives;

    @Valid
    @Size(max = 80)
    private List<AreaTaskSubtaskChecklistItemDto> checklist;
}
