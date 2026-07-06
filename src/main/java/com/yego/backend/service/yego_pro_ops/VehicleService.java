package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.FleetVehicleResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.MobileVehicleCard;
import com.yego.backend.entity.yego_pro_ops.api.response.MobileVehicleResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.VehicleTraceEvent;
import com.yego.backend.entity.yego_pro_ops.api.response.VehicleResponse;
import com.yego.backend.entity.yego_pro_ops.entities.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface VehicleService {

    Map<String, Object> listarVehiculosYango(String parkId, String cursor);

    VehicleResponse obtenerDetalleVehiculo(String carId, String parkId);

    Map<String, Object> obtenerHistorialQc(String carId, String parkId);

    // ── Flota cacheada (segmentación) ──

    /** Lista vehículos guardados; si segmentId es null, devuelve todos. */
    List<FleetVehicleResponse> listarVehiculosGuardados(UUID segmentId);

    /** Sincroniza todas las flotas activas con Yango. Devuelve nº de vehículos procesados. */
    int sincronizarTodas();

    /** Sincroniza una flota específica con Yango. Devuelve nº de vehículos procesados. */
    int sincronizarFlota(UUID segmentId);

    /** Ficha completa del vehículo buscando por placa (number). */
    VehicleResponse obtenerDetallePorPlaca(String placa);

    /** Trazabilidad unificada del vehículo (cambios de flota + documentos). */
    List<VehicleTraceEvent> obtenerTrazabilidad(String yangoCarId);

    /** Buscador ligero móvil por placa (parcial). Devuelve cards desde BD local. */
    List<MobileVehicleCard> buscarVehiculosMobile(String placa);

    /** Respuesta agregada para app móvil: todo del auto por yangoCarId (BD local). */
    MobileVehicleResponse obtenerVehiculoMobile(String yangoCarId);

    List<VehicleResponse.DocumentInfo> obtenerDocumentos(String yangoCarId);

    VehicleResponse.DocumentInfo agregarDocumento(String yangoCarId, VehicleDocument doc);

    /** Crea un documento subiendo el archivo a MinIO (bucket documentacion-flota, nombre {placa}/{TIPO}-{correlativo}). */
    VehicleResponse.DocumentInfo agregarDocumentoConArchivo(String yangoCarId, String tipo, String nombre, LocalDate fechaVigente, MultipartFile file, Long createdById);

    /** Sube un archivo de mantenimiento a MinIO y devuelve su URL. */
    String subirArchivoMantenimiento(String yangoCarId, MultipartFile file);

    /** Baja lógica (soft delete) de un documento, registrando quién lo eliminó. */
    void eliminarDocumento(Long docId, Long deletedById);

    List<VehicleResponse.MaintenanceInfo> obtenerMantenimientos(String yangoCarId);

    VehicleResponse.MaintenanceInfo agregarMantenimiento(String yangoCarId, VehicleMaintenance mant);

    void eliminarMantenimiento(Long mantId);

    List<VehicleResponse.MileageInfo> obtenerKilometraje(String yangoCarId);

    VehicleResponse.MileageInfo agregarKilometraje(String yangoCarId, VehicleMileage km);

    List<VehicleResponse.IncidentInfo> obtenerSiniestros(String yangoCarId);

    VehicleResponse.IncidentInfo agregarSiniestro(String yangoCarId, VehicleIncident inc);

    void eliminarSiniestro(Long incId);
}
