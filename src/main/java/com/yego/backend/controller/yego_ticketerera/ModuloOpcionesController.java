package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.Option;
import com.yego.backend.service.yego_ticketerera.OptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ticketera/modulo-opciones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ModuloOpcionesController {

    private final OptionService optionService;

    @GetMapping("/options")
    public ResponseEntity<List<Option>> obtenerModulosActivos() {
        return ResponseEntity.ok(optionService.obtenerModulosActivos());
    }

    @GetMapping("/{parentId}/suboptions")
    public ResponseEntity<List<Option>> obtenerSubopciones(@PathVariable Long parentId) {
        return ResponseEntity.ok(optionService.obtenerSubopciones(parentId));
    }
}
