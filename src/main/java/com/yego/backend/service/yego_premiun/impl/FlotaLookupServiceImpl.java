package com.yego.backend.service.yego_premiun.impl;

import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import com.yego.backend.service.yego_garantizado.FlotaService;
import com.yego.backend.service.yego_premiun.FlotaLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlotaLookupServiceImpl implements FlotaLookupService {

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    private final FlotaService flotaService;

    private volatile Map<String, String> nombrePorParkId = Map.of();
    private volatile long cacheExpiryMillis = 0L;

    private Map<String, String> mapaNombres() {
        long now = System.currentTimeMillis();
        if (now < cacheExpiryMillis && !nombrePorParkId.isEmpty()) {
            return nombrePorParkId;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (now < cacheExpiryMillis && !nombrePorParkId.isEmpty()) {
                return nombrePorParkId;
            }
            try {
                List<FlotaResponse> todas = flotaService.obtenerTodosLosPartners();
                Map<String, String> m = todas.stream()
                        .filter(f -> f.getId() != null && !f.getId().isBlank())
                        .collect(Collectors.toMap(
                                FlotaResponse::getId,
                                f -> f.getName() != null && !f.getName().isBlank()
                                        ? f.getName()
                                        : "Flota " + f.getId(),
                                (a, b) -> a));
                if (!m.isEmpty()) {
                    nombrePorParkId = m;
                } else if (nombrePorParkId.isEmpty()) {
                    log.warn("[FlotaLookup] API de partners devolvió lista vacía; nombres no disponibles hasta próximo intento");
                }
                cacheExpiryMillis = now + CACHE_TTL_MS;
                log.debug("[FlotaLookup] Mapa parkId→nombre cargado: {} entradas", nombrePorParkId.size());
            } catch (Exception e) {
                log.error("[FlotaLookup] Error cargando flotas desde FlotaService: {}", e.getMessage());
                cacheExpiryMillis = now + (nombrePorParkId.isEmpty() ? 60_000L : CACHE_TTL_MS);
            }
            return nombrePorParkId;
        }
    }

    @Override
    public String obtenerNombreFlota(String parkId) {
        if (parkId == null || parkId.isBlank()) {
            return "Flota desconocida";
        }
        return mapaNombres().getOrDefault(parkId, "Flota " + parkId);
    }
}
