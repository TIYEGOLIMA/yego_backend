package com.yego.backend.service.yego_garantizado.impl;

import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import com.yego.backend.service.yego_garantizado.FlotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Implementación del servicio de flotas del sistema YEGO Garantizado
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlotaServiceImpl implements FlotaService {

    private final RestTemplate restTemplate;
    
    // IDs de las flotas de Yego que queremos filtrar
    private static final List<String> YEGO_FLOTA_IDS = Arrays.asList(
        "05b1c831e66f41a9a87f5f3fa0a186ae", // Yego Cali
        "08e20910d81d42658d4334d3f6d10ac0", // Yego Lima
        "56e4607dfc354e0a9cde4f0aa7973003", // Yego Arequipa
        "ef21f793358144f589aabcbeb8bd7d50", // Yego Barranquilla
        "c054c8b5dfe14e75b882943b2a252706", // Yego Black
        "c58110bc70244430a70a8126fc69f22c", // Yego Líderes
        "5921e55cc5d042d28747dd722608955a", // Yego Prime
        "ff424287c4bd4cbba6066962951a121f", // Yego Promi
        "851e30755bba4d298e2e837f571b4ab8", // Yego Trujillo
        "ae57aaedeacd41eb9fdbe1ff7a89a3f2", // Yego,
        "2e39f6699c854bc49cc75197431fe25c"  //Yego.
    );

    @Override
    public List<FlotaResponse> obtenerFlotas() {
        try {
            log.info("🔍 Obteniendo flotas desde API externa");
            String url = "http://162.55.214.109:6000/v2/partners";
            Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);
            
            List<FlotaResponse> flotas = new ArrayList<>();
            
            if (response != null && response.containsKey("partners")) {
                List<Map<String, Object>> partners = (List<Map<String, Object>>) response.get("partners");
                
                for (Map<String, Object> item : partners) {
                    String id = item.get("id").toString();
                    
                    // Filtrar solo las flotas de Yego
                    if (YEGO_FLOTA_IDS.contains(id)) {
                        FlotaResponse flota = new FlotaResponse();
                        flota.setId(id);
                        flota.setName(item.get("name").toString());
                        flota.setCity(item.get("city") != null ? item.get("city").toString() : null);
                        flota.setSpecifications((List<String>) item.get("specifications"));
                        flotas.add(flota);
                        log.info("✅ Flota agregada: {} - {}", id, flota.getName());
                    }
                }
            }
            
            log.info("✅ Total de flotas Yego obtenidas: {}", flotas.size());
            return flotas;
            
        } catch (Exception e) {
            log.error("❌ Error obteniendo flotas: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
