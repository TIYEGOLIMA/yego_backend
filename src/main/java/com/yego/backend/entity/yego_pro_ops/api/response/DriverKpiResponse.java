package com.yego.backend.entity.yego_pro_ops.api.response;

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
public class DriverKpiResponse {
    
    @JsonProperty("viajeActivo")
    private Integer viajeActivo;
    
    @JsonProperty("noDisponibles")
    private Integer noDisponibles;
    
    @JsonProperty("disponibles")
    private Integer disponibles;
    
    @JsonProperty("sinGPS")
    private Integer sinGPS;
    
    @JsonProperty("items")
    private List<DriverInfoResponse> items;
}

