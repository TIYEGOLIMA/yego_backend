package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.entity.yego_principal.entities.*;
import com.yego.backend.repository.yego_principal.*;
import com.yego.backend.service.yego_principal.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de reportes del sistema YEGO Principal
 * Equivalente a ReportsService de NestJS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final SessionRepository sessionRepository;
    private final AuditLogRepository auditLogRepository;
    
    @Override
    public SystemStatsDto getSystemStats(Integer days) {
        // Obtener estadísticas básicas
        Long totalUsers = userRepository.count();
        Long activeUsers = userRepository.countActiveUsers();
        Long totalRoles = roleRepository.count();
        Long totalPermissions = permissionRepository.count();
        Long totalImports = 0L;
        Long activeSessions = sessionRepository.countActiveSessions();
        
        return SystemStatsDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalRoles(totalRoles)
                .totalPermissions(totalPermissions)
                .totalImports(totalImports)
                .activeSessions(activeSessions)
                .build();
    }
    
    @Override
    public DashboardDataDto getDashboardData() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        
        // Obtener estadísticas del sistema
        SystemStatsDto systemStats = getSystemStats(30);
        
        // Importaciones de hoy y ayer
        Long importsToday = 0L;
        Long importsYesterday = 0L;
        
        // Actividad reciente
        List<AuditLog> recentAuditLogs = auditLogRepository.findTop10ByOrderByCreatedAtDesc();
        List<RecentActivityDto> recentActivity = recentAuditLogs.stream()
                .map(this::mapToRecentActivityDto)
                .collect(Collectors.toList());
        
        // Conteo de errores
        Long errorCount = auditLogRepository.countByActionAndCreatedAtAfter("LOGIN_FAILED", weekAgo);
        
        // Estadísticas semanales
        WeeklyStatsDto weeklyStats = getWeeklyStats();
        
        // Estado del sistema (simulado)
        SystemStatusDto systemStatus = SystemStatusDto.builder()
                .database("OK")
                .api("OK")
                .websockets("OK")
                .storage("OK")
                .memory(75.5)
                .cpu(45.2)
                .uptime(System.currentTimeMillis())
                .lastCheck(LocalDateTime.now())
                .build();
        
        // Calcular cambios porcentuales
        String importsChange = "0%";
        if (importsYesterday > 0) {
            double change = ((double) (importsToday - importsYesterday) / importsYesterday) * 100;
            importsChange = String.format("%.1f%%", change);
        } else if (importsToday > 0) {
            importsChange = "100%";
        }
        
        // Construir métricas del dashboard
        DashboardMetricsDto metrics = DashboardMetricsDto.builder()
                .totalUsers(systemStats.getTotalUsers())
                .activeUsers(systemStats.getActiveUsers())
                .totalRoles(systemStats.getTotalRoles())
                .totalPermissions(systemStats.getTotalPermissions())
                .totalImports(systemStats.getTotalImports())
                .activeSessions(systemStats.getActiveSessions())
                .importsToday(importsToday)
                .importsChange(importsChange)
                .errorCount(errorCount)
                .build();
        
        return DashboardDataDto.builder()
                .metrics(metrics)
                .recentActivity(recentActivity)
                .systemStatus(systemStatus)
                .weeklyStats(weeklyStats)
                .build();
    }
    
    @Override
    public List<UserStatsDto> getUserStats() {
        List<User> users = userRepository.findAllByOrderByCreatedAtDesc();
        
        return users.stream()
                .map(this::mapToUserStatsDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public WeeklyStatsDto getWeeklyStats() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        
        Long newUsers = userRepository.countByCreatedAtAfter(weekAgo);
        Long weeklyImports = 0L;
        Long weeklyActivity = auditLogRepository.countByCreatedAtAfter(weekAgo);
        
        return WeeklyStatsDto.builder()
                .newUsers(newUsers)
                .imports(weeklyImports)
                .activity(weeklyActivity)
                .period("Últimos 7 días")
                .build();
    }
    
    @Override
    public byte[] exportReport(String type, Integer days) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte");
            
            switch (type.toLowerCase()) {
                case "users":
                    exportUsersReport(sheet);
                    break;
                case "audit":
                    exportAuditReport(sheet, days);
                    break;
                case "sessions":
                    exportSessionsReport(sheet, days);
                    break;
                case "general":
                default:
                    exportGeneralReport(sheet, days);
                    break;
            }
            
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
            
        } catch (IOException e) {
            log.error("Error generando reporte Excel YEGO Principal: {}", e.getMessage());
            throw new RuntimeException("Error generando reporte Excel", e);
        }
    }
    
    private void exportUsersReport(Sheet sheet) {
        // Crear encabezados
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Nombre", "Username", "Email", "Rol", "Estado", "Creado"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }
        
        // Obtener usuarios y llenar datos
        List<User> users = userRepository.findAll();
        int rowNum = 1;
        
        for (User user : users) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(user.getId());
            row.createCell(1).setCellValue(user.getName());
            row.createCell(2).setCellValue(user.getUsername());
            row.createCell(3).setCellValue(user.getEmail());
            row.createCell(4).setCellValue(user.getRoleName());
            row.createCell(5).setCellValue(user.getActive() ? "Activo" : "Inactivo");
            row.createCell(6).setCellValue(user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        
        // Ajustar ancho de columnas
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void exportAuditReport(Sheet sheet, Integer days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        // Crear encabezados
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Usuario", "Acción", "Recurso", "Detalles", "IP", "Fecha"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }
        
        // Obtener logs de auditoría y llenar datos
        List<AuditLog> auditLogs = auditLogRepository.findByCreatedAtAfterOrderByCreatedAtDesc(startDate);
        int rowNum = 1;
        
        for (AuditLog log : auditLogs) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(log.getId());
            row.createCell(1).setCellValue(log.getUserId() != null ? "Usuario " + log.getUserId() : "Sistema");
            row.createCell(2).setCellValue(log.getAction());
            row.createCell(3).setCellValue(log.getResource());
            row.createCell(4).setCellValue(log.getDetails());
            row.createCell(5).setCellValue(log.getIpAddress());
            row.createCell(6).setCellValue(log.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        }
        
        // Ajustar ancho de columnas
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void exportSessionsReport(Sheet sheet, Integer days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        // Crear encabezados
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Usuario", "IP", "Dispositivo", "Ciudad", "País", "Login", "Estado"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }
        
        // Obtener sesiones y llenar datos
        List<Session> sessions = sessionRepository.findByCreatedAtAfterOrderByCreatedAtDesc(startDate);
        int rowNum = 1;
        
        for (Session session : sessions) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(session.getId());
            row.createCell(1).setCellValue(session.getUserId() != null ? "Usuario " + session.getUserId() : "Desconocido");
            row.createCell(2).setCellValue(session.getIpAddress());
            row.createCell(3).setCellValue(session.getDevice());
            row.createCell(4).setCellValue(session.getCity());
            row.createCell(5).setCellValue(session.getCountry());
            row.createCell(6).setCellValue(session.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            row.createCell(7).setCellValue(session.getActive() ? "Activa" : "Cerrada");
        }
        
        // Ajustar ancho de columnas
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void exportGeneralReport(Sheet sheet, Integer days) {
        SystemStatsDto stats = getSystemStats(days);
        
        // Crear encabezados
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Métrica");
        headerRow.createCell(1).setCellValue("Valor");
        
        // Llenar datos
        int rowNum = 1;
        Row row1 = sheet.createRow(rowNum++);
        row1.createCell(0).setCellValue("Usuarios Totales");
        row1.createCell(1).setCellValue(stats.getTotalUsers());
        
        Row row2 = sheet.createRow(rowNum++);
        row2.createCell(0).setCellValue("Usuarios Activos");
        row2.createCell(1).setCellValue(stats.getActiveUsers());
        
        Row row3 = sheet.createRow(rowNum++);
        row3.createCell(0).setCellValue("Roles");
        row3.createCell(1).setCellValue(stats.getTotalRoles());
        
        Row row4 = sheet.createRow(rowNum++);
        row4.createCell(0).setCellValue("Permisos");
        row4.createCell(1).setCellValue(stats.getTotalPermissions());
        
        Row row5 = sheet.createRow(rowNum++);
        row5.createCell(0).setCellValue("Importaciones");
        row5.createCell(1).setCellValue(stats.getTotalImports());
        
        Row row6 = sheet.createRow(rowNum++);
        row6.createCell(0).setCellValue("Sesiones Activas");
        row6.createCell(1).setCellValue(stats.getActiveSessions());
        
        Row row7 = sheet.createRow(rowNum++);
        row7.createCell(0).setCellValue("Período (días)");
        row7.createCell(1).setCellValue(days);
        
        Row row8 = sheet.createRow(rowNum++);
        row8.createCell(0).setCellValue("Fecha de Reporte");
        row8.createCell(1).setCellValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        
        // Ajustar ancho de columnas
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }
    
    private RecentActivityDto mapToRecentActivityDto(AuditLog auditLog) {
        RecentActivityDto.ActivityUserDto userDto = null;
        if (auditLog.getUserId() != null) {
            userDto = RecentActivityDto.ActivityUserDto.builder()
                    .id(auditLog.getUserId())
                    .username("Usuario " + auditLog.getUserId())
                    .name("Usuario " + auditLog.getUserId())
                    .build();
        }
        
        return RecentActivityDto.builder()
                .id(auditLog.getId())
                .user(userDto)
                .action(auditLog.getAction())
                .resource(auditLog.getResource())
                .resourceId(auditLog.getResourceId())
                .details(auditLog.getDetails())
                .createdAt(auditLog.getCreatedAt())
                .ipAddress(auditLog.getIpAddress())
                .build();
    }
    
    private UserStatsDto mapToUserStatsDto(User user) {
        return UserStatsDto.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRoleName())
                .lastLogin(user.getLastLogin())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

