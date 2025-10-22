package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_principal.UserService;
import com.yego.backend.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final WebSocketService webSocketService;
    
    @Override
    @Transactional
    public UserResponseDto create(CreateUserDto createUserDto) {
        // Verificar si el usuario ya existe
        if (userRepository.existsByUsernameOrEmail(createUserDto.getUsername(), createUserDto.getEmail())) {
            throw new IllegalStateException("El usuario o email ya existe");
        }
        
        // Validar contraseña
        if (!validatePassword(createUserDto.getPassword())) {
            throw new IllegalArgumentException("La contraseña no cumple con los requisitos de seguridad");
        }
        
        // Crear usuario
        User user = User.builder()
                .username(createUserDto.getUsername())
                .email(createUserDto.getEmail())
                .name(createUserDto.getName())
                .lastName(createUserDto.getLastName())
                .password(passwordEncoder.encode(createUserDto.getPassword()))
                .role(createUserDto.getRole() != null ? createUserDto.getRole() : "usuario")
                .dni(createUserDto.getDni())
                .moduleId(createUserDto.getModuleId())
                .active(true)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Enviar notificación WebSocket para refrescar tabla
        webSocketService.enviarActualizacionUsuarios("USER_CREATED", savedUser.getId(), savedUser.getUsername());
        
        log.info("✅ Usuario YEGO Principal creado: {}", savedUser.getUsername());
        
        return mapToResponseDto(savedUser);
    }



    
    @Override
    public Object findAll(Integer page, Integer limit, String search, Boolean active) {
        if (page != null && limit != null) {
            return findAllConPaginacion(page, limit, search, active);
        } else {
            return findAllSinPaginacion(active);
        }
    }
    
    private UserPageDto findAllConPaginacion(Integer page, Integer limit, String search, Boolean active) {
        log.info("Filtrando usuarios - page: {}, limit: {}, search: {}, active: {}", page, limit, search, active);
        
        // Obtener el rol del usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse("");
        
        log.info("Usuario autenticado con rol: {}", userRole);
        
        // Primero obtener TODOS los usuarios sin paginación
        List<User> allUsers;
        
        if (search != null && !search.trim().isEmpty()) {
            if (active != null) {
                allUsers = userRepository.findBySearchAndActive(search, active);
            } else {
                allUsers = userRepository.findBySearch(search);
            }
        } else {
            if (active != null) {
                allUsers = userRepository.findByActive(active);
            } else {
                allUsers = userRepository.findAll();
            }
        }
        
        // Filtrar usuarios según el rol ANTES de paginar
        List<User> filteredUsers = filtrarUsuariosPorRol(allUsers, userRole);
        
        // Calcular paginación manual
        int totalElements = filteredUsers.size();
        int totalPages = (int) Math.ceil((double) totalElements / limit);
        int startIndex = (page - 1) * limit;
        int endIndex = Math.min(startIndex + limit, totalElements);
        
        // Obtener solo los usuarios de la página actual
        List<User> pagedUsers = filteredUsers.subList(startIndex, endIndex);
        
        List<UserResponseCompleteDto> users = pagedUsers.stream()
                .map(this::mapToUserResponseCompleteDto)
                .collect(Collectors.toList());
        
        log.info("Usuarios YEGO Principal obtenidos: {} de {} total", users.size(), totalElements);
        
        return UserPageDto.builder()
                .users(users)
                .total((long) totalElements)
                .page(page)
                .limit(limit)
                .totalPages(totalPages)
                .search(search)
                .active(active)
                .build();
    }
    
    private List<UserResponseDto> findAllSinPaginacion(Boolean active) {
        log.info("Obteniendo todos los usuarios sin paginación");
        
        // Obtener el rol del usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse("");
        
        log.info("Usuario autenticado con rol: {}", userRole);
        
        List<User> users;
        
        if (active != null) {
            users = userRepository.findByActive(active);
        } else {
            users = userRepository.findAll();
        }
        
        // Filtrar usuarios según el rol
        List<User> filteredUsers = filtrarUsuariosPorRol(users, userRole);
        
        return filteredUsers.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Filtra usuarios según el rol del usuario autenticado
     */
    private List<User> filtrarUsuariosPorRol(List<User> users, String userRole) {
        switch (userRole) {
            case "SUPERADMIN":
                // SUPERADMIN ve todos los usuarios
                log.info("SUPERADMIN: mostrando todos los usuarios");
                return users;
                
            case "ADMIN":
                // ADMIN ve todos menos SUPERADMIN
                log.info("ADMIN: mostrando todos menos SUPERADMIN");
                return users.stream()
                        .filter(user -> !"SUPERADMIN".equals(user.getRole()))
                        .collect(Collectors.toList());
                
            case "OPERADOR":
                // OPERADOR solo ve OPERADOR y SAC
                log.info("OPERADOR: mostrando solo OPERADOR y SAC");
                return users.stream()
                        .filter(user -> "OPERADOR".equals(user.getRole()) || "SAC".equals(user.getRole()))
                        .collect(Collectors.toList());
                
            default:
                // Cualquier otro rol no ve usuarios
                log.warn("Rol {} no tiene permisos para ver usuarios", userRole);
                return List.of();
        }
    }
    
    @Override
    public UserResponseDto findOne(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        return mapToResponseDto(user);
    }
    
    @Override
    public UserResponseDto findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        return mapToResponseDto(user);
    }
    
    @Override
    @Transactional
    public UserResponseDto update(Long id, UpdateUserDto updateUserDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        // Verificar conflictos de username
        if (updateUserDto.getUsername() != null && !updateUserDto.getUsername().equals(user.getUsername())) {
            if (userRepository.findByUsername(updateUserDto.getUsername()).isPresent()) {
                throw new IllegalStateException("El nombre de usuario ya existe");
            }
        }
        
        // Verificar conflictos de email
        if (updateUserDto.getEmail() != null && !updateUserDto.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(updateUserDto.getEmail()).isPresent()) {
                throw new IllegalStateException("El email ya existe");
            }
        }
        
        // Actualizar campos
        if (updateUserDto.getUsername() != null) {
            user.setUsername(updateUserDto.getUsername());
        }
        if (updateUserDto.getEmail() != null) {
            user.setEmail(updateUserDto.getEmail());
        }
        if (updateUserDto.getName() != null) {
            user.setName(updateUserDto.getName());
        }
        if (updateUserDto.getRole() != null) {
            user.setRole(updateUserDto.getRole());
        }

        if (updateUserDto.getLastName() != null) {
            user.setLastName(updateUserDto.getLastName());
        }
        
        // Hash password si se proporciona
        if (updateUserDto.getPassword() != null) {
            if (!validatePassword(updateUserDto.getPassword())) {
                throw new IllegalArgumentException("La contraseña no cumple con los requisitos de seguridad");
            }
            user.setPassword(passwordEncoder.encode(updateUserDto.getPassword()));
        }
        
        User savedUser = userRepository.save(user);
        
        // Verificar si el usuario actualizado está logueado y enviar notificación
        verificarYEnviarLogoutForzado(savedUser);
        
        // Enviar notificación WebSocket para refrescar tabla
        webSocketService.enviarActualizacionUsuarios("USER_UPDATED", savedUser.getId(), savedUser.getUsername());
        
        log.info("✅ Usuario YEGO Principal actualizado: {}", savedUser.getUsername());
        
        return mapToResponseDto(savedUser);
    }
    
    @Override
    @Transactional
    public void remove(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        // Soft delete - marcar como inactivo
        user.setActive(false);
        userRepository.save(user);
        
        // Enviar notificación WebSocket para refrescar tabla
        webSocketService.enviarActualizacionUsuarios("USER_DELETED", user.getId(), user.getUsername());
        
        log.info("🗑️ Usuario YEGO Principal eliminado (soft delete): {}", user.getUsername());
    }
    
    @Override
    @Transactional
    public UserResponseDto cambiarEstado(Long id, Boolean activo) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        user.setActive(activo);
        User savedUser = userRepository.save(user);
        
        // Si se desactiva el usuario, forzar logout con mensaje de bloqueo
        if (!activo) {
            enviarNotificacionBloqueo(savedUser);
        }
        
        // Enviar notificación WebSocket para refrescar tabla
        webSocketService.enviarActualizacionUsuarios("USER_STATUS_CHANGED", savedUser.getId(), savedUser.getUsername());
        
        log.info("{} Usuario YEGO Principal: {}", activo ? "Activado" : "Desactivado", savedUser.getUsername());
        
        return mapToResponseDto(savedUser);
    }
    
    @Override
    @Transactional
    public void changePassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        if (!validatePassword(newPassword)) {
            throw new IllegalArgumentException("La contraseña no cumple con los requisitos de seguridad");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Enviar notificación WebSocket para refrescar tabla
        webSocketService.enviarActualizacionUsuarios("USER_PASSWORD_CHANGED", user.getId(), user.getUsername());
        
        log.info("🔑 Contraseña cambiada para usuario YEGO Principal: {}", user.getUsername());
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
    
    private UserResponseDto mapToResponseDto(User user) {
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
    
    private UserResponseCompleteDto mapToUserResponseCompleteDto(User user) {
        return UserResponseCompleteDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .dni(user.getDni())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
    
    /**
     * Verifica si el usuario está logueado y envía notificación de logout forzado
     */
    private void verificarYEnviarLogoutForzado(User user) {
        try {
            // Verificar si el usuario está logueado (podemos usar lastLogin como indicador)
            // Si el lastLogin es reciente (menos de 24 horas), asumimos que está logueado
            if (user.getLastLogin() != null && 
                user.getLastLogin().isAfter(LocalDateTime.now().minusHours(24))) {
                
                log.info("🚨 Usuario {} está logueado, enviando notificación de logout forzado", user.getUsername());
                
                // Enviar notificación WebSocket
                webSocketService.enviarLogoutForzado(user.getId(), user.getUsername());
            }
        } catch (Exception e) {
            log.error("❌ Error enviando logout forzado para usuario {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    /**
     * Envía notificación de bloqueo de cuenta con logout automático
     */
    private void enviarNotificacionBloqueo(User user) {
        try {
            // Siempre enviar notificación de bloqueo, independientemente del último login
            log.info("🚨 Usuario {} desactivado, enviando notificación de bloqueo", user.getUsername());
            
            // Enviar notificación WebSocket de bloqueo
            webSocketService.enviarBloqueoCuenta(user.getId(), user.getUsername());
        } catch (Exception e) {
            log.error("❌ Error enviando notificación de bloqueo para usuario {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    @Override
    public ResponseEntity<DniResponseDto> consultarDni(String dni) {
        log.info("🆔 [UserService] Consultando DNI: {}", dni);
        
        try {
            // Validar formato de DNI
            if (!dni.matches("^\\d{8}$")) {
                return ResponseEntity.badRequest().body(DniResponseDto.builder()
                    .success(false)
                    .dni(dni)
                    .error("DNI debe tener 8 dígitos")
                    .build());
            }
            
            // Consultar API externa
            String apiUrl = "http://167.235.28.114:5000/api/v2/dni/" + dni;
            log.info("🌐 [UserService] Consultando API: {}", apiUrl);
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Basic c3lzdGVtM3c6NkVpWmpwaWp4a1hUZUFDbw==")
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            log.info("📡 [UserService] Respuesta API status: {}", response.statusCode());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                log.info("📋 [UserService] Respuesta API: {}", responseBody);
                
                // Extraer datos del JSON
                String nombres = extraerCampoJson(responseBody, "nombres");
                String apellidoPaterno = extraerCampoJson(responseBody, "apellido_paterno");
                String apellidoMaterno = extraerCampoJson(responseBody, "apellido_materno");
                
                String nombreCompleto = nombres + " " + apellidoPaterno + 
                    (apellidoMaterno != null && !apellidoMaterno.isEmpty() ? " " + apellidoMaterno : "");
                
                return ResponseEntity.ok(DniResponseDto.builder()
                    .success(true)
                    .dni(dni)
                    .nombres(nombres)
                    .apellidoPaterno(apellidoPaterno)
                    .apellidoMaterno(apellidoMaterno)
                    .nombreCompleto(nombreCompleto.trim())
                    .build());
                
            } else {
                log.error("❌ [UserService] Error en API DNI: status {}", response.statusCode());
                return ResponseEntity.status(response.statusCode()).body(DniResponseDto.builder()
                    .success(false)
                    .dni(dni)
                    .error("Error consultando DNI: status " + response.statusCode())
                    .build());
            }
            
        } catch (Exception e) {
            log.error("💥 [UserService] Error consultando DNI {}: {}", dni, e.getMessage());
            return ResponseEntity.internalServerError().body(DniResponseDto.builder()
                .success(false)
                .dni(dni)
                .error("Error interno: " + e.getMessage())
                .build());
        }
    }
    
    /**
     * Extrae un campo del JSON de respuesta de forma simple
     */
    private String extraerCampoJson(String json, String campo) {
        try {
            String patron = "\"" + campo + "\"\\s*:\\s*\"([^\"]+)\"";
            Pattern pattern = Pattern.compile(patron);
            Matcher matcher = pattern.matcher(json);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
            return "";
        } catch (Exception e) {
            log.warn("⚠️ [UserService] Error extrayendo campo {}: {}", campo, e.getMessage());
            return "";
        }
    }
    
}

