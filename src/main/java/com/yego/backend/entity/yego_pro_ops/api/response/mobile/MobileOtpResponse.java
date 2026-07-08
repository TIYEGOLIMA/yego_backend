package com.yego.backend.entity.yego_pro_ops.api.response.mobile;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MobileOtpResponse {

    private boolean success;
    private String message;
    private String maskedPhone;
    private Integer expiresInMinutes;
}
