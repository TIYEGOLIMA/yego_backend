package com.yego.backend.entity.yego_api_externo.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YangoPeriod {

    @JsonProperty("date_from")
    private String dateFrom;

    @JsonProperty("date_to")
    private String dateTo;
}
