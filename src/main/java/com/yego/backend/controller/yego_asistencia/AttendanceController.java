package com.yego.backend.controller.yego_asistencia;

import com.yego.backend.service.yego_asistencia.AttendanceService;
import com.yego.backend.service.yego_asistencia.TokenValidationService;
import com.yego.backend.service.yego_asistencia.MessageService;
import com.yego.backend.entity.yego_principal.entities.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final TokenValidationService tokenValidationService;
    private final MessageService messageService;

    public AttendanceController(AttendanceService attendanceService, TokenValidationService tokenValidationService, MessageService messageService) {
        this.attendanceService = attendanceService;
        this.tokenValidationService = tokenValidationService;
        this.messageService = messageService;
    }
    
    /**
     * Endpoint para registrar marcación
     */
    @PostMapping("/marcacion")
    public ResponseEntity<?> createAttendance(@RequestBody Map<String, Object> attendanceData, 
                                            @RequestHeader("Authorization") String token,
                                            HttpServletRequest request) {
        try {
            // Validar token
            User user = tokenValidationService.getUserByToken(token);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Token de acceso requerido"));
            }

            // Obtener IP del cliente desde HttpServletRequest
            String clientIp = getClientIpAddress(request);
            
            // Delegar validación de IP y procesamiento al servicio
            Map<String, Object> result = attendanceService.processAttendanceMarkingWithIpValidation(
                user.getId(), attendanceData, clientIp
            );

            // Determinar código de estado HTTP basado en el resultado
            if (!(Boolean) result.get("success")) {
                String message = (String) result.get("message");
                if (message != null && message.contains("IP no autorizada")) {
                    return ResponseEntity.status(403).body(result);
                } else if (message != null && message.contains("No se pudo determinar la IP")) {
                    return ResponseEntity.status(400).body(result);
                } else {
                    return ResponseEntity.status(400).body(result);
                }
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error interno del servidor"));
        }
    }

    /**
     * Obtener marcaciones del día
     */
    @GetMapping("/marcaciones/hoy")
    public ResponseEntity<?> getTodayAttendances(@RequestHeader("Authorization") String token) {
        try {
            User user = tokenValidationService.getUserByToken(token);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Token de acceso requerido"));
            }

            // Usar el método que sí existe en la interfaz
            var marcaciones = attendanceService.getTodayAttendances(user.getId());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "marcaciones", marcaciones,
                "total", marcaciones.size()
            ));
                                     
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error interno del servidor"));
        }
    }

    /**
     * Obtener marcaciones por rango de fechas
     */
    @GetMapping("/marcaciones/rango")
    public ResponseEntity<?> getAttendanceRecordsByRange(@RequestParam String fechaInicio,
                                                        @RequestParam String fechaFin,
                                                        @RequestHeader("Authorization") String token) {
        try {
            User user = tokenValidationService.getUserByToken(token);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Token de acceso requerido"));
            }

            // Parsear fechas
            LocalDate startDate = LocalDate.parse(fechaInicio);
            LocalDate endDate = LocalDate.parse(fechaFin);

            // Validar que la fecha de inicio no sea posterior a la fecha de fin
            if (startDate.isAfter(endDate)) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "message", "La fecha de inicio no puede ser posterior a la fecha de fin"
                ));
            }

            var marcaciones = attendanceService.getAttendanceRecordsByDateRange(user.getId(), startDate, endDate);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "marcaciones", marcaciones,
                "total", marcaciones.size(),
                "fechaInicio", fechaInicio,
                "fechaFin", fechaFin
            ));

        } catch (DateTimeParseException e) {
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "message", "Formato de fecha inválido. Use YYYY-MM-DD"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    
    /**
     * Obtener estadísticas del empleado
     */
    @GetMapping("/empleado/estadisticas")
    public ResponseEntity<?> getEmployeeStatistics(@RequestHeader("Authorization") String token) {
        try {
            User user = tokenValidationService.getUserByToken(token);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Token de acceso requerido"));
            }

            // Usar el método que sí existe en la interfaz
            var estadisticas = attendanceService.getEmployeeStatistics(user.getId());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "estadisticas", estadisticas
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error interno del servidor"));
        }
    }

    /**
     * Obtener mensaje motivacional
     */
    @GetMapping("/mensaje-motivacional")
    public ResponseEntity<?> getMotivationalMessage(@RequestParam(required = false) String tipo,
                                                   @RequestHeader("Authorization") String token) {
        try {
            User user = tokenValidationService.getUserByToken(token);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Token de acceso requerido"));
            }

            // Usar MessageService directamente ya que no está en AttendanceService simplificado
            String mensaje = messageService.getRandomMessage(tipo != null ? tipo : "general");
            return ResponseEntity.ok(Map.of(
                "success", true,
                "mensaje", mensaje,
                "tipo", tipo != null ? tipo : "general",
                "fecha", LocalDate.now().toString(),
                "diaDelAño", LocalDate.now().getDayOfYear()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error interno del servidor"));
        }
    }

    /**
     * Verificar IP autorizada
     */
    @GetMapping("/verificar-ip")
    public ResponseEntity<?> verifyIp(@RequestHeader("X-Forwarded-For") String forwardedFor,
                                    @RequestHeader("X-Real-IP") String realIp) {
        try {
            String ip = forwardedFor != null ? forwardedFor.split(",")[0].trim() : realIp;
            boolean ipValida = attendanceService.isAuthorizedIp(ip);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "ipValida", ipValida,
                "ip", ip,
                "mensaje", ipValida ? "IP autorizada" : "IP no autorizada"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error interno del servidor"));
        }
    }

    /**
     * Endpoint de salud
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "API de Yego funcionando correctamente",
            "timestamp", java.time.Instant.now().toString(),
            "version", "1.0.0"
        ));
    }
    
    /**
     * Obtener la IP real del cliente
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String xRealIp = request.getHeader("X-Real-IP");
        String remoteAddr = request.getRemoteAddr();
        
        log.info("🔍 [AttendanceController] Headers IP - X-Forwarded-For: {}, X-Real-IP: {}, RemoteAddr: {}", 
            xForwardedFor, xRealIp, remoteAddr);
        
        // Prioridad: X-Forwarded-For > X-Real-IP > RemoteAddr
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For puede contener múltiples IPs, tomar la primera
            return xForwardedFor.split(",")[0].trim();
        }
        
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        // Fallback a RemoteAddr
        return remoteAddr != null ? remoteAddr : "unknown";
    }
    
    /**
     * Obtener historial de asistencia - marcaciones por rol
     */
    @GetMapping("/asistencia/historial/{userId}")
    public ResponseEntity<?> getHistorialAsistencia(@PathVariable Long userId,
                                                   @RequestParam(required = false) String fecha,
                                                   @RequestHeader("Authorization") String token) {
        try {
            User user = tokenValidationService.getUserByToken(token);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Token de acceso requerido"));
            }

            // Si no se proporciona fecha, usar la fecha actual en zona horaria de Perú
            LocalDate fechaConsulta = fecha != null ? LocalDate.parse(fecha) : LocalDate.now(java.time.ZoneId.of("America/Lima"));
            
            var marcaciones = attendanceService.getAttendanceRecordsByRole(userId, user.getRoleName(), fechaConsulta);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "marcaciones", marcaciones,
                "total", marcaciones != null ? marcaciones.size() : 0,
                "fecha", fechaConsulta.toString(),
                "rolUsuario", user.getRoleName()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
    
    /**
     * Obtener usuarios por rol
     */
    @GetMapping("/usuarios/por-rol")
    public ResponseEntity<?> getUsersByRole(@RequestHeader("Authorization") String token) {
        try {
            User user = tokenValidationService.getUserByToken(token);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Token de acceso requerido"));
            }

            var usuarios = attendanceService.getUsersByRole(user.getRoleName());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "usuarios", usuarios,
                "total", usuarios != null ? usuarios.size() : 0,
                "rolUsuario", user.getRoleName()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
    
    /**
     * Exportar marcaciones a Excel por rango de fechas y rol
     */
    @GetMapping("/marcaciones/exportar")
    public ResponseEntity<?> exportarMarcaciones(@RequestParam String fechaInicio,
                                                  @RequestParam String fechaFin,
                                                  @RequestParam String rol,
                                                  @RequestHeader("Authorization") String token) {
        log.info("📥 [AttendanceController] Solicitud de exportación - Fecha Inicio: {}, Fecha Fin: {}, Rol: {}", 
            fechaInicio, fechaFin, rol);
        try {
            User user = tokenValidationService.getUserByToken(token);
            if (user == null) {
                log.warn("⚠️ [AttendanceController] Token inválido o expirado");
                return ResponseEntity.status(401).body(Map.of("message", "Token de acceso requerido"));
            }
            
            log.info("✅ [AttendanceController] Usuario autenticado: {} (Rol: {})", user.getUsername(), user.getRoleName());

            // Delegar toda la lógica al servicio
            byte[] excelBytes = attendanceService.exportarMarcacionesPorRangoDeFechasYRol(
                fechaInicio, fechaFin, rol, user.getRoleName());
            
            log.info("📊 [AttendanceController] Excel generado - Tamaño: {} bytes", excelBytes != null ? excelBytes.length : 0);
            
            if (excelBytes == null || excelBytes.length == 0) {
                log.warn("⚠️ [AttendanceController] Excel vacío o null - No hay marcaciones para exportar");
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "No se encontraron marcaciones para los parámetros especificados"
                ));
            }

            // Configurar headers para descarga
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String nombreArchivo = "marcaciones_" + fechaInicio + "_" + fechaFin + "_" + rol + ".xlsx";
            headers.setContentDispositionFormData("attachment", nombreArchivo);
            headers.setContentLength(excelBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);

        } catch (Exception e) {
            log.error("❌ [AttendanceController] Error exportando marcaciones: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
}

