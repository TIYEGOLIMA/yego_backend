package com.yego.backend.service.yego_garantizado.impl;

import com.yego.backend.entity.yego_garantizado.api.request.GarantizadoRequest;
import com.yego.backend.entity.yego_garantizado.entities.YegoGarantizado;
import com.yego.backend.repository.yego_garantizado.YegoGarantizadoRepository;
import com.yego.backend.service.yego_garantizado.ExternalApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalApiServiceImpl implements ExternalApiService {

    private final RestTemplate restTemplate;
    private final YegoGarantizadoRepository yegoGarantizadoRepository;
    private final JdbcTemplate jdbcTemplate;
    private static final String EXTERNAL_API_URL = "http://5.161.229.77:8087/procesar-conductor";

    @Override
    public YegoGarantizado procesarConductor(String licencia, String parkId, String semana) {
        try {
            log.info("🌐 [ExternalApiService] Consumiendo API externa para licencia: {} con parkId: {}", licencia, parkId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Crear un objeto JSON con la licencia, parkId (flota) y semana anterior
            String jsonRequest = "{\"licencia\":\"" + licencia + "\",\"parkId\":\"" + parkId + "\",\"semana\":\"" + semana + "\"}";
            HttpEntity<String> request = new HttpEntity<>(jsonRequest, headers);
            
            ResponseEntity<GarantizadoRequest> response = restTemplate.exchange(
                EXTERNAL_API_URL,
                HttpMethod.POST,
                request,
                GarantizadoRequest.class
            );
            
            if (response.getBody() != null) {
                GarantizadoRequest apiResponse = response.getBody();
                log.info("✅ [ExternalApiService] Datos recibidos de API externa: {}", apiResponse);
                
                return procesarDatosApi(apiResponse, parkId, semana);
            } else {
                log.warn("⚠️ [ExternalApiService] Respuesta vacía de API externa");
                return null;
            }
            
        } catch (Exception e) {
            log.error("❌ [ExternalApiService] Error consumiendo API externa: {}", e.getMessage());
            return null;
        }
    }
    
    private YegoGarantizado procesarDatosApi(GarantizadoRequest apiResponse, String parkId, String semana) {
        // Convertir centavos a pesos
        BigDecimal efectivo = convertirCentavosAPesos(apiResponse.getTarifas());
        BigDecimal pagoSinEfectivo = convertirCentavosAPesos(apiResponse.getPagoSinEfectivo());
        BigDecimal comYango = convertirCentavosAPesos(apiResponse.getComYango());
        BigDecimal comYego = convertirCentavosAPesos(apiResponse.getComYego());
        BigDecimal boSemAnt = convertirCentavosAPesos(apiResponse.getBoSemAnt());
        BigDecimal boSemAct = convertirCentavosAPesos(apiResponse.getBoSemAct());

        // Calcular total según Google Sheets: efectivo + pagoSinEfectivo + comYango + comYego - boSemAnt + boSemAct
        BigDecimal total = efectivo.add(pagoSinEfectivo).add(comYango).add(comYego).subtract(boSemAnt).add(boSemAct);
        
        // Calcular garantizado basado en rangos de boSemAct
        BigDecimal garantizado = calcularGarantizadoSegunBoSemAct(boSemAct, parkId);
        
        // Calcular diferencia (garantizado - total)
        BigDecimal diferencia = garantizado.subtract(total);
        
        // Determinar valor de garantizado
        String garantizadoValor = diferencia.compareTo(BigDecimal.ZERO) >= 0 ? "Garantizado" : "No Garantizado";
        
        // Obtener datos del conductor desde la tabla drivers
        String[] datosConductor = obtenerDatosConductor(apiResponse.getLicencia());
        String nombreCompleto = datosConductor[0];
        String telefono = datosConductor[1];
        
        // Guardar en la tabla yego_garantizado
        YegoGarantizado yegoGarantizado = new YegoGarantizado();
        yegoGarantizado.setNombreCompleto(nombreCompleto);
        yegoGarantizado.setNumeroLicencia(apiResponse.getLicencia());
        yegoGarantizado.setTelefono(telefono);
        yegoGarantizado.setViajes(apiResponse.getViajes());
        yegoGarantizado.setEfectivo(efectivo);
        yegoGarantizado.setPagoSinEfectivo(pagoSinEfectivo);
        yegoGarantizado.setComYango(comYango);
        yegoGarantizado.setComYego(comYego);
        yegoGarantizado.setBoSemAnt(boSemAnt);
        yegoGarantizado.setBoSemAct(boSemAct);
        yegoGarantizado.setTotal(total);
        yegoGarantizado.setGarantizado(garantizado);
        yegoGarantizado.setDiferencia(diferencia);
        yegoGarantizado.setSemana(semana);
        yegoGarantizado.setViajesActuales(apiResponse.getViajes());
        yegoGarantizado.setFlotaId(parkId);
        yegoGarantizado.setGarantizadoValor(garantizadoValor);
        yegoGarantizado.setActivo(true);
        
        YegoGarantizado savedGarantizado = yegoGarantizadoRepository.save(yegoGarantizado);
        log.info("✅ [ExternalApiService] Datos guardados en yego_garantizado_dev con ID: {}", savedGarantizado.getId());
        
        return savedGarantizado;
    }
    
    private BigDecimal convertirCentavosAPesos(int centavos) {
        return new BigDecimal(centavos).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calcularGarantizadoSegunBoSemAct(BigDecimal boSemAct, String parkId) {
        try {
            // Convertir BigDecimal a int para determinar el rango
            int boSemActValue = boSemAct.intValue();
            
            log.info("🔢 [ExternalApiService] boSemAct: {}, Valor calculado: {}, FlotaId: {}", boSemAct, boSemActValue, parkId);
            
            // Determinar si es Perú o extranjero basándose en los comentarios de FlotaServiceImpl
            boolean esPeruano = esFlotaPeruana(parkId);
            String moneda = esPeruano ? "Soles Peruanos" : "Pesos Colombianos";
            log.info("💰 [ExternalApiService] Moneda detectada: {} (Flota: {})", moneda, parkId);
            
            // Aplicar la lógica de rangos según el Google Sheets
            BigDecimal garantizadoSoles;
            if (boSemActValue >= 0 && boSemActValue <= 50) {
                garantizadoSoles = new BigDecimal("285.00");
            } else if (boSemActValue >= 51 && boSemActValue <= 100) {
                garantizadoSoles = new BigDecimal("520.00");
            } else if (boSemActValue >= 101 && boSemActValue <= 150) {
                garantizadoSoles = new BigDecimal("750.00");
            } else if (boSemActValue >= 151 && boSemActValue <= 200) {
                garantizadoSoles = new BigDecimal("950.00");
            } else if (boSemActValue >= 201 && boSemActValue <= 250) {
                garantizadoSoles = new BigDecimal("1250.00");
            } else {
                garantizadoSoles = new BigDecimal("1450.00");
            }
            
            // Si es extranjero (Colombia), convertir de soles a pesos colombianos
            if (!esPeruano) {
                // Tasa de cambio: 1 sol = 1 sol (usar valores peruanos por ahora)
                BigDecimal tasaCambio = new BigDecimal("1.00");
                BigDecimal garantizadoPesos = garantizadoSoles.multiply(tasaCambio);
                log.info("🔄 [ExternalApiService] Conversión: {} soles → {} pesos colombianos", garantizadoSoles, garantizadoPesos);
                return garantizadoPesos;
            } else {
                // Si es Perú, devolver en soles
                return garantizadoSoles;
            }
            
        } catch (Exception e) {
            log.error("❌ [ExternalApiService] Error calculando garantizado para boSemAct {}: {}", boSemAct, e.getMessage());
            return new BigDecimal("1450.00"); // Valor por defecto
        }
    }
    
    private boolean esFlotaPeruana(String parkId) {
        // Flotas peruanas según los comentarios en FlotaServiceImpl
        return "56e4607dfc354e0a9cde4f0aa7973003".equals(parkId) || // Yego Arequipa
               "c054c8b5dfe14e75b882943b2a252706".equals(parkId) || // Yego Black
               "c58110bc70244430a70a8126fc69f22c".equals(parkId) || // Yego Líderes
               "5921e55cc5d042d28747dd722608955a".equals(parkId) || // Yego Prime
               "ff424287c4bd4cbba6066962951a121f".equals(parkId) || // Yego Promi
               "851e30755bba4d298e2e837f571b4ab8".equals(parkId) || // Yego Trujillo
               "ae57aaedeacd41eb9fdbe1ff7a89a3f2".equals(parkId) || // Yego,
               "2e39f6699c854bc49cc75197431fe25c".equals(parkId);   // Yego.
    }

    /**
     * Obtiene los datos del conductor (nombre y teléfono) desde la tabla drivers en una sola consulta
     */
    private String[] obtenerDatosConductor(String numeroLicencia) {
        try {
            log.info("🔍 [ExternalApiService] Consultando datos del conductor para licencia: {}", numeroLicencia);
            
            String sql = "SELECT full_name, phone FROM drivers WHERE license_number = ?";
            Object[] resultado = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                String nombre = rs.getString("full_name");
                String telefono = rs.getString("phone");
                return new Object[]{nombre, telefono};
            }, numeroLicencia);
            
            String nombreCompleto = (String) resultado[0];
            String telefono = (String) resultado[1];
            
            // Validar y asignar fallbacks
            if (nombreCompleto == null || nombreCompleto.trim().isEmpty()) {
                nombreCompleto = "Conductor " + numeroLicencia;
                log.warn("[ExternalApiService] No se encontro nombre para licencia: {}", numeroLicencia);
            } else {
                log.info("[ExternalApiService] Nombre encontrado: {}", nombreCompleto);
            }
            
            if (telefono == null || telefono.trim().isEmpty()) {
                telefono = "N/A";
                log.warn("[ExternalApiService] No se encontro teléfono para licencia: {}", numeroLicencia);
            } else {
                log.info("[ExternalApiService] Teléfono encontrado: {}", telefono);
            }
            
            return new String[]{nombreCompleto, telefono};
            
        } catch (Exception e) {
            log.error("❌ [ExternalApiService] Error consultando datos del conductor {}: {}", numeroLicencia, e.getMessage());
            return new String[]{"Conductor " + numeroLicencia, "N/A"};
        }
    }
}
