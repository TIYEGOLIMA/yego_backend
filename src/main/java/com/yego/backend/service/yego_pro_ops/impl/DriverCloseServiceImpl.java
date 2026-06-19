package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.request.DriverCloseRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverCloseResponse;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import com.yego.backend.repository.yego_pro_ops.DriverCloseRepository;
import com.yego.backend.repository.yego_pro_ops.ShiftSessionRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_pro_ops.DriverCloseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DriverCloseServiceImpl implements DriverCloseService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DriverCloseRepository driverCloseRepository;
    private final UserRepository userRepository;
    private final ShiftSessionRepository shiftSessionRepository;
    private final TransactionTemplate transactionTemplate;

    public DriverCloseServiceImpl(
            DriverCloseRepository driverCloseRepository,
            UserRepository userRepository,
            ShiftSessionRepository shiftSessionRepository,
            PlatformTransactionManager transactionManager) {
        this.driverCloseRepository = driverCloseRepository;
        this.userRepository = userRepository;
        this.shiftSessionRepository = shiftSessionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public DriverClose registrarCierre(DriverCloseRequest request) {
        Long userId = request.getUserId();
        String driverId = request.getDriverId();
        LocalDate fecha = LocalDate.parse(request.getFecha(), DATE_FORMATTER);
        log.info("[DriverCloseService] registrar cierre driverId={} fecha={} userId={}", driverId, fecha, userId);

        DriverClose guardado = transactionTemplate.execute(status -> {
            Optional<DriverClose> existenteOpt = driverCloseRepository.findFirstByDriverIdAndFechaOrderByIdDesc(driverId, fecha);

            if (existenteOpt.isPresent()) {
                DriverClose existente = existenteOpt.get();
                log.warn("[DriverCloseService] cierre existente actualizado driverId={} fecha={}", driverId, fecha);
                aplicarCamposEditables(existente, request);
                existente.setUserId(userId);
                return driverCloseRepository.save(existente);
            }
            return driverCloseRepository.save(construirCierre(request, fecha, userId));
        });

        log.info("[DriverCloseService] cierre creado id={} userId={}", guardado.getId(), guardado.getUserId());
        return guardado;
    }

    @Override
    public Optional<DriverCloseResponse> obtenerCierrePorDriverIdYFecha(String driverId, String fecha) {
        if (esVacio(driverId) || esVacio(fecha)) return Optional.empty();
        try {
            LocalDate fechaLocal = LocalDate.parse(fecha.trim(), DATE_FORMATTER);
            return driverCloseRepository.findFirstByDriverIdAndFechaOrderByIdDesc(driverId.trim(), fechaLocal)
                .map(this::convertirAResponse);
        } catch (Exception e) {
            log.error("[DriverCloseService] error obteniendo cierre driverId={} fecha={}: {}",
                driverId, fecha, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<DriverCloseResponse> obtenerCierrePorSessionId(UUID sessionId) {
        if (sessionId == null) return Optional.empty();
        return driverCloseRepository.findFirstByShiftSessionIdOrderByIdDesc(sessionId)
                .map(cierre -> {
                    Optional<ShiftSession> sessionOpt = shiftSessionRepository.findById(sessionId);
                    if (sessionOpt.isPresent()) {
                        ShiftSession session = sessionOpt.get();
                        if (session.getTotalCash() != null) {
                            BigDecimal totalGastos = cierre.getTotalGastos() != null ? cierre.getTotalGastos() : BigDecimal.ZERO;
                            cierre.setTotalIngresos(session.getTotalCash());
                            cierre.setResta(session.getTotalCash().subtract(totalGastos));
                        }
                    }
                    return convertirAResponse(cierre);
                });
    }

    @Override
    public DriverClose actualizarCierre(DriverCloseRequest request) {
        Long id = request.getId();
        if (id == null) throw new RuntimeException("El id es requerido para actualizar el cierre");

        log.info("[DriverCloseService] actualizar cierre id={} driverId={} fecha={}",
            id, request.getDriverId(), request.getFecha());

        LocalDate fecha = LocalDate.parse(request.getFecha(), DATE_FORMATTER);

        DriverClose actualizado = transactionTemplate.execute(status -> {
            DriverClose cierre = driverCloseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró cierre con ID: " + id));
            aplicarCamposEditables(cierre, request);
            if (request.getUserId() != null) cierre.setUserIdModificado(request.getUserId());
            return driverCloseRepository.save(cierre);
        });

        log.info("[DriverCloseService] cierre actualizado id={} userIdMod={}",
            actualizado.getId(), actualizado.getUserIdModificado());
        return actualizado;
    }

    private DriverClose construirCierre(DriverCloseRequest req, LocalDate fecha, Long userId) {
        double ingresos = req.getTotalIngresos() != null ? req.getTotalIngresos() : 0;
        double gastos = req.getTotalGastos() != null ? req.getTotalGastos() : 0;
        return DriverClose.builder()
            .driverId(req.getDriverId())
            .fecha(fecha)
            .userId(userId)
            .gnvM3(req.getGnvM3())
            .gnvSoles(toBigDecimal(req.getGnvSoles()))
            .gasolinaGalones(req.getGasolinaGalones())
            .gasolinaSoles(toBigDecimal(req.getGasolinaSoles()))
            .liquidaEfectivo(toBigDecimal(req.getLiquidaEfectivo()))
            .liquidaYape(toBigDecimal(req.getLiquidaYape()))
            .operacionYape(req.getOperacionYape())
            .otrosGastos(toBigDecimal(req.getOtrosGastos()))
            .otrosGastosDescripcion(req.getOtrosGastosDescripcion())
            .totalIngresos(toBigDecimal(ingresos))
            .totalGastos(toBigDecimal(gastos))
            .resta(BigDecimal.valueOf(ingresos - gastos))
            .placa(req.getPlaca())
            .odometroInicial(req.getOdometroInicial())
            .odometroFinal(req.getOdometroFinal())
            .diferenciaOdometro(req.getDiferenciaOdometro())
            .shiftSessionId(req.getShiftSessionId())
            .build();
    }

    private void aplicarCamposEditables(DriverClose cierre, DriverCloseRequest req) {
        if (req.getGnvM3() != null) cierre.setGnvM3(req.getGnvM3());
        if (req.getGnvSoles() != null) cierre.setGnvSoles(toBigDecimal(req.getGnvSoles()));
        if (req.getGasolinaGalones() != null) cierre.setGasolinaGalones(req.getGasolinaGalones());
        if (req.getGasolinaSoles() != null) cierre.setGasolinaSoles(toBigDecimal(req.getGasolinaSoles()));
        if (req.getLiquidaEfectivo() != null) cierre.setLiquidaEfectivo(toBigDecimal(req.getLiquidaEfectivo()));
        if (req.getLiquidaYape() != null) cierre.setLiquidaYape(toBigDecimal(req.getLiquidaYape()));
        if (req.getOperacionYape() != null) cierre.setOperacionYape(req.getOperacionYape());
        if (req.getOtrosGastos() != null) cierre.setOtrosGastos(toBigDecimal(req.getOtrosGastos()));
        if (req.getOtrosGastosDescripcion() != null) cierre.setOtrosGastosDescripcion(req.getOtrosGastosDescripcion());
        if (req.getTotalIngresos() != null && req.getTotalIngresos() > 0) cierre.setTotalIngresos(toBigDecimal(req.getTotalIngresos()));
        if (req.getTotalGastos() != null) cierre.setTotalGastos(toBigDecimal(req.getTotalGastos()));
        if (req.getPlaca() != null) cierre.setPlaca(req.getPlaca());
        if (req.getOdometroInicial() != null) cierre.setOdometroInicial(req.getOdometroInicial());
        if (req.getOdometroFinal() != null) cierre.setOdometroFinal(req.getOdometroFinal());
        if (req.getDiferenciaOdometro() != null) cierre.setDiferenciaOdometro(req.getDiferenciaOdometro());
        if (req.getShiftSessionId() != null) cierre.setShiftSessionId(req.getShiftSessionId());

        double ingresos = cierre.getTotalIngresos() != null ? cierre.getTotalIngresos().doubleValue() : 0;
        double gastos = cierre.getTotalGastos() != null ? cierre.getTotalGastos().doubleValue() : 0;
        cierre.setResta(BigDecimal.valueOf(ingresos - gastos));
    }

    private DriverCloseResponse convertirAResponse(DriverClose cierre) {
        Map<Long, String> nombres = obtenerNombresUsuarios(cierre);
        String userName = nombreUsuario(nombres, cierre.getUserId());
        String userNameModificado = cierre.getUserIdModificado() != null
            ? nombreUsuario(nombres, cierre.getUserIdModificado())
            : null;

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
            .operacionYape(cierre.getOperacionYape())
            .otrosGastos(cierre.getOtrosGastos())
            .otrosGastosDescripcion(cierre.getOtrosGastosDescripcion())
            .totalIngresos(cierre.getTotalIngresos())
            .totalGastos(cierre.getTotalGastos())
            .resta(cierre.getResta())
            .placa(cierre.getPlaca())
            .odometroInicial(cierre.getOdometroInicial())
            .odometroFinal(cierre.getOdometroFinal())
            .diferenciaOdometro(cierre.getDiferenciaOdometro())
            .shiftSessionId(cierre.getShiftSessionId())
            .createdAt(cierre.getCreatedAt())
            .updatedAt(cierre.getUpdatedAt())
            .build();
    }

    private Map<Long, String> obtenerNombresUsuarios(DriverClose cierre) {
        List<Long> userIds = new ArrayList<>();
        if (cierre.getUserId() != null) userIds.add(cierre.getUserId());
        if (cierre.getUserIdModificado() != null) userIds.add(cierre.getUserIdModificado());
        if (userIds.isEmpty()) return Map.of();

        return userRepository.findByIdInWithRole(userIds).stream()
            .collect(Collectors.toMap(
                u -> u.getId(),
                u -> ((u.getName() != null ? u.getName() : "") + " " +
                      (u.getLastName() != null ? u.getLastName() : "")).trim(),
                (a, b) -> a));
    }

    private String nombreUsuario(Map<Long, String> nombres, Long userId) {
        String nombre = nombres.getOrDefault(userId, "").trim();
        return nombre.isEmpty() ? "Usuario desconocido" : nombre;
    }

    private static BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    private static boolean esVacio(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Override
    public List<DriverCloseResponse> obtenerCierresPorRango(String driverId, String desde, String hasta) {
        if (esVacio(driverId) || esVacio(desde) || esVacio(hasta)) return List.of();
        try {
            LocalDate fechaDesde = LocalDate.parse(desde.trim(), DATE_FORMATTER);
            LocalDate fechaHasta = LocalDate.parse(hasta.trim(), DATE_FORMATTER);
            return driverCloseRepository.findByDriverIdAndFechaBetween(driverId.trim(), fechaDesde, fechaHasta)
                    .stream()
                    .map(cierre -> {
                        if (cierre.getShiftSessionId() != null) {
                            Optional<ShiftSession> sessionOpt = shiftSessionRepository.findById(cierre.getShiftSessionId());
                            if (sessionOpt.isPresent()) {
                                ShiftSession session = sessionOpt.get();
                                if (session.getTotalCash() != null) {
                                    BigDecimal totalGastos = cierre.getTotalGastos() != null ? cierre.getTotalGastos() : BigDecimal.ZERO;
                                    cierre.setTotalIngresos(session.getTotalCash());
                                    cierre.setResta(session.getTotalCash().subtract(totalGastos));
                                }
                            }
                        }
                        return convertirAResponse(cierre);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[DriverCloseService] error obteniendo cierres por rango driverId={} desde={}: {}",
                driverId, desde, e.getMessage());
            return List.of();
        }
    }
}
