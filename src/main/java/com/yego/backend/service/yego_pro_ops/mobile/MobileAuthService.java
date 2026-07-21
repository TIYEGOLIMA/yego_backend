package com.yego.backend.service.yego_pro_ops.mobile;

import com.yego.backend.config.JwtTokenProvider;
import com.yego.backend.config.yego_pro_ops.YegoProOpsProperties;
import com.yego.backend.entity.yego_api_externo.entities.DriverApi;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileAuthResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileOtpResponse;
import com.yego.backend.repository.yego_api_externo.DriverApiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MobileAuthService {

    private static final String WORK_STATUS_WORKING = "working";
    private static final String MOBILE_TOKEN_TYPE = "mobile_driver";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DriverApiRepository driverRepository;
    private final MobileOtpEvolutionGoService otpWhatsAppService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MobileDriverSessionService mobileSessionService;
    private final MobileShiftService shiftService;
    private final YegoProOpsProperties proOpsProperties;
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final Map<String, RateLimitEntry> requestLimits = new ConcurrentHashMap<>();
    private final Map<String, VerifyFailureEntry> verifyFailures = new ConcurrentHashMap<>();

    @Value("${mobile.otp.ttl-minutes:${MOBILE_OTP_TTL_MINUTES:1}}")
    private int otpTtlMinutes;

    @Value("${jwt.expiration:604800}")
    private long jwtExpirationSeconds;

    @Value("${mobile.otp.log-code-enabled:${MOBILE_OTP_LOG_CODE_ENABLED:false}}")
    private boolean otpLogCodeEnabled;

    @Value("${mobile.otp.allow-log-only-without-provider:${MOBILE_OTP_ALLOW_LOG_ONLY_WITHOUT_PROVIDER:false}}")
    private boolean allowLogOnlyWithoutProvider;

    @Value("${mobile.app.min-version:${MOBILE_APP_MIN_VERSION:1.0.0}}")
    private String minAppVersion;

    @Value("${mobile.otp.request-limit:${MOBILE_OTP_REQUEST_LIMIT:5}}")
    private int otpRequestLimit;

    @Value("${mobile.otp.request-window-minutes:${MOBILE_OTP_REQUEST_WINDOW_MINUTES:10}}")
    private int otpRequestWindowMinutes;

    @Value("${mobile.otp.block-minutes:${MOBILE_OTP_BLOCK_MINUTES:15}}")
    private int otpBlockMinutes;

    @Value("${mobile.otp.verify-fail-limit:${MOBILE_OTP_VERIFY_FAIL_LIMIT:5}}")
    private int otpVerifyFailLimit;

    public MobileOtpResponse requestOtp(String licenseNumber, String deviceId, String appVersion, String clientIp) {
        assertSupportedAppVersion(appVersion);
        String license = normalizeLicense(licenseNumber);
        String otpDeviceId = mobileSessionService.normalizeDeviceId(deviceId);
        assertOtpRequestAllowed("ip:" + firstNonBlank(clientIp, "unknown"));
        assertOtpRequestAllowed("license:" + license);

        DriverApi driver = findDriverByLicense(license);
        String phone = normalizePhone(driver.getPhone());

        if (phone == null || phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El conductor no tiene telefono registrado");
        }
        assertOtpRequestAllowed("phone:" + phone);

        String code = generateCode();
        Instant expiresAt = Instant.now().plusSeconds(Math.max(1, otpTtlMinutes) * 60L);
        OtpEntry otpEntry = new OtpEntry(code, expiresAt, driver, otpDeviceId);
        OtpEntry previousEntry = otpStore.put(license, otpEntry);
        verifyFailures.remove(license);
        if (previousEntry != null) {
            log.info("Solicitud OTP anterior reemplazada: driver={}, licencia={}", driver.getDriverId(), license);
        }
        if (otpLogCodeEnabled) {
            log.info("OTP movil generado: driver={}, licencia={}, codigo={}, venceEnMinutos={}",
                    driver.getDriverId(), license, code, otpTtlMinutes);
        } else {
            log.info("OTP movil generado: driver={}, licencia={}, venceEnMinutos={}",
                    driver.getDriverId(), license, otpTtlMinutes);
        }

        String message = "Tu codigo de acceso a YEGO Pro Ops es: " + code
                + "\n\nVence en " + otpTtlMinutes + " minutos. No compartas este codigo.";
        boolean sent = otpWhatsAppService.sendOtp(phone, message);
        if (!sent) {
            if (allowLogOnlyWithoutProvider && otpLogCodeEnabled && !otpWhatsAppService.isProviderConfigured()) {
                log.warn("OTP movil en modo desarrollo sin proveedor: driver={}, licencia={}, usar codigo impreso en logs",
                        driver.getDriverId(), license);
                return MobileOtpResponse.builder()
                        .success(true)
                        .message("Codigo generado en logs de desarrollo")
                        .maskedPhone(maskPhone(phone))
                        .expiresInMinutes(otpTtlMinutes)
                        .build();
            }
            otpStore.remove(license, otpEntry);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo enviar el codigo al telefono registrado");
        }

        log.info("OTP movil enviado: driver={}, licencia={}, phone={}",
                driver.getDriverId(), license, maskPhone(phone));

        return MobileOtpResponse.builder()
                .success(true)
                .message("Codigo enviado al telefono registrado")
                .maskedPhone(maskPhone(phone))
                .expiresInMinutes(otpTtlMinutes)
                .build();
    }

    @Transactional
    public MobileAuthResponse verifyOtp(String licenseNumber, String code, String deviceId, String appVersion) {
        assertSupportedAppVersion(appVersion);
        String license = normalizeLicense(licenseNumber);
        String otpDeviceId = mobileSessionService.normalizeDeviceId(deviceId);
        OtpEntry entry = otpStore.get(license);

        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            if (entry != null) {
                otpStore.remove(license, entry);
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Codigo vencido o no solicitado");
        }

        if (!entry.deviceId().equals(otpDeviceId)) {
            throw otpReplacedOrDifferentDevice();
        }
        if (!entry.code().equals(code)) {
            if (otpStore.get(license) != entry) {
                throw otpReplacedOrDifferentDevice();
            }
            registerVerifyFailure(license, entry);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Codigo incorrecto");
        }
        if (!otpStore.remove(license, entry)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Codigo ya utilizado o reemplazado");
        }

        verifyFailures.remove(license);
        DriverApi driver = entry.driver();
        long tokenTtlSeconds = Math.max(60, jwtExpirationSeconds);
        String mobileSessionId = mobileSessionService
                .activateOrReplace(driver.getDriverId(), deviceId, tokenTtlSeconds)
                .toString();

        return MobileAuthResponse.builder()
                .success(true)
                .token(generateMobileToken(driver, mobileSessionId, tokenTtlSeconds))
                .driver(MobileAuthResponse.Driver.builder()
                        .id(driver.getDriverId())
                        .nombre(resolveName(driver))
                        .licencia(firstNonBlank(driver.getLicenseNumber(), driver.getLicenseNormalizedNumber(), license))
                        .telefono(normalizePhone(driver.getPhone()))
                        .build())
                .build();
    }

    @Transactional
    public void logout(String driverId, String mobileSessionId) {
        shiftService.assertDriverCanLogout(driverId);
        mobileSessionService.revoke(driverId, mobileSessionId);
    }

    private String generateMobileToken(DriverApi driver, String mobileSessionId, long tokenTtlSeconds) {
        return jwtTokenProvider.generate(
                driver.getDriverId(),
                Map.of(
                        "driverId", driver.getDriverId(),
                        "role", "CONDUCTOR",
                        "type", MOBILE_TOKEN_TYPE,
                        "mobileSessionId", mobileSessionId),
                tokenTtlSeconds);
    }

    private void assertSupportedAppVersion(String appVersion) {
        String minimum = firstNonBlank(minAppVersion, "");
        if (minimum.isBlank()) {
            return;
        }

        if (appVersion == null || appVersion.isBlank() || compareVersions(appVersion, minimum) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UPGRADE_REQUIRED,
                    "Actualiza la app para continuar"
            );
        }
    }

    private int compareVersions(String current, String minimum) {
        String[] currentParts = current.trim().split("\\.");
        String[] minimumParts = minimum.trim().split("\\.");
        int length = Math.max(currentParts.length, minimumParts.length);

        for (int i = 0; i < length; i++) {
            int currentValue = parseVersionPart(currentParts, i);
            int minimumValue = parseVersionPart(minimumParts, i);
            if (currentValue != minimumValue) {
                return Integer.compare(currentValue, minimumValue);
            }
        }
        return 0;
    }

    private int parseVersionPart(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        String digits = parts[index].replaceAll("\\D", "");
        if (digits.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void assertOtpRequestAllowed(String key) {
        Instant now = Instant.now();
        RateLimitEntry entry = requestLimits.get(key);

        if (entry != null && entry.blockedUntil() != null && entry.blockedUntil().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Intenta nuevamente mas tarde");
        }

        Instant windowStart = entry == null || entry.windowStart().plusSeconds(Math.max(1, otpRequestWindowMinutes) * 60L).isBefore(now)
                ? now
                : entry.windowStart();
        int attempts = entry == null || !entry.windowStart().equals(windowStart)
                ? 1
                : entry.attempts() + 1;
        Instant blockedUntil = attempts > Math.max(1, otpRequestLimit)
                ? now.plusSeconds(Math.max(1, otpBlockMinutes) * 60L)
                : null;

        requestLimits.put(key, new RateLimitEntry(attempts, windowStart, blockedUntil));
        if (blockedUntil != null) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Intenta nuevamente mas tarde");
        }
    }

    private void registerVerifyFailure(String license, OtpEntry otpEntry) {
        VerifyFailureEntry failures = verifyFailures.compute(
                license,
                (key, current) -> new VerifyFailureEntry(current == null ? 1 : current.attempts() + 1)
        );

        if (failures.attempts() >= Math.max(1, otpVerifyFailLimit)) {
            otpStore.remove(license, otpEntry);
            verifyFailures.remove(license, failures);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos fallidos. Solicita un nuevo codigo");
        }
    }

    private ResponseStatusException otpReplacedOrDifferentDevice() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "El codigo fue solicitado desde otro dispositivo o reemplazado por una solicitud mas reciente"
        );
    }

    @Scheduled(fixedDelayString = "${mobile.otp.cleanup-interval-ms:60000}")
    void cleanupExpiredState() {
        Instant now = Instant.now();
        otpStore.forEach((license, entry) -> {
            if (!entry.expiresAt().isAfter(now) && otpStore.remove(license, entry)) {
                verifyFailures.remove(license);
            }
        });
        verifyFailures.forEach((license, failures) -> {
            if (!otpStore.containsKey(license)) {
                verifyFailures.remove(license, failures);
            }
        });
        requestLimits.forEach((key, entry) -> {
            Instant retentionUntil = entry.blockedUntil() != null
                    ? entry.blockedUntil()
                    : entry.windowStart().plusSeconds(Math.max(1, otpRequestWindowMinutes) * 60L);
            if (!retentionUntil.isAfter(now)) {
                requestLimits.remove(key, entry);
            }
        });
    }

    private DriverApi findDriverByLicense(String license) {
        if (license == null || license.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar una licencia valida");
        }

        return driverRepository.findByParkIdAndLicense(proOpsProperties.getParkId(), license)
                .stream()
                .filter(this::isEnabledForMobileLogin)
                .max(Comparator.comparing(DriverApi::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Licencia no pertenece a Yego Pro Ops o no esta habilitada"
                ));
    }

    private boolean isEnabledForMobileLogin(DriverApi driver) {
        if (Boolean.FALSE.equals(driver.getActive()) || driver.getFireDate() != null) {
            return false;
        }

        String workStatus = driver.getWorkStatus();
        return workStatus == null || workStatus.isBlank() || WORK_STATUS_WORKING.equalsIgnoreCase(workStatus.trim());
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String normalizeLicense(String value) {
        if (value == null) return "";
        return value.trim().replace(" ", "").toUpperCase();
    }

    private String normalizePhone(String value) {
        if (value == null) return null;
        String digits = value.replaceAll("\\D", "");
        if (digits.length() == 9) {
            return "51" + digits;
        }
        return digits;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) return "****";
        return "*".repeat(Math.max(0, phone.length() - 4)) + phone.substring(phone.length() - 4);
    }

    private String resolveName(DriverApi driver) {
        return firstNonBlank(
                driver.getFullName(),
                (firstNonBlank(driver.getFirstName(), "") + " " + firstNonBlank(driver.getLastName(), "")).trim(),
                driver.getDriverId()
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private record OtpEntry(String code, Instant expiresAt, DriverApi driver, String deviceId) {}
    private record RateLimitEntry(int attempts, Instant windowStart, Instant blockedUntil) {}
    private record VerifyFailureEntry(int attempts) {}
}
