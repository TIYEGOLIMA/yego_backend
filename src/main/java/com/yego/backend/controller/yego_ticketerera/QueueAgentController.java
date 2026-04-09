package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.response.AsignarModuloResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.RecuperarModuloResponse;
import com.yego.backend.service.yego_ticketerera.QueueAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para la gestión de agentes en cola del sistema YEGO Ticketerera
 */
@RestController
@RequestMapping("/api/ticketera/queue-agents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QueueAgentController {
    
    private final QueueAgentService queueAgentService;

    @PostMapping("/asignar")
    public ResponseEntity<AsignarModuloResponse> asignarModuloAUsuario(
            @RequestBody Map<String, Object> request) {
        return queueAgentService.asignarModuloAUsuario(request);
    }

    @PostMapping("/liberar-modulo/{userId}")
    public ResponseEntity<Map<String, Object>> liberarModulo(@PathVariable Long userId) {
        return queueAgentService.liberarModuloDelUsuario(userId);
    }

    @PostMapping("/liberar-modulo-por-id/{moduleId}")
    public ResponseEntity<Map<String, Object>> liberarModuloPorModuleId(@PathVariable Long moduleId) {
        return queueAgentService.liberarModuloPorModuleId(moduleId);
    }

    @GetMapping("/recuperar-modulo/{userId}")
    public ResponseEntity<RecuperarModuloResponse> recuperarModulo(@PathVariable Long userId) {
        return queueAgentService.recuperarModuloAsignado(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/jwt-verify")
    public ResponseEntity<Map<String, Object>> verificarJWT(Authentication authentication) {
        return queueAgentService.verificarJWT(authentication);
    }
}