package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.entity.yego_principal.entities.Role;
import com.yego.backend.entity.yego_principal.entities.Area;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.repository.yego_principal.RoleRepository;
import com.yego.backend.repository.yego_principal.AreaRepository;
import com.yego.backend.service.yego_principal.AuthService;
import com.yego.backend.service.yego_principal.AuditService;
import com.yego.backend.service.yego_principal.SessionService;
import com.yego.backend.service.yego_ticketerera.QueueAgentService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de autenticación del sistema YEGO Principal
 * Equivalente a AuthService de NestJS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AreaRepository areaRepository;
    private final SessionService sessionService;
    private final AuditService auditService;
    private final QueueAgentService queueAgentService;
    /** Cost 10: buen equilibrio seguridad/velocidad; 12 era muy lento en cambio de contraseña. */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:28800}")
    private Long jwtExpiration;
    
    @Value("${auth.password-policy.excluded-user-ids:1,4,5,6}")
    private String excludedUserIdsConfig;
    
    private java.util.Set<Long> getExcludedUserIdsPasswordPolicy() {
        if (excludedUserIdsConfig == null || excludedUserIdsConfig.isBlank()) return java.util.Set.of(1L, 4L, 5L, 6L);
        java.util.Set<Long> set = new java.util.HashSet<>();
        for (String s : excludedUserIdsConfig.split(",")) {
            try {
                long id = Long.parseLong(s.trim());
                set.add(id);
            } catch (NumberFormatException ignored) { }
        }
        return set.isEmpty() ? java.util.Set.of(1L, 4L, 5L, 6L) : set;
    }
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    private JwtParser getJwtParser() {
        return Jwts.parser().setSigningKey(getSigningKey()).build();
    }
    
    @Override
    public UserResponseDto validateUser(String username, String password, HttpServletRequest request) {
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(username, username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            if (passwordEncoder.matches(password, user.getPassword())) {
                if (!user.getActive()) {
                    log.warn("Usuario inactivo intentó iniciar sesión: {}", user.getUsername());
                    auditService.logFailedLogin(username, getClientIpAddress(request), getUserAgent(request));
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario inactivo. Contacte al administrador del sistema");
                }
                
                // Verificar si el rol del usuario está activo
                if (user.getRole() != null && user.getRole().getActivo() != null && !user.getRole().getActivo()) {
                    log.warn("Usuario con rol inactivo intentó iniciar sesión: {} (Rol: {})", 
                        user.getUsername(), user.getRole().getName());
                    auditService.logFailedLogin(username, getClientIpAddress(request), getUserAgent(request));
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                        "Tu rol '" + user.getRole().getName() + "' ha sido desactivado temporalmente. No tienes acceso al sistema en este momento. Contacte al administrador.");
                }
                
                // Verificar si el usuario pertenece a un área y esa área está activa
                if (user.getAreaId() != null) {
                    Optional<Area> areaOpt = areaRepository.findById(user.getAreaId());
                    if (areaOpt.isPresent()) {
                        Area area = areaOpt.get();
                        if (area.getActivo() != null && !area.getActivo()) {
                            log.warn("Usuario de área inactiva intentó iniciar sesión: {} (Área: {})", 
                                user.getUsername(), area.getName());
                            auditService.logFailedLogin(username, getClientIpAddress(request), getUserAgent(request));
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                                "Tu área '" + area.getName() + "' ha sido desactivada. No tienes acceso al sistema. Contacte al administrador.");
                        }
                    }
                }
                
                return mapToUserResponseDto(user);
            } else {
                log.info("Contraseña incorrecta para usuario: {}", username);
                auditService.logFailedLogin(username, getClientIpAddress(request), getUserAgent(request));
            }
        } else {
            log.info("Usuario no encontrado: {}", username);
            auditService.logFailedLogin(username, getClientIpAddress(request), getUserAgent(request));
        }
        
        return null;
    }
    
    @Override
    public LoginTokenResult refreshToken(String token, HttpServletRequest request) {
        try {
            // Decodificar el token para obtener información del usuario
            Claims claims = getJwtParser()
                    .parseClaimsJws(token)
                    .getBody();
            
            String username = claims.get("username", String.class);
            Long userId = claims.get("userId", Long.class);
            
            if (username == null || userId == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
            }
            
            // Buscar el usuario
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || !userOpt.get().getActive()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado o inactivo");
            }
            
            User user = userOpt.get();
            
            // Verificar si el rol del usuario está activo
            if (user.getRole() != null && user.getRole().getActivo() != null && !user.getRole().getActivo()) {
                log.warn("Usuario con rol inactivo intentó refrescar token: {} (Rol: {})", 
                    user.getUsername(), user.getRole().getName());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Tu rol '" + user.getRole().getName() + "' ha sido desactivado temporalmente. No tienes acceso al sistema en este momento.");
            }
            // Verificar si el área del usuario está activa
            if (user.getAreaId() != null) {
                Optional<Area> areaOpt = areaRepository.findById(user.getAreaId());
                if (areaOpt.isPresent() && areaOpt.get().getActivo() != null && !areaOpt.get().getActivo()) {
                    log.warn("Usuario de área inactiva intentó refrescar token: {} (Área id: {})", 
                        user.getUsername(), user.getAreaId());
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                        "Tu área ha sido desactivada. No tienes acceso al sistema. Contacte al administrador.");
                }
            }
            UserResponseDto userDto = mapToUserResponseDto(user);
            
            // Generar nuevo JWT token
            String newAccessToken = generateToken(userDto);
            
            // Actualizar sesión
            CreateSessionDto sessionDto = CreateSessionDto.builder()
                    .userId(userId)
                    .tokenHash(passwordEncoder.encode(newAccessToken))
                    .userAgent(request.getHeader("User-Agent"))
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            
            sessionService.create(sessionDto, userId, request);

            return new LoginTokenResult(newAccessToken, "Token renovado correctamente");
                    
        } catch (Exception e) {
            log.warn("Error renovando token: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido o expirado");
        }
    }
    
    @Override
    @Transactional
    public LoginTokenResult login(LoginDto loginDto, HttpServletRequest request) {
        UserResponseDto user = validateUser(loginDto.getUsername(), loginDto.getPassword(), request);
        
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
        
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());
        
        // Generar JWT token
        String accessToken = generateToken(user);
        
        // Crear sesión con geolocalización (1 semana de expiración)
        CreateSessionDto sessionDto = CreateSessionDto.builder()
                .userId(user.getId())
                .tokenHash(passwordEncoder.encode(accessToken))
                .userAgent(request.getHeader("User-Agent"))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        
        sessionService.create(sessionDto, user.getId(), request);
        
        // Registrar login exitoso en auditoría
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        auditService.logLogin(user.getId(), user.getUsername(), clientIp, userAgent);
        
        return new LoginTokenResult(accessToken, "Inicio de sesión correcto");
    }
    
    @Override
    @Transactional
    public UserResponseDto register(RegisterDto registerDto) {
        // Verificar si el usuario ya existe
        if (userRepository.existsByUsernameOrEmail(registerDto.getUsername(), registerDto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario o email ya existe");
        }
        
        // Hash de la contraseña
        String passwordHash = passwordEncoder.encode(registerDto.getPassword());
        
        // Crear usuario
        // Buscar rol por defecto "usuario" o el primer rol activo disponible
        Role defaultRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.findActiveRoles().stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No hay roles disponibles en el sistema")));
        
        User user = User.builder()
                .username(registerDto.getUsername())
                .email(registerDto.getEmail())
                .name(registerDto.getNombre())
                .password(passwordHash)
                .role(defaultRole)
                .active(true)
                .passwordChangedAt(LocalDateTime.now())
                .build();
        
        User savedUser = userRepository.save(user);
        
        log.info("✅ Usuario registrado: {}", savedUser.getUsername());
        
        return mapToUserResponseDto(savedUser);
    }
    
    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        
        // Verificar contraseña actual
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }
        
        // Validar nueva contraseña
        if (!validatePassword(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña no cumple con los requisitos de seguridad");
        }
        
        // Verificar que la nueva no sea igual a la actual (comparación en claro, evita otra ronda BCrypt)
        if (currentPassword.equals(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña no puede ser igual a la actual");
        }

        // Hash nueva contraseña y actualizar (y fecha de cambio para política semanal)
        String newPasswordHash = passwordEncoder.encode(newPassword);
        userRepository.updatePassword(userId, newPasswordHash, LocalDateTime.now());
        
        log.info("✅ Contraseña cambiada para usuario: {}", user.getUsername());
    }
    
    @Override
    @Transactional
    public void resetPassword(ChangePasswordDto changePasswordDto, HttpServletRequest request) {
        UserResponseDto user = validateUser(changePasswordDto.getUsername(),
                changePasswordDto.getCurrentPassword(), request);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }

        changePassword(user.getId(), changePasswordDto.getCurrentPassword(),
                changePasswordDto.getNewPassword());
    }
    
    @Override
    public boolean validatePassword(String password) {
        // Verificar contraseñas débiles
        List<String> weakPasswords = Arrays.asList("123456", "admin", "password", "123456789", "qwerty");
        if (weakPasswords.contains(password.toLowerCase())) {
            return false;
        }
        
        // Verificar requisitos mínimos
        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasNumbers = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
        boolean isLongEnough = password.length() >= 8;
        
        return hasUpperCase && hasLowerCase && hasNumbers && hasSpecialChar && isLongEnough;
    }
    
    @Override
    public UserProfileDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        List<Area> areasComoJefe = areaRepository.findByManagerId(userId);
        boolean esJefe = areasComoJefe != null && !areasComoJefe.isEmpty();
        String nombreArea = esJefe ? areasComoJefe.stream().map(Area::getName).collect(Collectors.joining(", ")) : null;

        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRoleName())
                .moduleId(user.getModuleId() != null ? user.getModuleId().toString() : null)
                .active(user.getActive())
                .lastLogin(user.getLastLogin())
                .esJefe(esJefe)
                .nombreArea(nombreArea)
                .requirePasswordChange(shouldRequirePasswordChange(user.getId(), user.getPasswordChangedAt()))
                .build();
    }
    
    @Override
    public Object decodeToken(String token) {
        try {
            Claims claims = getJwtParser()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims;
        } catch (Exception e) {
            log.warn("⚠️ Error decodificando token: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public void cerrarSesion(Long userId, String token) {
        log.info(" Cerrando sesión para usuario {}", userId != null ? userId : "desconocido");
        
        try {
            User user = null;
            
            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
            }
            
            if (user == null && userId != null) {
                log.warn(" Usuario {} no encontrado para logout", userId);
            }
            
            if (user != null && "SAC".equalsIgnoreCase(user.getRoleName())) {
                try {
                    log.info("🔄 [AuthService] Usuario SAC - Liberando módulo asignado...");
                    queueAgentService.liberarModuloDelUsuario(user.getId());
                    log.info("✅ [AuthService] Módulo liberado exitosamente para usuario SAC: {}", user.getId());
                } catch (Exception moduleError) {
                    log.warn("⚠️ [AuthService] No se pudo liberar módulo del usuario SAC {}: {}", user.getId(), moduleError.getMessage());
                }
            }
            
            log.info("✅ Logout completado para usuario {} (ID: {})", 
                    user != null ? user.getUsername() : "desconocido", 
                    userId != null ? userId : "N/A");
            
        } catch (Exception error) {
            log.error("❌ Error en logout para usuario {}: {}", userId, error.getMessage());
            log.warn("⚠️ Continuando con logout a pesar del error");
        }
    }
    
    private String generateToken(UserResponseDto user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration * 1000);
        
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRoleName())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    private UserResponseDto mapToUserResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .lastName(user.getLastName())
                .dni(user.getDni())
                .role(user.getRoleId())
                .roleName(user.getRoleName())
                .moduleId(user.getModuleId())
                .active(user.getActive())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .passwordChangedAt(user.getPasswordChangedAt())
                .build();
    }

    /**
     * Usuarios con estos IDs no tienen obligación de cambiar contraseña cada semana.
     */
    private boolean shouldRequirePasswordChange(Long userId, java.time.LocalDateTime passwordChangedAt) {
        if (userId != null && getExcludedUserIdsPasswordPolicy().contains(userId)) return false;
        return isPasswordExpiredInternal(passwordChangedAt);
    }

    /**
     * true si la contraseña debe renovarse: han pasado 7 o más días desde el ÚLTIMO CAMBIO de contraseña.
     * Si el usuario cambia la contraseña en el transcurso de la semana (p. ej. desde el menú), se actualiza
     * password_changed_at y los 7 días vuelven a contar desde ese momento. No es "cada semana en día fijo".
     */
    private boolean isPasswordExpiredInternal(java.time.LocalDateTime passwordChangedAt) {
        if (passwordChangedAt == null) return true;
        return ChronoUnit.DAYS.between(passwordChangedAt, LocalDateTime.now()) >= 7;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPasswordExpired(Long userId) {
        if (userId == null) return false;
        if (getExcludedUserIdsPasswordPolicy().contains(userId)) return false;
        return userRepository.findById(userId)
                .map(u -> isPasswordExpiredInternal(u.getPasswordChangedAt()))
                .orElse(false);
    }
    
    /** Cuando request es null (p. ej. reset-password) devolvemos IP válida para INET en audit_logs; "unknown" no es válido para INET. */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        return (request != null && request.getHeader("User-Agent") != null) ? request.getHeader("User-Agent") : null;
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        
        String roleName = user.getRoleName();
        log.info("🔐 [AuthService] Usuario: {}, Rol: {}", username, roleName);
        
        if (roleName == null || roleName.isEmpty()) {
            log.warn("⚠️ [AuthService] Usuario {} no tiene rol asignado", username);
            roleName = "USER"; // Rol por defecto
        }
        
        String authority = "ROLE_" + roleName.toUpperCase();
        log.info("🔐 [AuthService] Authority creada: {}", authority);
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority(authority))
                .accountExpired(false)
                .accountLocked(!user.getActive())
                .credentialsExpired(false)
                .disabled(!user.getActive())
                .build();
    }
}

