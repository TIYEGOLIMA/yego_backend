package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.config.yego_pro_ops.OperationalMonitoringProperties;
import com.yego.backend.entity.yego_pro_ops.api.request.OperationalTripFactInput;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalShiftEvent;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalTripFact;
import com.yego.backend.repository.yego_pro_ops.OperationalShiftEventRepository;
import com.yego.backend.repository.yego_pro_ops.OperationalTripFactRepository;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import com.yego.backend.service.yego_pro_ops.OperationalVehicleKeyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalTripFactServiceImplTest {

    @Mock
    private OperationalTripFactRepository tripFactRepository;
    @Mock
    private OperationalShiftEventRepository shiftEventRepository;
    @Mock
    private DriverOrdersService driverOrdersService;

    private OperationalTripFactServiceImpl service;

    @BeforeEach
    void setUp() {
        OperationalVehicleKeyResolver resolver = new OperationalVehicleKeyResolverImpl();
        OperationalMonitoringProperties properties = new OperationalMonitoringProperties(
                "America/Lima", 8, 90, 90, "complete", true);
        service = new OperationalTripFactServiceImpl(
                tripFactRepository,
                shiftEventRepository,
                resolver,
                driverOrdersService,
                new OperationalDateRangeParser(properties));
    }

    @Test
    void upsertByExternalTripIdDoesNotDuplicate() {
        AtomicReference<OperationalTripFact> store = new AtomicReference<>();
        List<OperationalShiftEvent> events = new ArrayList<>();

        when(tripFactRepository.findByExternalTripId("trip-1")).thenAnswer(invocation -> Optional.ofNullable(store.get()));
        when(tripFactRepository.save(any(OperationalTripFact.class))).thenAnswer(invocation -> {
            OperationalTripFact fact = invocation.getArgument(0);
            if (fact.getId() == null) {
                fact.setId(UUID.randomUUID());
            }
            store.set(fact);
            return fact;
        });
        when(shiftEventRepository.save(any(OperationalShiftEvent.class))).thenAnswer(invocation -> {
            OperationalShiftEvent event = invocation.getArgument(0);
            events.add(event);
            return event;
        });

        OperationalTripFactInput first = OperationalTripFactInput.builder()
                .externalTripId("trip-1")
                .driverId("driver-1")
                .vehiclePlate("abc-123")
                .tripStatus("complete")
                .bookedAt(LocalDateTime.of(2026, 6, 25, 8, 0))
                .observedAt(LocalDateTime.of(2026, 6, 25, 8, 30))
                .build();
        OperationalTripFactInput second = OperationalTripFactInput.builder()
                .externalTripId("trip-1")
                .driverId("driver-1")
                .vehiclePlate("abc-123")
                .tripStatus("complete")
                .bookedAt(LocalDateTime.of(2026, 6, 25, 8, 0))
                .endedAt(LocalDateTime.of(2026, 6, 25, 8, 20))
                .observedAt(LocalDateTime.of(2026, 6, 25, 8, 35))
                .build();

        service.upsertTripFact(first);
        OperationalTripFact saved = service.upsertTripFact(second);

        assertNotNull(saved.getId());
        assertEquals(LocalDateTime.of(2026, 6, 25, 8, 20), saved.getEndedAt());
        assertEquals(2, events.size());
        verify(tripFactRepository, times(2)).save(any(OperationalTripFact.class));
    }

    @Test
    void tripWithoutVehiclePersistsAsUnresolved() {
        when(tripFactRepository.findByExternalTripId("trip-2")).thenReturn(Optional.empty());
        when(tripFactRepository.save(any(OperationalTripFact.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shiftEventRepository.save(any(OperationalShiftEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.upsertTripFact(OperationalTripFactInput.builder()
                .externalTripId("trip-2")
                .driverId("driver-2")
                .tripStatus("complete")
                .bookedAt(LocalDateTime.of(2026, 6, 25, 10, 0))
                .observedAt(LocalDateTime.of(2026, 6, 25, 10, 10))
                .build());

        ArgumentCaptor<OperationalTripFact> captor = ArgumentCaptor.forClass(OperationalTripFact.class);
        verify(tripFactRepository).save(captor.capture());
        assertEquals(OperationalMonitoringConstants.VEHICLE_KEY_SOURCE_UNRESOLVED, captor.getValue().getVehicleKeySource());
        assertEquals("driver-2", captor.getValue().getDriverId());
    }
}
