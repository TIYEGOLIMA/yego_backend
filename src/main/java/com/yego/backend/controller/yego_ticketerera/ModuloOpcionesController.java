package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.Option;
import com.yego.backend.service.yego_ticketerera.OptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de opciones y módulos del sistema YEGO Ticketerera
 */
@RestController
@RequestMapping("/api/ticketera/modulo-opciones")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ModuloOpcionesController {
    
    private final OptionService optionService;
    
    @GetMapping
   public ResponseEntity<List<Option>> obtenerTodasLasOpciones() {
        log.info("Endpoint: Obtener todas las opciones");
        List<Option> options = optionService.obtenerTodasLasOpciones();
        return ResponseEntity.ok(options);
    }
    
    @GetMapping("/options")
    public ResponseEntity<List<Option>> obtenerModulosActivos() {
        log.info("Endpoint: Obtener módulos activos");
        List<Option> modules = optionService.obtenerModulosActivos();
        return ResponseEntity.ok(modules);
    }
    
    @GetMapping("/{parentId}/suboptions")
    public ResponseEntity<List<Option>> obtenerSubopciones(@PathVariable Long parentId) {
        log.info("Endpoint: Obtener subopciones del módulo {}", parentId);
        List<Option> suboptions = optionService.obtenerSubopciones(parentId);
        return ResponseEntity.ok(suboptions);
    }
}

