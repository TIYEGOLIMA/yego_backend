package com.yego.backend.entity.yego_ticketerera.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yego.backend.entity.yego_ticketerera.entities.Dispositivo;
import com.yego.backend.entity.yego_ticketerera.entities.Dispositivo.TipoDispositivo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispositivoResponse {

    private Long id;
    private String name;
    private TipoDispositivo type;
    private Long sedeId;
    private String sedeNombre;
    private Long moduleId;
    private String moduleNombre;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Token en texto plano. Solo se rellena al crear o regenerar. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String accessTokenPlain;

    public static DispositivoResponse from(Dispositivo dispositivo, String sedeNombre, String moduleNombre) {
        return DispositivoResponse.builder()
                .id(dispositivo.getId())
                .name(dispositivo.getName())
                .type(dispositivo.getType())
                .sedeId(dispositivo.getSedeId())
                .sedeNombre(sedeNombre)
                .moduleId(dispositivo.getModuleId())
                .moduleNombre(moduleNombre)
                .description(dispositivo.getDescription())
                .active(dispositivo.getActive())
                .createdAt(dispositivo.getCreatedAt())
                .updatedAt(dispositivo.getUpdatedAt())
                .build();
    }
}
