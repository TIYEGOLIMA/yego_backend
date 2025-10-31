package com.yego.backend.service.yego_garantizado.impl;

import com.yego.backend.entity.yego_garantizado.api.request.CrearCiudadRequest;
import com.yego.backend.entity.yego_garantizado.api.request.CrearPaisRequest;
import com.yego.backend.entity.yego_garantizado.api.response.*;
import com.yego.backend.entity.yego_garantizado.entities.Ubicacion;
import com.yego.backend.repository.yego_garantizado.UbicacionRepository;
import com.yego.backend.service.yego_garantizado.UbicacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de ubicaciones del sistema YEGO Garantizado
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UbicacionServiceImpl implements UbicacionService {

    private final UbicacionRepository ubicacionRepository;

    @Override
    public UbicacionResponse crearPais(CrearPaisRequest request) {
        log.info("🌍 [UbicacionService] Creando país: {}", request.getNombre());
        
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setNombre(request.getNombre());
        ubicacion.setMoneda(request.getMoneda());
        ubicacion.setSimboloMoneda(request.getSimbolo_moneda());
        ubicacion.setNivel("PAIS");
        ubicacion.setParentId(null);
        ubicacion.setActivo(true);
        
        Ubicacion saved = ubicacionRepository.save(ubicacion);
        log.info("✅ [UbicacionService] País creado con ID: {}", saved.getId());
        
        return mapToResponse(saved);
    }

    @Override
    public UbicacionResponse crearCiudad(CrearCiudadRequest request) {
        log.info("🏙️ [UbicacionService] Creando ciudad: {} para país ID: {}", request.getNombre(), request.getPais_id());
        
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setNombre(request.getNombre());
        ubicacion.setParentId(request.getPais_id());
        ubicacion.setNivel("CIUDAD");
        ubicacion.setActivo(true);
        
        Ubicacion saved = ubicacionRepository.save(ubicacion);
        log.info("✅ [UbicacionService] Ciudad creada con ID: {}", saved.getId());
        
        return mapToResponse(saved);
    }

    @Override
    public List<PaisListResponse> obtenerPaises() {
        log.info("🔍 [UbicacionService] Obteniendo países activos");
        
        List<Ubicacion> paises = ubicacionRepository.findByNivelAndActivoTrue("PAIS");
        log.info("✅ [UbicacionService] Encontrados {} países", paises.size());
        
        return paises.stream()
                .map(this::mapToPaisListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UbicacionResponse> obtenerCiudades(Long paisId) {
        log.info("🔍 [UbicacionService] Obteniendo ciudades para país ID: {}", paisId);
        
        List<Ubicacion> ciudades = ubicacionRepository.findByParentIdAndNivelAndActivoTrue(paisId, "CIUDAD");
        log.info("✅ [UbicacionService] Encontradas {} ciudades", ciudades.size());
        
        return ciudades.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UbicacionesCompletasResponse obtenerTodasLasUbicaciones() {
        log.info("🔍 [UbicacionService] Obteniendo todas las ubicaciones");
        
        // Obtener todos los países activos
        List<Ubicacion> paises = ubicacionRepository.findByNivelAndActivoTrue("PAIS");
        log.info("✅ [UbicacionService] Encontrados {} países", paises.size());
        
        // Obtener todas las ciudades activas
        List<Ubicacion> ciudades = ubicacionRepository.findByNivelAndActivoTrue("CIUDAD");
        log.info("✅ [UbicacionService] Encontradas {} ciudades", ciudades.size());
        
        // Agrupar ciudades por paisId
        Map<Long, List<CiudadListResponse>> ciudadesPorPais = ciudades.stream()
                .map(this::mapToCiudadListResponse)
                .collect(Collectors.groupingBy(CiudadListResponse::getPais_id));
        
        // Mapear países con sus ciudades
        List<PaisConCiudadesResponse> paisesConCiudades = paises.stream()
                .map(pais -> {
                    List<CiudadListResponse> ciudadesDelPais = ciudadesPorPais.getOrDefault(
                            pais.getId(), List.of());
                    
                    return PaisConCiudadesResponse.builder()
                            .id(pais.getId())
                            .nombre(pais.getNombre())
                            .simbolo_moneda(pais.getSimboloMoneda())
                            .ciudades(ciudadesDelPais)
                            .build();
                })
                .collect(Collectors.toList());
        
        log.info("✅ [UbicacionService] Estructura completa generada exitosamente");
        
        return UbicacionesCompletasResponse.builder()
                .paises(paisesConCiudades)
                .build();
    }

    /**
     * Mapea una entidad Ubicacion a su DTO de respuesta
     */
    private UbicacionResponse mapToResponse(Ubicacion ubicacion) {
        return UbicacionResponse.builder()
                .id(ubicacion.getId())
                .nombre(ubicacion.getNombre())
                .moneda(ubicacion.getMoneda())
                .simbolo_moneda(ubicacion.getSimboloMoneda())
                .parent_id(ubicacion.getParentId())
                .nivel(ubicacion.getNivel())
                .activo(ubicacion.getActivo())
                .build();
    }

    /**
     * Mapea una entidad Ubicacion a su DTO simplificado para listas de países
     */
    private PaisListResponse mapToPaisListResponse(Ubicacion ubicacion) {
        return PaisListResponse.builder()
                .id(ubicacion.getId())
                .nombre(ubicacion.getNombre())
                .build();
    }

    /**
     * Mapea una entidad Ubicacion a su DTO simplificado para listas de ciudades
     */
    private CiudadListResponse mapToCiudadListResponse(Ubicacion ubicacion) {
        return CiudadListResponse.builder()
                .id(ubicacion.getId())
                .nombre(ubicacion.getNombre())
                .pais_id(ubicacion.getParentId())
                .build();
    }
}

