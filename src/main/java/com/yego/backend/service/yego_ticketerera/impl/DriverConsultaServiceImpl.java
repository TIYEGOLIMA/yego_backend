package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.service.yego_ticketerera.DriverConsultaService;
import com.yego.backend.integration.FactilizaDniClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverConsultaServiceImpl implements DriverConsultaService {

    private final JdbcTemplate jdbcTemplate;
    private final FactilizaDniClient factilizaDniClient;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(2);

    private static class CacheEntry {
        final Optional<Map<String, Object>> result;
        final long timestamp;

        CacheEntry(Optional<Map<String, Object>> result) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    private Optional<Map<String, Object>> buscarPorTelefono(String telefono) {
        CacheEntry cached = cache.get(telefono);
        if (cached != null && !cached.isExpired()) {
            return cached.result;
        }
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());

        try {
            String sql = "SELECT full_name, phone FROM drivers WHERE phone = ?";
            Map<String, Object> conductor = jdbcTemplate.queryForMap(sql, telefono);

            String phoneDisplay = conductor.get("phone").toString();
            if (phoneDisplay.startsWith("+51") && phoneDisplay.length() >= 12) {
                phoneDisplay = phoneDisplay.substring(3);
            }
            conductor.put("phone", phoneDisplay);

            Optional<Map<String, Object>> result = Optional.of(conductor);
            cache.put(telefono, new CacheEntry(result));
            return result;
        } catch (EmptyResultDataAccessException e) {
            Optional<Map<String, Object>> result = Optional.empty();
            cache.put(telefono, new CacheEntry(result));
            return result;
        } catch (Exception e) {
            log.error("[DriverConsultaService] Error BD: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Object> consultarYRegistrarPorDni(String dni, String phone) {
        try {
            FactilizaDniClient.DniData data = factilizaDniClient.consultar(dni);
            return registrarNuevoConductor(data.nombres(), data.apellidos(), phone);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[DriverConsultaService] Error consultando DNI", e);
            throw new RuntimeException("Error consultando DNI: " + e.getMessage());
        }
    }

    private Map<String, Object> registrarNuevoConductor(String firstName, String lastName, String phone) {
        try {
            String phoneCompleto = phone.startsWith("+51") ? phone : "+51" + phone;
            String driverId = UUID.randomUUID().toString().replace("-", "");
            String fullName = firstName + " " + lastName;
            Date hireDate = Date.valueOf(LocalDate.now());

            String sql = "INSERT INTO drivers (driver_id, first_name, last_name, full_name, phone, work_status, hire_date, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, 'working', ?, NOW())";
            jdbcTemplate.update(sql, driverId, firstName, lastName, fullName, phoneCompleto, hireDate);

            cache.remove(phoneCompleto);

            String phoneSinPrefijo = phoneCompleto.startsWith("+51") ? phoneCompleto.substring(3) : phoneCompleto;
            return Map.of(
                    "driver_id", driverId,
                    "full_name", fullName,
                    "phone", phoneSinPrefijo,
                    "first_name", firstName,
                    "last_name", lastName,
                    "work_status", "working",
                    "hire_date", hireDate.toString()
            );
        } catch (Exception e) {
            log.error("[DriverConsultaService] Error registrando conductor", e);
            throw new RuntimeException("Error registrando conductor: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> buscarConductorConRespuestaCompleta(String phoneDigits) {
        Optional<Map<String, Object>> conductor = buscarPorTelefono(phoneDigits);

        if (conductor.isPresent()) {
            return ResponseEntity.ok(conductor.get());
        }

        if (phoneDigits.matches("^\\+519\\d{8}$") && phoneDigits.length() == 12) {
            return ResponseEntity.status(404).body(Map.of(
                    "found", false,
                    "message", "Conductor no encontrado",
                    "phone", phoneDigits,
                    "canRegister", true,
                    "registerEndpoint", "/api/ticketera/drivers/registrar-dni"
            ));
        }

        return ResponseEntity.badRequest().body(Map.of(
                "found", false,
                "error", "Formato inválido: debe ser +51 seguido de 9 dígitos empezando con 9",
                "phone", phoneDigits,
                "canRegister", false
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> registrarConductorManualConRespuesta(Map<String, String> datos) {
        try {
            return ResponseEntity.ok(registrarNuevoConductor(
                    datos.get("firstName"),
                    datos.get("lastName"),
                    datos.get("phone")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> registrarConductorPorDniConRespuesta(Map<String, String> datos) {
        try {
            return ResponseEntity.ok(consultarYRegistrarPorDni(datos.get("dni"), datos.get("phone")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
