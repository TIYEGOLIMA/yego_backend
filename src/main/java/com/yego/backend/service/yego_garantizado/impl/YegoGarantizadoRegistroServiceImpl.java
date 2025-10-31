package com.yego.backend.service.yego_garantizado.impl;

import com.yego.backend.entity.yego_garantizado.api.response.GarantizadoListResponse;
import com.yego.backend.entity.yego_garantizado.api.response.GarantizadoResponse;
import com.yego.backend.entity.yego_garantizado.api.response.RegistroCompletoResponse;
import com.yego.backend.entity.yego_garantizado.entities.YegoGarantizado;
import com.yego.backend.entity.yego_garantizado.entities.Registro;
import com.yego.backend.repository.yego_garantizado.YegoGarantizadoRegistroRepository;
import com.yego.backend.repository.yego_garantizado.YegoGarantizadoRepository;
import com.yego.backend.service.yego_garantizado.ExternalApiService;
import com.yego.backend.service.yego_garantizado.YegoGarantizadoRegistroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class YegoGarantizadoRegistroServiceImpl implements YegoGarantizadoRegistroService {

    private final YegoGarantizadoRegistroRepository yegoGarantizadoRegistroRepository;
    private final YegoGarantizadoRepository yegoGarantizadoRepository;
    private final ExternalApiService externalApiService;

    @Override
    @Transactional
    public List<YegoGarantizado> procesarConductoresPorSemana(String semana) {
        log.info("🌐 [YegoGarantizadoRegistroService] Procesando conductores de la semana: {}", semana);

        try {
            // Obtener todos los conductores de la semana específica
            List<Registro> conductores = yegoGarantizadoRegistroRepository.findByYegSemana(semana);
            List<YegoGarantizado> resultados = new ArrayList<>();

            log.info("📋 [YegoGarantizadoRegistroService] Encontrados {} conductores para la semana {}", conductores.size(), semana);

            for (Registro conductor : conductores) {
                try {
                    log.info("🔄 [YegoGarantizadoRegistroService] Procesando conductor: {} de la semana {}", conductor.getYegLicenciaNumero(), semana);


                    // Consumir API externa para cada conductor
                    YegoGarantizado response = externalApiService.procesarConductor(conductor.getYegLicenciaNumero(), conductor.getYegFlota(), semana);

                    if (response != null) {
                        // Guardar el resultado procesado en la base de datos
                        YegoGarantizado guardado = yegoGarantizadoRepository.save(response);
                        resultados.add(guardado);
                        log.info(" [YegoGarantizadoRegistroService] Conductor {} de la semana {} procesado y guardado exitosamente", conductor.getYegLicenciaNumero(), semana);
                    } else {
                        log.warn(" [YegoGarantizadoRegistroService] No se pudo procesar conductor: {} de la semana {}", conductor.getYegLicenciaNumero(), semana);
                    }

                } catch (Exception e) {
                    log.error("[YegoGarantizadoRegistroService] Error procesando conductor {} de la semana {}: {}", conductor.getYegLicenciaNumero(), semana, e.getMessage());
                }
            }

            log.info("[YegoGarantizadoRegistroService] Procesamiento de semana {} completado. {} conductores procesados exitosamente", semana, resultados.size());
            return resultados;
        } catch (Exception e) {
            log.error(" [YegoGarantizadoRegistroService] Error procesando conductores de la semana {}: {}", semana, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public GarantizadoListResponse obtenerGarantizadosPorFlota(String flotaId) {
        log.info("📋 [YegoGarantizadoRegistroService] Obteniendo garantizados por flota: {}", flotaId);
        
        try {
            // Obtener la semana anterior
            String semanaAnterior = obtenerSemanaAnterior();
            
            // Filtrar por flota Y semana anterior
            List<YegoGarantizado> garantizados = yegoGarantizadoRepository.findByFlotaIdAndSemanaAndActivoTrue(flotaId, semanaAnterior);
            List<GarantizadoResponse> conductores = garantizados.stream()
                    .map(this::convertirAGarantizadoResponse)
                    .collect(Collectors.toList());
            
            // Obtener también la semana actual
            String semanaActual = obtenerSemanaActual();
            
            // Calcular total de diferencias solo de conductores garantizados
            BigDecimal totalDiferenciaGarantizados = calcularTotalDiferenciaGarantizados(conductores);
            
            GarantizadoListResponse response = GarantizadoListResponse.builder()
                    .semanaAnterior(semanaAnterior)
                    .semanaActual(semanaActual)
                    .conductores(conductores)
                    .totalDiferenciaGarantizados(totalDiferenciaGarantizados)
                    .build();
            
            log.info(" [YegoGarantizadoRegistroService] Encontrados {} garantizados para flota {} de la semana {}", conductores.size(), flotaId, semanaAnterior);
            return response;
        } catch (Exception e) {
            log.error(" [YegoGarantizadoRegistroService] Error obteniendo garantizados por flota {}: {}", flotaId, e.getMessage());
            return GarantizadoListResponse.builder()
                    .semanaAnterior("SEMANA0")
                    .semanaActual("SEMANA0")
                    .conductores(new ArrayList<>())
                    .totalDiferenciaGarantizados(BigDecimal.ZERO)
                    .build();
        }
    }

    @Override
    public GarantizadoListResponse procesarYDevolverSemanaAnterior() {
        log.info("🌐 [YegoGarantizadoRegistroService] Procesando y devolviendo semana anterior");
        
        try {
            // Obtener la semana anterior
            String semanaAnterior = obtenerSemanaAnterior();
            
            // Procesar los conductores de la semana anterior
            procesarConductoresPorSemana(semanaAnterior);

            // Luego obtener solo los garantizados de la semana anterior
            List<YegoGarantizado> garantizados = yegoGarantizadoRepository.findBySemanaAndActivoTrue(semanaAnterior);
            List<GarantizadoResponse> conductores = garantizados.stream()
                    .map(this::convertirAGarantizadoResponse)
                    .collect(Collectors.toList());
            
            // Obtener también la semana actual
            String semanaActual = obtenerSemanaActual();
            
            // Calcular total de diferencias solo de conductores garantizados
            BigDecimal totalDiferenciaGarantizados = calcularTotalDiferenciaGarantizados(conductores);
            
            GarantizadoListResponse response = GarantizadoListResponse.builder()
                    .semanaAnterior(semanaAnterior)
                    .semanaActual(semanaActual)
                    .conductores(conductores)
                    .totalDiferenciaGarantizados(totalDiferenciaGarantizados)
                    .build();
            
            log.info("[YegoGarantizadoRegistroService] Procesados y devueltos {} conductores de la semana anterior", conductores.size());
            return response;
        } catch (Exception e) {
            log.error(" YegoGarantizadoRegistroService] Error procesando semana anterior: {}", e.getMessage());
            return GarantizadoListResponse.builder()
                    .semanaAnterior("SEMANA0")
                    .semanaActual("SEMANA0")
                    .conductores(new ArrayList<>())
                    .totalDiferenciaGarantizados(BigDecimal.ZERO)
                    .build();
        }
    }

    @Override
    public GarantizadoListResponse listarGarantizadosSemanaAnterior() {
        log.info("📋 [YegoGarantizadoRegistroService] Listando garantizados de la semana anterior");
        
        try {
            // Obtener la semana anterior
            String semanaAnterior = obtenerSemanaAnterior();
            
            // Solo obtener los garantizados de la semana anterior (SIN procesar)
            List<YegoGarantizado> garantizados = yegoGarantizadoRepository.findBySemanaAndActivoTrue(semanaAnterior);
            List<GarantizadoResponse> conductores = garantizados.stream()
                    .map(this::convertirAGarantizadoResponse)
                    .collect(Collectors.toList());
            
            // Obtener también la semana actual
            String semanaActual = obtenerSemanaActual();
            
            // Calcular total de diferencias solo de conductores garantizados
            BigDecimal totalDiferenciaGarantizados = calcularTotalDiferenciaGarantizados(conductores);
            
            GarantizadoListResponse response = GarantizadoListResponse.builder()
                    .semanaAnterior(semanaAnterior)
                    .semanaActual(semanaActual)
                    .conductores(conductores)
                    .totalDiferenciaGarantizados(totalDiferenciaGarantizados)
                    .build();
            
            log.info("✅ [YegoGarantizadoRegistroService] Listados {} conductores de la semana anterior", conductores.size());
            return response;
        } catch (Exception e) {
            log.error("❌ [YegoGarantizadoRegistroService] Error listando garantizados de la semana anterior: {}", e.getMessage());
            return GarantizadoListResponse.builder()
                    .semanaAnterior("SEMANA0")
                    .semanaActual("SEMANA0")
                    .conductores(new ArrayList<>())
                    .totalDiferenciaGarantizados(BigDecimal.ZERO)
                    .build();
        }
    }

    @Override
    public List<RegistroCompletoResponse> obtenerRegistrosSemanaActualCompletos() {
        log.info("📋 [YegoGarantizadoRegistroService] Obteniendo registros completos de la semana actual");
        
        try {
            // Calcular la semana actual
            String semanaActualStr = obtenerSemanaActual();
            log.info("📅 [YegoGarantizadoRegistroService] Semana actual: {}", semanaActualStr);
            
            // Usar consulta SQL nativa optimizada del repositorio
            List<Object[]> resultados = yegoGarantizadoRegistroRepository.findRegistrosCompletosBySemana(semanaActualStr);
            log.info("🔍 [YegoGarantizadoRegistroService] Encontrados {} registros completos", resultados.size());
            
            if (resultados.isEmpty()) {
                log.info("ℹ️ [YegoGarantizadoRegistroService] No hay registros para la semana actual");
                return new ArrayList<>();
            }
            
            List<RegistroCompletoResponse> registrosCompletos = new ArrayList<>();
            
            for (Object[] fila : resultados) {
                String flotaId = fila[2].toString();
                String nombreFlota = externalApiService.obtenerNombreFlota(flotaId);
                
                // Ya viene como Timestamp desde la consulta SQL
                LocalDateTime fechaRegistro = ((Timestamp) fila[1]).toLocalDateTime();
                
                RegistroCompletoResponse registroCompleto = RegistroCompletoResponse.builder()
                        .yegLicenciaNumero(fila[0].toString())  // Licencia (índice 0)
                        .yegFechaRegistro(fechaRegistro)  // Hora de registro (índice 1)
                        .yegFlota(flotaId)  // yegoFlota (índice 2)
                        .flotaNombre(nombreFlota)  // Nombre real de la flota desde API
                        .yegSemana(fila[3].toString())  // Semana (índice 3)
                        .build();
                
                registrosCompletos.add(registroCompleto);
            }
            
            log.info("✅ [YegoGarantizadoRegistroService] Encontrados {} registros completos para la semana {}", 
                    registrosCompletos.size(), semanaActualStr);
            
            return registrosCompletos;
            
        } catch (Exception e) {
            log.error("❌ [YegoGarantizadoRegistroService] Error obteniendo registros completos: {}", e.getMessage());
            return new ArrayList<>();
        }
    }




    @Override
    public byte[] exportarExcel(String flotaId, String estado, String semana) {
        log.info("📊 [YegoGarantizadoRegistroService] Exportando Excel - flotaId: {}, estado: {}, semana: {}", flotaId, estado, semana);
        
        try {
            GarantizadoListResponse response;
            
            if (flotaId != null && !flotaId.isEmpty()) {
                // Filtrar por flota específica
                response = obtenerGarantizadosPorFlota(flotaId);
            } else {
                // Obtener todos los datos de la semana anterior directamente de queue_garantizado
                List<YegoGarantizado> garantizados = yegoGarantizadoRepository.findBySemanaAndActivoTrue(semana);
                List<GarantizadoResponse> conductores = garantizados.stream()
                        .map(this::convertirAGarantizadoResponse)
                        .collect(Collectors.toList());

                String semanaActual = obtenerSemanaActual();
                
                // Calcular total de diferencias solo de conductores garantizados
                BigDecimal totalDiferenciaGarantizados = calcularTotalDiferenciaGarantizados(conductores);
                
                response = GarantizadoListResponse.builder()
                        .semanaAnterior(semana)
                        .semanaActual(semanaActual)
                        .conductores(conductores)
                        .totalDiferenciaGarantizados(totalDiferenciaGarantizados)
                        .build();
            }
            
            return generarExcel(response);
        } catch (Exception e) {
            log.error("❌ [YegoGarantizadoRegistroService] Error exportando Excel: {}", e.getMessage());
            return new byte[0];
        }
    }

    private byte[] generarExcel(GarantizadoListResponse response) {
        log.info("📊 [YegoGarantizadoRegistroService] Generando Excel para {} conductores", response.getConductores().size());
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Garantizados");
            
            // Crear estilo para el encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // Crear fila de encabezado
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "ID", "Nombre Completo", "Número de Licencia", "Teléfono", "Viajes",
                "Efectivo", "Pago Sin Efectivo", "Comisión Yango", "Comisión Yego",
                "Bono Semana Anterior", "Bono Semana Actual", "Total", "Garantizado",
                "Diferencia", "Semana", "Viajes Actuales", "Flota ID", "Garantizado Valor"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Llenar datos
            int rowNum = 1;
            for (GarantizadoResponse conductor : response.getConductores()) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(conductor.getId() != null ? conductor.getId().toString() : "");
                row.createCell(1).setCellValue(conductor.getNombreCompleto() != null ? conductor.getNombreCompleto() : "");
                row.createCell(2).setCellValue(conductor.getNumeroLicencia() != null ? conductor.getNumeroLicencia() : "");
                row.createCell(3).setCellValue(conductor.getTelefono() != null ? conductor.getTelefono() : "");
                row.createCell(4).setCellValue(conductor.getViajes() != null ? conductor.getViajes().toString() : "");
                row.createCell(5).setCellValue(conductor.getEfectivo() != null ? conductor.getEfectivo().toString() : "");
                row.createCell(6).setCellValue(conductor.getPagoSinEfectivo() != null ? conductor.getPagoSinEfectivo().toString() : "");
                row.createCell(7).setCellValue(conductor.getComYango() != null ? conductor.getComYango().toString() : "");
                row.createCell(8).setCellValue(conductor.getComYego() != null ? conductor.getComYego().toString() : "");
                row.createCell(9).setCellValue(conductor.getBoSemAnt() != null ? conductor.getBoSemAnt().toString() : "");
                row.createCell(10).setCellValue(conductor.getBoSemAct() != null ? conductor.getBoSemAct().toString() : "");
                row.createCell(11).setCellValue(conductor.getTotal() != null ? conductor.getTotal().toString() : "");
                row.createCell(12).setCellValue(conductor.getGarantizado() != null ? conductor.getGarantizado().toString() : "");
                row.createCell(13).setCellValue(conductor.getDiferencia() != null ? conductor.getDiferencia().toString() : "");
                row.createCell(14).setCellValue(conductor.getSemana() != null ? conductor.getSemana() : "");
                row.createCell(15).setCellValue(conductor.getViajesActuales() != null ? conductor.getViajesActuales().toString() : "");
                row.createCell(16).setCellValue(conductor.getFlotaId() != null ? conductor.getFlotaId() : "");
                row.createCell(17).setCellValue(conductor.getGarantizadoValor() != null ? conductor.getGarantizadoValor() : "");
            }
            
            // Ajustar ancho de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Convertir a byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            log.info("✅ [YegoGarantizadoRegistroService] Excel generado exitosamente");
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("❌ [YegoGarantizadoRegistroService] Error generando Excel: {}", e.getMessage());
            return new byte[0];
        }
    }

    @Override
    @Transactional
    public boolean marcarComoPagado(Long id, Authentication authentication) {
        log.info("💰 [YegoGarantizadoRegistroService] Marcando como pagado el registro ID: {}", id);
        
        try {
            // Extraer ID del usuario autenticado
            Long usuarioId = Long.parseLong(authentication.getName());
            log.debug("🔍 [YegoGarantizadoRegistroService] Usuario autenticado ID: {}", usuarioId);
            
            YegoGarantizado garantizado = yegoGarantizadoRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado con ID: " + id));
            
            // Solo se puede marcar como pagado si está garantizado
            if (!"Garantizado".equals(garantizado.getGarantizadoValor())) {
                log.warn("⚠️ [YegoGarantizadoRegistroService] No se puede marcar como pagado - Estado: {}", garantizado.getGarantizadoValor());
                return false;
            }
            
            garantizado.setEstadoPago("Pagado");
            garantizado.setUsuarioPagoId(usuarioId);
            yegoGarantizadoRepository.save(garantizado);
            
            log.info("✅ [YegoGarantizadoRegistroService] Registro {} marcado como pagado por usuario {}", id, usuarioId);
            return true;
            
        } catch (Exception e) {
            log.error("❌ [YegoGarantizadoRegistroService] Error marcando como pagado: {}", e.getMessage());
            return false;
        }
    }

    
    /**
     * Calcula la semana anterior del año
     */
    private String obtenerSemanaAnterior() {
        LocalDateTime ahora = LocalDateTime.now();
        int semanaActual = ahora.get(java.time.temporal.WeekFields.ISO.weekOfYear());
        int semanaAnterior = semanaActual - 1;
        
        // Si estamos en la semana 1, la semana anterior es la última semana del año anterior
        if (semanaAnterior <= 0) {
            semanaAnterior = 52; // Asumimos que el año anterior tenía 52 semanas
        }
        
        return "SEMANA" + semanaAnterior;
    }

    /**
     * Calcula la semana actual del año
     */
    private String obtenerSemanaActual() {
        LocalDateTime ahora = LocalDateTime.now();
        int semanaActual = ahora.get(java.time.temporal.WeekFields.ISO.weekOfYear());
        return "SEMANA" + semanaActual;
    }

    /**
     * Convierte YegoGarantizado a GarantizadoResponse usando BeanUtils
     */
    private GarantizadoResponse convertirAGarantizadoResponse(YegoGarantizado garantizado) {
        GarantizadoResponse response = new GarantizadoResponse();
        BeanUtils.copyProperties(garantizado, response);
        return response;
    }

    /**
     * Calcula el total de diferencias solo de conductores con estado "Garantizado"
     */
    private BigDecimal calcularTotalDiferenciaGarantizados(List<GarantizadoResponse> conductores) {
        return conductores.stream()
                .filter(conductor -> "Garantizado".equals(conductor.getGarantizadoValor()))
                .map(GarantizadoResponse::getDiferencia)
                .filter(diferencia -> diferencia != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
