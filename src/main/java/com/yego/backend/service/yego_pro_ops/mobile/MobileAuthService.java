package com.yego.backend.service.yego_pro_ops.mobile;

import com.yego.backend.entity.yego_api_externo.entities.DriverApi;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileAuthResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileOtpResponse;
import com.yego.backend.repository.yego_api_externo.DriverApiRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class MobileAuthService {

    private static final String PARK_ID_YEGO_PRO = "64085dd85e124e2c808806f70d527ea8";
    private static final String WORK_STATUS_WORKING = "working";
    private static final String MOBILE_TOKEN_TYPE = "mobile_driver";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DriverApiRepository driverRepository;
    private final MobileOtpEvolutionGoService otpWhatsAppService;
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    @Value("${mobile.otp.ttl-minutes:${MOBILE_OTP_TTL_MINUTES:1}}")
    private int otpTtlMinutes;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:604800}")
    private long jwtExpirationSeconds;

    @Value("${mobile.otp.log-code-enabled:${MOBILE_OTP_LOG_CODE_ENABLED:false}}")
    private boolean otpLogCodeEnabled;

    public MobileOtpResponse requestOtp(String licenseNumber) {
        String license = normalizeLicense(licenseNumber);
        DriverApi driver = findDriverByLicense(license);
        String phone = normalizePhone(driver.getPhone());

        if (phone == null || phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El conductor no tiene telefono registrado");
        }

        String code = generateCode();
        Instant expiresAt = Instant.now().plusSeconds(Math.max(1, otpTtlMinutes) * 60L);
        otpStore.put(license, new OtpEntry(code, expiresAt, driver));
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
            otpStore.remove(license);
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

    public MobileAuthResponse verifyOtp(String licenseNumber, String code) {
        String license = normalizeLicense(licenseNumber);
        OtpEntry entry = otpStore.get(license);

        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            otpStore.remove(license);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Codigo vencido o no solicitado");
        }

        if (!entry.code().equals(code)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Codigo incorrecto");
        }

        otpStore.remove(license);
        DriverApi driver = entry.driver();

        return MobileAuthResponse.builder()
                .success(true)
                .token(generateMobileToken(driver))
                .driver(MobileAuthResponse.Driver.builder()
                        .id(driver.getDriverId())
                        .nombre(resolveName(driver))
                        .licencia(firstNonBlank(driver.getLicenseNumber(), driver.getLicenseNormalizedNumber(), license))
                        .telefono(normalizePhone(driver.getPhone()))
                        .build())
                .build();
    }

    private String generateMobileToken(DriverApi driver) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + Math.max(60, jwtExpirationSeconds) * 1000L);

        return Jwts.builder()
                .setSubject(driver.getDriverId())
                .claim("driverId", driver.getDriverId())
                .claim("role", "CONDUCTOR")
                .claim("type", MOBILE_TOKEN_TYPE)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    private DriverApi findDriverByLicense(String license) {
        if (license == null || license.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar una licencia valida");
        }

        return driverRepository.findByParkIdAndLicense(PARK_ID_YEGO_PRO, license)
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

    private record OtpEntry(String code, Instant expiresAt, DriverApi driver) {}
}
