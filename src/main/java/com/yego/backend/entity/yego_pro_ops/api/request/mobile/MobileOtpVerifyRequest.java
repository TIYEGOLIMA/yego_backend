package com.yego.backend.entity.yego_pro_ops.api.request.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MobileOtpVerifyRequest {

    @NotBlank(message = "licenseNumber es requerido")
    private String licenseNumber;

    @NotBlank(message = "code es requerido")
    @Pattern(regexp = "\\d{6}", message = "code debe tener 6 digitos")
    private String code;

    private String appVersion;
}
