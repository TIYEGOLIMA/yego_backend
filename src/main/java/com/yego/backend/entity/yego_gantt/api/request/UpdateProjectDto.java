package com.yego.backend.entity.yego_gantt.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectDto {

    private String name;

    private String description;

    private String iconKey;

    /** IDs de usuarios (jefes de área) que participan en el proyecto */
    private List<Long> memberUserIds;
}
