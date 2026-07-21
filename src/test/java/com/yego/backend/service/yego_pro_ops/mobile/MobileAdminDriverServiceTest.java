package com.yego.backend.service.yego_pro_ops.mobile;

import com.yego.backend.config.yego_pro_ops.YegoProOpsProperties;
import com.yego.backend.entity.yego_api_externo.entities.DriverApi;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.AdminDriverHistoryResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileShiftResponse;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import com.yego.backend.repository.yego_api_externo.DriverApiRepository;
import com.yego.backend.repository.yego_pro_ops.DriverCloseRepository;
import com.yego.backend.repository.yego_pro_ops.ShiftSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileAdminDriverServiceTest {

    private static final String PARK_ID = "test-park";

    @Mock private DriverApiRepository driverRepository;
    @Mock private ShiftSessionRepository shiftRepository;
    @Mock private DriverCloseRepository closeRepository;
    @Mock private MobileShiftResponseMapper responseMapper;

    private MobileAdminDriverService service;

    @BeforeEach
    void setUp() {
        YegoProOpsProperties properties = new YegoProOpsProperties();
        properties.setParkId(PARK_ID);
        service = new MobileAdminDriverService(
                driverRepository,
                shiftRepository,
                closeRepository,
                responseMapper,
                properties
        );
    }

    @Test
    void historyIsPagedAndCapsRequestedPageSize() {
        String driverId = "driver-1";
        DriverApi driver = new DriverApi();
        driver.setDriverId(driverId);
        driver.setParkId(PARK_ID);
        driver.setFullName("Ana Torres");
        ShiftSession session = ShiftSession.builder()
                .id(UUID.randomUUID())
                .driverId(driverId)
                .startedAt(LocalDateTime.now())
                .status("active")
                .build();
        MobileShiftResponse mapped = MobileShiftResponse.builder()
                .sessionId(session.getId().toString())
                .driverId(driverId)
                .build();

        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(shiftRepository.findByDriverIdAndDeletedFalseOrderByStartedAtDescIdDesc(eq(driverId), any()))
                .thenAnswer(invocation -> new PageImpl<>(List.of(session), invocation.getArgument(1), 1));
        when(closeRepository.findByShiftSessionIdIn(any())).thenReturn(List.of());
        when(responseMapper.toResponse(session, null, null)).thenReturn(mapped);

        AdminDriverHistoryResponse response = service.getHistory(driverId, 0, 500);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(shiftRepository).findByDriverIdAndDeletedFalseOrderByStartedAtDescIdDesc(eq(driverId), pageable.capture());
        assertEquals(50, pageable.getValue().getPageSize());
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getShifts().size());
        assertFalse(response.isHasMore());
    }

    @Test
    void derivesDniFromLicenseWithoutDroppingLeadingZero() {
        DriverApi driver = new DriverApi();
        driver.setDriverId("driver-2");
        driver.setParkId(PARK_ID);
        driver.setFullName("Luis Ramos");
        driver.setLicenseNumber("W02632648");

        when(driverRepository.searchByPark(eq(PARK_ID), eq("02632648"), any()))
                .thenReturn(List.of(driver));

        var results = service.search("02632648");

        assertEquals("W02632648", results.get(0).getLicenseNumber());
        assertEquals("02632648", results.get(0).getDocumentNumber());
    }
}
