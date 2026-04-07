package com.yego.backend.entity.yego_gantt.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectDto {

    @NotBlank
    private String name;

    private String description;

    /** IDs de usuarios (jefes de área) que participan en el proyecto */
    private List<Long> memberUserIds;
}
