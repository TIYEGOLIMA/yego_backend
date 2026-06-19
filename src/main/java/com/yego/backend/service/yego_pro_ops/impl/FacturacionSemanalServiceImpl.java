package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.response.BillingConfigResponse;
import com.yego.backend.entity.yego_pro_ops.entities.BonusThreshold;
import com.yego.backend.entity.yego_pro_ops.entities.FacturacionSemanal;
import com.yego.backend.entity.yego_pro_ops.entities.PaymentPercentage;
import com.yego.backend.repository.yego_pro_ops.BonusThresholdRepository;
import com.yego.backend.repository.yego_pro_ops.FacturacionSemanalRepository;
import com.yego.backend.repository.yego_pro_ops.PaymentPercentageRepository;
import com.yego.backend.service.yego_pro_ops.FacturacionSemanalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturacionSemanalServiceImpl implements FacturacionSemanalService {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final FacturacionSemanalRepository facturacionSemanalRepository;
    private final PaymentPercentageRepository paymentPercentageRepository;
    private final BonusThresholdRepository bonusThresholdRepository;

    @Override
    public FacturacionSemanal registrarFacturacionSemanal(FacturacionSemanal facturacion) {
        Optional<FacturacionSemanal> existente = facturacionSemanalRepository
                .findByDriverIdAndFechaInicioAndFechaFin(
                        facturacion.getDriverId(),
                        facturacion.getFechaInicio(),
                        facturacion.getFechaFin());

        if (existente.isPresent()) {
            FacturacionSemanal actual = existente.get();
            actual.setTotalViajes(facturacion.getTotalViajes());
            actual.setViajesValidos(facturacion.getViajesValidos());
            actual.setHorasTrabajo(facturacion.getHorasTrabajo());
            actual.setMontoTotalProducido(facturacion.getMontoTotalProducido());
            actual.setComisionApp(facturacion.getComisionApp());
            actual.setMontoNeto(facturacion.getMontoNeto());
            actual.setKmRecorrido(facturacion.getKmRecorrido());
            actual.setGastoCombustible(facturacion.getGastoCombustible());
            actual.setBonoYango(facturacion.getBonoYango());
            actual.setGastoMantenimiento(facturacion.getGastoMantenimiento());
            actual.setProduccionBonificable(facturacion.getProduccionBonificable());
            actual.setBonoAdicViajes(facturacion.getBonoAdicViajes());
            actual.setBono(facturacion.getBono());
            actual.setPorcentajePago(facturacion.getPorcentajePago());
            actual.setPago(facturacion.getPago());
            actual.setBonificacion(facturacion.getBonificacion());
            actual.setGarantia(facturacion.getGarantia());
            actual.setDescuento(facturacion.getDescuento());
            actual.setGeneral(facturacion.getGeneral());
            actual.setPagoTotal(facturacion.getPagoTotal());
            actual.setBonificacionEmpresa(facturacion.getBonificacionEmpresa());
            actual.setPagoTotalFinal(facturacion.getPagoTotalFinal());
            actual.setUtilidad(facturacion.getUtilidad());
            actual.setUtilidadPorViaje(facturacion.getUtilidadPorViaje());
            actual.setPagoPorViaje(facturacion.getPagoPorViaje());
            actual.setDiasTrabajados(facturacion.getDiasTrabajados());
            actual.setDiasLiquidados(facturacion.getDiasLiquidados());
            actual.setTurno(facturacion.getTurno());
            actual.setEstado(facturacion.getEstado() != null ? facturacion.getEstado() : "pendiente");
            actual.setUserId(facturacion.getUserId());
            return facturacionSemanalRepository.save(actual);
        }

        return facturacionSemanalRepository.save(facturacion);
    }

    @Override
    public List<FacturacionSemanal> obtenerHistorialFacturacion(String fechaInicio, String fechaFin) {
        if (fechaInicio != null && !fechaInicio.isEmpty() && fechaFin != null && !fechaFin.isEmpty()) {
            LocalDate inicio = LocalDate.parse(fechaInicio, DATE_FORMATTER);
            LocalDate fin = LocalDate.parse(fechaFin, DATE_FORMATTER);
            return facturacionSemanalRepository.findByRangoFechas(inicio, fin);
        }
        if (fechaInicio != null && !fechaInicio.isEmpty()) {
            LocalDate inicio = LocalDate.parse(fechaInicio, DATE_FORMATTER);
            return facturacionSemanalRepository.findByRangoFechas(inicio, LocalDate.now(LIMA_ZONE));
        }
        return facturacionSemanalRepository.findAllByOrderByFechaInicioDesc();
    }

    @Override
    public BillingConfigResponse obtenerConfiguracionBilling() {
        List<BonusThreshold> bonusThresholds = bonusThresholdRepository.findAll();
        List<PaymentPercentage> paymentPercentages = paymentPercentageRepository.findAll();
        return BillingConfigResponse.builder()
                .bonusThresholds(bonusThresholds)
                .paymentPercentages(paymentPercentages)
                .build();
    }

    @Override
    public BillingConfigResponse guardarConfiguracionBilling(BillingConfigResponse config, Long userId) {
        if (config.getBonusThresholds() != null) {
            bonusThresholdRepository.deleteAll();
            bonusThresholdRepository.flush();
            for (BonusThreshold bt : config.getBonusThresholds()) {
                bt.setId(null);
                bt.setUpdatedBy(userId);
                bonusThresholdRepository.save(bt);
            }
        }
        if (config.getPaymentPercentages() != null) {
            paymentPercentageRepository.deleteAll();
            paymentPercentageRepository.flush();
            for (PaymentPercentage pp : config.getPaymentPercentages()) {
                pp.setId(null);
                pp.setUpdatedBy(userId);
                paymentPercentageRepository.save(pp);
            }
        }
        return obtenerConfiguracionBilling();
    }
}
