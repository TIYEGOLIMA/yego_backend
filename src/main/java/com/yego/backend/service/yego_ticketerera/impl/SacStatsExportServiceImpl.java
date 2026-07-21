package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse;
import com.yego.backend.service.yego_ticketerera.SacStatsExportService;
import com.yego.backend.service.yego_ticketerera.SacStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SacStatsExportServiceImpl implements SacStatsExportService {

    private static final DateTimeFormatter TRACE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    private final SacStatsService sacStatsService;
    
    @Override
    public ResponseEntity<byte[]> exportarAExcel(String fechaInicio, String fechaFin, Long sedeId) {
        log.info("Exportando estadísticas de SAC a Excel - Fecha inicio: {}, Fecha fin: {}, sedeId: {}",
                fechaInicio, fechaFin, sedeId);
        
        try {
            SacStatsResponse stats = sacStatsService.obtenerTodasLasEstadisticas(fechaInicio, fechaFin, sedeId);
            ByteArrayOutputStream excelData = generarExcel(stats, fechaInicio, fechaFin);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", obtenerNombreArchivoExcel(fechaInicio, fechaFin));
            
            log.info("Exportación a Excel completada exitosamente");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData.toByteArray());
                    
        } catch (Exception e) {
            log.error("Error al exportar a Excel: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @Override
    public ResponseEntity<byte[]> exportarAImagen(String formato, String fechaInicio, String fechaFin, Long sedeId) {
        log.info("Exportando estadísticas de SAC a imagen: {} - Fecha inicio: {}, Fecha fin: {}, sedeId: {}",
                formato, fechaInicio, fechaFin, sedeId);
        
        try {
            // Validar formato
            if (!esFormatoValido(formato)) {
                log.warn("Formato de imagen no válido: {}", formato);
                return ResponseEntity.badRequest().build();
            }
            
            SacStatsResponse stats = sacStatsService.obtenerTodasLasEstadisticas(fechaInicio, fechaFin, sedeId);
            ByteArrayOutputStream imageData = generarImagen(stats, formato, fechaInicio, fechaFin);
            
            HttpHeaders headers = new HttpHeaders();
            String mediaType = formato.equalsIgnoreCase("PNG") ? "image/png" : "image/jpeg";
            headers.setContentType(MediaType.parseMediaType(mediaType));
            headers.setContentDispositionFormData("attachment", obtenerNombreArchivoImagen(formato, fechaInicio, fechaFin));
            
            log.info("Exportación a imagen completada exitosamente");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageData.toByteArray());
                    
        } catch (Exception e) {
            log.error("Error al exportar a imagen: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private boolean esFormatoValido(String formato) {
        return formato != null && 
               (formato.equalsIgnoreCase("PNG") || 
                formato.equalsIgnoreCase("JPEG") || 
                formato.equalsIgnoreCase("JPG"));
    }
    
    private ByteArrayOutputStream generarExcel(SacStatsResponse stats, String fechaInicio, String fechaFin) {
        log.info("Exportando estadísticas de SAC a Excel");
        
        try (Workbook workbook = new XSSFWorkbook()) {
            // Crear hoja principal
            Sheet sheet = workbook.createSheet("Monitoreo Ticketera");
            
            // Crear estilos
            CellStyle headerStyle = crearEstiloEncabezado(workbook);
            CellStyle dataStyle = crearEstiloDatos(workbook);
            CellStyle numberStyle = crearEstiloNumero(workbook);
            
            int rowNum = 0;
            
            // Título principal
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("MONITOREO Y TRAZABILIDAD DE TICKETERA - YEGO");
            titleCell.setCellStyle(headerStyle);
            
            // Fecha de generación y rango de fechas (si se proporcionaron)
            Row dateRow = sheet.createRow(rowNum++);
            Cell dateCell = dateRow.createCell(0);
            String fechaTexto = "Fecha de generación: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            if (fechaInicio != null && fechaFin != null && !fechaInicio.isEmpty() && !fechaFin.isEmpty()) {
                fechaTexto += " | Período: " + fechaInicio + " al " + fechaFin;
            }
            dateCell.setCellValue(fechaTexto);
            dateCell.setCellStyle(dataStyle);
            
            rowNum++; // Espacio
            
            // Estadísticas generales
            crearSeccionEstadisticasGenerales(sheet, stats, rowNum, headerStyle, dataStyle, numberStyle);
            rowNum += 10;
            
            // Rendimiento por SAC
            crearSeccionRendimientoSac(sheet, stats.getSacPerformance(), rowNum, headerStyle, dataStyle, numberStyle);
            
            // Ajustar ancho de columnas
            ajustarAnchoColumnas(sheet);

            crearHojaTrazabilidad(workbook, stats.getTicketTraceability(), headerStyle, dataStyle, numberStyle);
            
            // Convertir a ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            log.info("Exportación a Excel completada exitosamente");
            return outputStream;
            
        } catch (IOException e) {
            log.error("Error al exportar a Excel: {}", e.getMessage());
            throw new RuntimeException("Error al generar archivo Excel", e);
        }
    }
    
    private ByteArrayOutputStream generarImagen(SacStatsResponse stats, String formato, String fechaInicio, String fechaFin) {
        log.info("Exportando estadísticas de SAC a imagen: {}", formato);
        
        try {
            BufferedImage image = crearImagenEstadisticas(stats, fechaInicio, fechaFin);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            if ("PNG".equalsIgnoreCase(formato)) {
                javax.imageio.ImageIO.write(image, "PNG", outputStream);
            } else if ("JPEG".equalsIgnoreCase(formato) || "JPG".equalsIgnoreCase(formato)) {
                javax.imageio.ImageIO.write(image, "JPEG", outputStream);
            } else {
                throw new IllegalArgumentException("Formato de imagen no soportado: " + formato);
            }
            
            log.info("Exportación a imagen completada exitosamente");
            return outputStream;
            
        } catch (IOException e) {
            log.error("Error al exportar a imagen: {}", e.getMessage());
            throw new RuntimeException("Error al generar imagen", e);
        }
    }
    
    private String obtenerNombreArchivoExcel(String fechaInicio, String fechaFin) {
        String nombreBase = "estadisticas_sac_";
        if (fechaInicio != null && fechaFin != null && !fechaInicio.isEmpty() && !fechaFin.isEmpty()) {
            // Incluir fechas en el nombre del archivo
            nombreBase += fechaInicio.replace("-", "") + "_" + fechaFin.replace("-", "") + "_";
        }
        return nombreBase + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
    }
    
    private String obtenerNombreArchivoImagen(String formato, String fechaInicio, String fechaFin) {
        String extension = "PNG".equalsIgnoreCase(formato) ? "png" : "jpg";
        String nombreBase = "estadisticas_sac_";
        if (fechaInicio != null && fechaFin != null && !fechaInicio.isEmpty() && !fechaFin.isEmpty()) {
            // Incluir fechas en el nombre del archivo
            nombreBase += fechaInicio.replace("-", "") + "_" + fechaFin.replace("-", "") + "_";
        }
        return nombreBase + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + extension;
    }
    
    private CellStyle crearEstiloEncabezado(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex()); // Letras blancas
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.RED.getIndex()); // Fondo rojo
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
    
    private CellStyle crearEstiloDatos(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
    
    private CellStyle crearEstiloNumero(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
    
    private void crearSeccionEstadisticasGenerales(Sheet sheet, SacStatsResponse stats, int startRow, 
                                                   CellStyle headerStyle, CellStyle dataStyle, CellStyle numberStyle) {
        Row headerRow = sheet.createRow(startRow);
        headerRow.createCell(0).setCellValue("ESTADÍSTICAS GENERALES");
        headerRow.getCell(0).setCellStyle(headerStyle);
        
        String[][] generalData = {
            {"Tickets generados", stats.getTotalTickets().toString()},
            {"Abiertos", stats.getOpenTickets().toString()},
            {"Completados", stats.getCompletedTickets().toString()},
            {"Cancelados", stats.getCancelledTickets().toString()},
            {"Calificación Promedio", String.format("%.1f", stats.getAverageRating())},
            {"Total Calificaciones", stats.getTotalRatings().toString()}
        };
        
        for (int i = 0; i < generalData.length; i++) {
            Row row = sheet.createRow(startRow + 2 + i);
            row.createCell(0).setCellValue(generalData[i][0]);
            row.getCell(0).setCellStyle(dataStyle);
            row.createCell(1).setCellValue(generalData[i][1]);
            row.getCell(1).setCellStyle(numberStyle);
        }
    }
    
    private void crearSeccionRendimientoSac(Sheet sheet, List<SacStatsResponse.SacPerformanceResponse> sacPerformance, 
                                           int startRow, CellStyle headerStyle, CellStyle dataStyle, CellStyle numberStyle) {
        Row headerRow = sheet.createRow(startRow);
        headerRow.createCell(0).setCellValue("RENDIMIENTO POR SAC");
        headerRow.getCell(0).setCellStyle(headerStyle);
        
        Row subHeaderRow = sheet.createRow(startRow + 2);
        String[] headers = {"Nombre", "Usuario", "Total Tickets", "Completados", "Calificación", "Calificaciones", "% Resolución", "Tiempo Respuesta"};
        for (int i = 0; i < headers.length; i++) {
            subHeaderRow.createCell(i).setCellValue(headers[i]);
            subHeaderRow.getCell(i).setCellStyle(headerStyle);
        }
        
        for (int i = 0; i < sacPerformance.size(); i++) {
            SacStatsResponse.SacPerformanceResponse sac = sacPerformance.get(i);
            Row row = sheet.createRow(startRow + 3 + i);
            row.createCell(0).setCellValue(sac.getName());
            row.getCell(0).setCellStyle(dataStyle);
            row.createCell(1).setCellValue(sac.getUsername());
            row.getCell(1).setCellStyle(dataStyle);
            row.createCell(2).setCellValue(sac.getTotalTickets());
            row.getCell(2).setCellStyle(numberStyle);
            row.createCell(3).setCellValue(sac.getCompletedTickets());
            row.getCell(3).setCellStyle(numberStyle);
            row.createCell(4).setCellValue(sac.getAverageRating());
            row.getCell(4).setCellStyle(numberStyle);
            row.createCell(5).setCellValue(sac.getTotalRatings());
            row.getCell(5).setCellStyle(numberStyle);
            row.createCell(6).setCellValue(String.format("%.1f%%", sac.getSatisfactionPercentage()));
            row.getCell(6).setCellStyle(numberStyle);
            row.createCell(7).setCellValue(sac.getAverageResponseTime());
            row.getCell(7).setCellStyle(dataStyle);
        }
    }

    private void crearHojaTrazabilidad(
            Workbook workbook,
            List<SacStatsResponse.TicketTraceabilityResponse> traceability,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle numberStyle) {
        Sheet sheet = workbook.createSheet("Trazabilidad");
        String[] headers = {
                "Ticket", "Estado", "Sede", "Categoría", "Opción marcada", "ID opción",
                "Conductor", "Operador", "Módulo", "Generado", "Llamado", "Finalizado",
                "Calificación", "Recorrido"
        };
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
            header.getCell(i).setCellStyle(headerStyle);
        }

        List<SacStatsResponse.TicketTraceabilityResponse> tickets = traceability != null
                ? traceability
                : Collections.emptyList();
        for (int i = 0; i < tickets.size(); i++) {
            SacStatsResponse.TicketTraceabilityResponse ticket = tickets.get(i);
            Row row = sheet.createRow(i + 1);
            String recorrido = ticket.getEvents() == null ? "" : ticket.getEvents().stream()
                    .map(event -> formatearFecha(event.getOccurredAt()) + " - " + event.getLabel())
                    .collect(Collectors.joining(" | "));
            String[] values = {
                    valor(ticket.getTicketNumber()), valor(ticket.getStatus()), valor(ticket.getSedeName()),
                    valor(ticket.getCategoryName()), valor(ticket.getOptionName()),
                    ticket.getOptionId() != null ? ticket.getOptionId().toString() : "",
                    valor(ticket.getLicenseNumber()), valor(ticket.getOperatorName()), valor(ticket.getModuleName()),
                    formatearFecha(ticket.getCreatedAt()), formatearFecha(ticket.getCalledAt()),
                    formatearFecha(ticket.getCompletedAt()),
                    ticket.getRating() != null ? ticket.getRating().toString() : "", recorrido
            };
            for (int column = 0; column < values.length; column++) {
                row.createCell(column).setCellValue(values[column]);
                row.getCell(column).setCellStyle(column == 5 || column == 12 ? numberStyle : dataStyle);
            }
        }

        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, tickets.size()), 0, headers.length - 1));
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i), i == 13 ? 12000 : 6000));
        }
    }

    private String formatearFecha(LocalDateTime value) {
        return value != null ? value.format(TRACE_DATE_FORMATTER) : "";
    }

    private String valor(String value) {
        return value != null ? value : "";
    }
    
    private void ajustarAnchoColumnas(Sheet sheet) {
        for (int i = 0; i < 10; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(currentWidth, 4000));
        }
    }
    
    private BufferedImage crearImagenEstadisticas(SacStatsResponse stats, String fechaInicio, String fechaFin) {
        // Aumentar resolución para mejor calidad
        int scale = 2; // Factor de escala para mayor calidad
        int width = 1200 * scale;
        // Calcular altura dinámica basada en el contenido (mostrar todos los SACs)
        int maxRows = stats.getSacPerformance().size();
        int tablaHeight = (50 + (maxRows * 55) + 60) * scale;
        int height = (350 + tablaHeight / scale) * scale;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Configurar renderizado de alta calidad
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        
        // Escalar el contexto gráfico para mayor calidad
        g2d.scale(scale, scale);
        
        // Usar dimensiones originales para el dibujo (el escalado se aplica automáticamente)
        int originalWidth = 1200;
        int originalHeight = height / scale;
        
        // Fondo con gradiente
        dibujarFondoGradiente(g2d, originalWidth, originalHeight);
        
        // Header con logo y título
        dibujarHeader(g2d, originalWidth, fechaInicio, fechaFin);
        
        // Tarjetas de estadísticas principales (centradas)
        int cardAreaWidth = 1000;
        int cardStartX = (originalWidth - cardAreaWidth) / 2;
        dibujarTarjetasEstadisticas(g2d, stats, cardStartX, 120);
        
        // Tabla de rendimiento detallado (centrada)
        int tablaWidth = 1100;
        int tablaStartX = (originalWidth - tablaWidth) / 2;
        dibujarTablaRendimiento(g2d, stats, tablaStartX, 300);
        
        g2d.dispose();
        return image;
    }
    
    private void dibujarFondoGradiente(Graphics2D g2d, int width, int height) {
        // Fondo gris claro
        g2d.setColor(new java.awt.Color(248, 249, 250));
        g2d.fillRect(0, 0, width, height);
        
        // Gradiente sutil en la parte superior
        for (int i = 0; i < 100; i++) {
            float ratio = (float) i / 100.0f;
            java.awt.Color color = new java.awt.Color(
                (int) (248 + (255 - 248) * ratio),
                (int) (249 + (255 - 249) * ratio),
                (int) (250 + (255 - 250) * ratio)
            );
            g2d.setColor(color);
            g2d.drawLine(0, i, width, i);
        }
    }
    
    private void dibujarHeader(Graphics2D g2d, int width, String fechaInicio, String fechaFin) {
        // Logo YEGO
        g2d.setColor(new java.awt.Color(220, 38, 38)); // Rojo YEGO
        g2d.fillRoundRect(30, 20, 40, 40, 8, 8);
        g2d.setColor(java.awt.Color.WHITE);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24));
        g2d.drawString("Y", 45, 45);
        
        // Título
        g2d.setColor(java.awt.Color.BLACK);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 28));
        g2d.drawString("YEGO Integral", 85, 35);
        
        g2d.setColor(new java.awt.Color(107, 114, 128));
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 16));
        g2d.drawString("Monitoreo Ticketera", 85, 55);
        
        // Fecha en la esquina derecha
        g2d.setColor(new java.awt.Color(75, 85, 99));
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 14));
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String fechaTexto = "Generado: " + fecha;
        if (fechaInicio != null && fechaFin != null && !fechaInicio.isEmpty() && !fechaFin.isEmpty()) {
            fechaTexto += " | " + fechaInicio + " - " + fechaFin;
        }
        g2d.drawString(fechaTexto, width - 400, 40);
    }
    
    private void dibujarTarjetasEstadisticas(Graphics2D g2d, SacStatsResponse stats, int x, int y) {
        int cardWidth = 220;
        int cardHeight = 130;
        int spacing = 20;
        int totalWidth = 4 * cardWidth + 3 * spacing;
        int startX = x + (1000 - totalWidth) / 2; // Centrar las tarjetas
        
        // Colores de las tarjetas
        java.awt.Color[] colores = {
            new java.awt.Color(59, 130, 246), // Azul
            new java.awt.Color(34, 197, 94),  // Verde
            new java.awt.Color(251, 191, 36), // Amarillo
            new java.awt.Color(147, 51, 234)  // Púrpura
        };
        
        String[] titulos = {"Tickets generados", "Abiertos ahora", "Completados", "Calificación"};
        String[] valores = {
            stats.getTotalTickets().toString(),
            stats.getOpenTickets().toString(),
            stats.getCompletedTickets().toString(),
            String.format("%.1f/5", stats.getAverageRating())
        };
        
        for (int i = 0; i < 4; i++) {
            int cardX = startX + i * (cardWidth + spacing);
            
            // Sombra de la tarjeta
            g2d.setColor(new java.awt.Color(0, 0, 0, 30));
            g2d.fillRoundRect(cardX + 3, y + 3, cardWidth, cardHeight, 15, 15);
            
            // Tarjeta principal
            g2d.setColor(colores[i]);
            g2d.fillRoundRect(cardX, y, cardWidth, cardHeight, 15, 15);
            
            // Título
            g2d.setColor(java.awt.Color.WHITE);
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
            g2d.drawString(titulos[i], cardX + 20, y + 35);
            
            // Valor
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 42));
            g2d.drawString(valores[i], cardX + 20, y + 80);
            
            // Icono decorativo
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 28));
            g2d.drawString("●", cardX + cardWidth - 35, y + 60);
        }
    }
    
    private void dibujarTablaRendimiento(Graphics2D g2d, SacStatsResponse stats, int x, int y) {
        // Título de la sección
        g2d.setColor(java.awt.Color.BLACK);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 22));
        g2d.drawString("Desempeño Detallado por SAC", x, y);
        
        // Calcular altura dinámica basada en el número de filas (mostrar todos los SACs)
        int maxRows = stats.getSacPerformance().size(); // Mostrar todos los SACs
        int tablaHeight = 50 + (maxRows * 55) + 60; // Header + filas + padding + margen final
        
        // Fondo de la tabla
        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRoundRect(x, y + 25, 1100, tablaHeight, 15, 15);
        
        // Borde de la tabla
        g2d.setColor(new java.awt.Color(229, 231, 235));
        g2d.drawRoundRect(x, y + 25, 1100, tablaHeight, 15, 15);
        
        // Encabezados de la tabla
        String[] headers = {"SAC", "Total Tickets", "Completados", "Calificación", "Resolución", "Tiempo Respuesta"};
        int[] columnWidths = {220, 130, 130, 130, 130, 130};
        int currentX = x + 20;
        
        // Fondo del header
        g2d.setColor(new java.awt.Color(243, 244, 246));
        g2d.fillRect(x + 20, y + 45, 1060, 45);
        
        // Línea inferior del header
        g2d.setColor(new java.awt.Color(229, 231, 235));
        g2d.drawLine(x + 20, y + 90, x + 1080, y + 90);
        
        g2d.setColor(java.awt.Color.BLACK);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        
        for (int i = 0; i < headers.length; i++) {
            g2d.drawString(headers[i], currentX + 15, y + 70);
            currentX += columnWidths[i];
        }
        
        // Filas de datos
        int rowY = y + 120;
        
        for (int i = 0; i < maxRows; i++) {
            SacStatsResponse.SacPerformanceResponse sac = stats.getSacPerformance().get(i);
            
            // Fondo alternado de filas
            if (i % 2 == 0) {
                g2d.setColor(new java.awt.Color(249, 250, 251));
                g2d.fillRect(x + 20, rowY - 30, 1060, 55);
            }
            
            // Línea separadora entre filas
            if (i > 0) {
                g2d.setColor(new java.awt.Color(243, 244, 246));
                g2d.drawLine(x + 20, rowY - 30, x + 1080, rowY - 30);
            }
            
            currentX = x + 20;
            
            // Avatar (círculo con inicial)
            g2d.setColor(new java.awt.Color(59, 130, 246));
            g2d.fillOval(currentX + 10, rowY - 20, 35, 35);
            g2d.setColor(java.awt.Color.WHITE);
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            g2d.drawString(sac.getName().substring(0, 1), currentX + 20, rowY - 2);
            
            // Nombre y username
            g2d.setColor(java.awt.Color.BLACK);
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 13));
            g2d.drawString(sac.getName(), currentX + 55, rowY - 10);
            g2d.setColor(new java.awt.Color(107, 114, 128));
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
            g2d.drawString("@" + sac.getUsername(), currentX + 55, rowY + 5);
            
            currentX += columnWidths[0];
            
            // Datos de la fila
            g2d.setColor(java.awt.Color.BLACK);
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            
            g2d.drawString(sac.getTotalTickets().toString(), currentX + 15, rowY - 5);
            currentX += columnWidths[1];
            
            g2d.drawString(sac.getCompletedTickets().toString(), currentX + 15, rowY - 5);
            currentX += columnWidths[2];
            
            g2d.drawString("★" + sac.getAverageRating(), currentX + 15, rowY - 5);
            currentX += columnWidths[3];
            
            // Resolución con color
            java.awt.Color resolutionColor = sac.getSatisfactionPercentage() > 50 ?
                new java.awt.Color(34, 197, 94) : new java.awt.Color(239, 68, 68);
            g2d.setColor(resolutionColor);
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            g2d.drawString(String.format("%.1f%%", sac.getSatisfactionPercentage()), currentX + 15, rowY - 5);
            currentX += columnWidths[4];
            
            g2d.setColor(java.awt.Color.BLACK);
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            g2d.drawString(sac.getAverageResponseTime(), currentX + 15, rowY - 5);
            
            rowY += 55;
        }
        
        // Pie de tabla con información adicional y decoración
        int pieY = y + 25 + tablaHeight - 25;
        
        // Línea decorativa sutil
        g2d.setColor(new java.awt.Color(229, 231, 235));
        g2d.drawLine(x + 20, pieY - 15, x + 1080, pieY - 15);
        
        // Información del pie
        g2d.setColor(new java.awt.Color(107, 114, 128));
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
        g2d.drawString("Mostrando " + maxRows + " de " + stats.getSacPerformance().size() + " SACs", 
                      x + 20, pieY);
        
        // Fecha de generación en el pie
        String fechaGeneracion = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        g2d.drawString("Generado: " + fechaGeneracion, x + 900, pieY);
    }
}
