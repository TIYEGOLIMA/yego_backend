package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.OperationalTripFactInput;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalTripFactResponse;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalTripFact;

import java.time.LocalDateTime;
import java.util.List;

public interface OperationalTripFactService {

    OperationalTripFact upsertTripFact(OperationalTripFactInput input);

    List<OperationalTripFact> upsertTripFacts(List<OperationalTripFactInput> inputs);

    List<OperationalTripFact> importFromDriverOrders(String driverId, String dateFrom, String dateTo);

    List<OperationalTripFactResponse> searchTripFacts(
            LocalDateTime from,
            LocalDateTime to,
            String driverId,
            String vehicleKey,
            String status,
            Integer limit);
}
