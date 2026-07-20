package com.yego.backend.entity.yego_pro_ops.api.response.mobile;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminDriverResponse {
    String driverId;
    String fullName;
    String licenseNumber;
    String documentType;
    String documentNumber;
    String phone;
    String workStatus;
    Boolean active;
    String carNumber;
}
