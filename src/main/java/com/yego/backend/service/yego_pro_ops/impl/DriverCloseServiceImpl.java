package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.request.DriverCloseRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverCloseResponse;
import com.yego.backend.entity.yego_pro_ops.entities.CalculatedShift;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.repository.yego_pro_ops.CalculatedShiftRepository;
import com.yego.backend.repository.yego_pro_ops.DriverCloseRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_pro_ops.CalculatedShiftService;
import com.yego.backend.service.yego_pro_ops.DriverCloseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
@Service
public class DriverCloseServiceImpl implements DriverCloseService {

    private final DriverCloseRepository driverCloseRepository;
    private final UserRepository userRepository;
    private final CalculatedShiftRepository calculatedShiftRepository;
    private final CalculatedShiftService calculatedShiftService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DriverCloseServiceImpl(
            DriverCloseRepository driverCloseRepository,
            UserRepository userRepository,
            CalculatedShiftRepository calculatedShiftRepository,
            @Lazy CalculatedShiftService calculatedShiftService) {
        this.driverCloseRepository = driverCloseRepository;
        this.userRepository = userRepository;
        this.calculatedShiftRepository = calculatedShiftRepository;
        this.calculatedShiftService = calculatedShiftService;
    }
    
    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    @Override
    @Transactional
    public DriverClose registrarCierre(DriverCloseRequest request) {
        Long userId = request.getUserId();
        log.info("💰 [DriverCloseService] Registrando cierre para driver_id: {}, fecha: {}, registrado por user_id: {}", 
            request.getDriverId(), request.getFecha(), userId);

        LocalDate fecha = LocalDate.parse(request.getFecha(), DATE_FORMATTER);

        driverCloseRepository.findFirstByDriverIdAndFechaOrderByIdDesc(request.getDriverId(), fecha)
            .ifPresent(existingClose -> {
                log.warn("⚠️ [DriverCloseService] Ya existe un cierre para driver_id: {} y fecha: {}. Se actualizará el registro existente.", 
                    request.getDriverId(), request.getFecha());
                driverCloseRepository.delete(existingClose);
            });

        List<CalculatedShift> turnosExistentes = calculatedShiftRepository.findByDriverIdAndFecha(
            request.getDriverId(), fecha);
        
        if (turnosExistentes.isEmpty()) {
            log.info("🔄 [DriverCloseService] No se encontraron turnos calculados para driver_id: {} y fecha: {}. Calculando turnos automáticamente...", 
                request.getDriverId(), fecha);
            try {
                String fechaStr = fecha.format(DATE_FORMATTER);
                turnosExistentes = calculatedShiftService.obtenerOCalcularTurnos(request.getDriverId(), fechaStr);
                log.info("✅ [DriverCloseService] Turnos calculados exitosamente para driver_id: {} y fecha: {} ({} turnos encontrados)", 
                    request.getDriverId(), fecha, turnosExistentes.size());
                
                if (turnosExistentes.isEmpty()) {
                    log.warn("⚠️ [DriverCloseService] No se encontraron turnos después de calcular. El conductor puede no tener viajes para esta fecha.");
                }
            } catch (Exception e) {
                log.error("❌ [DriverCloseService] Error calculando turnos para driver_id: {} y fecha: {}: {}",
                    request.getDriverId(), fecha, e.getMessage(), e);
            }
        }

        List<Long> turnoIds = request.getTurnoIds() != null && !request.getTurnoIds().isEmpty()
            ? request.getTurnoIds()
            : turnosExistentes.stream().map(CalculatedShift::getId).collect(Collectors.toList());
        
        String calculatedShiftIds = turnoIds.isEmpty()
            ? null
            : turnoIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        
        if (!turnoIds.isEmpty()) {
            marcarTurnosComoPagados(turnoIds);
        }
        
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
            .odometroInicial(request.getOdometroInicial())
            .odometroFinal(request.getOdometroFinal())
            .diferenciaOdometro(request.getDiferenciaOdometro())
            .calculatedShiftIds(calculatedShiftIds)
            .build();

        DriverClose saved = driverCloseRepository.save(driverClose);
        log.info("✅ [DriverCloseService] Cierre registrado exitosamente con ID: {}, creado por user_id: {}, turnos marcados como pagados: {}",
            saved.getId(), saved.getUserId(), turnoIds.size());
        calculatedShiftService.invalidarCacheDetalle(request.getFecha());
        return saved;
    }

