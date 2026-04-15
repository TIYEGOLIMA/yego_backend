package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaResponseDto {
    private Long id;
    private String name;
    private String description;
    private Long managerId;
    private String managerName;
    private Long supervisorId;
    private String supervisorName;
    private Boolean activo;
    private Long colaboradoresCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
