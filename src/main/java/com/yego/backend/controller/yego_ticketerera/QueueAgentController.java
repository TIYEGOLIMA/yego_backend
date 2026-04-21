package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.response.AsignarModuloResponse;
import com.yego.backend.service.yego_ticketerera.QueueAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
}