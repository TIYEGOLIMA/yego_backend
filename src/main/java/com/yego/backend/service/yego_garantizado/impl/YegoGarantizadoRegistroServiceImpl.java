package com.yego.backend.service.yego_garantizado.impl;

import com.yego.backend.entity.yego_garantizado.api.response.GarantizadoListResponse;
import com.yego.backend.entity.yego_garantizado.api.response.GarantizadoResponse;
import com.yego.backend.entity.yego_garantizado.entities.YegoGarantizado;
import com.yego.backend.entity.yego_garantizado.entities.YegoGarantizadoRegistro;
import com.yego.backend.repository.yego_garantizado.YegoGarantizadoRegistroRepository;
import com.yego.backend.repository.yego_garantizado.YegoGarantizadoRepository;
import com.yego.backend.service.yego_garantizado.ExternalApiService;
import com.yego.backend.service.yego_garantizado.YegoGarantizadoRegistroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class YegoGarantizadoRegistroServiceImpl implements YegoGarantizadoRegistroService {

    private final YegoGarantizadoRegistroRepository yegoGarantizadoRegistroRepository;
    private final YegoGarantizadoRepository yegoGarantizadoRepository;
    private final ExternalApiService externalApiService;


    @Override
    public List<YegoGarantizado> procesarConductoresPorSemana(String semana) {
        log.info("🌐 [YegoGarantizadoRegistroService] Procesando conductores de la semana: {}", semana);

        try {
            // Obtener todos los conductores de la semana específica
            List<YegoGarantizadoRegistro> conductores = yegoGarantizadoRegistroRepository.findBySemana(semana);
            List<YegoGarantizado> resultados = new ArrayList<>();

            log.info("📋 [YegoGarantizadoRegistroService] Encontrados {} conductores para la semana {}", conductores.size(), semana);

            for (YegoGarantizadoRegistro conductor : conductores) {
                try {
                    log.info("🔄 [YegoGarantizadoRegistroService] Procesando conductor: {} de la semana {}", conductor.getLicenciaNumero(), semana);

                    // Usar flota directamente como String
                    String flotaId = conductor.getFlota();

                    // Consumir API externa para cada conductor
                    YegoGarantizado response = externalApiService.procesarConductor(conductor.getLicenciaNumero(), conductor.getFlota(), flotaId);

                    if (response != null) {
                        // Guardar el resultado procesado en la base de datos
                        YegoGarantizado guardado = yegoGarantizadoRepository.save(response);
                        resultados.add(guardado);
                        log.info("✅ [YegoGarantizadoRegistroService] Conductor {} de la semana {} procesado y guardado exitosamente", conductor.getLicenciaNumero(), semana);
                    } else {
                        log.warn("⚠️ [YegoGarantizadoRegistroService] No se pudo procesar conductor: {} de la semana {}", conductor.getLicenciaNumero(), semana);
                    }

                } catch (Exception e) {
                    log.error("❌ [YegoGarantizadoRegistroService] Error procesando conductor {} de la semana {}: {}", conductor.getLicenciaNumero(), semana, e.getMessage());
                }
            }

            log.info("🎉 [YegoGarantizadoRegistroService] Procesamiento de semana {} completado. {} conductores procesados exitosamente", semana, resultados.size());
            return resultados;

        } catch (Exception e) {
            log.error("❌ [YegoGarantizadoRegistroService] Error procesando conductores de la semana {}: {}", semana, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public GarantizadoListResponse obtenerGarantizadosPorFlota(String flotaId) {
        log.info("📋 [YegoGarantizadoRegistroService] Obteniendo garantizados por flota: {}", flotaId);
        
        try {
            List<YegoGarantizado> garantizados = yegoGarantizadoRepository.findByFlotaIdAndActivoTrue(flotaId);
            List<GarantizadoResponse> conductores = garantizados.stream()
                    .map(this::convertirAGarantizadoResponse)
                    .collect(Collectors.toList());
            
            String semanaActual = obtenerSemanaActual();
            
            GarantizadoListResponse response = GarantizadoListResponse.builder()
                    .semanaActual(semanaActual)
                    .conductores(conductores)
                    .build();
            
            log.info("✅ [YegoGarantizadoRegistroService] Encontrados {} garantizados para flota {}", conductores.size(), flotaId);
            return response;
        } catch (Exception e) {
            log.error("❌ [YegoGarantizadoRegistroService] Error obteniendo garantizados por flota {}: {}", flotaId, e.getMessage());
            return GarantizadoListResponse.builder()
                    .semanaActual("SEMANA0")
                    .conductores(new ArrayList<>())
                    .build();
        }
    }

    @Override
    public GarantizadoListResponse procesarYDevolverSemanaActual() {
        log.info("🌐 [YegoGarantizadoRegistroService] Procesando y devolviendo semana actual");
        
        try {
            // Primero procesar los conductores de la semana actual
            procesarSemanaActual();

            // Luego obtener todos los garantizados procesados
            List<YegoGarantizado> garantizados = yegoGarantizadoRepository.findByActivoTrue();
            List<GarantizadoResponse> conductores = garantizados.stream()
                    .map(this::convertirAGarantizadoResponse)
                    .collect(Collectors.toList());
            
            String semanaActual = obtenerSemanaActual();
            
            GarantizadoListResponse response = GarantizadoListResponse.builder()
                    .semanaActual(semanaActual)
                    .conductores(conductores)
                    .build();
            
            log.info("✅ [YegoGarantizadoRegistroService] Procesados y devueltos {} conductores", conductores.size());
            return response;
        } catch (Exception e) {
            log.error("❌ [YegoGarantizadoRegistroService] Error procesando semana actual: {}", e.getMessage());
            return GarantizadoListResponse.builder()
                    .semanaActual("SEMANA0")
                    .conductores(new ArrayList<>())
                    .build();
        }
    }


    /**
     * Convierte YegoGarantizado a GarantizadoResponse usando BeanUtils
     */
    private GarantizadoResponse convertirAGarantizadoResponse(YegoGarantizado garantizado) {
        GarantizadoResponse response = new GarantizadoResponse();
        BeanUtils.copyProperties(garantizado, response);
        return response;
    }

    @Override
    public GarantizadoListResponse procesarYDevolverSemanaAnterior() {
        log.info("🌐 [YegoGarantizadoRegistroService] Procesando y devolviendo semana anterior");
        
        try {
            // Obtener la semana anterior
            String semanaAnterior = obtenerSemanaAnterior();
            
            // Procesar los conductores de la semana anterior
            procesarConductoresPorSemana(semanaAnterior);

            // Luego obtener todos los garantizados procesados
            List<YegoGarantizado> garantizados = yegoGarantizadoRepository.findByActivoTrue();
            List<GarantizadoResponse> conductores = garantizados.stream()
                    .map(this::convertirAGarantizadoResponse)
                    .collect(Collectors.toList());
            
            GarantizadoListResponse response = GarantizadoListResponse.builder()
                    .semanaActual(semanaAnterior)
                    .conductores(conductores)
                    .build();
            
            log.info("✅ [YegoGarantizadoRegistroService] Procesados y devueltos {} conductores de la semana anterior", conductores.size());
            return response;
        } catch (Exception e) {
            log.error("❌ [YegoGarantizadoRegistroService] Error procesando semana anterior: {}", e.getMessage());
            return GarantizadoListResponse.builder()
                    .semanaActual("SEMANA0")
                    .conductores(new ArrayList<>())
                    .build();
        }
    }

    /**
     * Calcula la semana anterior del año
     */
    private String obtenerSemanaAnterior() {
        LocalDateTime ahora = LocalDateTime.now();
        int diaDelAnio = ahora.getDayOfYear();
        int semana = (diaDelAnio / 7) + 1;
        int semanaAnterior = semana - 1;
        return "SEMANA" + semanaAnterior;
    }

    /**
     * Calcula la semana actual del año
     */
    private String obtenerSemanaActual() {
        return "Semana " + (java.time.LocalDateTime.now().getDayOfYear() / 7 + 1);
    }

    /**
     * Procesa los conductores de la semana actual
     */
    @Override
    public List<YegoGarantizado> procesarSemanaActual() {
        return procesarConductoresPorSemana(obtenerSemanaActual());
    }

    @Override
    public byte[] exportarExcel(String flotaId, String estado, String semana) {
        log.info("📊 [YegoGarantizadoRegistroService] Exportando Excel - flotaId: {}, estado: {}, semana: {}", flotaId, estado, semana);

        try {
            // Obtener datos según los filtros
            GarantizadoListResponse response;
            
            if (flotaId != null && !flotaId.trim().isEmpty()) {
                // Filtrar por flota específica
                response = obtenerGarantizadosPorFlota(flotaId);
            } else {
                // Obtener todos los datos de la semana actual
                response = procesarYDevolverSemanaActual();
            }

            // Aplicar filtro por estado si se proporciona
            if (estado != null && !estado.trim().isEmpty() && !estado.equals("TODOS")) {
                List<GarantizadoResponse> conductoresFiltrados = response.getConductores().stream()
                        .filter(conductor -> conductor.getGarantizadoValor().equals(estado))
                        .collect(Collectors.toList());
                
                response = GarantizadoListResponse.builder()
                        .semanaActual(response.getSemanaActual())
                        .conductores(conductoresFiltrados)
                        .build();
            }

            // Generar Excel
            return generarExcel(response, semana);

        } catch (Exception e) {
            log.error("❌ [YegoGarantizadoRegistroService] Error exportando Excel: {}", e.getMessage());
            throw new RuntimeException("Error generando Excel", e);
        }
    }

    private byte[] generarExcel(GarantizadoListResponse response, String semana) {
        log.info("📊 [YegoGarantizadoRegistroService] Generando Excel para {} conductores", response.getConductores().size());
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Garantizado " + semana);
            
            // Crear estilos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            
            // Crear fila de encabezados
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "ID", "Nombre Completo", "Número de Licencia", "Teléfono", "Viajes",
                "Efectivo", "Pago Sin Efectivo", "Com. Yango", "Com. Yego",
                "Bono Sem. Ant.", "Bono Sem. Act.", "Total", "Garantizado",
                "Diferencia", "Semana", "Viajes Actuales", "Flota ID",
                "Estado Garantizado", "Fecha Creación", "Fecha Actualización"
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
                
                // ID
                createCell(row, 0, conductor.getId(), dataStyle);
                
                // Nombre Completo
                createCell(row, 1, conductor.getNombreCompleto(), dataStyle);
                
                // Número de Licencia
                createCell(row, 2, conductor.getNumeroLicencia(), dataStyle);
                
                // Teléfono
                createCell(row, 3, conductor.getTelefono(), dataStyle);
                
                // Viajes
                createCell(row, 4, conductor.getViajes(), dataStyle);
                
                // Efectivo
                createCell(row, 5, conductor.getEfectivo(), currencyStyle);
                
                // Pago Sin Efectivo
                createCell(row, 6, conductor.getPagoSinEfectivo(), currencyStyle);
                
                // Com. Yango
                createCell(row, 7, conductor.getComYango(), currencyStyle);
                
                // Com. Yego
                createCell(row, 8, conductor.getComYego(), currencyStyle);
                
                // Bono Sem. Ant.
                createCell(row, 9, conductor.getBoSemAnt(), currencyStyle);
                
                // Bono Sem. Act.
                createCell(row, 10, conductor.getBoSemAct(), currencyStyle);
                
                // Total
                createCell(row, 11, conductor.getTotal(), currencyStyle);
                
                // Garantizado
                createCell(row, 12, conductor.getGarantizado(), currencyStyle);
                
                // Diferencia
                createCell(row, 13, conductor.getDiferencia(), currencyStyle);
                
                // Semana
                createCell(row, 14, conductor.getSemana(), dataStyle);
                
                // Viajes Actuales
                createCell(row, 15, conductor.getViajesActuales(), dataStyle);
                
                // Flota ID
                createCell(row, 16, conductor.getFlotaId(), dataStyle);
                
                // Estado Garantizado
                createCell(row, 17, conductor.getGarantizadoValor(), dataStyle);
                
                // Fecha Creación
                createCell(row, 18, conductor.getFechaCreacion(), dateStyle);
                
                // Fecha Actualización
                createCell(row, 19, conductor.getFechaActualizacion(), dateStyle);
            }
            
            // Autoajustar columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Convertir a bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            log.info("✅ [YegoGarantizadoRegistroService] Excel generado exitosamente");
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            log.error("❌ [YegoGarantizadoRegistroService] Error generando Excel: {}", e.getMessage());
            throw new RuntimeException("Error generando Excel", e);
        }
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }
    
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("dd/mm/yyyy hh:mm"));
        return style;
    }
    
    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellStyle(style);
        
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue((LocalDateTime) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }
}