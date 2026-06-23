package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.VehicleResponse;
import com.yego.backend.entity.yego_pro_ops.entities.*;

import java.util.List;
import java.util.Map;

public interface VehicleService {

    Map<String, Object> listarVehiculosYango(String parkId, String cursor);

    VehicleResponse obtenerDetalleVehiculo(String carId, String parkId);

    Map<String, Object> obtenerHistorialQc(String carId, String parkId);

    List<VehicleResponse.DocumentInfo> obtenerDocumentos(String yangoCarId);

    VehicleResponse.DocumentInfo agregarDocumento(String yangoCarId, VehicleDocument doc);

    void eliminarDocumento(Long docId);

    List<VehicleResponse.MaintenanceInfo> obtenerMantenimientos(String yangoCarId);

    VehicleResponse.MaintenanceInfo agregarMantenimiento(String yangoCarId, VehicleMaintenance mant);

    void eliminarMantenimiento(Long mantId);

    List<VehicleResponse.MileageInfo> obtenerKilometraje(String yangoCarId);

    VehicleResponse.MileageInfo agregarKilometraje(String yangoCarId, VehicleMileage km);

    List<VehicleResponse.IncidentInfo> obtenerSiniestros(String yangoCarId);

    VehicleResponse.IncidentInfo agregarSiniestro(String yangoCarId, VehicleIncident inc);

    void eliminarSiniestro(Long incId);
}
