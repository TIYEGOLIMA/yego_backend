package com.yego.backend.entity.yego_gantt.api.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    private Long assignedUserId;

    private LocalDate dueDate;
}
