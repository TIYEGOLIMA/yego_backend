package com.yego.backend.service.yego_pro_ops.mobile;

import com.yego.backend.config.JwtTokenProvider;
import com.yego.backend.entity.yego_api_externo.entities.DriverApi;
import com.yego.backend.repository.yego_api_externo.DriverApiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileAuthServiceTest {

    private static final String LICENSE = "Z40159900";
    private static final String DRIVER_ID = "driver-1";

    @Mock
    private DriverApiRepository driverRepository;

    @Mock
    private MobileOtpEvolutionGoService otpWhatsAppService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MobileDriverSessionService mobileSessionService;

    @Mock
    private MobileShiftService shiftService;

    @InjectMocks
    private MobileAuthService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "otpTtlMinutes", 1);
        ReflectionTestUtils.setField(service, "jwtExpirationSeconds", 604800L);
        ReflectionTestUtils.setField(service, "otpLogCodeEnabled", false);
        ReflectionTestUtils.setField(service, "allowLogOnlyWithoutProvider", false);
        ReflectionTestUtils.setField(service, "minAppVersion", "1.0.0");
        ReflectionTestUtils.setField(service, "otpRequestLimit", 10);
        ReflectionTestUtils.setField(service, "otpRequestWindowMinutes", 10);
        ReflectionTestUtils.setField(service, "otpBlockMinutes", 15);
        ReflectionTestUtils.setField(service, "otpVerifyFailLimit", 5);

        DriverApi driver = new DriverApi();
        driver.setDriverId(DRIVER_ID);
        driver.setParkId("64085dd85e124e2c808806f70d527ea8");
        driver.setLicenseNumber(LICENSE);
        driver.setFullName("Conductor Prueba");
        driver.setPhone("999999999");
        driver.setActive(true);
        driver.setWorkStatus("working");

        when(driverRepository.findByParkIdAndLicense(anyString(), anyString())).thenReturn(List.of(driver));
        when(otpWhatsAppService.sendOtp(anyString(), anyString())).thenReturn(true);
        when(mobileSessionService.normalizeDeviceId(anyString()))
                .thenAnswer(invocation -> invocation.<String>getArgument(0).trim());
    }

    @Test
    void latestDeviceRequestReplacesPreviousOtp() {
        service.requestOtp(LICENSE, "device-a", "1.0.4", "127.0.0.1");
        String firstCode = currentOtpCode();

        service.requestOtp(LICENSE, "device-b", "1.0.4", "127.0.0.1");
        String latestCode = currentOtpCode();

        assertThrows(
                ResponseStatusException.class,
                () -> service.verifyOtp(LICENSE, firstCode, "device-a", "1.0.4")
        );

        when(mobileSessionService.activateOrReplace(DRIVER_ID, "device-b", 604800L))
                .thenReturn(UUID.randomUUID());
        when(jwtTokenProvider.generate(anyString(), anyMap(), anyLong())).thenReturn("token");

        service.verifyOtp(LICENSE, latestCode, "device-b", "1.0.4");

        verify(mobileSessionService).activateOrReplace(DRIVER_ID, "device-b", 604800L);
    }

    @Test
    void concurrentVerificationConsumesOtpOnlyOnce() throws Exception {
        service.requestOtp(LICENSE, "device-a", "1.0.4", "127.0.0.1");
        String code = currentOtpCode();
        when(mobileSessionService.activateOrReplace(DRIVER_ID, "device-a", 604800L))
                .thenReturn(UUID.randomUUID());
        when(jwtTokenProvider.generate(anyString(), anyMap(), anyLong())).thenReturn("token");

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> verifyAfterSignal(start, code));
            Future<Boolean> second = executor.submit(() -> verifyAfterSignal(start, code));
            start.countDown();

            int successfulVerifications = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
            assertEquals(1, successfulVerifications);
            verify(mobileSessionService, times(1))
                    .activateOrReplace(DRIVER_ID, "device-a", 604800L);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean verifyAfterSignal(CountDownLatch start, String code) throws InterruptedException {
        start.await();
        try {
            service.verifyOtp(LICENSE, code, "device-a", "1.0.4");
            return true;
        } catch (ResponseStatusException ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private String currentOtpCode() {
        Map<String, Object> store = (Map<String, Object>) ReflectionTestUtils.getField(service, "otpStore");
        Object entry = store.get(LICENSE);
        return ReflectionTestUtils.invokeMethod(entry, "code");
    }
}
