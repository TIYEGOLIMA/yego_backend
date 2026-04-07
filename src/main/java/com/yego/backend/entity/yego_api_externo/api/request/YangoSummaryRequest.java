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

    /** Fecha de referencia (yyyy-MM-dd). Si no se envía, se usa hoy (Lima). */
    @JsonProperty("date")
    private String date;

    @JsonProperty("park_id")
    private String parkId;
}
