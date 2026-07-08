package com.yego.backend.entity.yego_pro_ops.api.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MobileShiftResponse {
    private UUID sessionId;
    private String driverId;
    private String vehicleId;
    private String placa;
    private String marca;
    private String modelo;
    private LocalDateTime startedAt;
    private LocalDateTime closedAt;
    private String duracion;
    private Integer kmInicial;
    private Integer kmFinal;
    private Integer kmRecorridos;
    private String status;
    private Integer totalViajes;
    private BigDecimal producido;
    private BigDecimal efectivoYango;
    private BigDecimal yapeYango;
    private BigDecimal efectivo;
    private BigDecimal yape;
    private String numeroOperacion;
    private BigDecimal gasolinaMonto;
    private BigDecimal gasolinaGalones;
    private BigDecimal gnvMonto;
    private BigDecimal gnvM3;
    private BigDecimal otrosGastos;
    private BigDecimal totalGastos;
    private BigDecimal totalIngresos;
    private BigDecimal balance;
    private List<String> carPhotos;
    private String selfieUri;
    private List<String> carPhotosCierre;
    private List<String> fotosEvidencia;
    private String observaciones;
    private Boolean mantenimientoRequerido;
    private String mantenimientoDescripcion;
}
