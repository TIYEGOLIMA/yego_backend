package com.yego.backend.entity.yego_gantt.api.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    private Long assignedUserId;

    private Boolean unassignUser;

    private LocalDate dueDate;

    private Boolean clearDueDate;

    private Long areaId;

    private Long workspaceId;

    /** Quitar proyecto explícito; la subtarea hereda del padre. */
    private Boolean clearWorkspace;
}
