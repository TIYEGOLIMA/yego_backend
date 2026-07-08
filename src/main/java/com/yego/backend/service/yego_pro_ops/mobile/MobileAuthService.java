package com.yego.backend.service.yego_pro_ops.mobile;

import com.yego.backend.entity.yego_api_externo.entities.DriverApi;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileAuthResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileOtpResponse;
import com.yego.backend.repository.yego_api_externo.DriverApiRepository;
import com.yego.backend.service.WhatsAppService;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MobileAuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final DriverApiRepository driverRepository;
    private final WhatsAppService whatsAppService;
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    @Value("${mobile.otp.ttl-minutes:${MOBILE_OTP_TTL_MINUTES:5}}")
    private int otpTtlMinutes;

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

        String message = "Tu codigo de acceso a YEGO Pro Ops es: " + code
                + "\n\nVence en " + otpTtlMinutes + " minutos. No compartas este codigo.";
        boolean sent = whatsAppService.enviarTexto(phone, message);
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
                .token("mobile_" + UUID.randomUUID())
                .driver(MobileAuthResponse.Driver.builder()
                        .id(driver.getDriverId())
                        .nombre(resolveName(driver))
                        .licencia(firstNonBlank(driver.getLicenseNumber(), driver.getLicenseNormalizedNumber(), license))
                        .telefono(normalizePhone(driver.getPhone()))
                        .build())
                .build();
    }

    private DriverApi findDriverByLicense(String license) {
        return driverRepository
                .findByLicenseNumberIgnoreCaseOrLicenseNormalizedNumberIgnoreCase(license, license)
                .stream()
                .max(Comparator.comparing(DriverApi::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Licencia no encontrada"));
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
