package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverValidationResponse {
    private String licenseNumber;
    private boolean existe;
    private String mensaje;
    private List<DriverInfo> drivers;
}

