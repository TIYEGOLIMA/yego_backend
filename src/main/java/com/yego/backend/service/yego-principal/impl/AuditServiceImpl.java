package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.entity.yego_principal.entities.AuditLog;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_principal.AuditLogRepository;
import com.yego.backend.service.yego_principal.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de auditoría del sistema YEGO Principal
 * Equivalente a AuditService de NestJS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    
    private final AuditLogRepository auditLogRepository;
    
    @Override
    @Transactional
    public AuditLogResponseDto create(CreateAuditLogDto createAuditLogDto, Long userId) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(createAuditLogDto.getAction())
                .resource(createAuditLogDto.getResource())
                .resourceId(createAuditLogDto.getResourceId())
                .details(createAuditLogDto.getDetails())
                .ipAddress(createAuditLogDto.getIpAddress())
                .userAgent(createAuditLogDto.getUserAgent())
                .build();
        
        AuditLog savedLog = auditLogRepository.save(auditLog);
        
        log.info("📝 Log de auditoría YEGO Principal creado: {} por usuario {}", 
                savedLog.getAction(), userId != null ? userId : "sistema");
        
        return mapToResponseDto(savedLog);
    }
    
    @Override
    public AuditLogPageDto findAll(Integer page, Integer limit, AuditFilterDto filters) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        
        Page<AuditLog> auditPage = auditLogRepository.findWithFilters(
                filters.getUserId(),
                filters.getAction(),
                filters.getResource(),
                filters.getStartDate(),
                filters.getEndDate(),
                filters.getSearch(),
                pageable
        );
        
        List<AuditLogResponseDto> logs = auditPage.getContent().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        
        return AuditLogPageDto.builder()
                .logs(logs)
                .total(auditPage.getTotalElements())
                .page(page)
                .limit(limit)
                .build();
    }
    
    @Override
    public AuditLogResponseDto findOne(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Log de auditoría con ID " + id + " no encontrado"));
        
        return mapToResponseDto(auditLog);
    }
    
    @Override
    public List<AuditLogResponseDto> findByUser(Long userId, Integer limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<AuditLog> auditLogs = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        return auditLogs.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AuditLogResponseDto> findByAction(String action, Integer limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<AuditLog> auditLogs = auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable);
        
        return auditLogs.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AuditLogResponseDto> findByResource(String resource, Integer limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<AuditLog> auditLogs = auditLogRepository.findByResourceOrderByCreatedAtDesc(resource, pageable);
        
        return auditLogs.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public AuditStatsDto getStats(Integer days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        // Total de logs
        Long totalLogs = auditLogRepository.countByCreatedAtAfter(startDate);
        
        // Estadísticas por acción
        List<Object[]> actionStatsRaw = auditLogRepository.getActionStats(startDate);
        List<AuditStatsDto.ActionStatsDto> actions = actionStatsRaw.stream()
                .map(row -> AuditStatsDto.ActionStatsDto.builder()
                        .action((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());
        
        // Estadísticas por recurso
        List<Object[]> resourceStatsRaw = auditLogRepository.getResourceStats(startDate);
        List<AuditStatsDto.ResourceStatsDto> resources = resourceStatsRaw.stream()
                .map(row -> AuditStatsDto.ResourceStatsDto.builder()
                        .resource((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());
        
        // Estadísticas por usuario
        List<Object[]> userStatsRaw = auditLogRepository.getUserStats(startDate);
        List<AuditStatsDto.UserStatsDto> users = userStatsRaw.stream()
                .map(row -> AuditStatsDto.UserStatsDto.builder()
                        .userId(((Number) row[0]).longValue())
                        .username((String) row[1])
                        .count(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());
        
        // Estadísticas diarias
        List<Object[]> dailyStatsRaw = auditLogRepository.getDailyStats(startDate);
        List<AuditStatsDto.DailyStatsDto> dailyStats = dailyStatsRaw.stream()
                .map(row -> AuditStatsDto.DailyStatsDto.builder()
                        .date(row[0].toString())
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());
        
        return AuditStatsDto.builder()
                .totalLogs(totalLogs)
                .actions(actions)
                .resources(resources)
                .users(users)
                .dailyStats(dailyStats)
                .build();
    }
    
    @Override
    public List<AuditLogResponseDto> getRecentActivity(Integer limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<AuditLog> recentLogs = auditLogRepository.findRecentActivity(pageable);
        
        return recentLogs.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void logLogin(Long userId, String ipAddress, String userAgent) {
        CreateAuditLogDto dto = CreateAuditLogDto.builder()
                .action("LOGIN")
                .resource("auth")
                .resourceId(userId.toString())
                .details(Map.of("success", true, "type", "normal"))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        
        create(dto, userId);
    }
    
    @Override
    @Transactional
    public void logLogout(Long userId, String ipAddress, String userAgent) {
        CreateAuditLogDto dto = CreateAuditLogDto.builder()
                .action("LOGOUT")
                .resource("auth")
                .resourceId(userId.toString())
                .details(Map.of("success", true))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        
        create(dto, userId);
    }
    
    @Override
    @Transactional
    public void logFailedLogin(String username, String ipAddress, String userAgent) {
        CreateAuditLogDto dto = CreateAuditLogDto.builder()
                .action("LOGIN_FAILED")
                .resource("auth")
                .resourceId(username)
                .details(Map.of("username", username, "reason", "Credenciales inválidas"))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        
        create(dto, null);
    }
    
    @Override
    @Transactional
    public void logUserAction(Long userId, String action, String resource, String resourceId, 
                             Map<String, Object> details, String ipAddress, String userAgent) {
        CreateAuditLogDto dto = CreateAuditLogDto.builder()
                .action(action)
                .resource(resource)
                .resourceId(resourceId)
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        
        create(dto, userId);
    }
    
    @Override
    @Transactional
    public void logSystemAction(String action, String resource, String resourceId, Map<String, Object> details) {
        CreateAuditLogDto dto = CreateAuditLogDto.builder()
                .action(action)
                .resource(resource)
                .resourceId(resourceId)
                .details(details)
                .build();
        
        create(dto, null);
    }
    
    private AuditLogResponseDto mapToResponseDto(AuditLog auditLog) {
        AuditLogResponseDto.AuditUserDto userDto = null;
        
        if (auditLog.getUser() != null) {
            User user = auditLog.getUser();
            userDto = AuditLogResponseDto.AuditUserDto.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .name(user.getName())
                    .email(user.getEmail())
                    .build();
        }
        
        return AuditLogResponseDto.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUserId())
                .action(auditLog.getAction())
                .resource(auditLog.getResource())
                .resourceId(auditLog.getResourceId())
                .details(auditLog.getDetails())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .createdAt(auditLog.getCreatedAt())
                .user(userDto)
                .build();
    }
}
