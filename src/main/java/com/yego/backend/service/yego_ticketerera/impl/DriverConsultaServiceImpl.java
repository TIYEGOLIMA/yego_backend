package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.service.yego_ticketerera.DriverConsultaService;
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

/**
 * Implementación del servicio de consulta de conductores del sistema YEGO Ticketerera
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverConsultaServiceImpl implements DriverConsultaService {

    private final JdbcTemplate jdbcTemplate;
    
    // Cache para evitar consultas repetidas
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(2); // 2 minutos
    
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

    @Override
    public Optional<Map<String, Object>> buscarPorTelefono(String telefono) {
        log.info("🔍 [DriverConsultaService] Búsqueda conductor: '{}'", telefono);
        
        // Verificar cache primero
        CacheEntry cached = cache.get(telefono);
        if (cached != null && !cached.isExpired()) {
            log.info("🚀 [DriverConsultaService] Resultado desde CACHE para: '{}'", telefono);
            return cached.result;
        }
        
        // Limpiar entradas expiradas del cache
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        try {
            // Consulta optimizada con índices y sin SUBSTRING en tiempo real
            String sql = "SELECT full_name, phone FROM drivers WHERE phone = ?";
            log.info("💾 [DriverConsultaService] Consultando BD para: '{}'", telefono);
            
            Map<String, Object> conductor = jdbcTemplate.queryForMap(sql, telefono);
            
            // Procesar el teléfono para mostrar solo los 9 dígitos
            String phoneDisplay = conductor.get("phone").toString();
            if (phoneDisplay.startsWith("+51") && phoneDisplay.length() >= 12) {
                phoneDisplay = phoneDisplay.substring(3); // Quitar +51
            }
            conductor.put("phone", phoneDisplay);
            
            log.info("✅ [DriverConsultaService] Conductor encontrado: {}", conductor.get("full_name"));
            
            Optional<Map<String, Object>> result = Optional.of(conductor);
            cache.put(telefono, new CacheEntry(result));
            return result;
            
        } catch (EmptyResultDataAccessException e) {
            log.warn("❌ [DriverConsultaService] No encontrado: '{}'", telefono);
            
            Optional<Map<String, Object>> result = Optional.empty();
            cache.put(telefono, new CacheEntry(result));
            return result;
            
        } catch (Exception e) {
            log.error("💥 [DriverConsultaService] Error BD: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Map<String, Object> consultarYRegistrarPorDni(String dni, String phone) {
        log.info("🆔 [DriverConsultaService] Consultando DNI y registrando conductor");
        log.info("📄 [DriverConsultaService] DNI: '{}', Teléfono: '{}'", dni, phone);
        
        try {
            // Consultar API del DNI
            String apiUrl = "http://167.235.28.114:5000/api/v2/dni/" + dni;
            log.info("🌐 [DriverConsultaService] Consultando API: {}", apiUrl);
            
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(apiUrl))
                    .header("Authorization", "Basic c3lzdGVtM3c6NkVpWmpwaWp4a1hUZUFDbw==")
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            
            java.net.http.HttpResponse<String> response = client.send(request, 
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            
            log.info("📡 [DriverConsultaService] Respuesta API status: {}", response.statusCode());
            log.info("📋 [DriverConsultaService] Respuesta API body: {}", response.body());
            
            if (response.statusCode() == 200) {
                // Parsear la respuesta JSON manualmente (simple)
                String responseBody = response.body();
                
                String nombres = extraerCampoJson(responseBody, "nombres");
                String apellidoPaterno = extraerCampoJson(responseBody, "apellido_paterno");
                String apellidoMaterno = extraerCampoJson(responseBody, "apellido_materno");
                
                log.info("👤 [DriverConsultaService] Nombres: '{}'", nombres);
                log.info("👤 [DriverConsultaService] Apellido Paterno: '{}'", apellidoPaterno);
                log.info("👤 [DriverConsultaService] Apellido Materno: '{}'", apellidoMaterno);
                
                // Combinar para first_name, last_name y full_name
                String firstName = nombres;
                String lastName = apellidoPaterno + (apellidoMaterno != null && !apellidoMaterno.isEmpty() ? " " + apellidoMaterno : "");
                
                log.info("✅ [DriverConsultaService] Procesado - First: '{}', Last: '{}'", firstName, lastName);
                
                // Registrar conductor con los datos obtenidos
                return registrarNuevoConductor(firstName, lastName, phone);
                
            } else {
                log.error("❌ [DriverConsultaService] Error en API DNI: status {}", response.statusCode());
                throw new RuntimeException("Error consultando DNI: status " + response.statusCode());
            }
            
        } catch (Exception e) {
            log.error("💥 [DriverConsultaService] Error consultando DNI: {}", e.getMessage());
            log.error("📋 [DriverConsultaService] Stack trace:", e);
            e.printStackTrace();
            throw new RuntimeException("Error consultando DNI: " + e.getMessage());
        }
    }
    
    private String extraerCampoJson(String json, String campo) {
        try {
            String patron = "\"" + campo + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patron);
            java.util.regex.Matcher matcher = pattern.matcher(json);
            
            if (matcher.find()) {
                String valor = matcher.group(1);
                log.debug("🔍 [DriverConsultaService] Extraído {}: '{}'", campo, valor);
                return valor;
            } else {
                log.warn("⚠️ [DriverConsultaService] Campo '{}' no encontrado en JSON", campo);
                return "";
            }
        } catch (Exception e) {
            log.error("💥 [DriverConsultaService] Error extrayendo campo '{}': {}", campo, e.getMessage());
            return "";
        }
    }


    @Override
    public Map<String, Object> registrarNuevoConductor(String firstName, String lastName, String phone) {
        log.info("🆕 [DriverConsultaService] Registrando nuevo conductor");
        log.info("👤 [DriverConsultaService] Nombre: '{}', Apellido: '{}'", firstName, lastName);
        log.info("📱 [DriverConsultaService] Teléfono recibido: '{}'", phone);
        
        try {
            // Agregar +51 si no lo tiene
            String phoneCompleto;
            if (phone.startsWith("+51")) {
                phoneCompleto = phone;
                log.info("📱 [DriverConsultaService] Teléfono ya tiene +51: '{}'", phoneCompleto);
            } else {
                phoneCompleto = "+51" + phone;
                log.info("📱 [DriverConsultaService] Agregando +51 al teléfono: '{}'", phoneCompleto);
            }
            
            // Generar driver_id aleatorio de 32 caracteres
            String driverId = UUID.randomUUID().toString().replace("-", "");
            String fullName = firstName + " " + lastName;
            LocalDate hireDateLocal = LocalDate.now();
            Date hireDate = Date.valueOf(hireDateLocal); // Convertir a java.sql.Date para PostgreSQL
            
            log.info("🆔 [DriverConsultaService] Driver ID generado: '{}'", driverId);
            log.info("👥 [DriverConsultaService] Nombre completo: '{}'", fullName);
            log.info("📅 [DriverConsultaService] Fecha contratación: '{}'", hireDate);
            
            String sql = "INSERT INTO drivers (driver_id, first_name, last_name, full_name, phone, work_status, hire_date, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'working', ?, NOW())";
            
            log.info("💾 [DriverConsultaService] Ejecutando INSERT con parámetros preparados");
            
            int filasAfectadas = jdbcTemplate.update(sql, driverId, firstName, lastName, fullName, phoneCompleto, hireDate);
            log.info("✅ [DriverConsultaService] Conductor registrado exitosamente. Filas afectadas: {}", filasAfectadas);
            
            // Limpiar cache inmediatamente para que la consulta funcione al instante
            cache.remove(phoneCompleto);
            log.info("🧹 [DriverConsultaService] Cache limpiado para teléfono: '{}'", phoneCompleto);
            
            // Devolver los datos del conductor recién creado (teléfono sin +51 para frontend)
            String phoneSinPrefijo = phoneCompleto.startsWith("+51") ? phoneCompleto.substring(3) : phoneCompleto;
            
            Map<String, Object> nuevoConductor = Map.of(
                "driver_id", driverId,
                "full_name", fullName,
                "phone", phoneSinPrefijo, // Sin +51 para el frontend
                "first_name", firstName,
                "last_name", lastName,
                "work_status", "working",
                "hire_date", hireDate.toString() // Convertir a String para el frontend
            );
            
            log.info("📋 [DriverConsultaService] Datos del nuevo conductor: {}", nuevoConductor);
            return nuevoConductor;
            
        } catch (Exception e) {
            log.error("💥 [DriverConsultaService] Error registrando conductor: {}", e.getMessage());
            log.error("📋 [DriverConsultaService] Stack trace:", e);
            throw new RuntimeException("Error registrando conductor: " + e.getMessage());
        }
    }
    
    @Override
    public void limpiarCache() {
        int sizeBefore = cache.size();
        cache.clear();
        log.info("🧹 [DriverConsultaService] Cache limpiado. Entradas eliminadas: {}", sizeBefore);
    }
    
    // Métodos simplificados para el controlador (sin lógica de negocio en el controlador)
    
    @Override
    public ResponseEntity<Map<String, Object>> buscarConductorConRespuestaCompleta(String phoneDigits) {
        log.info("🔒 [DriverController] Procesando: '{}'", phoneDigits);
        
        Optional<Map<String, Object>> conductor = buscarPorTelefono(phoneDigits);
        
        if (conductor.isPresent()) {
            log.info("✅ [DriverController] Encontrado: {}", conductor.get().get("full_name"));
            return ResponseEntity.ok(conductor.get());
        } else {
            log.warn("❌ [DriverController] No encontrado: '{}'", phoneDigits);
            
            // Validar formato antes de sugerir registro por DNI
            if (phoneDigits.matches("^\\+519\\d{8}$") && phoneDigits.length() == 12) {
                return ResponseEntity.status(404).body(Map.of(
                    "found", false,
                    "message", "Conductor no encontrado. Use el endpoint /api/ticketerera/drivers/registrar-dni",
                    "phone", phoneDigits,
                    "canRegister", true,
                    "registerEndpoint", "/api/ticketerera/drivers/registrar-dni"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "found", false,
                    "error", "Formato inválido: debe ser +51 seguido de 9 dígitos empezando con 9",
                    "phone", phoneDigits,
                    "canRegister", false
                ));
            }
        }
    }
    
    @Override
    public ResponseEntity<Map<String, Object>> registrarConductorManualConRespuesta(Map<String, String> datos) {
        log.info("🆕 [DriverController] Registrando conductor manualmente");
        log.info("📋 [DriverController] Datos recibidos: {}", datos);
        
        String firstName = datos.get("firstName");
        String lastName = datos.get("lastName");
        String phone = datos.get("phone");
        
        log.info("👤 [DriverController] Nombre: '{}', Apellido: '{}', Teléfono: '{}'", firstName, lastName, phone);
        
        try {
            Map<String, Object> nuevoConductor = registrarNuevoConductor(firstName, lastName, phone);
            log.info("✅ [DriverController] Conductor registrado exitosamente (manual)");
            return ResponseEntity.ok(nuevoConductor);
        } catch (Exception e) {
            log.error("❌ [DriverController] Error registrando conductor manual: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @Override
    public ResponseEntity<Map<String, Object>> registrarConductorPorDniConRespuesta(Map<String, String> datos) {
        log.info("🆔 [DriverController] Registrando conductor por DNI");
        log.info("📋 [DriverController] Datos recibidos: {}", datos);
        
        String dni = datos.get("dni");
        String phone = datos.get("phone");
        
        log.info("📄 [DriverController] DNI: '{}', Teléfono: '{}'", dni, phone);
        
        try {
            Map<String, Object> nuevoConductor = consultarYRegistrarPorDni(dni, phone);
            log.info("✅ [DriverController] Conductor registrado exitosamente desde DNI");
            return ResponseEntity.ok(nuevoConductor);
        } catch (Exception e) {
            log.error("❌ [DriverController] Error registrando conductor por DNI: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @Override
    public ResponseEntity<Map<String, Object>> limpiarCacheConRespuesta() {
        log.info("🧹 [DriverController] Limpiando cache de conductores");
        limpiarCache();
        return ResponseEntity.ok(Map.of("message", "Cache limpiado correctamente"));
    }
}
