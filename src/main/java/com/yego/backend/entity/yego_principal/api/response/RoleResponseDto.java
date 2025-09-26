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
public class RoleResponseDto {
    private Long id;
    private String name;
    private String description;
    private String permissions;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

