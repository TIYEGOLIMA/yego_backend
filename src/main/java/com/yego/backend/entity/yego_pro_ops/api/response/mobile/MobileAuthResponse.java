package com.yego.backend.entity.yego_pro_ops.api.response.mobile;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MobileAuthResponse {

    private boolean success;
    private String token;
    private Driver driver;

    @Data
    @Builder
    public static class Driver {
        private String id;
        private String nombre;
        private String licencia;
        private String telefono;
    }
}
