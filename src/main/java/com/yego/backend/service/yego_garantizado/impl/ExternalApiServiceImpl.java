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
        // Validar que tenga viajes mínimos - Solo 50+ viajes son procesados
        if (apiResponse.getViajes() < 50) {
            log.info("⏭️ [ExternalApiService] Conductor no procesado - Viajes: {} (mínimo requerido: 50)", apiResponse.getViajes());
            return null;
        }
        
        // Los valores ya vienen en pesos, no necesitan conversión
        BigDecimal efectivo = new BigDecimal(apiResponse.getTarifas());
        BigDecimal pagoSinEfectivo = new BigDecimal(apiResponse.getPagoSinEfectivo());
        BigDecimal comYango = new BigDecimal(apiResponse.getComYango());
        BigDecimal comYego = new BigDecimal(apiResponse.getComYego());
        BigDecimal boSemAnt = new BigDecimal(apiResponse.getBoSemAnt());
        BigDecimal boSemAct = new BigDecimal(apiResponse.getBoSemAct());

        // Calcular total según Google Sheets: efectivo + pagoSinEfectivo + comYango + comYego - boSemAnt + boSemAct
        BigDecimal total = efectivo.add(pagoSinEfectivo).add(comYango).add(comYego).subtract(boSemAnt).add(boSemAct);
        
        // Calcular garantizado basado en VIAJES (restricción) y BONO SEMANA ACTUAL según tabla
        BigDecimal garantizado = calcularGarantizadoSegunBoSemAct(boSemAct, parkId, apiResponse.getViajes());
        
        // Calcular diferencia (garantizado - total)
        BigDecimal diferencia = garantizado.subtract(total);
        
        // Determinar valor de garantizado
        String garantizadoValor = diferencia.compareTo(BigDecimal.ZERO) >= 0 ? "Garantizado" : "No Garantizado";
        
        // Obtener datos del conductor desde la tabla drivers
        String[] datosConductor = obtenerDatosConductor(apiResponse.getLicencia(), parkId);
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
        
        // Inicializar estado de pago: "No Pagado" solo para conductores garantizados
        if ("Garantizado".equals(garantizadoValor)) {
            yegoGarantizado.setEstadoPago("No Pagado");
        } else {
            yegoGarantizado.setEstadoPago("N/A"); // No aplica para no garantizados
        }
        
        YegoGarantizado savedGarantizado = yegoGarantizadoRepository.save(yegoGarantizado);
        log.info("✅ [ExternalApiService] Datos guardados en yego_garantizado_dev con ID: {}", savedGarantizado.getId());
        
        return savedGarantizado;
    }
    
    /**
     * Calcula el garantizado basado en VIAJES (restricción) y BONO SEMANA ACTUAL según tabla
     * 
     * Tabla para Lima:
     * - 140+ viajes + Bono S/.520 → Garantizamos S/.1,450
     * - 110+ viajes + Bono S/.415 → Garantizamos S/.1,225
     * - 90+ viajes + Bono S/.340 → Garantizamos S/.1,050
     * - 70+ viajes + Bono S/.265 → Garantizamos S/.825
     * - 50+ viajes + Bono S/.190 → Garantizamos S/.600
     * 
     * Tabla para Trujillo:
     * - 155+ viajes + Bono S/.350 → Garantizamos S/.1,450
     * - 125+ viajes + Bono S/.290 → Garantizamos S/.1,075
     * - 100+ viajes + Bono S/.235 → Garantizamos S/.950
     * - 75+ viajes + Bono S/.180 → Garantizamos S/.675
     * - 50+ viajes + Bono S/.125 → Garantizamos S/.475
     * 
     * Tabla para Arequipa:
     * - 155+ viajes + Bono S/.350 → Garantizamos S/.1,450
     * - 125+ viajes + Bono S/.290 → Garantizamos S/.1,075
     * - 100+ viajes + Bono S/.235 → Garantizamos S/.950
     * - 75+ viajes + Bono S/.180 → Garantizamos S/.675
     * - 50+ viajes + Bono S/.125 → Garantizamos S/.475
     */
    private BigDecimal calcularGarantizadoSegunBoSemAct(BigDecimal boSemAct, String parkId, Integer viajes) {
        try {
            log.info("📊 [ExternalApiService] Calculando garantizado - Viajes: {}, Bono: {}, Flota: {}", viajes, boSemAct, parkId);
            
            // Calcular garantizado según ubicación
            String ubicacion;
            BigDecimal garantizado;
            
            if (esFlotaTrujillo(parkId)) {
                ubicacion = "Trujillo";
                garantizado = calcularGarantizadoTrujillo(viajes, boSemAct);
            } else if (esFlotaArequipa(parkId)) {
                ubicacion = "Arequipa";
                garantizado = calcularGarantizadoArequipa(viajes, boSemAct);
            } else {
                ubicacion = "Lima";
                garantizado = calcularGarantizadoLima(viajes, boSemAct);
            }
            
            log.info("📍 [ExternalApiService] {} - Garantizado: S/.{}", ubicacion, garantizado);
            
            // Conversión para Colombia
            if (!esFlotaPeruana(parkId)) {
                return garantizado.multiply(new BigDecimal("1.00"));
            }
            
            return garantizado;
            
        } catch (Exception e) {
            log.error("❌ [ExternalApiService] Error calculando garantizado: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    
    private boolean esFlotaPeruana(String parkId) {
        // Flotas peruanas según los comentarios en FlotaServiceImpl
        return "56e4607dfc354e0a9cde4f0aa7973003".equals(parkId) || // Yego Arequipa
                "08e20910d81d42658d4334d3f6d10ac0".equals(parkId) || // Yego 
               "c054c8b5dfe14e75b882943b2a252706".equals(parkId) || // Yego Black
               "c58110bc70244430a70a8126fc69f22c".equals(parkId) || // Yego Líderes
               "5921e55cc5d042d28747dd722608955a".equals(parkId) || // Yego Prime
               "ff424287c4bd4cbba6066962951a121f".equals(parkId) || // Yego Promi
               "851e30755bba4d298e2e837f571b4ab8".equals(parkId) || // Yego Trujillo
               "ae57aaedeacd41eb9fdbe1ff7a89a3f2".equals(parkId) || // Yego,
               "2e39f6699c854bc49cc75197431fe25c".equals(parkId);   // Yego.
    }
    
    private boolean esFlotaTrujillo(String parkId) {
        return "851e30755bba4d298e2e837f571b4ab8".equals(parkId); // Yego Trujillo
    }
    
    private boolean esFlotaArequipa(String parkId) {
        return "56e4607dfc354e0a9cde4f0aa7973003".equals(parkId); // Yego Arequipa
    }
    
    /**
     * Calcula garantizado para Trujillo
     */
    private BigDecimal calcularGarantizadoTrujillo(Integer viajes, BigDecimal boSemAct) {
        int bono = boSemAct.intValue();
        
        if (viajes >= 195 && bono >= 350) {
            return new BigDecimal("1350.00");
        } else if (viajes >= 155 && bono >= 280) {
            return new BigDecimal("1125.00");
        } else if (viajes >= 125 && bono >= 235) {
            return new BigDecimal("875.00");
        } else if (viajes >= 95 && bono >= 185) {
            return new BigDecimal("675.00");
        } else if (viajes >= 65 && bono >= 130) {
            return new BigDecimal("475.00");
        } else {
            log.warn("⚠️ [Trujillo] No cumple criterios - Viajes: {}, Bono: S/.{}", viajes, bono);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Calcula garantizado para Arequipa
     */
    private BigDecimal calcularGarantizadoArequipa(Integer viajes, BigDecimal boSemAct) {
        int bono = boSemAct.intValue();
        
        if (viajes >= 155 && bono >= 350) {
            return new BigDecimal("1450.00");
        } else if (viajes >= 125 && bono >= 290) {
            return new BigDecimal("1075.00");
        } else if (viajes >= 100 && bono >= 235) {
            return new BigDecimal("950.00");
        } else if (viajes >= 75 && bono >= 180) {
            return new BigDecimal("675.00");
        } else if (viajes >= 50 && bono >= 125) {
            return new BigDecimal("475.00");
        } else {
            log.warn("⚠️ [Arequipa] No cumple criterios - Viajes: {}, Bono: S/.{}", viajes, bono);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Calcula garantizado para Lima
     */
    private BigDecimal calcularGarantizadoLima(Integer viajes, BigDecimal boSemAct) {
        int bono = boSemAct.intValue();
        
        if (viajes >= 140 && bono >= 520) {
            return new BigDecimal("1450.00");
        } else if (viajes >= 110 && bono >= 415) {
            return new BigDecimal("1225.00");
        } else if (viajes >= 90 && bono >= 340) {
            return new BigDecimal("1050.00");
        } else if (viajes >= 70 && bono >= 265) {
            return new BigDecimal("825.00");
        } else if (viajes >= 50 && bono >= 190) {
            return new BigDecimal("600.00");
        } else {
            log.warn("⚠️ [Lima] No cumple criterios - Viajes: {}, Bono: S/.{}", viajes, bono);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Obtiene el nombre de la flota desde mapeo local (más confiable que API externa)
     */
    public String obtenerNombreFlota(String flotaId) {
        log.info("🌐 [ExternalApiService] Consultando nombre de flota para ID: {}", flotaId);
        
        // Usar mapeo local de flotas conocidas (más confiable)
        String nombreFlota = obtenerNombreFlotaLocal(flotaId);
        log.info("✅ [ExternalApiService] Nombre de flota: {}", nombreFlota);
        return nombreFlota;
    }
    
    /**
     * Mapeo local de flotas conocidas (basado en FlotaServiceImpl)
     */
    private String obtenerNombreFlotaLocal(String flotaId) {
        switch (flotaId) {
            case "05b1c831e66f41a9a87f5f3fa0a186ae":
                return "Yego Cali";
            case "08e20910d81d42658d4334d3f6d10ac0":
                return "Yego Lima";
            case "56e4607dfc354e0a9cde4f0aa7973003":
                return "Yego Arequipa";
            case "ef21f793358144f589aabcbeb8bd7d50":
                return "Yego Barranquilla";
            case "c054c8b5dfe14e75b882943b2a252706":
                return "Yego Black";
            case "c58110bc70244430a70a8126fc69f22c":
                return "Yego Líderes";
            case "5921e55cc5d042d28747dd722608955a":
                return "Yego Prime";
            case "ff424287c4bd4cbba6066962951a121f":
                return "Yego Promi";
            case "851e30755bba4d298e2e837f571b4ab8":
                return "Yego Trujillo";
            case "ae57aaedeacd41eb9fdbe1ff7a89a3f2":
                return "Yego,";
            case "2e39f6699c854bc49cc75197431fe25c":
                return "Yego.";
            default:
                return "Flota " + flotaId;
        }
    }
    
    /**
     * Obtiene los datos del conductor (nombre y teléfono) desde la tabla drivers en una sola consulta
     */
    private String[] obtenerDatosConductor(String numeroLicencia, String parkId) {
        try {
            log.info("🔍 [ExternalApiService] Consultando datos del conductor para licencia: {}", numeroLicencia);
            
            String sql = "SELECT full_name, phone FROM drivers WHERE license_number = ? AND park_id = ?";
            Object[] resultado = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                String nombre = rs.getString("full_name");
                String telefono = rs.getString("phone");
                return new Object[]{nombre, telefono};
            }, numeroLicencia, parkId);
            
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
