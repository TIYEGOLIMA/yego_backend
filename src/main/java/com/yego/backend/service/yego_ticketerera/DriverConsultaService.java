package com.yego.backend.service.yego_ticketerera;

import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface DriverConsultaService {

    ResponseEntity<Map<String, Object>> buscarConductorConRespuestaCompleta(String phoneDigits);

    ResponseEntity<Map<String, Object>> registrarConductorManualConRespuesta(Map<String, String> datos);

    ResponseEntity<Map<String, Object>> registrarConductorPorDniConRespuesta(Map<String, String> datos);
}
