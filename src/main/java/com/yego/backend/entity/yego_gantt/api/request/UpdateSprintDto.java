package com.yego.backend.entity.yego_gantt.api.request;

import com.yego.backend.entity.yego_gantt.entities.SprintStatus;
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
public class UpdateSprintDto {

    @Size(max = 200)
    private String name;

    @Size(max = 2000)
    private String goal;

    private LocalDate startDate;
    private LocalDate endDate;
    private SprintStatus status;
}
