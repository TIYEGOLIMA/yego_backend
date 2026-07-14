package com.yego.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Component
public class FactilizaDniClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiToken;

    public FactilizaDniClient(
            RestTemplateBuilder builder,
            @Value("${factiliza.api-url:https://api.factiliza.com/pe/v1/dni/info}") String baseUrl,
            @Value("${factiliza.api-token:}") String apiToken) {
        this.restTemplate = builder
                .connectTimeout(CONNECT_TIMEOUT)
                .readTimeout(READ_TIMEOUT)
                .build();
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.apiToken = apiToken == null ? "" : apiToken.trim();
    }

    public DniData consultar(String dni) {
        if (dni == null || !dni.matches("^\\d{8}$")) {
            throw new IllegalArgumentException("El DNI debe contener 8 dígitos");
        }
        if (apiToken.isEmpty()) {
            throw new FactilizaException("La integración de consulta DNI no está configurada");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/" + dni,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    JsonNode.class);
            JsonNode body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                throw new FactilizaException("El servicio DNI no devolvió una respuesta válida");
            }
            if (!body.path("success").asBoolean(false)) {
                throw new FactilizaException("El servicio DNI rechazó la consulta");
            }

            JsonNode data = body.path("data");
            if (data.isMissingNode() || data.isNull()) {
                throw new FactilizaException("El servicio DNI no devolvió datos");
            }
            return new DniData(
                    data.path("nombres").asText(""),
                    data.path("apellido_paterno").asText(""),
                    data.path("apellido_materno").asText(""));
        } catch (FactilizaException e) {
            throw e;
        } catch (RestClientException e) {
            throw new FactilizaException("No se pudo consultar el servicio DNI", e);
        }
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("factiliza.api-url es obligatorio");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public record DniData(String nombres, String apellidoPaterno, String apellidoMaterno) {
        public String apellidos() {
            return (apellidoPaterno + " " + apellidoMaterno).trim();
        }
    }

    public static class FactilizaException extends RuntimeException {
        public FactilizaException(String message) {
            super(message);
        }

        public FactilizaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
