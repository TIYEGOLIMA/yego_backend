package com.yego.backend.service.yego_garantizado.impl;

import com.yego.backend.entity.yego_garantizado.api.request.GarantizadoRequest;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import com.yego.backend.entity.yego_garantizado.entities.CalculoGarantizado;
import com.yego.backend.entity.yego_garantizado.entities.YegoGarantizado;
import com.yego.backend.repository.yego_garantizado.CalculoGarantizadoRepository;
import com.yego.backend.repository.yego_garantizado.YegoGarantizadoRepository;
import com.yego.backend.service.yego_garantizado.ExternalApiService;
import com.yego.backend.service.yego_garantizado.FlotaService;
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
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalApiServiceImpl implements ExternalApiService {

    private final RestTemplate restTemplate;
    private final YegoGarantizadoRepository yegoGarantizadoRepository;
    private final JdbcTemplate jdbcTemplate;
    private final CalculoGarantizadoRepository calculoGarantizadoRepository;
    private final FlotaService flotaService;
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
        
        // Calcular garantizado basado en VIAJES, BONO y HORAS MÍNIMAS según tabla
        // PRIMERO intenta usar valores guardados del frontend, si no usa cálculo automático
        BigDecimal garantizado = calcularGarantizadoSegunBoSemAct(apiResponse, parkId, semana);
        
        // Si el garantizado es 0, significa que NO cumple con los criterios (viajes, bono, horas)
        // Omitir completamente el procesamiento - NO guardar en la base de datos
        if (garantizado == null || garantizado.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("⏭️ [ExternalApiService] Conductor OMITIDO - No cumple criterios mínimos (garantizado: {}) - Licencia: {}", 
                garantizado, apiResponse.getLicencia());
            return null; // NO procesar ni guardar
        }
        
        // Calcular diferencia (garantizado - total)
        BigDecimal diferencia = garantizado.subtract(total);
        
        // Determinar valor de garantizado y motivo de rechazo
        String garantizadoValor;
        String motivoRechazo;
        
        if (diferencia.compareTo(BigDecimal.ZERO) >= 0) {
            garantizadoValor = "Garantizado";
            motivoRechazo = "No hay motivo";
        } else {
            garantizadoValor = "No Garantizado";
            motivoRechazo = "Total supera garantizado (S/." + total + " > S/." + garantizado + ")";
        }
        
        // Obtener datos del conductor desde la tabla drivers
        String[] datosConductor = obtenerDatosConductor(apiResponse.getLicencia(), parkId);
        String nombreCompleto = datosConductor[0];
        String telefono = datosConductor[1];
        
        // Buscar si ya existe un registro para esta licencia, flota y semana
        Optional<YegoGarantizado> existenteOpt = yegoGarantizadoRepository.findByNumeroLicenciaAndFlotaIdAndSemana(
            apiResponse.getLicencia(), parkId, semana);
        
        YegoGarantizado yegoGarantizado;
        if (existenteOpt.isPresent()) {
            // Actualizar registro existente
            yegoGarantizado = existenteOpt.get();
            log.info("🔄 [ExternalApiService] Actualizando registro existente - ID: {}, Licencia: {}", 
                yegoGarantizado.getId(), apiResponse.getLicencia());
        } else {
            // Crear nuevo registro
            yegoGarantizado = new YegoGarantizado();
            log.info("✨ [ExternalApiService] Creando nuevo registro - Licencia: {}", apiResponse.getLicencia());
        }
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
        
        // Guardar horas trabajadas
        yegoGarantizado.setHorasTrabajadas(apiResponse.getHorasTrabajadas());
        yegoGarantizado.setHorasTrabajadasEntero(apiResponse.getHorasTrabajadasEntero());
        
        // Guardar motivo de rechazo si aplica
        yegoGarantizado.setMotivoRechazo(motivoRechazo);
        
        // Guardar brandeo
        yegoGarantizado.setBrandeo(apiResponse.isBrandeo());
        
        YegoGarantizado savedGarantizado = yegoGarantizadoRepository.save(yegoGarantizado);
        log.info("✅ [ExternalApiService] Datos guardados en yego_garantizado_dev con ID: {}", savedGarantizado.getId());
        
        return savedGarantizado;
    }
    
    /**
     * Calcula el garantizado basado en VIAJES, BONO y HORAS MÍNIMAS según tabla
     * PRIMERO intenta buscar valores guardados desde el frontend, si no existen usa cálculo automático
     */
    private BigDecimal calcularGarantizadoSegunBoSemAct(GarantizadoRequest apiResponse, String parkId, String semana) {
        try {
            Integer viajes = apiResponse.getViajes();
            BigDecimal boSemAct = new BigDecimal(apiResponse.getBoSemAct());
            Integer horasTrabajadas = apiResponse.getHorasTrabajadasEntero();
            boolean brandeo = apiResponse.isBrandeo();
            
            log.info("📊 [ExternalApiService] Calculando garantizado - Viajes: {}, Bono: {}, Horas: {}, Brandeo: {}, Flota: {}, Semana: {}", 
                viajes, boSemAct, horasTrabajadas, brandeo, parkId, semana);
            
            // Obtener país y ciudad desde el parkId
            String[] paisYCiudad = obtenerPaisYCiudadDesdeFlota(parkId);
            
            // Validar que se pudo obtener país y ciudad
            if (paisYCiudad == null || paisYCiudad.length < 2) {
                log.error("❌ [ExternalApiService] No se pudo obtener país/ciudad para parkId: {}", parkId);
                return BigDecimal.ZERO;
            }
            
            String pais = paisYCiudad[0];
            String ciudad = paisYCiudad[1];
            
            log.info("🌍 [ExternalApiService] País: {}, Ciudad: {}", pais, ciudad);
            
            // Consultar TODAS las configuraciones guardadas por el frontend para esta semana
            // Ahora hay registros separados: uno para conBrandeo y otro para sinBrandeo
            List<CalculoGarantizado> calculosGuardados = calculoGarantizadoRepository
                    .findAllByPaisAndCiudadAndSemana(pais.toLowerCase(), ciudad.toLowerCase(), semana);
            
            if (!calculosGuardados.isEmpty()) {
                log.info("📋 [ExternalApiService] Se encontraron {} registros de cálculo para {} - {} - {}", 
                    calculosGuardados.size(), pais, ciudad, semana);
                
                // Filtrar registros según el tipo de brandeo y evaluar TODOS para encontrar el que el conductor cumple
                List<CalculoGarantizado> registrosFiltrados;
                
                if (brandeo) {
                    // Si tiene brandeo, filtrar registros donde viajesConBrandeo > 0
                    registrosFiltrados = calculosGuardados.stream()
                            .filter(c -> c.getViajesConBrandeo() != null && c.getViajesConBrandeo() > 0)
                            .collect(java.util.stream.Collectors.toList());
                    log.info("📊 [ExternalApiService] Filtrando {} registros CON BRANDEO de {} totales", registrosFiltrados.size(), calculosGuardados.size());
                } else {
                    // Si NO tiene brandeo, filtrar registros donde viajesSinBrandeo > 0
                    registrosFiltrados = calculosGuardados.stream()
                            .filter(c -> c.getViajesSinBrandeo() != null && c.getViajesSinBrandeo() > 0)
                            .collect(java.util.stream.Collectors.toList());
                    log.info("📊 [ExternalApiService] Filtrando {} registros SIN BRANDEO de {} totales", registrosFiltrados.size(), calculosGuardados.size());
                }
                
                if (registrosFiltrados.isEmpty()) {
                    log.warn("⚠️ [ExternalApiService] Se encontraron {} registros pero ninguno corresponde al tipo de brandeo ({}) para semana {} en {} - {}", 
                        calculosGuardados.size(), brandeo, semana, pais, ciudad);
                    return BigDecimal.ZERO;
                }
                
                // Evaluar TODOS los registros filtrados para encontrar el que el conductor cumple
                BigDecimal mejorGarantizado = BigDecimal.ZERO;
                CalculoGarantizado calculoQueCumple = null;
                
                for (CalculoGarantizado calculo : registrosFiltrados) {
                    // Evaluar si este registro cumple los criterios
                    BigDecimal garantizadoEvaluado = calcularGarantizadoDesdeValoresGuardados(
                        viajes, boSemAct, horasTrabajadas, brandeo, 
                        calculo.getViajesConBrandeo(), calculo.getBonoConBrandeo(), 
                        calculo.getGarantizadoConBrandeo(), calculo.getHorasConBrandeo(), 
                        calculo.getViajesSinBrandeo(), calculo.getBonoSinBrandeo(), 
                        calculo.getGarantizadoSinBrandeo(), calculo.getHorasSinBrandeo()
                    );
                    
                    // Si este registro cumple los criterios y tiene un garantizado mayor
                    if (garantizadoEvaluado.compareTo(BigDecimal.ZERO) > 0 && 
                        garantizadoEvaluado.compareTo(mejorGarantizado) > 0) {
                        mejorGarantizado = garantizadoEvaluado;
                        calculoQueCumple = calculo;
                    }
                }
                
                if (calculoQueCumple != null && mejorGarantizado.compareTo(BigDecimal.ZERO) > 0) {
                    log.info("✅ [ExternalApiService] Usando garantizado guardado de semana {} (Brandeo: {}): S/.{}", 
                        semana, brandeo, mejorGarantizado);
                    return mejorGarantizado;
                } else {
                    log.warn("⚠️ [ExternalApiService] Se encontraron {} registros {} pero el conductor NO cumple criterios de ninguno", 
                        registrosFiltrados.size(), brandeo ? "CON BRANDEO" : "SIN BRANDEO");
                    return BigDecimal.ZERO;
                }
            } else {
                // No se encontraron valores guardados - retornar 0
                log.warn("⚠️ [ExternalApiService] No se encontraron valores guardados para semana {} en {} - {}", semana, pais, ciudad);
                return BigDecimal.ZERO;
            }
            
        } catch (Exception e) {
            log.error("❌ [ExternalApiService] Error calculando garantizado: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Calcula el garantizado usando los valores guardados desde el frontend
     * Compara viajes, bono y horas actuales vs guardadas para determinar cuál usar
     * Si brandeo=true, solo compara con conBrandeo; si brandeo=false, solo con sinBrandeo
     */
    private BigDecimal calcularGarantizadoDesdeValoresGuardados(Integer viajes, BigDecimal bono, Integer horasTrabajadas, boolean brandeo,
            Integer viajesConBrandeo, BigDecimal bonoConBrandeo, BigDecimal garantizadoConBrandeo, Integer horasConBrandeo,
            Integer viajesSinBrandeo, BigDecimal bonoSinBrandeo, BigDecimal garantizadoSinBrandeo, Integer horasSinBrandeo) {
        
        log.info("📊 [ExternalApiService] Comparando valores actuales vs guardados (Brandeo: {}):", brandeo);
        log.info("📊   Actual: Viajes={}, Bono={}, Horas={}", viajes, bono, horasTrabajadas);
        log.info("📊   ConBrandeo: Viajes={}, Bono={}, Horas={}, Garantizado={}", 
            viajesConBrandeo, bonoConBrandeo, horasConBrandeo, garantizadoConBrandeo);
        log.info("📊   SinBrandeo: Viajes={}, Bono={}, Horas={}, Garantizado={}", 
            viajesSinBrandeo, bonoSinBrandeo, horasSinBrandeo, garantizadoSinBrandeo);
        
        // Verificar si cumple criterios de conBrandeo (con logs detallados)
        boolean cumpleConBrandeo = false;
        String motivoNoCumpleConBrandeo = "";
        
        if (viajesConBrandeo == null || viajesConBrandeo <= 0) {
            motivoNoCumpleConBrandeo = "No hay configuración de viajes con brandeo";
        } else if (viajes < viajesConBrandeo) {
            motivoNoCumpleConBrandeo = String.format("Viajes insuficientes: %d < %d (requiere mínimo %d viajes)", 
                viajes, viajesConBrandeo, viajesConBrandeo);
        } else if (bono.compareTo(bonoConBrandeo) < 0) {
            motivoNoCumpleConBrandeo = String.format("Bono insuficiente: %.2f < %.2f (requiere mínimo %.2f)", 
                bono.doubleValue(), bonoConBrandeo.doubleValue(), bonoConBrandeo.doubleValue());
        } else if (horasTrabajadas < (horasConBrandeo != null ? horasConBrandeo : 0)) {
            motivoNoCumpleConBrandeo = String.format("Horas insuficientes: %d < %d (requiere mínimo %d horas)", 
                horasTrabajadas, horasConBrandeo != null ? horasConBrandeo : 0, horasConBrandeo != null ? horasConBrandeo : 0);
        } else {
            cumpleConBrandeo = true;
        }
        
        // Verificar si cumple criterios de sinBrandeo (con logs detallados)
        boolean cumpleSinBrandeo = false;
        String motivoNoCumpleSinBrandeo = "";
        
        if (viajesSinBrandeo == null || viajesSinBrandeo <= 0) {
            motivoNoCumpleSinBrandeo = "No hay configuración de viajes sin brandeo";
        } else if (viajes < viajesSinBrandeo) {
            motivoNoCumpleSinBrandeo = String.format("Viajes insuficientes: %d < %d (requiere mínimo %d viajes)", 
                viajes, viajesSinBrandeo, viajesSinBrandeo);
        } else if (bono.compareTo(bonoSinBrandeo) < 0) {
            motivoNoCumpleSinBrandeo = String.format("Bono insuficiente: %.2f < %.2f (requiere mínimo %.2f)", 
                bono.doubleValue(), bonoSinBrandeo.doubleValue(), bonoSinBrandeo.doubleValue());
        } else if (horasTrabajadas < (horasSinBrandeo != null ? horasSinBrandeo : 0)) {
            motivoNoCumpleSinBrandeo = String.format("Horas insuficientes: %d < %d (requiere mínimo %d horas)", 
                horasTrabajadas, horasSinBrandeo != null ? horasSinBrandeo : 0, horasSinBrandeo != null ? horasSinBrandeo : 0);
        } else {
            cumpleSinBrandeo = true;
        }
        
        // Si tiene brandeo, solo verifica conBrandeo
        if (brandeo) {
            if (cumpleConBrandeo) {
                log.info("✅ [ExternalApiService] Con brandeo, cumple TODOS los criterios: S/.{}", garantizadoConBrandeo);
                return garantizadoConBrandeo;
            } else {
                log.warn("⚠️ [ExternalApiService] Con brandeo pero NO cumple criterios: {}", motivoNoCumpleConBrandeo);
                return BigDecimal.ZERO;
            }
        } else {
            // Si NO tiene brandeo, solo verifica sinBrandeo
            if (cumpleSinBrandeo) {
                log.info("✅ [ExternalApiService] Sin brandeo, cumple TODOS los criterios: S/.{}", garantizadoSinBrandeo);
                return garantizadoSinBrandeo;
            } else {
                log.warn("⚠️ [ExternalApiService] Sin brandeo pero NO cumple criterios: {}", motivoNoCumpleSinBrandeo);
                return BigDecimal.ZERO;
            }
        }
    }
    
    /**
     * Obtiene país y ciudad desde el parkId (flota)
     * Retorna null si no se puede obtener (MEJOR PRÁCTICA: no usar fallback)
     */
    private String[] obtenerPaisYCiudadDesdeFlota(String parkId) {
        try {
            List<FlotaResponse> flotas = flotaService.obtenerFlotas();
            for (FlotaResponse flota : flotas) {
                if (flota.getId().equals(parkId)) {
                    String city = flota.getCity();
                    if (city != null && !city.isEmpty()) {
                        // Mapear city a pais según reglas conocidas
                        String pais = determinarPaisDesdeCiudad(city);
                        // Normalizar a minúsculas para coincidir con los valores guardados
                        log.info("🌍 [ExternalApiService] Flota: {}, Ciudad: {}, País: {}", parkId, city.toLowerCase(), pais);
                        return new String[]{pais, city.toLowerCase()};
                    }
                }
            }
            log.error("❌ [ExternalApiService] No se encontró flota con parkId: {}", parkId);
            return null;
        } catch (Exception e) {
            log.error("❌ [ExternalApiService] Error obteniendo ciudad desde flota: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Determina país desde la ciudad
     */
    private String determinarPaisDesdeCiudad(String ciudad) {
        String ciudadLower = ciudad.toLowerCase();
        if (ciudadLower.contains("cali") || ciudadLower.contains("barranquilla") || 
            ciudadLower.contains("bogota") || ciudadLower.contains("bucaramanga")) {
            return "colombia";
        } else {
            return "peru";
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
