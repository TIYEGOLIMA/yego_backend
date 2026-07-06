package com.yego.backend.service.yego_pro_ops.mobile;

import com.yego.backend.entity.yego_pro_ops.api.request.mobile.OpenShiftMobileRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.mobile.CloseShiftMobileRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileShiftResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileShiftSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.entity.yego_pro_ops.entities.FleetVehicle;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OrderInfoResponse;
import com.yego.backend.repository.yego_pro_ops.DriverCloseRepository;
import com.yego.backend.repository.yego_pro_ops.FleetVehicleRepository;
import com.yego.backend.repository.yego_pro_ops.ShiftSessionRepository;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de turnos para la app móvil.
 * Maneja apertura, cierre, resumen Yango e historial de turnos del conductor.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MobileShiftService {

    private final ShiftSessionRepository shiftRepo;
    private final DriverCloseRepository closeRepo;
    private final FleetVehicleRepository vehicleRepo;
    private final DriverOrdersService driverOrdersService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─── Abrir Turno ─────────────────────────────────────────────

    public MobileShiftResponse openShift(OpenShiftMobileRequest req) {
        assertNoActiveShift(req.getDriverId());

        FleetVehicle vehicle = vehicleRepo.findById(req.getVehicleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));

        LocalDateTime startedAt = now();

        ShiftSession session = ShiftSession.builder()
                .driverId(req.getDriverId())
                .startedAt(startedAt)
                .status("active")
                .build();
        session = shiftRepo.save(session);

        DriverClose close = DriverClose.builder()
                .driverId(req.getDriverId())
                .userId(0L)
                .shiftSessionId(session.getId())
                .placa(req.getPlaca())
                .odometroInicial(req.getKmInicial())
                .fecha(LocalDate.now())
                .carPhotos(toJson(req.getCarPhotos()))
                .selfieUri(req.getSelfieUri())
                .observacionesApertura(req.getObservaciones())
                .saldoAnterior(req.getSaldoAnterior())
                .saldoDescripcion(req.getSaldoDescripcion())
                .build();
        closeRepo.save(close);

        log.info("Turno abierto: session={}, driver={}, placa={}", session.getId(), req.getDriverId(), req.getPlaca());

        return buildResponse(session, close, vehicle, null);
    }

    // ─── Resumen Yango (ANTES de cerrar) ─────────────────────────

    @Transactional(readOnly = true)
    public MobileShiftSummaryResponse getSummary(String sessionId) {
        ShiftSession session = findSession(sessionId);

        YangoTripResult trips = fetchYangoTrips(session);
        DriverClose close = findClose(sessionId);

        return MobileShiftSummaryResponse.builder()
                .viajes(trips.viajes)
                .producido(trips.producido)
                .efectivo(trips.efectivo)
                .yape(trips.yape)
                .duracion(formatDuration(session.getStartedAt(), now()))
                .kmInicial(close.getOdometroInicial())
                .build();
    }

    // ─── Cerrar Turno ────────────────────────────────────────────

    public MobileShiftResponse closeShift(String sessionId, CloseShiftMobileRequest req) {
        ShiftSession session = findSession(sessionId);
        validateActive(session);

        LocalDateTime closedAt = now();

        // 1. Consultar Yango
        YangoTripResult trips = fetchYangoTrips(session);

        // 2. Actualizar sesión
        session.setClosedAt(closedAt);
        session.setStatus("por_validar");
        session.setTotalTrips(trips.viajes);
        session.setTotalAmount(trips.producido);
        session.setTotalCash(trips.efectivo);
        shiftRepo.save(session);

        // 3. Actualizar cierre financiero
        DriverClose close = findClose(sessionId);
        close.setOdometroFinal(req.getKmFinal());
        close.setDiferenciaOdometro(req.getKmFinal() - close.getOdometroInicial());
        close.setLiquidaEfectivo(req.getEfectivo());
        close.setLiquidaYape(req.getYape());
        close.setOperacionYape(req.getNumeroOperacion());
        close.setGasolinaSoles(req.getGasolinaMonto());
        close.setGasolinaGalones(req.getGasolinaGalones() != null
                ? req.getGasolinaGalones().toString() : null);
        close.setGnvSoles(req.getGnvMonto());
        close.setGnvM3(req.getGnvM3() != null ? req.getGnvM3().toString() : null);
        close.setOtrosGastos(req.getOtrosGastos());
        close.setOtrosGastosDescripcion(req.getOtrosGastosDescripcion());
        close.setMontoTotalProducido(trips.producido);
        close.setCarPhotosCierre(toJson(req.getCarPhotosCierre()));
        close.setObservacionesCierre(req.getObservaciones());
        close.setMantenimientoRequerido(req.getMantenimientoRequerido());
        close.setMantenimientoDescripcion(req.getMantenimientoDescripcion());
        closeRepo.save(close);

        log.info("Turno cerrado: session={}, driver={}, kmRecorridos={}",
                sessionId, session.getDriverId(), req.getKmFinal() - close.getOdometroInicial());

        return buildResponse(session, close, null, trips);
    }

    // ─── Consultas ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<MobileShiftResponse> findActiveByDriver(String driverId) {
        return shiftRepo.findByDriverIdAndStatusAndDeletedFalse(driverId, "active")
                .map(session -> {
                    DriverClose close = findCloseBySessionId(session.getId());
                    return buildResponse(session, close, null, null);
                });
    }

    @Transactional(readOnly = true)
    public List<MobileShiftResponse> getDriverHistory(String driverId) {
        return shiftRepo.findByDriverIdOrderByStartedAtDesc(driverId)
                .stream()
                .filter(s -> !"active".equals(s.getStatus()))
                .map(session -> {
                    DriverClose close = findCloseBySessionId(session.getId());
                    return buildResponse(session, close, null, null);
                })
                .collect(Collectors.toList());
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private void assertNoActiveShift(String driverId) {
        shiftRepo.findByDriverIdAndStatusAndDeletedFalse(driverId, "active")
                .ifPresent(s -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes un turno activo");
                });
    }

    private ShiftSession findSession(String id) {
        try {
            return shiftRepo.findById(java.util.UUID.fromString(id))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado");
        }
    }

    private DriverClose findCloseBySessionId(UUID sessionId) {
        return closeRepo.findFirstByShiftSessionIdOrderByIdDesc(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cierre no encontrado"));
    }

    private DriverClose findClose(String sessionId) {
        UUID uuid;
        try {
            uuid = java.util.UUID.fromString(sessionId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado");
        }
        return findCloseBySessionId(uuid);
    }

    private void validateActive(ShiftSession session) {
        if (!"active".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El turno ya fue cerrado");
        }
    }

    private YangoTripResult fetchYangoTrips(ShiftSession session) {
        try {
            LocalDateTime from = session.getStartedAt();
            LocalDateTime to = now();

            String dateFrom = from.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String dateTo = to.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            DriverOrdersResponse response = driverOrdersService.obtenerViajesCompletos(
                    session.getDriverId(), dateFrom, dateTo);

            if (response == null || response.getOrders() == null || response.getOrders().isEmpty()) {
                return new YangoTripResult(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            }

            int viajes = response.getOrders().size();
            BigDecimal producido = BigDecimal.ZERO;
            BigDecimal efectivo = BigDecimal.ZERO;
            BigDecimal yape = BigDecimal.ZERO;

            for (OrderInfoResponse order : response.getOrders()) {
                Double price = order.getPrice() != null ? order.getPrice() : 0.0;
                Double cash  = order.getCash()  != null ? order.getCash()  : 0.0;

                BigDecimal total = BigDecimal.valueOf(price);
                producido = producido.add(total);
                efectivo = efectivo.add(BigDecimal.valueOf(cash));
                // El resto (card, bonus, etc.) no es cash → va como yape para resumen
                yape = yape.add(total.subtract(BigDecimal.valueOf(cash)));
            }

            log.info("Yango trips: driver={}, viajes={}, producido={}, efectivo={}, yape={}",
                    session.getDriverId(), viajes, producido, efectivo, yape);

            return new YangoTripResult(viajes, producido, efectivo, yape);

        } catch (Exception e) {
            log.warn("No se pudieron obtener viajes de Yango: {}", e.getMessage());
            return new YangoTripResult(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    private MobileShiftResponse buildResponse(
            ShiftSession session, DriverClose close,
            FleetVehicle vehicle, YangoTripResult trips
    ) {
        Integer kmRecorridos = close != null && close.getOdometroFinal() != null
                ? close.getOdometroFinal() - close.getOdometroInicial() : null;

        BigDecimal totalGastos = BigDecimal.ZERO
                .add(nvl(close.getGasolinaSoles()))
                .add(nvl(close.getGnvSoles()))
                .add(nvl(close.getOtrosGastos()));

        BigDecimal totalIngresos = nvl(close.getLiquidaEfectivo())
                .add(nvl(close.getLiquidaYape()));

        BigDecimal balance = totalIngresos.subtract(totalGastos);

        return MobileShiftResponse.builder()
                .sessionId(session.getId().toString())
                .driverId(session.getDriverId())
                .placa(close != null ? close.getPlaca() : null)
                .marca(vehicle != null ? vehicle.getBrand() : null)
                .modelo(vehicle != null ? vehicle.getModel() : null)
                .startedAt(toInstant(session.getStartedAt()))
                .closedAt(toInstant(session.getClosedAt()))
                .duracion(session.getClosedAt() != null
                        ? formatDuration(session.getStartedAt(), session.getClosedAt()) : null)
                .kmInicial(close != null ? close.getOdometroInicial() : null)
                .kmFinal(close != null ? close.getOdometroFinal() : null)
                .kmRecorridos(kmRecorridos)
                .status(session.getStatus())
                .totalViajes(trips != null ? trips.viajes : session.getTotalTrips())
                .producido(trips != null ? trips.producido : session.getTotalAmount())
                .efectivoYango(trips != null ? trips.efectivo : session.getTotalCash())
                .yapeYango(trips != null ? trips.yape : BigDecimal.ZERO)
                .efectivo(close != null ? close.getLiquidaEfectivo() : null)
                .yape(close != null ? close.getLiquidaYape() : null)
                .numeroOperacion(close != null ? close.getOperacionYape() : null)
                .gasolinaMonto(close != null ? close.getGasolinaSoles() : null)
                .gasolinaGalones(close != null && close.getGasolinaGalones() != null
                        ? new BigDecimal(close.getGasolinaGalones()) : null)
                .gnvMonto(close != null ? close.getGnvSoles() : null)
                .gnvM3(close != null && close.getGnvM3() != null
                        ? new BigDecimal(close.getGnvM3()) : null)
                .otrosGastos(close != null ? close.getOtrosGastos() : null)
                .totalGastos(totalGastos)
                .totalIngresos(totalIngresos)
                .balance(balance)
                .carPhotos(close != null ? fromJson(close.getCarPhotos()) : Collections.emptyList())
                .selfieUri(close != null ? close.getSelfieUri() : null)
                .carPhotosCierre(close != null ? fromJson(close.getCarPhotosCierre()) : Collections.emptyList())
                .fotosEvidencia(close != null ? fromJson(close.getFotosEvidencia()) : Collections.emptyList())
                .observaciones(close != null ? close.getObservacionesCierre() : null)
                .mantenimientoRequerido(close != null && close.getMantenimientoRequerido() != null
                        ? close.getMantenimientoRequerido() : false)
                .mantenimientoDescripcion(close != null ? close.getMantenimientoDescripcion() : null)
                .build();
    }

    // ─── Utilidades ──────────────────────────────────────────────

    private LocalDateTime now() { return LocalDateTime.now(); }

    private BigDecimal nvl(BigDecimal value) { return value != null ? value : BigDecimal.ZERO; }

    private Instant toInstant(LocalDateTime ldt) {
        return ldt != null ? ldt.atZone(ZoneId.systemDefault()).toInstant() : null;
    }

    private String formatDuration(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) return null;
        Duration d = Duration.between(from, to);
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        return String.format("%dh %dm", hours, minutes);
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("Error serializando JSON: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            log.warn("Error deserializando JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ─── Inner class ─────────────────────────────────────────────

    private static class YangoTripResult {
        final int viajes;
        final BigDecimal producido;
        final BigDecimal efectivo;
        final BigDecimal yape;

        YangoTripResult(int viajes, BigDecimal producido, BigDecimal efectivo, BigDecimal yape) {
            this.viajes = viajes;
            this.producido = producido;
            this.efectivo = efectivo;
            this.yape = yape;
        }
    }
}
