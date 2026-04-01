package com.yego.backend.entity.yego_gantt.api.request;

import com.yego.backend.entity.yego_gantt.entities.AreaTaskPriority;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAreaTaskDto {

    @NotNull
    private Long areaId;

    @NotBlank
    @Size(max = 500)
    private String title;

    @Size(max = 4000)
    private String description;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private AreaTaskStatus status;

    private AreaTaskPriority priority;

    @Min(0)
    @Max(100)
    private Integer progressPercent;

    private Long assignedUserId;

    private Integer sortOrder;
}
