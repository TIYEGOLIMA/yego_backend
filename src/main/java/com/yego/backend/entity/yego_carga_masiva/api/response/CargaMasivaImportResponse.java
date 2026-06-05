package com.yego.backend.entity.yego_carga_masiva.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargaMasivaImportResponse {

    @JsonProperty("importado")
    private boolean importado;

    @JsonProperty("filasImportadas")
    private int filasImportadas;

    @JsonProperty("cargaId")
    private String cargaId;

    @JsonProperty("mensaje")
    private String mensaje;
}
