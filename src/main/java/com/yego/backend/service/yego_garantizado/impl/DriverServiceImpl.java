package com.yego.backend.service.yego_garantizado.impl;

import com.yego.backend.entity.yego_garantizado.api.response.DriverInfo;
import com.yego.backend.entity.yego_garantizado.api.response.DriverValidationResponse;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaDisponibleResponse;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaInfo;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import com.yego.backend.entity.yego_garantizado.entities.Driver;
import com.yego.backend.repository.yego_garantizado.DriverRepository;
import com.yego.backend.service.yego_garantizado.DriverService;
import com.yego.backend.service.yego_garantizado.FlotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final FlotaService flotaService;
    
    // Park IDs permitidos (filtro)
    private static final Set<String> PARK_IDS_PERMITIDOS = Set.of(
        "08e20910d81d42658d4334d3f6d10ac0", // Yego 
        "56e4607dfc354e0a9cde4f0aa7973003", // Yego Arequipa
        "ef21f793358144f589aabcbeb8bd7d50", // Yego Barranquilla
        "c054c8b5dfe14e75b882943b2a252706", // Yego Black
        "c58110bc70244430a70a8126fc69f22c", // Yego Líderes
        "5921e55cc5d042d28747dd722608955a", // Yego Prime
        "ff424287c4bd4cbba6066962951a121f", // Yego Promi
        "851e30755bba4d298e2e837f571b4ab8"  // Yego Trujillo
    );

    public DriverServiceImpl(DriverRepository driverRepository, FlotaService flotaService) {
        this.driverRepository = driverRepository;
        this.flotaService = flotaService;
    }

    @Override
    public Optional<DriverInfo> validarYObtenerLicencia(String licenseNumber) {
        log.info("Validando y obteniendo datos de licencia: {}", licenseNumber);
        List<Driver> registros = driverRepository.findAllByLicenseNumber(licenseNumber);
        if (!registros.isEmpty()) {
            DriverInfo driverInfo = convertirADriverInfo(registros.get(0));
            log.info("Licencia {} encontrada - Conductor: {}", licenseNumber, driverInfo.getFullName());
            return Optional.of(driverInfo);
        }
        log.warn("Licencia {} no encontrada en la base de datos", licenseNumber);
        return Optional.empty();
    }

    @Override
    public DriverValidationResponse validarLicencia(String licenseNumber) {
        log.info("Validando licencia: {}", licenseNumber);
        List<Driver> registros = driverRepository.findAllByLicenseNumber(licenseNumber);
        List<DriverInfo> lista = registros.stream()
                .map(this::convertirADriverInfo)
                .collect(Collectors.toList());
        boolean existe = !lista.isEmpty();
        if (existe) {
            log.info("Licencia {} existe con {} coincidencias", licenseNumber, lista.size());
        } else {
            log.warn("Licencia {} no existe", licenseNumber);
        }
        return DriverValidationResponse.builder()
                .licenseNumber(licenseNumber)
                .existe(existe)
                .mensaje(existe ? "Coincidencias encontradas" : "Licencia no encontrada")
                .drivers(lista)
                .build();
    }
    
    @Override
    public FlotaDisponibleResponse obtenerConductorConFlotas(String licenseNumber) {
        log.info("Obteniendo conductor con flotas disponibles: {}", licenseNumber);
        
        // 1. Buscar TODOS los registros del conductor
        List<Driver> todosLosRegistros = driverRepository.findAllByLicenseNumber(licenseNumber);
        
        if (todosLosRegistros.isEmpty()) {
            log.warn("No se encontraron registros para la licencia: {}", licenseNumber);
            return FlotaDisponibleResponse.builder()
                    .licenseNumber(licenseNumber)
                    .conductor(null)
                    .flotasDisponibles(new ArrayList<>())
                    .mensaje("Licencia no encontrada")
                    .build();
        }
        
        // 2 Obtener info del conductor (del primer registro)
        Driver primerRegistro = todosLosRegistros.get(0);
        DriverInfo conductorInfo = convertirADriverInfo(primerRegistro);
        
        // 3. Filtrar solo los park_id permitidos
        Set<String> parkIdsDelConductor = todosLosRegistros.stream()
                .map(Driver::getParkId)
                .filter(PARK_IDS_PERMITIDOS::contains)
                .collect(Collectors.toSet());
        
        log.info("Conductor tiene {} flotas, {} son permitidas", 
                todosLosRegistros.size(), parkIdsDelConductor.size());
        
        // 4. Obtener información de flotas desde la API
        List<FlotaResponse> todasLasFlotas = flotaService.obtenerFlotas();
        
        // 5. Filtrar solo las flotas del conductor
        List<FlotaInfo> flotasDisponibles = todasLasFlotas.stream()
                .filter(flota -> parkIdsDelConductor.contains(flota.getId()))
                .map(flota -> FlotaInfo.builder()
                        .parkId(flota.getId())
                        .parkName(flota.getName())
                        .city(flota.getCity())
                        .activo(true)
                        .build())
                .collect(Collectors.toList());
        
        log.info("Flotas disponibles para {}: {}", licenseNumber, flotasDisponibles.size());
        
        return FlotaDisponibleResponse.builder()
                .licenseNumber(licenseNumber)
                .conductor(conductorInfo)
                .flotasDisponibles(flotasDisponibles)
                .mensaje("Conductor encontrado con " + flotasDisponibles.size() + " flotas disponibles")
                .build();
    }
    
    private DriverInfo convertirADriverInfo(Driver driver) {
        DriverInfo info = new DriverInfo();
        info.setDriverId(driver.getDriverId());
        info.setFullName(driver.getFullName());
        info.setPhone(driver.getPhone());
        info.setRating(driver.getRating());
        info.setLicenseNumber(driver.getLicenseNumber());
        return info;
    }
}

