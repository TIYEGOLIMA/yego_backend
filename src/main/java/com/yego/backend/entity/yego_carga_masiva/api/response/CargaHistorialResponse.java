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
public class CargaHistorialResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("cargaId")
    private String cargaId;

    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("totalFilas")
    private int totalFilas;

    @JsonProperty("filasInsertadas")
    private int filasInsertadas;

    @JsonProperty("duplicadosOmitidos")
    private int duplicadosOmitidos;

    @JsonProperty("fechaMin")
    private String fechaMin;

    @JsonProperty("fechaMax")
    private String fechaMax;

    @JsonProperty("estado")
    private String estado;

    @JsonProperty("createdAt")
    private String createdAt;
}
