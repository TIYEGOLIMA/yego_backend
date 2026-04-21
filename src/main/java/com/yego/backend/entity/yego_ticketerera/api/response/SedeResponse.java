package com.yego.backend.entity.yego_ticketerera.api.response;

import com.yego.backend.entity.yego_ticketerera.entities.Sede;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SedeResponse {

    private Long id;
    private String name;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SedeResponse from(Sede sede) {
        return SedeResponse.builder()
                .id(sede.getId())
                .name(sede.getName())
                .description(sede.getDescription())
                .active(sede.getActive())
                .createdAt(sede.getCreatedAt())
                .updatedAt(sede.getUpdatedAt())
                .build();
    }
}
