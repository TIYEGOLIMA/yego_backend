package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionDiaInfo {

    @JsonProperty("sessionId")
    private UUID sessionId;

    @JsonProperty("inicio")
    private String inicio;

    @JsonProperty("fin")
    private String fin;

    @JsonProperty("viajes")
    private int viajes;

    @JsonProperty("ingresos")
    private BigDecimal ingresos;

    @JsonProperty("km")
    private BigDecimal km;

    @JsonProperty("status")
    private String status;
}
