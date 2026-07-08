package com.yego.backend.service.yego_pro_ops.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_pro_ops.api.request.CloseShiftMobileRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.OpenShiftMobileRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.MobileShiftResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.MobileShiftSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OrderInfoResponse;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import com.yego.backend.entity.yego_pro_ops.entities.Trip;
import com.yego.backend.repository.yego_pro_ops.DriverCloseRepository;
import com.yego.backend.repository.yego_pro_ops.ShiftSessionRepository;
import com.yego.backend.repository.yego_pro_ops.TripRepository;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import com.yego.backend.service.yego_pro_ops.LiquidacionService;
import com.yego.backend.service.yego_pro_ops.MobileShiftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MobileShiftServiceImpl implements MobileShiftService {

    private static final DateTimeFormatter YANGO_RANGE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter YANGO_RESPONSE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ShiftSessionRepository shiftSessionRepository;
    private final TripRepository tripRepository;
    private final DriverCloseRepository driverCloseRepository;
    private final DriverOrdersService driverOrdersService;
    private final LiquidacionService liquidacionService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public MobileShiftResponse openShift(OpenShiftMobileRequest request) {
        shiftSessionRepository.findByDriverIdAndStatusAndDeletedFalse(request.getDriverId(), "active")
                .ifPresent(active -> {
                    throw new IllegalStateException("El conductor ya tiene un turno activo");
                });

        ShiftSession session = ShiftSession.builder()
                .driverId(request.getDriverId())
                .vehicleId(request.getVehicleId())
                .placa(request.getPlaca())
                .modelo(request.getModelo())
                .kmInicial(request.getKmInicial())
                .selfieUri(request.getSelfieUri())
                .carPhotos(writeList(request.getCarPhotos()))
                .observaciones(request.getObservaciones())
                .startedAt(LocalDateTime.now())
                .status("active")
                .totalTrips(0)
                .totalAmount(BigDecimal.ZERO)
                .totalCash(BigDecimal.ZERO)
                .build();

        return toResponse(shiftSessionRepository.save(session), null);
    }

    @Override
    @Transactional
    public MobileShiftResponse closeShift(UUID sessionId, CloseShiftMobileRequest request) {
        ShiftSession session = shiftSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado"));

        if (Boolean.TRUE.equals(session.getDeleted())) {
            throw new IllegalStateException("El turno fue cancelado");
        }
        if (!"active".equals(session.getStatus())) {
            throw new IllegalStateException("Solo se puede cerrar un turno activo");
        }
        if (request.getKmFinal() != null && session.getKmInicial() != null
                && request.getKmFinal() < session.getKmInicial()) {
            throw new IllegalArgumentException("El kilometraje final no puede ser menor al inicial");
        }

        LocalDateTime closedAt = LocalDateTime.now();
        List<OrderInfoResponse> orders = fetchOrders(session.getDriverId(), session.getStartedAt(), closedAt);
        replaceTrips(session, orders);

        BigDecimal totalAmount = orders.stream()
                .map(order -> bd(order.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCash = orders.stream()
                .map(order -> bd(order.getCash()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        session.setClosedAt(closedAt);
        session.setStatus("closed");
        session.setKmFinal(request.getKmFinal());
        session.setCarPhotosCierre(writeList(request.getCarPhotosCierre()));
        session.setObservaciones(request.getObservaciones());
        session.setMantenimientoRequerido(Boolean.TRUE.equals(request.getMantenimientoRequerido()));
        session.setMantenimientoDescripcion(request.getMantenimientoDescripcion());
        session.setTotalTrips(orders.size());
        session.setTotalAmount(totalAmount);
        session.setTotalCash(totalCash);
        session = shiftSessionRepository.save(session);

        BigDecimal producido = liquidacionService.calcularProducidoYango(
                session.getDriverId(), session.getStartedAt(), session.getClosedAt());
        upsertDriverClose(session, request, producido);

        log.info("[MobileShift] turno cerrado sessionId={} driverId={} viajes={} producido={}",
                session.getId(), session.getDriverId(), orders.size(), producido);

        return toResponse(session, producedOrTotal(producido, totalAmount));
    }

    @Override
    @Transactional
    public void cancelShift(UUID sessionId, Long userId, String reason) {
        ShiftSession session = shiftSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado"));
        if (!"active".equals(session.getStatus())) {
            throw new IllegalStateException("Solo se puede cancelar un turno activo");
        }
        session.setDeleted(true);
        session.setDeletedBy(userId);
        session.setDeletedAt(LocalDateTime.now());
        session.setDeleteReason(reason);
        shiftSessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public MobileShiftResponse getActiveShift(String driverId) {
        return shiftSessionRepository.findByDriverIdAndStatusAndDeletedFalse(driverId, "active")
                .map(session -> toResponse(session, null))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MobileShiftResponse> getDriverHistory(String driverId) {
        return shiftSessionRepository.findByDriverIdOrderByStartedAtDesc(driverId).stream()
                .filter(session -> !"active".equals(session.getStatus()))
                .map(session -> toResponse(session, null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MobileShiftSummaryResponse getSummary(UUID sessionId) {
        ShiftSession session = shiftSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado"));
        BigDecimal producido = session.getClosedAt() != null
                ? liquidacionService.calcularProducidoYango(session.getDriverId(), session.getStartedAt(), session.getClosedAt())
                : BigDecimal.ZERO;
        return MobileShiftSummaryResponse.builder()
                .viajes(session.getTotalTrips())
                .producido(producedOrTotal(producido, session.getTotalAmount()))
                .efectivo(session.getTotalCash())
                .yape(BigDecimal.ZERO)
                .duracion(formatDuration(session.getStartedAt(), session.getClosedAt()))
                .kmInicial(session.getKmInicial())
                .build();
    }

    private List<OrderInfoResponse> fetchOrders(String driverId, LocalDateTime startedAt, LocalDateTime closedAt) {
        String dateFrom = startedAt.format(YANGO_RANGE_FORMATTER) + "-05:00";
        String dateTo = closedAt.format(YANGO_RANGE_FORMATTER) + "-05:00";
        DriverOrdersResponse response = driverOrdersService.obtenerViajesCompletos(driverId, dateFrom, dateTo);
        return response != null && response.getOrders() != null ? response.getOrders() : List.of();
    }

    private void replaceTrips(ShiftSession session, List<OrderInfoResponse> orders) {
        List<Trip> existing = tripRepository.findByShiftSessionId(session.getId());
        if (!existing.isEmpty()) {
            tripRepository.deleteAll(existing);
        }
        for (OrderInfoResponse order : orders) {
            tripRepository.save(Trip.builder()
                    .driverId(session.getDriverId())
                    .shiftSessionId(session.getId())
                    .externalTripId(order.getId())
                    .completedAt(parseYangoDate(order.getEndedAt()))
                    .amount(bd(order.getPrice()))
                    .distanceKm(bd(order.getDistance()))
                    .build());
        }
    }

    private void upsertDriverClose(ShiftSession session, CloseShiftMobileRequest request, BigDecimal producido) {
        Optional<DriverClose> existing = driverCloseRepository.findFirstByShiftSessionIdOrderByIdDesc(session.getId());
        DriverClose close = existing.orElseGet(DriverClose::new);

        BigDecimal gasolina = bd(request.getGasolinaMonto());
        BigDecimal gnv = bd(request.getGnvMonto());
        BigDecimal otros = bd(request.getOtrosGastos());
        BigDecimal efectivo = bd(request.getEfectivo());
        BigDecimal yape = bd(request.getYape());
        BigDecimal totalIngresos = efectivo.add(yape);
        BigDecimal totalGastos = gasolina.add(gnv).add(otros);

        close.setDriverId(session.getDriverId());
        close.setFecha(session.getStartedAt().toLocalDate());
        close.setUserId(request.getUserId() != null ? request.getUserId() : 0L);
        close.setShiftSessionId(session.getId());
        close.setPlaca(session.getPlaca());
        close.setOdometroInicial(session.getKmInicial());
        close.setOdometroFinal(session.getKmFinal());
        close.setDiferenciaOdometro(kmRecorridos(session));
        close.setGnvM3(request.getGnvM3() != null ? String.valueOf(request.getGnvM3()) : null);
        close.setGnvSoles(gnv);
        close.setGasolinaGalones(request.getGasolinaGalones() != null ? String.valueOf(request.getGasolinaGalones()) : null);
        close.setGasolinaSoles(gasolina);
        close.setLiquidaEfectivo(efectivo);
        close.setLiquidaYape(yape);
        close.setOperacionYape(request.getNumeroOperacion());
        close.setOtrosGastos(otros);
        close.setOtrosGastosDescripcion(request.getOtrosGastosDescripcion());
        close.setTotalIngresos(totalIngresos);
        close.setTotalGastos(totalGastos);
        close.setResta(totalIngresos.subtract(totalGastos));
        close.setMontoTotalProducido(producedOrTotal(producido, session.getTotalAmount()));
        driverCloseRepository.save(close);
    }

    private MobileShiftResponse toResponse(ShiftSession session, BigDecimal producedOverride) {
        DriverClose close = driverCloseRepository.findFirstByShiftSessionIdOrderByIdDesc(session.getId()).orElse(null);
        BigDecimal efectivo = close != null ? nz(close.getLiquidaEfectivo()) : BigDecimal.ZERO;
        BigDecimal yape = close != null ? nz(close.getLiquidaYape()) : BigDecimal.ZERO;
        BigDecimal gasolina = close != null ? nz(close.getGasolinaSoles()) : BigDecimal.ZERO;
        BigDecimal gnv = close != null ? nz(close.getGnvSoles()) : BigDecimal.ZERO;
        BigDecimal otros = close != null ? nz(close.getOtrosGastos()) : BigDecimal.ZERO;
        BigDecimal totalGastos = gasolina.add(gnv).add(otros);
        BigDecimal totalIngresos = efectivo.add(yape);
        BigDecimal producido = producedOverride != null
                ? producedOverride
                : close != null ? producedOrTotal(close.getMontoTotalProducido(), session.getTotalAmount()) : nz(session.getTotalAmount());

        return MobileShiftResponse.builder()
                .sessionId(session.getId())
                .driverId(session.getDriverId())
                .vehicleId(session.getVehicleId())
                .placa(session.getPlaca())
                .modelo(session.getModelo())
                .startedAt(session.getStartedAt())
                .closedAt(session.getClosedAt())
                .duracion(formatDuration(session.getStartedAt(), session.getClosedAt()))
                .kmInicial(session.getKmInicial())
                .kmFinal(session.getKmFinal())
                .kmRecorridos(kmRecorridos(session))
                .status(session.getStatus())
                .totalViajes(session.getTotalTrips())
                .producido(producido)
                .efectivoYango(nz(session.getTotalCash()))
                .yapeYango(BigDecimal.ZERO)
                .efectivo(efectivo)
                .yape(yape)
                .numeroOperacion(close != null ? close.getOperacionYape() : null)
                .gasolinaMonto(gasolina)
                .gasolinaGalones(close != null ? parseDecimal(close.getGasolinaGalones()) : BigDecimal.ZERO)
                .gnvMonto(gnv)
                .gnvM3(close != null ? parseDecimal(close.getGnvM3()) : BigDecimal.ZERO)
                .otrosGastos(otros)
                .totalGastos(totalGastos)
                .totalIngresos(totalIngresos)
                .balance(totalIngresos.subtract(totalGastos))
                .carPhotos(readList(session.getCarPhotos()))
                .selfieUri(session.getSelfieUri())
                .carPhotosCierre(readList(session.getCarPhotosCierre()))
                .fotosEvidencia(new ArrayList<>())
                .observaciones(session.getObservaciones())
                .mantenimientoRequerido(Boolean.TRUE.equals(session.getMantenimientoRequerido()))
                .mantenimientoDescripcion(session.getMantenimientoDescripcion())
                .build();
    }

    private String writeList(List<String> values) {
        try {
            return values == null ? null : objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> readList(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private LocalDateTime parseYangoDate(String value) {
        if (value == null || value.isBlank()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(value, YANGO_RESPONSE_FORMATTER);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String formatDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null) return "0m";
        LocalDateTime finish = end != null ? end : LocalDateTime.now();
        Duration duration = Duration.between(start, finish);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return hours > 0 ? hours + "h " + minutes + "m" : Math.max(0, duration.toMinutes()) + "m";
    }

    private Integer kmRecorridos(ShiftSession session) {
        if (session.getKmInicial() == null || session.getKmFinal() == null) return 0;
        return Math.max(0, session.getKmFinal() - session.getKmInicial());
    }

    private BigDecimal bd(Double value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal producedOrTotal(BigDecimal produced, BigDecimal total) {
        BigDecimal p = nz(produced);
        return p.compareTo(BigDecimal.ZERO) > 0 ? p : nz(total);
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
