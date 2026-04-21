package com.yego.backend.entity.yego_ticketerera.api.response;

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
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DispositivoResponse from(Dispositivo dispositivo) {
        return DispositivoResponse.builder()
                .id(dispositivo.getId())
                .name(dispositivo.getName())
                .type(dispositivo.getType())
                .sedeId(dispositivo.getSedeId())
                .moduleId(dispositivo.getModuleId())
                .description(dispositivo.getDescription())
                .active(dispositivo.getActive())
                .createdAt(dispositivo.getCreatedAt())
                .updatedAt(dispositivo.getUpdatedAt())
                .build();
    }

    public static DispositivoResponse from(Dispositivo dispositivo, String sedeNombre) {
        DispositivoResponse response = from(dispositivo);
        response.setSedeNombre(sedeNombre);
        return response;
    }
}
