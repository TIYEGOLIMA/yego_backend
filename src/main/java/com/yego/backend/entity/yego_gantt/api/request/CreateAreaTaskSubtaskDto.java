package com.yego.backend.entity.yego_gantt.api.request;

import com.yego.backend.entity.yego_gantt.api.AreaTaskSubtaskChecklistItemDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskStatus;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Creación de subtarea: el peso es opcional en API (por defecto 1, coherente con {@code AreaTaskSubtask}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAreaTaskSubtaskDto {

    @NotBlank
    @Size(max = 500)
    private String title;

    @DecimalMin(value = "0.0001", inclusive = true)
    private BigDecimal weight;

    private Integer sortOrder;

    private Boolean done;

    /** Opcional (por defecto PENDING si no marcada como hecha). */
    private AreaTaskStatus status;

    private Long assignedUserId;

    private LocalDate dueDate;

    @Size(max = 4000)
    private String description;

    /** Objetivos o criterios de la subtarea. */
    @Size(max = 4000)
    private String objectives;

    /** Lista de checklist (texto máx. por ítem según ítem validado). */
    @Valid
    @Size(max = 80)
    private List<AreaTaskSubtaskChecklistItemDto> checklist;

    /** Opcional; por defecto se usa el equipo de la tarea padre. */
    private Long areaId;

    /** Opcional (espacio de trabajo / proyecto); por defecto el del padre. */
    private Long workspaceId;
}
