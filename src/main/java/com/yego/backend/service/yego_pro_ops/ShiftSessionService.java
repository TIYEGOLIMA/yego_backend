package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.CloseShiftSessionRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.RegisterTripRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.SettleShiftSessionRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.RegisterTripResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.ShiftSessionResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.ShiftSessionSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.TripResponse;

import java.util.List;
import java.util.UUID;

public interface ShiftSessionService {

    ShiftSessionResponse getActiveSession(String driverId);

    ShiftSessionSummaryResponse getSessionSummary(UUID sessionId);

    List<ShiftSessionResponse> getDriverSessionHistory(String driverId);

    List<TripResponse> getSessionTrips(UUID sessionId);

    RegisterTripResponse registerTrip(RegisterTripRequest request);

    ShiftSessionResponse closeSession(UUID sessionId, Long closedBy);

    ShiftSessionResponse settleSession(UUID sessionId, Long settledBy);
}
