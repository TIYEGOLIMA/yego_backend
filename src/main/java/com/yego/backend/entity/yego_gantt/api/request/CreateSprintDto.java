package com.yego.backend.entity.yego_gantt.api.request;

import com.yego.backend.entity.yego_gantt.entities.SprintStatus;
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
public class CreateSprintDto {

    @NotNull
    private Long projectId;

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 2000)
    private String goal;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private SprintStatus status;
}
