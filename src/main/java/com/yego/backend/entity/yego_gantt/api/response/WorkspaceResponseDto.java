package com.yego.backend.entity.yego_gantt.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceResponseDto {

    private Long id;
    private String name;
    private String description;
    private Boolean activo;

    private String iconKey;

    /** IDs de usuarios miembros del espacio de trabajo */
    private List<Long> memberUserIds;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
