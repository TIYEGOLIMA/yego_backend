package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.CloseShiftMobileRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.OpenShiftMobileRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.MobileShiftResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.MobileShiftSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface MobileShiftService {
    MobileShiftResponse openShift(OpenShiftMobileRequest request);

    MobileShiftResponse closeShift(UUID sessionId, CloseShiftMobileRequest request);

    void cancelShift(UUID sessionId, Long userId, String reason);

    MobileShiftResponse getActiveShift(String driverId);

    List<MobileShiftResponse> getDriverHistory(String driverId);

    MobileShiftSummaryResponse getSummary(UUID sessionId);
}
