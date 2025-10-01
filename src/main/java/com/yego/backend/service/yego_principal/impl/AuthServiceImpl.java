package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_principal.UserRepository;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de autenticación del sistema YEGO Principal
 * Equivalente a AuthService de NestJS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final AuditService auditService;
    private final QueueAgentService queueAgentService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400}")
    private Long jwtExpiration;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    private JwtParser getJwtParser() {
        return Jwts.parser().setSigningKey(getSigningKey()).build();
    }
    
    @Override
    public UserResponseDto validateUser(String username, String password, HttpServletRequest request) {
        // Buscar usuario por username o email
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(username, username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            if (passwordEncoder.matches(password, user.getPassword())) {
                return mapToUserResponseDto(user);
            } else {
                // Registrar intento fallido de login - contraseña incorrecta
                log.info("🔐 Contraseña incorrecta para usuario: {}", username);
                String clientIp = getClientIpAddress(request);
                String userAgent = request.getHeader("User-Agent");
                auditService.logFailedLogin(username, clientIp, userAgent);
            }
        } else {
            // Registrar intento fallido de login - usuario no existe
            log.info("👤 Usuario no encontrado: {}", username);
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            auditService.logFailedLogin(username, clientIp, userAgent);
        }
        
        return null;
    }
    
    @Override
    @Transactional
    public LoginResponseDto login(LoginDto loginDto, HttpServletRequest request) {
        UserResponseDto user = validateUser(loginDto.getUsername(), loginDto.getPassword(), request);
        
        if (user == null) {
            throw new RuntimeException("Credenciales inválidas");
        }
        
        // Actualizar last_login
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());
        
        // Generar JWT token
        String accessToken = generateToken(user);
        
        // Crear sesión con geolocalización
        CreateSessionDto sessionDto = CreateSessionDto.builder()
                .userId(user.getId())
                .tokenHash(passwordEncoder.encode(accessToken))
                .userAgent(request.getHeader("User-Agent"))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        
        sessionService.create(sessionDto, user.getId(), request);
        
        // Registrar login exitoso en auditoría
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        auditService.logLogin(user.getId(), user.getUsername(), clientIp, userAgent);
        
        // Construir respuesta
        LoginResponseDto.LoginUserDto loginUser = LoginResponseDto.LoginUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .moduleId(user.getModuleId() != null ? user.getModuleId().toString() : null)
                .active(user.getActive())
                .lastLogin(LocalDateTime.now())
                .build();
        
        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .user(loginUser)
                .build();
    }
    
    @Override
    @Transactional
    public UserResponseDto register(RegisterDto registerDto) {
        // Verificar si el usuario ya existe
        if (userRepository.existsByUsernameOrEmail(registerDto.getUsername(), registerDto.getEmail())) {
            throw new RuntimeException("El usuario o email ya existe");
        }
        
        // Hash de la contraseña
        String passwordHash = passwordEncoder.encode(registerDto.getPassword());
        
        // Crear usuario
        User user = User.builder()
                .username(registerDto.getUsername())
                .email(registerDto.getEmail())
                .name(registerDto.getNombre())
                .password(passwordHash)
                .role("usuario")
                .active(true)
                .build();
        
        User savedUser = userRepository.save(user);
        
        log.info("✅ Usuario registrado: {}", savedUser.getUsername());
        
        return mapToUserResponseDto(savedUser);
    }
    
    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        // Verificar contraseña actual
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Contraseña actual incorrecta");
        }
        
        // Validar nueva contraseña
        if (!validatePassword(newPassword)) {
            throw new RuntimeException("La nueva contraseña no cumple con los requisitos de seguridad");
        }
        
        // Verificar que la nueva contraseña no sea igual a la actual
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("La nueva contraseña no puede ser igual a la actual");
        }
        
        // Hash nueva contraseña y actualizar
        String newPasswordHash = passwordEncoder.encode(newPassword);
        userRepository.updatePassword(userId, newPasswordHash);
        
        log.info("✅ Contraseña cambiada para usuario: {}", user.getUsername());
    }
    
    @Override
    @Transactional
    public void resetPassword(ChangePasswordDto changePasswordDto) {
        UserResponseDto user = validateUser(changePasswordDto.getUsername(), 
                changePasswordDto.getCurrentPassword(), null);
        
        if (user == null) {
            throw new RuntimeException("Credenciales inválidas");
        }
        
        // Verificar que el usuario requiera cambio de contraseña
        User userEntity = userRepository.findById(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        // Por ahora, permitir cambio de contraseña sin verificar flag
        // if (!userEntity.getRequiereCambioPassword()) {
        //     throw new RuntimeException("Este usuario no requiere cambio de contraseña");
        // }
        
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
        
        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .moduleId(user.getModuleId() != null ? user.getModuleId().toString() : null)
                .active(user.getActive())
                .lastLogin(user.getLastLogin())
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
            
            // Liberar módulo usando el servicio de ticketera
            if (user != null) {
                try {
                    log.info("🔄 [AuthService] Liberando módulo asignado del usuario...");
                    queueAgentService.liberarModuloDelUsuario(user.getId());
                    log.info("✅ [AuthService] Módulo liberado exitosamente para usuario: {}", user.getId());
                } catch (Exception moduleError) {
                    log.warn("⚠️ [AuthService] No se pudo liberar módulo del usuario {}: {}", user.getId(), moduleError.getMessage());
                }
            } else if (userId != null) {
                try {
                    log.info("🔄 [AuthService] Liberando módulo asignado del usuario usando userId...");
                    queueAgentService.liberarModuloDelUsuario(userId);
                    log.info("✅ [AuthService] Módulo liberado exitosamente para usuario: {}", userId);
                } catch (Exception moduleError) {
                    log.warn("⚠️ [AuthService] No se pudo liberar módulo del usuario {}: {}", userId, moduleError.getMessage());
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
                .claim("role", user.getRole())
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
                .role(user.getRole())
                .moduleId(user.getModuleId())
                .active(user.getActive())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
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
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                .accountExpired(false)
                .accountLocked(!user.getActive())
                .credentialsExpired(false)
                .disabled(!user.getActive())
                .build();
    }
}

