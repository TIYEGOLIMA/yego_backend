package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.SessionUserDto;
import com.yego.backend.entity.yego_principal.api.response.*;
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
import java.util.Collections;
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

    private static final String ACTION_FORCED_LOGOUT = "forced_logout";
    private static final String LEGACY_ACTION_FORCED_LOGOUT = "FORCED_LOGOUT";
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_ADMIN_LIMIT = 500;

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
            SessionPageDto page = findActiveSessionsPage(0, DEFAULT_ADMIN_LIMIT, null);
            return page.getContent();
        }
        return sessions.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public SessionPageDto findActiveSessionsPage(int page, int size, String search) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Session> pageResult;
        if (search != null && !search.trim().isEmpty()) {
            String term = search.trim();
            List<Long> userIds = userRepository.findUserIdsBySearch(term);
            if (userIds.isEmpty()) userIds = Collections.singletonList(-1L);
            pageResult = sessionRepository.findByActiveTrueAndSearch(userIds, term, pageable);
        } else {
            pageResult = sessionRepository.findByActiveTrueOrderByCreatedAtDesc(pageable);
        }
        List<SessionResponseDto> content = pageResult.getContent().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return SessionPageDto.builder()
                .content(content)
                .total(pageResult.getTotalElements())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalPages(pageResult.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public void deactivateByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        int updated = sessionRepository.deactivateByIdIn(ids);
        log.info("🚪 {} sesiones YEGO Principal desactivadas (bulk)", updated);
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
    public SessionStatsDto getSessionStats() {
        Long totalActive = sessionRepository.countActiveSessions();
        Long totalCreatedToday = sessionRepository.countSessionsCreatedAfter(LocalDateTime.now().minusDays(1));
        
        return SessionStatsDto.builder()
                .totalActive(totalActive)
                .totalCreatedToday(totalCreatedToday)
                .build();
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
                .action(ACTION_FORCED_LOGOUT)
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
    
    private SessionUserDto buildSessionUserDto(User user) {
        if (user == null) return null;
        String nombre = ((user.getName() != null ? user.getName() : "") + " " + (user.getLastName() != null ? user.getLastName() : "")).trim();
        if (nombre.isBlank()) nombre = user.getUsername();
        return SessionUserDto.builder()
                .username(user.getUsername())
                .nombre(nombre)
                .email(user.getEmail())
                .build();
    }

    private SessionResponseDto mapToResponseDto(Session session) {
        User user = userRepository.findById(session.getUserId()).orElse(null);
        SessionUserDto userDto = buildSessionUserDto(user);
        return SessionResponseDto.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .user(userDto)
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
        User user = log.getUserId() != null ? userRepository.findById(log.getUserId()).orElse(null) : null;
        SessionUserDto userDto = buildSessionUserDto(user);
        String action = LEGACY_ACTION_FORCED_LOGOUT.equals(log.getAction()) ? ACTION_FORCED_LOGOUT : log.getAction();
        return ConnectionLogResponseDto.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .sessionId(log.getSessionId())
                .action(action)
                .user(userDto)
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
