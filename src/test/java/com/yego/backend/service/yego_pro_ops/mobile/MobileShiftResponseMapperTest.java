package com.yego.backend.service.yego_pro_ops.mobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileShiftResponse;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MobileShiftResponseMapperTest {

    private final MobileShiftResponseMapper mapper = new MobileShiftResponseMapper(new ObjectMapper());

    @Test
    void mapsFinancialTotalsAndEvidenceWithoutBusinessSideEffects() {
        UUID sessionId = UUID.randomUUID();
        ShiftSession session = ShiftSession.builder()
                .id(sessionId)
                .driverId("driver-1")
                .vehicleId("vehicle-1")
                .placa("CRM549")
                .modelo("Toyota Corolla")
                .startedAt(LocalDateTime.of(2026, 7, 20, 8, 0))
                .closedAt(LocalDateTime.of(2026, 7, 20, 12, 30))
                .status("por_validar")
                .totalTrips(9)
                .totalAmount(new BigDecimal("150.00"))
                .totalCash(new BigDecimal("120.00"))
                .totalYape(new BigDecimal("30.00"))
                .build();
        DriverClose close = DriverClose.builder()
                .shiftSessionId(sessionId)
                .placa("CRM549")
                .odometroInicial(1000)
                .odometroFinal(1085)
                .liquidaEfectivo(new BigDecimal("110.00"))
                .liquidaYape(new BigDecimal("20.00"))
                .operacionYape("987654")
                .yapeComprobanteUri("https://files/yape.jpg")
                .gasolinaSoles(new BigDecimal("25.00"))
                .gnvSoles(new BigDecimal("10.00"))
                .otrosGastos(new BigDecimal("5.00"))
                .fotosEvidencia("[\"https://files/receipt.jpg\"]")
                .build();

        MobileShiftResponse response = mapper.toResponse(session, close, null);

        assertEquals("vehicle-1", response.getVehicleId());
        assertEquals(9, response.getTotalViajes());
        assertEquals(85, response.getKmRecorridos());
        assertEquals(new BigDecimal("40.00"), response.getTotalGastos());
        assertEquals(new BigDecimal("130.00"), response.getTotalIngresos());
        assertEquals(new BigDecimal("90.00"), response.getBalance());
        assertEquals("https://files/yape.jpg", response.getYapeComprobanteUri());
        assertEquals(1, response.getFotosEvidencia().size());
    }
}
