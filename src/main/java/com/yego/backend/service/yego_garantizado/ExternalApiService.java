package com.yego.backend.service.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.entities.YegoGarantizado;

public interface ExternalApiService {
    
    YegoGarantizado procesarConductor(String licencia, String parkId, String flotaId);
}
