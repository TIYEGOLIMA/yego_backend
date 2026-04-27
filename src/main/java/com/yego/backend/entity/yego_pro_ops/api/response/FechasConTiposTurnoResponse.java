package com.yego.backend.entity.yego_pro_ops.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FechasConTiposTurnoResponse {

    private String driverId;
    private List<FechaConTiposTurno> fechas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FechaConTiposTurno {
        private LocalDate fecha;
        private List<TipoTurnoInfo> tiposTurno;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TipoTurnoInfo {
        private Long id;
        private String tipoTurno;
    }
}
