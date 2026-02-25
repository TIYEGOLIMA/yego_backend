package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.service.yego_principal.SessionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Controlador REST para sesiones del sistema YEGO Principal
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    private static boolean isAdmin(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPERADMIN".equals(a.getAuthority()));
    }

    private static Long requireUserId(Authentication auth) {
        if (auth == null) return null;
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ResponseEntity<?> handle(Supplier<ResponseEntity<?>> action, String notFoundLog, String errorLog) {
        try {
            return action.get();
        } catch (EntityNotFoundException e) {
            log.warn("⚠️ [SessionController] {}", notFoundLog, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [SessionController] {}", errorLog, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getSessions(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        Long userId = requireUserId(authentication);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (isAdmin(authentication)) {
            return ResponseEntity.ok(sessionService.findActiveSessionsPage(page - 1, size, search));
        }
        return ResponseEntity.ok(sessionService.findAll(userId));
    }

    @GetMapping("/connection-logs")
    public ResponseEntity<List<ConnectionLogResponseDto>> getConnectionLogs(
            @RequestParam(required = false, defaultValue = "30") int days,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        return ResponseEntity.ok(sessionService.getConnectionLogs(days, limit, null, null));
    }

    @GetMapping("/stats")
    public ResponseEntity<SessionStatsDto> getSessionStats() {
        return ResponseEntity.ok(sessionService.getSessionStats());
    }

    @PostMapping("/bulk/deactivate")
    public ResponseEntity<?> bulkDeactivate(@RequestBody List<Long> ids, Authentication authentication) {
        Long adminUserId = requireUserId(authentication);
        if (adminUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No se enviaron IDs de sesión"));
        }
        return handle(
                () -> {
                    sessionService.deactivateByIds(ids);
                    return ResponseEntity.ok().build();
                },
                "N/A",
                "Error cerrando sesiones: {}"
        );
    }

    @PostMapping("/{sessionId}/force-logout")
    public ResponseEntity<?> forceLogout(@PathVariable Long sessionId, Authentication authentication) {
        Long adminUserId = requireUserId(authentication);
        if (adminUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return handle(
                () -> {
                    sessionService.forceLogout(sessionId, adminUserId);
                    return ResponseEntity.ok().build();
                },
                "Sesión no encontrada: {}",
                "Error al forzar cierre de sesión: {}"
        );
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> closeUserSessions(@PathVariable Long userId) {
        return handle(
                () -> {
                    sessionService.deactivateByUserId(userId, "Cerrado por administrador");
                    return ResponseEntity.ok().build();
                },
                "N/A",
                "Error cerrando sesiones del usuario: {}"
        );
    }
}