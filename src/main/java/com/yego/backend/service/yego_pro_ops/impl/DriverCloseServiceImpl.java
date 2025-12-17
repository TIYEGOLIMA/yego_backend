package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.request.DriverCloseRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverCloseResponse;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.repository.yego_pro_ops.DriverCloseRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_pro_ops.DriverCloseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverCloseServiceImpl implements DriverCloseService {

    private final DriverCloseRepository driverCloseRepository;
    private final UserRepository userRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Convierte un Double a BigDecimal, retorna null si el valor es null
     */
    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    @Override
    @Transactional
    public DriverClose registrarCierre(DriverCloseRequest request) {
        Long userId = request.getUserId();
        log.info("💰 [DriverCloseService] Registrando cierre para driver_id: {}, fecha: {}, registrado por user_id: {}", 
            request.getDriverId(), request.getFecha(), userId);

        // Parsear la fecha
        LocalDate fecha = LocalDate.parse(request.getFecha(), DATE_FORMATTER);

        // Verificar si ya existe un cierre para este driver y fecha
        driverCloseRepository.findFirstByDriverIdAndFechaOrderByIdDesc(request.getDriverId(), fecha)
            .ifPresent(existingClose -> {
                log.warn("⚠️ [DriverCloseService] Ya existe un cierre para driver_id: {} y fecha: {}. Se actualizará el registro existente.", 
                    request.getDriverId(), request.getFecha());
                driverCloseRepository.delete(existingClose);
            });
        
        // Crear nueva entidad
        DriverClose driverClose = DriverClose.builder()
            .driverId(request.getDriverId())
            .fecha(fecha)
            .userId(userId)
            .gnvM3(request.getGnvM3())
            .gnvSoles(toBigDecimal(request.getGnvSoles()))
            .gasolinaGalones(request.getGasolinaGalones())
            .gasolinaSoles(toBigDecimal(request.getGasolinaSoles()))
            .liquidaEfectivo(toBigDecimal(request.getLiquidaEfectivo()))
            .liquidaYape(toBigDecimal(request.getLiquidaYape()))
            .otrosGastos(toBigDecimal(request.getOtrosGastos()))
            .otrosGastosDescripcion(request.getOtrosGastosDescripcion())
            .totalIngresos(toBigDecimal(request.getTotalIngresos()))
            .totalGastos(toBigDecimal(request.getTotalGastos()))
            .resta(toBigDecimal(request.getResta()))
            .build();

        DriverClose saved = driverCloseRepository.save(driverClose);
        log.info("✅ [DriverCloseService] Cierre registrado exitosamente con ID: {}, creado por user_id: {}", 
            saved.getId(), saved.getUserId());
        
        return saved;
    }

    @Override
    public Optional<DriverCloseResponse> obtenerCierrePorDriverIdYFecha(String driverId, String fecha) {
        if (driverId == null || driverId.trim().isEmpty() || fecha == null || fecha.trim().isEmpty()) {
            return Optional.empty();
        }
        
        try {
            String driverIdLimpio = driverId.trim();
            String fechaLimpia = fecha.trim();
            LocalDate fechaLocalDate = LocalDate.parse(fechaLimpia, DATE_FORMATTER);
            
            Optional<DriverClose> cierre = driverCloseRepository.findFirstByDriverIdAndFechaOrderByIdDesc(driverIdLimpio, fechaLocalDate);
            
            if (cierre.isPresent()) {
                return Optional.of(convertirAResponse(cierre.get()));
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.error("❌ [DriverCloseService] Error obteniendo cierre: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public DriverClose actualizarCierre(DriverCloseRequest request) {
        Long id = request.getId();
        if (id == null) {
            throw new RuntimeException("El id es requerido para actualizar el cierre");
        }
        
        log.info("🔄 [DriverCloseService] Actualizando cierre con ID: {}, driver_id: {}, fecha: {}", 
            id, request.getDriverId(), request.getFecha());
        
        DriverClose cierreExistente = driverCloseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("No se encontró cierre con ID: " + id + " para actualizar"));

        // Actualizar campos (NO se modifica userId que es quien creó el registro)
        cierreExistente.setGnvM3(request.getGnvM3());
        cierreExistente.setGnvSoles(toBigDecimal(request.getGnvSoles()));
        cierreExistente.setGasolinaGalones(request.getGasolinaGalones());
        cierreExistente.setGasolinaSoles(toBigDecimal(request.getGasolinaSoles()));
        cierreExistente.setLiquidaEfectivo(toBigDecimal(request.getLiquidaEfectivo()));
        cierreExistente.setLiquidaYape(toBigDecimal(request.getLiquidaYape()));
        cierreExistente.setOtrosGastos(toBigDecimal(request.getOtrosGastos()));
        cierreExistente.setOtrosGastosDescripcion(request.getOtrosGastosDescripcion());
        cierreExistente.setTotalIngresos(toBigDecimal(request.getTotalIngresos()));
        cierreExistente.setTotalGastos(toBigDecimal(request.getTotalGastos()));
        cierreExistente.setResta(toBigDecimal(request.getResta()));
        
        // Guardar el userId que modificó el registro (userId original NO se modifica)
        // El userId del request es quien está actualizando, se guarda en userIdModificado
        if (request.getUserId() != null) {
            cierreExistente.setUserIdModificado(request.getUserId());
        }

        DriverClose actualizado = driverCloseRepository.save(cierreExistente);
        log.info("✅ [DriverCloseService] Cierre actualizado exitosamente con ID: {}, modificado por user_id: {}", 
            actualizado.getId(), actualizado.getUserIdModificado());
        
        return actualizado;
    }

    /**
     * Convierte la entidad DriverClose a DriverCloseResponse
     */
    private DriverCloseResponse convertirAResponse(DriverClose cierre) {
        // Obtener el nombre del usuario que registró el cierre
        String userName = userRepository.findById(cierre.getUserId())
            .map(user -> {
                String name = user.getName() != null ? user.getName() : "";
                String lastName = user.getLastName() != null ? user.getLastName() : "";
                return (name + " " + lastName).trim();
            })
            .orElse("Usuario desconocido");
        
        // Obtener el nombre del usuario que modificó el cierre (si existe)
        String userNameModificado = null;
        if (cierre.getUserIdModificado() != null) {
            userNameModificado = userRepository.findById(cierre.getUserIdModificado())
                .map(user -> {
                    String name = user.getName() != null ? user.getName() : "";
                    String lastName = user.getLastName() != null ? user.getLastName() : "";
                    return (name + " " + lastName).trim();
                })
                .orElse("Usuario desconocido");
        }
        
        return DriverCloseResponse.builder()
            .id(cierre.getId())
            .driverId(cierre.getDriverId())
            .fecha(cierre.getFecha())
            .userId(cierre.getUserId())
            .userName(userName)
            .userIdModificado(cierre.getUserIdModificado())
            .userNameModificado(userNameModificado)
            .gnvM3(cierre.getGnvM3())
            .gnvSoles(cierre.getGnvSoles())
            .gasolinaGalones(cierre.getGasolinaGalones())
            .gasolinaSoles(cierre.getGasolinaSoles())
            .liquidaEfectivo(cierre.getLiquidaEfectivo())
            .liquidaYape(cierre.getLiquidaYape())
            .otrosGastos(cierre.getOtrosGastos())
            .otrosGastosDescripcion(cierre.getOtrosGastosDescripcion())
            .totalIngresos(cierre.getTotalIngresos())
            .totalGastos(cierre.getTotalGastos())
            .resta(cierre.getResta())
            .createdAt(cierre.getCreatedAt())
            .updatedAt(cierre.getUpdatedAt())
            .build();
    }
}

