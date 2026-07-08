package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.ShiftSessionResponse;

import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

public interface ShiftSessionService {

    ShiftSessionResponse getActiveSession(String driverId);

    List<ShiftSessionResponse> getDriverSessionHistory(String driverId);

    List<ShiftSessionResponse> getClosedSessionsForExternalConsult(String driverId, LocalDateTime desde, LocalDateTime hasta);

    ShiftSessionResponse closeSession(UUID sessionId, Long closedBy);

    ShiftSessionResponse settleSession(UUID sessionId, Long settledBy);

    ShiftSessionResponse updateSessionStatus(UUID sessionId, String newStatus);

    void eliminarSesion(UUID sessionId, Long userId, String reason);
}