    @Override
    public Optional<DriverCloseResponse> obtenerCierrePorDriverIdYFecha(String driverId, String fecha) {
        if (driverId == null || driverId.trim().isEmpty() || fecha == null || fecha.trim().isEmpty()) {
            return Optional.empty();
        }
        
        try {
            LocalDate fechaLocalDate = LocalDate.parse(fecha.trim(), DATE_FORMATTER);
            return driverCloseRepository.findFirstByDriverIdAndFechaOrderByIdDesc(driverId.trim(), fechaLocalDate)
                    .map(this::convertirAResponse);
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
        cierreExistente.setOdometroInicial(request.getOdometroInicial());
        cierreExistente.setOdometroFinal(request.getOdometroFinal());
        cierreExistente.setDiferenciaOdometro(request.getDiferenciaOdometro());
        
        List<Long> turnoIds = request.getTurnoIds() != null && !request.getTurnoIds().isEmpty()
            ? request.getTurnoIds()
            : obtenerCalculatedShiftIdsComoLista(request.getDriverId(), LocalDate.parse(request.getFecha(), DATE_FORMATTER));
        String calculatedShiftIds = turnoIds.isEmpty()
            ? null
            : turnoIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        cierreExistente.setCalculatedShiftIds(calculatedShiftIds);
        if (!turnoIds.isEmpty()) {
            marcarTurnosComoPagados(turnoIds);
        }
        if (request.getUserId() != null) {
            cierreExistente.setUserIdModificado(request.getUserId());
        }

        DriverClose actualizado = driverCloseRepository.save(cierreExistente);
        log.info("✅ [DriverCloseService] Cierre actualizado exitosamente con ID: {}, modificado por user_id: {}",
            actualizado.getId(), actualizado.getUserIdModificado());
        calculatedShiftService.invalidarCacheDetalle(request.getFecha());
        return actualizado;
    }

    private DriverCloseResponse convertirAResponse(DriverClose cierre) {
        List<Long> userIds = new ArrayList<>();
        userIds.add(cierre.getUserId());
        if (cierre.getUserIdModificado() != null) userIds.add(cierre.getUserIdModificado());
        Map<Long, String> nameByUserId = userRepository.findByIdInWithRole(userIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> ((u.getName() != null ? u.getName() : "") + " " + (u.getLastName() != null ? u.getLastName() : "")).trim(), (a, b) -> a));
        String userName = nameByUserId.getOrDefault(cierre.getUserId(), "Usuario desconocido").trim();
        if (userName.isEmpty()) userName = "Usuario desconocido";
        String userNameModificado = cierre.getUserIdModificado() != null ? nameByUserId.getOrDefault(cierre.getUserIdModificado(), "Usuario desconocido").trim() : null;
        if (userNameModificado != null && userNameModificado.isEmpty()) userNameModificado = "Usuario desconocido";

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
            .odometroInicial(cierre.getOdometroInicial())
            .odometroFinal(cierre.getOdometroFinal())
            .diferenciaOdometro(cierre.getDiferenciaOdometro())
            .tiposTurno(obtenerTiposTurnoDesdeIds(cierre.getCalculatedShiftIds()))
            .createdAt(cierre.getCreatedAt())
            .updatedAt(cierre.getUpdatedAt())
            .build();
    }

    private List<Long> obtenerCalculatedShiftIdsComoLista(String driverId, LocalDate fecha) {
        try {
            List<CalculatedShift> shifts = calculatedShiftRepository.findByDriverIdAndFecha(driverId, fecha);
            if (shifts.isEmpty()) return new ArrayList<>();
            return shifts.stream().map(CalculatedShift::getId).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ [DriverCloseService] Error obteniendo IDs para driver_id: {} y fecha: {}", driverId, fecha, e);
            return new ArrayList<>();
        }
    }

    private void marcarTurnosComoPagados(List<Long> turnoIds) {
        if (turnoIds == null || turnoIds.isEmpty()) return;
        calculatedShiftRepository.markAsPaidByIds(turnoIds);
    }

    private List<String> obtenerTiposTurnoDesdeIds(String calculatedShiftIds) {
        if (calculatedShiftIds == null || calculatedShiftIds.trim().isEmpty()) return new ArrayList<>();
        try {
            List<Long> ids = Arrays.stream(calculatedShiftIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            if (ids.isEmpty()) return new ArrayList<>();
            return calculatedShiftRepository.findTipoTurnoByIdIn(ids).stream()
                    .map(Enum::name)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ [DriverCloseService] Error obteniendo tipos de turno: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}

