package com.yego.backend.service.yego_pro_ops.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_garantizado.entities.Driver;
import com.yego.backend.entity.yego_pro_ops.api.request.DriverListRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverInfoResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverKpiResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverListResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.WorkRulesResponse;
import com.yego.backend.repository.yego_garantizado.DriverRepository;
import com.yego.backend.service.yego_pro_ops.FleetDriverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FleetDriverServiceImpl implements FleetDriverService {
    
    private final RestTemplate restTemplate;
    private final RestTemplate yangoProxyRestTemplate;
    private final com.yego.backend.config.yego_pro_ops.ProxyConfig proxyConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DriverRepository driverRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Map<String, DriverKpiResponse> kpisCache = new ConcurrentHashMap<>();
    
    public FleetDriverServiceImpl(
            RestTemplate restTemplate,
            @Qualifier("yangoProxyRestTemplate") RestTemplate yangoProxyRestTemplate,
            com.yego.backend.config.yego_pro_ops.ProxyConfig proxyConfig,
            DriverRepository driverRepository,
            JdbcTemplate jdbcTemplate) {
        this.restTemplate = restTemplate;
        this.yangoProxyRestTemplate = yangoProxyRestTemplate;
        this.proxyConfig = proxyConfig;
        this.driverRepository = driverRepository;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    private static final String YANGO_COOKIE_BASE = "yashr=299832191758141387; receive-cookie-deprecation=1; gdpr=0; _ym_uid=175814138831171998; _ym_d=1758141396; yandexuid=9201743261758137514; yuidss=9201743261758137514; yandex_login=Jhajaira.ochoa; L=ZxtXZnxqBmR/SAVhdVZyXwJfaA9cBV1cLlAoE1ddHRAZOQxQLTM=.1759148388.1235855.353634.d47e5d7bafd679e1c83d4f42c6e23cd9; Session_id=3:1764107903.5.0.1758141420632:WbD9Jg:3a2a.1.2:1|2015824474.1006968.2.0:3.2:1006968.3:1759148388|60:11433100.181615.QwZv5z-R92wHzh4j43-daY0-K7g; sessar=1.1396519.CiDPtjUqckvAWEzdml3-Xvlj9hjrxSX6tJqOaxrdeZn5IA.jv1_Nvn5vO56j4mwzyFG1Wg8pHX_1BWCf-tAOHo6bQk; sessionid2=3:1764107903.5.0.1758141420632:WbD9Jg:3a2a.1.2:1|2015824474.1006968.2.0:3.2:1006968.3:1759148388|60:11433100.181615.fakesign0000000000000000000; i=XLXWMoCXAOgX6hzpx/AmT+HOGGwAwQhiTRKOzxl2tkMXu90DChcwoTT5z8qvZlmQyhkwZXurYZsuUa9AHb5foPXF2Rc=; _ym_isad=2; yp=2074508388.udn.cDpKaGFqYWlyYS5vY2hvYQ%3D%3D#1764940883.yu.9201743261758137514; ymex=1767446483.oyu.9201743261758137514#2073501389.yrts.1758141389; _ym_visorc=b; bh=Ej8iQ2hyb21pdW0iO3Y9IjE0MiIsIkdvb2dsZSBDaHJvbWUiO3Y9IjE0MiIsIk5vdF9BIEJyYW5kIjt2PSI5OSIaA3g4NiIOMTQyLjAuNzQ0NC4xNzYqAj8wOgdXaW5kb3dzQgYxMC4wLjBKAjY0UlsiQ2hyb21pdW0iO3Y9IjE0Mi4wLjc0NDQuMTc2IiwiR29vZ2xlIENocm9tZSI7dj0iMTQyLjAuNzQ0NC4xNzYiLCJOb3RfQSBCcmFuZCI7dj0iOTkuMC4wLjAiYPzux8kGah7cyuH/CJLYobEDn8/h6gP7+vDnDev//fYP+JzMhwg=; _yasc=fAS4rNTIoRKZq+JMg4Gaj9eUc5ZA62kxyJug7jaq8e08yG17i2jy7kvgtoikFrhpuyUc";
    private static final String YANGO_API_URL = "https://fleet.yango.com/api/fleet/map/v2/drivers/points";
    private static final String YANGO_CONTRACTORS_API_URL = "https://fleet.yango.com/api/fleet/contractor-profiles-manager/v2/contractors/list";
    private static final String YANGO_WORK_RULES_API_URL = "https://fleet.yango.com/api/fleet/driver-work-rules/v1/work-rules/light-list";
    private static final String PARK_ID = "64085dd85e124e2c808806f70d527ea8";
    
    private RestTemplate getRestTemplate() {
        if (proxyConfig != null && proxyConfig.isEnabled() && yangoProxyRestTemplate != null) {
            return yangoProxyRestTemplate;
        }
        return restTemplate;
    }
    
    @Override
    public DriverKpiResponse consultarConductores() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Cookie", YANGO_COOKIE_BASE);
            headers.set("x-park-id", PARK_ID);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(new HashMap<>(), headers);
            ResponseEntity<String> response = getRestTemplate().exchange(YANGO_API_URL, HttpMethod.POST, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                DriverKpiResponse kpis = transformarAKpis(jsonResponse);
                kpisCache.put(PARK_ID, kpis);
                return kpis;
            }
            
            return obtenerKpisCached();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("❌ Error HTTP al consultar conductores: {}", e.getStatusCode());
            return obtenerKpisCached();
        } catch (Exception e) {
            log.error("❌ Error consultando conductores: {}", e.getMessage());
            return obtenerKpisCached();
        }
    }
    
    private DriverKpiResponse transformarAKpis(JsonNode jsonResponse) {
        JsonNode totals = jsonResponse.get("totals");
        if (totals == null) {
            return crearKpisVacio();
        }
        
        JsonNode status = totals.get("status");
        int viajeActivo = status != null && status.has("in_order") ? status.get("in_order").asInt() : 0;
        int noDisponibles = status != null && status.has("busy") ? status.get("busy").asInt() : 0;
        int disponibles = status != null && status.has("free") ? status.get("free").asInt() : 0;
        int sinGPS = totals.has("no_gps") ? totals.get("no_gps").asInt() : 0;
        
        List<DriverInfoResponse> items = transformarItems(jsonResponse.get("items"));
        
        return DriverKpiResponse.builder()
            .viajeActivo(viajeActivo)
            .noDisponibles(noDisponibles)
            .disponibles(disponibles)
            .sinGPS(sinGPS)
            .items(items)
            .build();
    }
    
    private List<DriverInfoResponse> transformarItems(JsonNode itemsNode) {
        List<DriverInfoResponse> items = new ArrayList<>();
        if (itemsNode == null || !itemsNode.isArray()) {
            return items;
        }
        
        // Obtener todos los driver_ids primero
        List<String> driverIds = new ArrayList<>();
        for (JsonNode item : itemsNode) {
            String driverId = item.has("driver_id") ? item.get("driver_id").asText() : null;
            if (driverId != null) {
                driverIds.add(driverId);
            }
        }
        
        // Hacer UNA sola consulta SQL para obtener todos los datos
        Map<String, DriverData> datosConductores = obtenerDatosConductoresBatch(driverIds);
        
        // Construir las respuestas
        for (JsonNode item : itemsNode) {
            String driverId = item.has("driver_id") ? item.get("driver_id").asText() : null;
            if (driverId == null) continue;
            
            DriverData driverData = datosConductores.get(driverId);
            String fullName = driverData != null ? driverData.fullName : null;
            String carNumber = driverData != null ? driverData.carNumber : null;
            
            JsonNode coords = item.get("coordinates");
            DriverInfoResponse.CoordinatesResponse coordinates = null;
            if (coords != null) {
                coordinates = DriverInfoResponse.CoordinatesResponse.builder()
                    .lon(coords.has("lon") ? coords.get("lon").asDouble() : null)
                    .lat(coords.has("lat") ? coords.get("lat").asDouble() : null)
                    .build();
            }
            
            DriverInfoResponse driverInfo = DriverInfoResponse.builder()
                .driverId(driverId)
                .fullName(fullName)
                .carNumber(carNumber)
                .coordinates(coordinates)
                .status(item.has("status") ? item.get("status").asText() : null)
                .balance(item.has("balance") ? item.get("balance").asDouble() : null)
                .statusDuration(item.has("status_duration") ? item.get("status_duration").asInt() : null)
                .build();
            
            items.add(driverInfo);
        }
        
        return items;
    }
    
    /**
     * Clase interna para almacenar datos del conductor
     */
    private static class DriverData {
        final String fullName;
        final String carNumber;
        
        DriverData(String fullName, String carNumber) {
            this.fullName = fullName;
            this.carNumber = carNumber;
        }
    }
    
    /**
     * Optimizado: Obtiene todos los datos de conductores en UNA sola consulta SQL
     */
    private Map<String, DriverData> obtenerDatosConductoresBatch(List<String> driverIds) {
        Map<String, DriverData> resultado = new HashMap<>();
        
        if (driverIds == null || driverIds.isEmpty()) {
            return resultado;
        }
        
        try {
            // Construir la consulta SQL con IN clause
            String placeholders = String.join(",", java.util.Collections.nCopies(driverIds.size(), "?"));
            String sql = "SELECT driver_id, full_name, car_number FROM drivers WHERE driver_id IN (" + placeholders + ")";
            
            jdbcTemplate.query(sql, driverIds.toArray(), (rs, rowNum) -> {
                String driverId = rs.getString("driver_id");
                String fullName = rs.getString("full_name");
                String carNumber = rs.getString("car_number");
                resultado.put(driverId, new DriverData(fullName, carNumber));
                return null;
            });
            
            log.debug("✅ Datos obtenidos para {} conductores en una sola consulta", resultado.size());
            
        } catch (Exception e) {
            log.error("❌ Error obteniendo datos de conductores en batch: {}", e.getMessage(), e);
        }
        
        return resultado;
    }
    
    private DriverKpiResponse obtenerKpisCached() {
        return kpisCache.getOrDefault(PARK_ID, crearKpisVacio());
    }
    
    private DriverKpiResponse crearKpisVacio() {
        return DriverKpiResponse.builder()
            .viajeActivo(0)
            .noDisponibles(0)
            .disponibles(0)
            .sinGPS(0)
            .items(new ArrayList<>())
            .build();
    }
    
    @Override
    public DriverKpiResponse obtenerKpisActuales() {
        return obtenerKpisCached();
    }

    @Override
    public DriverListResponse obtenerListaConductores(List<String> workRuleIds) {
        try {
            // Crear filtro - si se proporcionan work_rule_ids, agregarlos al filtro
            Map<String, Object> filter = new HashMap<>();
            if (workRuleIds != null && !workRuleIds.isEmpty()) {
                filter.put("work_rule_ids", workRuleIds);
                log.info("📋 [FleetDriverService] Filtrando conductores por work_rule_ids: {}", workRuleIds);
            } else {
                log.info("📋 [FleetDriverService] Obteniendo todos los conductores sin filtro de work_rule_ids");
            }
            
            // Crear request body
            DriverListRequest requestBody = DriverListRequest.builder()
                .filter(filter)
                .limit(50)
                .projection(java.util.Arrays.asList(
                    "full_name", "avatar_url", "name", "status", "id", "phone",
                    "orders_count", "groups", "violations", "attestation_issues",
                    "balance", "balance_limit", "unblock_date", "photocheck_restrictions"
                ))
                .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Cookie", YANGO_COOKIE_BASE);
            headers.set("x-park-id", PARK_ID);
            headers.set("language", "es-419");
            headers.set("x-client-version", "fleet/19321");
            headers.set("origin", "https://fleet.yango.com");

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> request = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = getRestTemplate().exchange(
                YANGO_CONTRACTORS_API_URL,
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return transformarAListaConductores(jsonResponse);
            }

            log.warn("⚠️ Respuesta HTTP no exitosa al obtener lista de conductores: {}", response.getStatusCode());
            return DriverListResponse.builder()
                .contractors(new ArrayList<>())
                .build();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("❌ Error HTTP al obtener lista de conductores: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return DriverListResponse.builder()
                .contractors(new ArrayList<>())
                .build();
        } catch (Exception e) {
            log.error("❌ Error obteniendo lista de conductores: {}", e.getMessage(), e);
            return DriverListResponse.builder()
                .contractors(new ArrayList<>())
                .build();
        }
    }

    private DriverListResponse transformarAListaConductores(JsonNode jsonResponse) {
        List<DriverListResponse.ContractorResponse> contractors = new ArrayList<>();

        JsonNode contractorsNode = jsonResponse.get("contractors");
        if (contractorsNode != null && contractorsNode.isArray()) {
            for (JsonNode contractorNode : contractorsNode) {
                try {
                    DriverListResponse.ContractorResponse contractor = mapearContractor(contractorNode);
                    if (contractor != null) {
                        contractors.add(contractor);
                    }
                } catch (Exception e) {
                    log.error("❌ Error mapeando contractor: {}", e.getMessage());
                }
            }
        }

        return DriverListResponse.builder()
            .contractors(contractors)
            .build();
    }

    private DriverListResponse.ContractorResponse mapearContractor(JsonNode contractorNode) {
        try {
            // Mapear nombre
            DriverListResponse.ContractorResponse.NameResponse nameResponse = null;
            if (contractorNode.has("name")) {
                JsonNode nameNode = contractorNode.get("name");
                nameResponse = DriverListResponse.ContractorResponse.NameResponse.builder()
                    .first(nameNode.has("first") ? nameNode.get("first").asText() : null)
                    .last(nameNode.has("last") ? nameNode.get("last").asText() : null)
                    .middle(nameNode.has("middle") ? nameNode.get("middle").asText() : null)
                    .build();
            }

            // Mapear grupos
            List<String> groups = new ArrayList<>();
            if (contractorNode.has("groups") && contractorNode.get("groups").isArray()) {
                for (JsonNode groupNode : contractorNode.get("groups")) {
                    groups.add(groupNode.asText());
                }
            }

            // Mapear violations
            List<String> violations = new ArrayList<>();
            if (contractorNode.has("violations") && contractorNode.get("violations").isArray()) {
                for (JsonNode violationNode : contractorNode.get("violations")) {
                    violations.add(violationNode.asText());
                }
            }

            // Mapear attestation_issues
            List<String> attestationIssues = new ArrayList<>();
            if (contractorNode.has("attestation_issues") && contractorNode.get("attestation_issues").isArray()) {
                for (JsonNode issueNode : contractorNode.get("attestation_issues")) {
                    attestationIssues.add(issueNode.asText());
                }
            }

            // Mapear photocheck_restrictions
            List<String> photocheckRestrictions = new ArrayList<>();
            if (contractorNode.has("photocheck_restrictions") && contractorNode.get("photocheck_restrictions").isArray()) {
                for (JsonNode restrictionNode : contractorNode.get("photocheck_restrictions")) {
                    photocheckRestrictions.add(restrictionNode.asText());
                }
            }

            return DriverListResponse.ContractorResponse.builder()
                .id(contractorNode.has("id") ? contractorNode.get("id").asText() : null)
                .leadId(contractorNode.has("lead_id") ? contractorNode.get("lead_id").asText() : null)
                .avatarUrl(contractorNode.has("avatar_url") ? contractorNode.get("avatar_url").asText() : null)
                .balance(contractorNode.has("balance") ? contractorNode.get("balance").asText() : null)
                .balanceLimit(contractorNode.has("balance_limit") ? contractorNode.get("balance_limit").asText() : null)
                .fullName(contractorNode.has("full_name") ? contractorNode.get("full_name").asText() : null)
                .groups(groups)
                .hiringSegment(contractorNode.has("hiring_segment") ? contractorNode.get("hiring_segment").asText() : null)
                .lastOrderDate(contractorNode.has("last_order_date") ? contractorNode.get("last_order_date").asText() : null)
                .lifecycleStep(contractorNode.has("lifecycle_step") ? contractorNode.get("lifecycle_step").asText() : null)
                .name(nameResponse)
                .ordersCount(contractorNode.has("orders_count") ? contractorNode.get("orders_count").asInt() : null)
                .phone(contractorNode.has("phone") ? contractorNode.get("phone").asText() : null)
                .status(contractorNode.has("status") ? contractorNode.get("status").asText() : null)
                .violations(violations)
                .attestationIssues(attestationIssues)
                .unblockDate(contractorNode.has("unblock_date") ? contractorNode.get("unblock_date").asText() : null)
                .photocheckRestrictions(photocheckRestrictions)
                .build();
        } catch (Exception e) {
            log.error("❌ Error mapeando contractor: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public WorkRulesResponse obtenerReglasTrabajo() {
        try {
            log.info("📋 [FleetDriverService] Obteniendo reglas de trabajo");
            
            // Crear request body con has_contractors: true
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("has_contractors", true);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Cookie", YANGO_COOKIE_BASE);
            headers.set("x-park-id", PARK_ID);
            headers.set("language", "es-419");
            headers.set("x-client-version", "fleet/19321");
            headers.set("origin", "https://fleet.yango.com");
            
            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> request = new HttpEntity<>(requestJson, headers);
            
            ResponseEntity<String> response = getRestTemplate().exchange(
                YANGO_WORK_RULES_API_URL,
                HttpMethod.POST,
                request,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return transformarAReglasTrabajo(jsonResponse);
            }
            
            log.warn("⚠️ Respuesta HTTP no exitosa al obtener reglas de trabajo: {}", response.getStatusCode());
            return WorkRulesResponse.builder()
                .workRules(new ArrayList<>())
                .build();
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("❌ Error HTTP al obtener reglas de trabajo: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return WorkRulesResponse.builder()
                .workRules(new ArrayList<>())
                .build();
        } catch (Exception e) {
            log.error("❌ Error obteniendo reglas de trabajo: {}", e.getMessage(), e);
            return WorkRulesResponse.builder()
                .workRules(new ArrayList<>())
                .build();
        }
    }
    
    private WorkRulesResponse transformarAReglasTrabajo(JsonNode jsonResponse) {
        try {
            List<WorkRulesResponse.WorkRule> workRules = new ArrayList<>();
            JsonNode lightWorkRulesNode = jsonResponse.get("light_work_rules");
            
            if (lightWorkRulesNode != null && lightWorkRulesNode.isArray()) {
                for (JsonNode ruleNode : lightWorkRulesNode) {
                    WorkRulesResponse.WorkRule rule = WorkRulesResponse.WorkRule.builder()
                        .id(ruleNode.has("id") ? ruleNode.get("id").asText() : null)
                        .name(ruleNode.has("name") ? ruleNode.get("name").asText() : null)
                        .build();
                    workRules.add(rule);
                }
            }
            
            return WorkRulesResponse.builder()
                .workRules(workRules)
                .build();
                
        } catch (Exception e) {
            log.error("❌ Error transformando reglas de trabajo: {}", e.getMessage(), e);
            return WorkRulesResponse.builder()
                .workRules(new ArrayList<>())
                .build();
        }
    }
}

