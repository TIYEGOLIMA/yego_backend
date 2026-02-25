package com.yego.backend.controller.yego_asistencia;

import com.yego.backend.util.HttpRequestUtils;
import com.yego.backend.service.yego_asistencia.AttendanceService;
import com.yego.backend.service.yego_asistencia.TokenValidationService;
import com.yego.backend.entity.yego_principal.entities.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/asistencia")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final TokenValidationService tokenValidationService;

    public AttendanceController(AttendanceService attendanceService, TokenValidationService tokenValidationService) {
        this.attendanceService = attendanceService;
        this.tokenValidationService = tokenValidationService;
    }

    /**
     * Registrar marcación (validación de IP y secuencia en el servicio).
     */
    @PostMapping("/marcacion")
    public ResponseEntity<?> createAttendance(@RequestBody Map<String, Object> attendanceData,
                                              @RequestHeader("Authorization") String token,
                                              HttpServletRequest request) {
        try {
            User user = tokenValidationService.getUserByToken(token);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Token de acceso requerido"));
            }
            String clientIp = HttpRequestUtils.getClientIpAddress(request);
            Map<String, Object> result = attendanceService.processAttendanceMarkingWithIpValidation(
                user.getId(), attendanceData, clientIp);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            }
            String errorCode = (String) result.get("errorCode");
            if ("IP_UNAUTHORIZED".equals(errorCode)) {
                return ResponseEntity.status(403).body(result);
            }
            return ResponseEntity.status(400).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error interno del servidor"));
        }
    }

    @GetMapping("/marcaciones/hoy")
    public ResponseEntity<?> getTodayAttendances(@RequestHeader("Authorization") String token) {
        try {
            User user = tokenValidationService.getUserByToken(token);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Token de acceso requerido"));
            }
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

    @GetMapping("/marcaciones/rango")
    public ResponseEntity<?> getAttendanceRecordsByRange(@RequestParam String fechaInicio,
                                                         @RequestParam String fechaFin,
                                                         @RequestHeader("Authorization") String token) {
        try {
            User user = tokenValidationService.getUserByToken(token);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Token de acceso requerido"));
            }
            Map<String, Object> result = attendanceService.getAttendanceRecordsByDateRangeValidated(
                user.getId(), fechaInicio, fechaFin);
            if (Boolean.FALSE.equals(result.get("success"))) {
                return ResponseEntity.status(400).body(result);
            }
            @SuppressWarnings("unchecked")
            var marcaciones = (java.util.List<?>) result.get("marcaciones");
            return ResponseEntity.ok(Map.of(
                "success", true,
                "marcaciones", result.get("marcaciones"),
                "total", marcaciones != null ? marcaciones.size() : 0,
                "fechaInicio", result.get("fechaInicio"),
                "fechaFin", result.get("fechaFin")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/empleado/estadisticas")
    public ResponseEntity<?> getEmployeeStatistics(@RequestHeader("Authorization") String token) {
        try {
            User user = tokenValidationService.getUserByToken(token);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Token de acceso requerido"));
            }
            var estadisticas = attendanceService.getEmployeeStatistics(user.getId());
            return ResponseEntity.ok(Map.of("success", true, "estadisticas", estadisticas));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error interno del servidor"));
        }
    }

    @GetMapping("/verificar-ip")
    public ResponseEntity<?> verifyIp(HttpServletRequest request) {
        try {
            String ip = HttpRequestUtils.getClientIpAddress(request);
            return ResponseEntity.ok(attendanceService.verifyIpResponse(ip));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error interno del servidor"));
        }
    }

    @GetMapping("/historial/{userId}")
    public ResponseEntity<?> getHistorialAsistencia(@PathVariable Long userId,
                                                   @RequestParam(required = false) String fecha,
                                                   @RequestHeader("Authorization") String token) {
        try {
            User user = tokenValidationService.getUserByToken(token);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Token de acceso requerido"));
            }
            var marcaciones = attendanceService.getAttendanceRecordsByRole(userId, user.getRoleName(), fecha);
            String fechaResp = (fecha != null && !fecha.isBlank())
                ? fecha
                : java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")).toString();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "marcaciones", marcaciones,
                "total", marcaciones != null ? marcaciones.size() : 0,
                "fecha", fechaResp,
                "rolUsuario", user.getRoleName()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
    
    @GetMapping("/usuarios-por-rol")
    public ResponseEntity<?> getUsersByRole(@RequestHeader("Authorization") String token) {
        try {
            User user = tokenValidationService.getUserByToken(token);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Token de acceso requerido"));
            }
            var usuarios = attendanceService.getUsersByRole(user.getId(), user.getRoleName());
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

    @GetMapping("/marcaciones/exportar")
    public ResponseEntity<?> exportarMarcaciones(@RequestParam String fechaInicio,
                                                 @RequestParam String fechaFin,
                                                 @RequestParam String rol,
                                                 @RequestHeader("Authorization") String token) {
        try {
            User user = tokenValidationService.getUserByToken(token);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Token de acceso requerido"));
            }
            var result = attendanceService.exportarMarcacionesPorRangoDeFechasYRol(
                fechaInicio, fechaFin, rol, user.getRoleName());
            if (!result.hasContent()) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "No se encontraron marcaciones para los parámetros especificados"
                ));
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", result.getFileName());
            headers.setContentLength(result.getContent().length);
            return ResponseEntity.ok().headers(headers).body(result.getContent());
        } catch (Exception e) {
            log.error("❌ [AttendanceController] Error exportando marcaciones: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
}

