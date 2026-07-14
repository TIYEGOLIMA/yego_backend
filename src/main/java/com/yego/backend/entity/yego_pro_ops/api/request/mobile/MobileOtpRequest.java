package com.yego.backend.entity.yego_pro_ops.api.request.mobile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MobileOtpRequest {

    @NotBlank(message = "licenseNumber es requerido")
    private String licenseNumber;

    @NotBlank(message = "deviceId es requerido")
    private String deviceId;

    private String appVersion;
}
