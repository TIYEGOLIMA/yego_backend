package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.entity.yego_principal.entities.Session;
import com.yego.backend.entity.yego_principal.entities.ConnectionLog;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_principal.SessionRepository;
import com.yego.backend.repository.yego_principal.ConnectionLogRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_principal.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de sesiones del sistema YEGO Principal
 * Equivalente a SessionsService de NestJS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {
    
    private final SessionRepository sessionRepository;
    private final ConnectionLogRepository connectionLogRepository;
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public SessionResponseDto create(CreateSessionDto createSessionDto, Long userId, HttpServletRequest request) {
        // Obtener información adicional del request si es necesario
        String ipAddress = createSessionDto.getIpAddress();
        if (ipAddress == null && request != null) {
            ipAddress = getClientIP(request);
        }
        
        // Buscar el usuario
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        // Crear sesión
        Session session = Session.builder()
                .userId(createSessionDto.getUserId())
                .tokenHash(createSessionDto.getTokenHash())
                .ipAddress(ipAddress)
                .userAgent(createSessionDto.getUserAgent())
                .device(createSessionDto.getDevice())
                .browser(createSessionDto.getBrowser())
                .operatingSystem(createSessionDto.getOperatingSystem())
                .city(createSessionDto.getCity())
                .region(createSessionDto.getRegion())
                .country(createSessionDto.getCountry())
                .countryCode(createSessionDto.getCountryCode())
                .latitude(createSessionDto.getLatitude())
                .longitude(createSessionDto.getLongitude())
                .timezone(createSessionDto.getTimezone())
                .isp(createSessionDto.getIsp())
                .organization(createSessionDto.getOrganization())
                .expiresAt(createSessionDto.getExpiresAt())
                .active(true)
                .build();
        
        Session savedSession = sessionRepository.save(session);
        
        log.info("✅ Sesión YEGO Principal creada para usuario {}: {} desde {}", 
                userId, savedSession.getId(), ipAddress);
        
        return mapToResponseDto(savedSession);
    }
    
    @Override
    public List<SessionResponseDto> findAll(Long userId) {
        List<Session> sessions;
        
        if (userId != null) {
            sessions = sessionRepository.findActiveSessionsByUserId(userId);
        } else {
            // Obtener todas las sesiones activas
            Pageable pageable = PageRequest.of(0, 100); // Limitar a 100 por rendimiento
            Page<Session> sessionPage = sessionRepository.findAll(pageable);
            sessions = sessionPage.getContent().stream()
                    .filter(Session::getActive)
                    .collect(Collectors.toList());
        }
        
        return sessions.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public SessionResponseDto findOne(Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sesión no encontrada"));
        
        return mapToResponseDto(session);
    }
    
    @Override
    public Session findByTokenHash(String tokenHash) {
        return sessionRepository.findByTokenHash(tokenHash)
                .orElse(null);
    }
    
    @Override
    @Transactional
    public void deactivate(Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sesión no encontrada"));
        
        session.setActive(false);
        sessionRepository.save(session);
        
        log.info("🚪 Sesión YEGO Principal {} desactivada", id);
    }
    
    @Override
    @Transactional
    public void deactivateByUserId(Long userId, String reason) {
        List<Session> sessions = sessionRepository.findActiveSessionsByUserId(userId);
        
        for (Session session : sessions) {
            session.setActive(false);
            // No hay campo endedAt en la entidad Session
            sessionRepository.save(session);
        }
        
        log.info("🚪 {} sesiones YEGO Principal desactivadas para usuario {}: {}", 
                sessions.size(), userId, reason);
    }
    
    @Override
    @Transactional
    public void deactivateByTokenHash(String tokenHash) {
        Session session = sessionRepository.findByTokenHash(tokenHash).orElse(null);
        
        if (session != null) {
            session.setActive(false);
            // No hay campo endedAt en la entidad Session
            sessionRepository.save(session);
            
            log.info("🚪 Sesión YEGO Principal con token {} desactivada", tokenHash);
        }
    }
    
    @Override
    @Transactional
    public Integer cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<Session> expiredSessions = sessionRepository.findInactiveSessionsForCleanup(now);
        
        for (Session session : expiredSessions) {
            session.setActive(false);
            // No hay campo endedAt en la entidad Session
            sessionRepository.save(session);
        }
        
        log.info("🧹 {} sesiones expiradas YEGO Principal limpiadas", expiredSessions.size());
        return expiredSessions.size();
    }
    
    @Override
    public Long getActiveSessionsCount(Long userId) {
        if (userId != null) {
            return (long) sessionRepository.findActiveSessionsByUserId(userId).size();
        } else {
            return sessionRepository.countActiveSessions();
        }
    }
    
    @Override
    public SessionStatsDto getSessionStats() {
        Long totalActive = sessionRepository.countActiveSessions();
        Long totalCreatedToday = sessionRepository.countSessionsCreatedAfter(LocalDateTime.now().minusDays(1));
        
        return SessionStatsDto.builder()
                .totalActive(totalActive)
                .totalCreatedToday(totalCreatedToday)
                .build();
    }
    
    @Override
    public ConnectionStatsDto getWebSocketStats() {
        // Simulación de estadísticas WebSocket
        // En una implementación real, esto vendría del WebSocketGateway
        return ConnectionStatsDto.builder()
                .totalConnections(0L)
                .activeConnections(0L)
                .build();
    }
    
    @Override
    public List<SessionDataDto> getWebSocketSessions() {
        // Simulación de sesiones WebSocket activas
        // En una implementación real, esto vendría del WebSocketGateway
        return List.of();
    }
    
    @Override
    public List<ConnectionLogResponseDto> getConnectionLogs(Integer days, Integer limit, Long userId, String roleName) {
        try {
            log.info("🔍 Iniciando getConnectionLogs YEGO Principal - days: {}, limit: {}, userId: {}, roleName: {}", 
                    days, limit, userId, roleName);
            
            Pageable pageable = PageRequest.of(0, limit != null ? limit : 50);
            Page<ConnectionLog> logs = connectionLogRepository.findTop50ByOrderByCreatedAtDesc(pageable);
            
            List<ConnectionLogResponseDto> result = logs.getContent().stream()
                    .map(this::mapToConnectionLogResponseDto)
                    .collect(Collectors.toList());
            
            log.info("📊 Obtenidos {} logs de conexión YEGO Principal", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error al obtener logs de conexión YEGO Principal: {}", e.getMessage());
            return List.of(); // Retornar lista vacía en caso de error
        }
    }
    
    @Override
    @Transactional
    public void forceLogout(Long sessionId, Long adminUserId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Sesión no encontrada"));
        
        session.setActive(false);
        // No hay campo endedAt en la entidad Session
        sessionRepository.save(session);
        
        // Registrar el log de conexión
        ConnectionLog connectionLog = ConnectionLog.builder()
                .userId(session.getUserId())
                .sessionId(sessionId)
                .action("FORCED_LOGOUT")
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .device(session.getDevice())
                .browser(session.getBrowser())
                .operatingSystem(session.getOperatingSystem())
                .city(session.getCity())
                .region(session.getRegion())
                .country(session.getCountry())
                .countryCode(session.getCountryCode())
                .latitude(session.getLatitude())
                .longitude(session.getLongitude())
                .timezone(session.getTimezone())
                .isp(session.getIsp())
                .organization(session.getOrganization())
                .roleName("admin")
                .build();
        
        connectionLogRepository.save(connectionLog);
        
        log.info("🚪 Sesión YEGO Principal {} forzada a cerrar por admin {}", sessionId, adminUserId);
    }
    
    private SessionResponseDto mapToResponseDto(Session session) {
        return SessionResponseDto.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .tokenHash(session.getTokenHash())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .expiresAt(session.getExpiresAt())
                .active(session.getActive())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .device(session.getDevice())
                .browser(session.getBrowser())
                .operatingSystem(session.getOperatingSystem())
                .city(session.getCity())
                .region(session.getRegion())
                .country(session.getCountry())
                .countryCode(session.getCountryCode())
                .latitude(session.getLatitude())
                .longitude(session.getLongitude())
                .timezone(session.getTimezone())
                .isp(session.getIsp())
                .organization(session.getOrganization())
                .build();
    }
    
    private ConnectionLogResponseDto mapToConnectionLogResponseDto(ConnectionLog log) {
        return ConnectionLogResponseDto.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .sessionId(log.getSessionId())
                .action(log.getAction())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .device(log.getDevice())
                .browser(log.getBrowser())
                .operatingSystem(log.getOperatingSystem())
                .city(log.getCity())
                .region(log.getRegion())
                .country(log.getCountry())
                .countryCode(log.getCountryCode())
                .latitude(log.getLatitude())
                .longitude(log.getLongitude())
                .timezone(log.getTimezone())
                .isp(log.getIsp())
                .organization(log.getOrganization())
                .sessionDuration(log.getSessionDuration())
                .roleName(log.getRoleName())
                .createdAt(log.getCreatedAt())
                .build();
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
