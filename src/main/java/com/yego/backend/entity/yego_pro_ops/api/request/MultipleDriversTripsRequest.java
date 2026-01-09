package com.yego.backend.entity.yego_pro_ops.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultipleDriversTripsRequest {
    
    @NotEmpty(message = "driver_ids no puede estar vacío")
    @JsonProperty("driver_ids")
    private List<String> driverIds;
    
    @JsonProperty("date_from")
    private String dateFrom;
    
    @JsonProperty("date_to")
    private String dateTo;
}


