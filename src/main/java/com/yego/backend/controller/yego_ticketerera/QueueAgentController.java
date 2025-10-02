package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueAgent;
import com.yego.backend.entity.yego_ticketerera.api.response.UserModuleStatusResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.RecuperarModuloResponse;
import com.yego.backend.service.yego_ticketerera.QueueAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de agentes en cola del sistema YEGO Ticketerera
 */
@RestController
@RequestMapping("/api/ticketera/queue-agents")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class QueueAgentController {
    
    private final QueueAgentService queueAgentService;
    
    @PostMapping("/asignar")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('OPERADOR') or hasRole('SAC') or hasRole('TV') or hasRole('TABLET1') or hasRole('TABLET2') or hasRole('PRINCIPAL')")
    public ResponseEntity<QueueAgent> asignarModuloAUsuario(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        return queueAgentService.asignarModuloAUsuario(request, authentication);
    }
    
    @PostMapping("/liberar")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('OPERADOR') or hasRole('SAC') or hasRole('TV') or hasRole('TABLET1') or hasRole('TABLET2') or hasRole('PRINCIPAL')")
    public ResponseEntity<Void> liberarModuloDeUsuario(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        return queueAgentService.liberarModuloDeUsuario(request, authentication);
    }
    
    @GetMapping("/activos")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('OPERADOR') or hasRole('SAC') or hasRole('TV') or hasRole('TABLET1') or hasRole('TABLET2') or hasRole('PRINCIPAL')")
    public ResponseEntity<List<QueueAgent>> obtenerAgentesActivos() {
        List<QueueAgent> agentes = queueAgentService.obtenerAgentesActivos();
        return ResponseEntity.ok(agentes);
    }
    
    @GetMapping("/user/{userId}/status")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('OPERADOR') or hasRole('SAC') or hasRole('TV') or hasRole('TABLET1') or hasRole('TABLET2') or hasRole('PRINCIPAL')")
    public ResponseEntity<UserModuleStatusResponse> verificarEstadoModuloUsuario(@PathVariable Long userId) {
        UserModuleStatusResponse status = queueAgentService.verificarEstadoModuloUsuario(userId);
        return ResponseEntity.ok(status);
    }
    
    @PostMapping("/user/{userId}/restore")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('OPERADOR') or hasRole('SAC') or hasRole('TV') or hasRole('TABLET1') or hasRole('TABLET2') or hasRole('PRINCIPAL')")
    public ResponseEntity<RecuperarModuloResponse> restaurarModuloUsuario(@PathVariable Long userId) {
        RecuperarModuloResponse response = queueAgentService.restaurarModuloUsuario(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/jwt-verify")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('OPERADOR') or hasRole('SAC') or hasRole('TV') or hasRole('TABLET1') or hasRole('TABLET2') or hasRole('PRINCIPAL')")
    public ResponseEntity<Map<String, Object>> verificarJWT(Authentication authentication) {
        return queueAgentService.verificarJWT(authentication);
    }
}