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
public class DriverSimpleResponse {

    @JsonProperty("conductores")
    private List<DriverInfo> conductores;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverInfo {
        @JsonProperty("driverId")
        private String driverId;

        @JsonProperty("nombre")
        private String nombre;

        @JsonProperty("telefono")
        private String telefono;

        @JsonProperty("avatarUrl")
        private String avatarUrl;
    }
}
