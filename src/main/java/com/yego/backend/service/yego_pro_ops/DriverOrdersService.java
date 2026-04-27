package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverTripsSimplifiedResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.MultipleDriversTripsSimplifiedResponse;

import java.util.List;

public interface DriverOrdersService {

    DriverOrdersResponse obtenerViajesCompletos(String driverId, String dateFrom, String dateTo);

    MultipleDriversTripsSimplifiedResponse obtenerViajesSimplificadosMultiples(
        List<String> driverIds, String dateFrom, String dateTo);

    DriverTripsSimplifiedResponse obtenerViajesSimplificadosPorFecha(String driverId, String fecha);
}
