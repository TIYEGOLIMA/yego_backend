package com.yego.backend.controller.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.request.CrearCiudadRequest;
import com.yego.backend.entity.yego_garantizado.api.request.CrearPaisRequest;
import com.yego.backend.entity.yego_garantizado.api.response.PaisListResponse;
import com.yego.backend.entity.yego_garantizado.api.response.UbicacionResponse;
import com.yego.backend.entity.yego_garantizado.api.response.UbicacionesCompletasResponse;
import com.yego.backend.service.yego_garantizado.UbicacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para manejo de ubicaciones (países y ciudades)
 * del sistema YEGO Garantizado
 */
@Slf4j
@RestController
@RequestMapping("/api/garantizado/ubicaciones")
@RequiredArgsConstructor
public class UbicacionController {

    private final UbicacionService ubicacionService;

    /**
     * Crear un país
     * POST /api/garantizado/ubicaciones/paises
     */
    @PostMapping("/paises")
    public ResponseEntity<UbicacionResponse> crearPais(@RequestBody CrearPaisRequest request) {
        log.info("📥 [UbicacionController] Solicitud para crear país: {}", request.getNombre());
        
        try {
            UbicacionResponse pais = ubicacionService.crearPais(request);
            log.info("✅ [UbicacionController] País creado exitosamente con ID: {}", pais.getId());
            return ResponseEntity.ok(pais);
        } catch (Exception e) {
            log.error("❌ [UbicacionController] Error creando país: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Crear una ciudad
     * POST /api/garantizado/ubicaciones/ciudades
     */
    @PostMapping("/ciudades")
    public ResponseEntity<UbicacionResponse> crearCiudad(@RequestBody CrearCiudadRequest request) {
        log.info("📥 [UbicacionController] Solicitud para crear ciudad: {} en país ID: {}", 
            request.getNombre(), request.getPais_id());
        
        try {
            UbicacionResponse ciudad = ubicacionService.crearCiudad(request);
            log.info("✅ [UbicacionController] Ciudad creada exitosamente con ID: {}", ciudad.getId());
            return ResponseEntity.ok(ciudad);
        } catch (Exception e) {
            log.error("❌ [UbicacionController] Error creando ciudad: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener todos los países activos
     * GET /api/garantizado/ubicaciones/paises
     */
    @GetMapping("/paises")
    public ResponseEntity<List<PaisListResponse>> obtenerPaises() {
        log.info("📥 [UbicacionController] Solicitud para obtener países");
        
        try {
            List<PaisListResponse> paises = ubicacionService.obtenerPaises();
            log.info("✅ [UbicacionController] Encontrados {} países", paises.size());
            return ResponseEntity.ok(paises);
        } catch (Exception e) {
            log.error("❌ [UbicacionController] Error obteniendo países: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener ciudades de un país
     * GET /api/garantizado/ubicaciones/ciudades/pais/{paisId}
     */
    @GetMapping("/ciudades/pais/{paisId}")
    public ResponseEntity<List<UbicacionResponse>> obtenerCiudades(@PathVariable Long paisId) {
        log.info("📥 [UbicacionController] Solicitud para obtener ciudades del país ID: {}", paisId);
        
        try {
            List<UbicacionResponse> ciudades = ubicacionService.obtenerCiudades(paisId);
            log.info("✅ [UbicacionController] Encontradas {} ciudades", ciudades.size());
            return ResponseEntity.ok(ciudades);
        } catch (Exception e) {
            log.error("❌ [UbicacionController] Error obteniendo ciudades: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener todas las ubicaciones completas (países con sus ciudades)
     * GET /api/garantizado/ubicaciones/find-all
     */
    @GetMapping("/find-all")
    public ResponseEntity<UbicacionesCompletasResponse> obtenerTodasLasUbicaciones() {
        log.info("📥 [UbicacionController] Solicitud para obtener todas las ubicaciones");
        
        try {
            UbicacionesCompletasResponse response = ubicacionService.obtenerTodasLasUbicaciones();
            log.info("✅ [UbicacionController] Ubicaciones completas obtenidas exitosamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [UbicacionController] Error obteniendo ubicaciones completas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

