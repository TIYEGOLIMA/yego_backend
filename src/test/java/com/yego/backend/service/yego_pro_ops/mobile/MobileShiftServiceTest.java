package com.yego.backend.service.yego_pro_ops.mobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_pro_ops.api.request.mobile.CloseShiftMobileRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileShiftResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileShiftSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import com.yego.backend.repository.yego_pro_ops.DriverCloseRepository;
import com.yego.backend.repository.yego_pro_ops.FleetVehicleRepository;
import com.yego.backend.repository.yego_pro_ops.ShiftSessionRepository;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileShiftServiceTest {

    @Mock private ShiftSessionRepository shiftRepository;
    @Mock private DriverCloseRepository closeRepository;
    @Mock private FleetVehicleRepository vehicleRepository;
    @Mock private DriverOrdersService driverOrdersService;
    @Mock private DatabaseLockService lockService;

    private MobileShiftService service;

    @BeforeEach
    void setUp() {
        service = new MobileShiftService(
                shiftRepository,
                closeRepository,
                vehicleRepository,
                driverOrdersService,
                new ObjectMapper(),
                lockService,
                new MobileShiftResponseMapper(new ObjectMapper())
        );
    }

    @Test
    void closedShiftReturnsStoredSummaryWithoutCallingYango() {
        UUID sessionId = UUID.randomUUID();
        LocalDateTime startedAt = LocalDateTime.of(2026, 7, 14, 8, 0);
        LocalDateTime closedAt = LocalDateTime.of(2026, 7, 14, 12, 0);
        ShiftSession session = ShiftSession.builder()
                .id(sessionId)
                .driverId("driver-1")
                .status("por_validar")
                .startedAt(startedAt)
                .closedAt(closedAt)
                .totalTrips(8)
                .totalAmount(new BigDecimal("120.50"))
                .totalCash(new BigDecimal("80.00"))
                .totalYape(new BigDecimal("40.50"))
                .totalDistance(new BigDecimal("72.40"))
                .averagePerTrip(new BigDecimal("15.06"))
                .summarySnapshotSaved(true)
                .build();
        DriverClose close = DriverClose.builder()
                .shiftSessionId(sessionId)
                .odometroInicial(15000)
                .build();
        when(shiftRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(closeRepository.findFirstByShiftSessionIdOrderByIdDesc(sessionId)).thenReturn(Optional.of(close));

        MobileShiftSummaryResponse summary = service.getSummary(sessionId.toString());

        assertFalse(summary.isLive());
        assertEquals(8, summary.getViajes());
        assertEquals(new BigDecimal("120.50"), summary.getProducido());
        assertEquals(new BigDecimal("72.40"), summary.getDistancia());
        verifyNoInteractions(driverOrdersService);
    }

    @Test
    void repeatedCloseReturnsExistingResultWithoutCallingYangoAgain() {
        UUID sessionId = UUID.randomUUID();
        ShiftSession session = ShiftSession.builder()
                .id(sessionId)
                .driverId("driver-1")
                .status("por_validar")
                .startedAt(LocalDateTime.of(2026, 7, 14, 8, 0))
                .closedAt(LocalDateTime.of(2026, 7, 14, 12, 0))
                .totalTrips(6)
                .totalAmount(new BigDecimal("95.40"))
                .build();
        DriverClose close = DriverClose.builder()
                .shiftSessionId(sessionId)
                .placa("CRM549")
                .odometroInicial(15000)
                .odometroFinal(15080)
                .build();
        when(shiftRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(closeRepository.findFirstByShiftSessionIdOrderByIdDesc(sessionId)).thenReturn(Optional.of(close));

        MobileShiftResponse response = service.closeShift(sessionId.toString(), new CloseShiftMobileRequest());

        assertEquals("por_validar", response.getStatus());
        assertEquals(6, response.getTotalViajes());
        assertEquals(new BigDecimal("95.40"), response.getProducido());
        verifyNoInteractions(driverOrdersService);
    }

    @Test
    void closeRejectsMileageLowerThanOpeningWithoutCallingYango() {
        UUID sessionId = UUID.randomUUID();
        ShiftSession session = ShiftSession.builder()
                .id(sessionId)
                .driverId("driver-1")
                .status("active")
                .startedAt(LocalDateTime.of(2026, 7, 14, 8, 0))
                .build();
        DriverClose close = DriverClose.builder()
                .shiftSessionId(sessionId)
                .odometroInicial(15000)
                .build();
        CloseShiftMobileRequest request = new CloseShiftMobileRequest();
        request.setKmFinal(14999);
        when(shiftRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(closeRepository.findFirstByShiftSessionIdOrderByIdDesc(sessionId)).thenReturn(Optional.of(close));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.closeShift(sessionId.toString(), request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(driverOrdersService);
    }
}
