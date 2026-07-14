package com.yego.backend.service.yego_pro_ops.mobile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class MobileOtpEvolutionGoService {

    private final RestTemplate restTemplate;

    @Value("${evolution-go.base-url:${EVOLUTION_GO_BASE_URL:https://go.yego.pro}}")
    private String baseUrl;

    @Value("${evolution-go.otp.token:}")
    private String token;

    @Value("${evolution-go.otp.delay-ms:${EVOLUTION_GO_WHATSAPP_DELAY_MS:1200}}")
    private long delayMs;

    public MobileOtpEvolutionGoService(@Qualifier("whatsAppRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean sendOtp(String phone, String message) {
        String cleanToken = normalizeToken(token);
        if (cleanToken == null) {
            log.error("[MobileOtpEvolutionGoService] EVOLUTION_GO_PRO_OPS_TOKEN no configurado");
            return false;
        }

        String number = normalizePhone(phone);
        if (number == null) {
            log.error("[MobileOtpEvolutionGoService] Telefono invalido para OTP");
            return false;
        }

        if (message == null || message.isBlank()) {
            log.error("[MobileOtpEvolutionGoService] Mensaje OTP vacio");
            return false;
        }

        String url = normalizeBaseUrl(baseUrl) + "/send/text";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set("apikey", cleanToken);
        headers.setBearerAuth(cleanToken);

        Map<String, Object> body = new HashMap<>();
        body.put("number", number);
        body.put("text", message);
        body.put("delay", delayMs);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                log.info("[MobileOtpEvolutionGoService] OTP enviado por Evolution GO a {}", maskPhone(number));
            } else {
                log.error("[MobileOtpEvolutionGoService] Error Evolution GO enviando OTP. status={}", response.getStatusCode());
            }
            return success;
        } catch (RestClientResponseException e) {
            log.error("[MobileOtpEvolutionGoService] Error Evolution GO enviando OTP. status={} body={}",
                    e.getRawStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("[MobileOtpEvolutionGoService] Error enviando OTP por Evolution GO: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean isProviderConfigured() {
        return normalizeToken(token) != null;
    }

    private String normalizeBaseUrl(String value) {
        String clean = value == null || value.isBlank() ? "https://go.yego.pro" : value.trim();
        return clean.replaceAll("/+$", "");
    }

    private String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizePhone(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String digits = value.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }
        if (digits.length() == 9) {
            digits = "51" + digits;
        }
        return digits;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) {
            return "****";
        }
        return "*".repeat(Math.max(0, phone.length() - 4)) + phone.substring(phone.length() - 4);
    }
}
