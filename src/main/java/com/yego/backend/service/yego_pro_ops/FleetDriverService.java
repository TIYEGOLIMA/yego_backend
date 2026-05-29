package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.ContractorSuggestionsResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverListResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverSimpleResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriversInOrderResponse;

public interface FleetDriverService {

    DriverListResponse obtenerListaConductores();

    DriversInOrderResponse obtenerConductoresEnOrden(Integer page, Integer limit);

    default DriversInOrderResponse obtenerConductoresEnOrden() {
        return obtenerConductoresEnOrden(0, Integer.MAX_VALUE);
    }

    DriverSimpleResponse obtenerListaConductoresSimplificada();

    ContractorSuggestionsResponse getContractorSuggestions(String parkId, String telefono);

    String obtenerPlacaConductor(String driverId);
}
