package com.yego.backend.entity.yego_api_externo.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class YangoSummaryRequest {

    @JsonProperty("text")
    private String text;

    /**
     * ID de contratista en Yango (mismo valor que {@code resolved_contractor_id} en la respuesta).
     * Si se envía, se omite la búsqueda por suggestions y se ahorra una ida y vuelta HTTP.
     */
    @JsonProperty("contractor_id")
    private String contractorId;

    /** Fecha de referencia (yyyy-MM-dd). Si no se envía, se usa hoy (Lima). */
    @JsonProperty("date")
    private String date;

    @JsonProperty("park_id")
    private String parkId;
}
