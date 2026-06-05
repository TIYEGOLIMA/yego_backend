package com.yego.backend.entity.yego_carga_masiva.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargaMasivaPreviewResponse {

    @JsonProperty("cargaId")
    private String cargaId;

    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("totalFilas")
    private int totalFilas;

    @JsonProperty("headers")
    private List<String> headers;

    @JsonProperty("preview")
    private List<List<String>> preview;

    @JsonProperty("fechaMin")
    private String fechaMin;

    @JsonProperty("fechaMax")
    private String fechaMax;

    @JsonProperty("duplicados")
    private int duplicados;

    @JsonProperty("solapamiento")
    private boolean solapamiento;

    @JsonProperty("fechasDuplicadas")
    private boolean fechasDuplicadas;

    @JsonProperty("mensaje")
    private String mensaje;
}
