package com.yego.backend.service.yego_pro_ops.mobile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileShiftResponse;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.entity.yego_pro_ops.entities.FleetVehicle;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MobileShiftResponseMapper {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public MobileShiftResponse toResponse(ShiftSession session, DriverClose close, FleetVehicle vehicle) {
        Integer kmInicial = close != null ? close.getOdometroInicial() : session.getKmInicial();
        Integer kmFinal = close != null ? close.getOdometroFinal() : session.getKmFinal();
        Integer kmRecorridos = kmInicial != null && kmFinal != null ? kmFinal - kmInicial : null;
        BigDecimal totalGastos = sumExpenses(close);
        BigDecimal totalIngresos = sumIncomes(close);

        return MobileShiftResponse.builder()
                .sessionId(session.getId().toString())
                .driverId(session.getDriverId())
                .vehicleId(session.getVehicleId())
                .placa(firstNotBlank(close != null ? close.getPlaca() : null, session.getPlaca()))
                .marca(vehicle != null ? vehicle.getBrand() : null)
                .modelo(firstNotBlank(vehicle != null ? vehicle.getModel() : null, session.getModelo()))
                .startedAt(toInstant(session.getStartedAt()))
                .closedAt(toInstant(session.getClosedAt()))
                .duracion(formatDuration(session.getStartedAt(), session.getClosedAt()))
                .kmInicial(kmInicial)
                .kmFinal(kmFinal)
                .kmRecorridos(kmRecorridos)
                .status(session.getStatus())
                .totalViajes(session.getTotalTrips())
                .producido(session.getTotalAmount())
                .efectivoYango(session.getTotalCash())
                .yapeYango(session.getTotalYape())
                .efectivo(close != null ? close.getLiquidaEfectivo() : null)
                .yape(close != null ? close.getLiquidaYape() : null)
                .numeroOperacion(close != null ? close.getOperacionYape() : null)
                .gasolinaMonto(close != null ? close.getGasolinaSoles() : null)
                .gasolinaGalones(close != null ? decimalOrNull(close.getGasolinaGalones()) : null)
                .gnvMonto(close != null ? close.getGnvSoles() : null)
                .gnvM3(close != null ? decimalOrNull(close.getGnvM3()) : null)
                .otrosGastos(close != null ? close.getOtrosGastos() : null)
                .totalGastos(totalGastos)
                .totalIngresos(totalIngresos)
                .balance(totalIngresos.subtract(totalGastos))
                .carPhotos(readImages(close != null ? close.getCarPhotos() : session.getCarPhotos()))
                .selfieUri(firstNotBlank(close != null ? close.getSelfieUri() : null, session.getSelfieUri()))
                .carPhotosCierre(readImages(close != null ? close.getCarPhotosCierre() : session.getCarPhotosCierre()))
                .fotosEvidencia(readImages(close != null ? close.getFotosEvidencia() : null))
                .observaciones(firstNotBlank(close != null ? close.getObservacionesCierre() : null, session.getObservaciones()))
                .mantenimientoRequerido(close != null && close.getMantenimientoRequerido() != null
                        ? close.getMantenimientoRequerido()
                        : Boolean.TRUE.equals(session.getMantenimientoRequerido()))
                .mantenimientoDescripcion(firstNotBlank(
                        close != null ? close.getMantenimientoDescripcion() : null,
                        session.getMantenimientoDescripcion()
                ))
                .build();
    }

    private BigDecimal sumExpenses(DriverClose close) {
        if (close == null) return BigDecimal.ZERO;
        return nvl(close.getGasolinaSoles())
                .add(nvl(close.getGnvSoles()))
                .add(nvl(close.getOtrosGastos()));
    }

    private BigDecimal sumIncomes(DriverClose close) {
        if (close == null) return BigDecimal.ZERO;
        return nvl(close.getLiquidaEfectivo()).add(nvl(close.getLiquidaYape()));
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal decimalOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Valor decimal inválido en cierre: {}", value);
            return null;
        }
    }

    private List<String> readImages(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            log.warn("No se pudo leer la lista de imágenes del turno: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    private Instant toInstant(java.time.LocalDateTime value) {
        return value != null ? value.atZone(ZoneId.systemDefault()).toInstant() : null;
    }

    private String formatDuration(java.time.LocalDateTime from, java.time.LocalDateTime to) {
        if (from == null || to == null) return null;
        Duration duration = Duration.between(from, to);
        return String.format("%dh %dm", duration.toHours(), duration.toMinutesPart());
    }
}
