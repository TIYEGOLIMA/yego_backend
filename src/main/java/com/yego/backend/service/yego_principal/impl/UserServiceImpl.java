package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.entity.yego_principal.entities.Role;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.repository.yego_principal.RoleRepository;
import com.yego.backend.service.yego_principal.UserService;
import com.yego.backend.handler.yego_principal.UserNotificationHandler;
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
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserNotificationHandler userNotificationHandler;
    private final ObjectMapper objectMapper;
    
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
        
        // Validar que el rol existe
        Role role = roleRepository.findById(createUserDto.getRoleId())
                .orElseThrow(() -> new EntityNotFoundException("Rol con ID " + createUserDto.getRoleId() + " no encontrado"));
        
        // Crear usuario
        User user = User.builder()
                .username(createUserDto.getUsername())
                .email(createUserDto.getEmail())
                .name(createUserDto.getName())
                .lastName(createUserDto.getLastName())
                .password(passwordEncoder.encode(createUserDto.getPassword()))
                .role(role)
                .dni(createUserDto.getDni())
                .moduleId(createUserDto.getModuleId())
                .active(true)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Enviar notificación WebSocket para refrescar tabla
        userNotificationHandler.enviarActualizacionUsuarios("USER_CREATED", savedUser.getId(), savedUser.getUsername());
        
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
                .map(authority -> {
                    String authorityStr = authority.getAuthority();
                    log.debug("🔍 Authority completo: {}", authorityStr);
                    return authorityStr.replace("ROLE_", "").toUpperCase();
                })
                .orElse("");
        
        log.info("👤 Usuario autenticado con rol extraído: {}", userRole);
        
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
        
        // Calcular paginación manual
        int totalElements = allUsers.size();
        int totalPages = (int) Math.ceil((double) totalElements / limit);
        int startIndex = (page - 1) * limit;
        int endIndex = Math.min(startIndex + limit, totalElements);
        
        // Obtener solo los usuarios de la página actual
        List<User> pagedUsers = allUsers.subList(startIndex, endIndex);
        
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
                .map(authority -> {
                    String authorityStr = authority.getAuthority();
                    log.debug("🔍 Authority completo: {}", authorityStr);
                    return authorityStr.replace("ROLE_", "").toUpperCase();
                })
                .orElse("");
        
        log.info("👤 Usuario autenticado con rol extraído: {}", userRole);
        
        List<User> users;
        
        if (active != null) {
            users = userRepository.findByActive(active);
        } else {
            users = userRepository.findAll();
        }
        
        return users.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
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
    public ResponseEntity<?> update(Long id, UpdateUserDto updateUserDto) {
        try {
            // Validar campos obligatorios y formato
            ResponseEntity<?> validationError = validateUpdateUserDto(updateUserDto);
            if (validationError != null) {
                return validationError;
            }
            
            User user = userRepository.findById(id)
                    .orElse(null);
            
            if (user == null) {
                return badRequest("Usuario no encontrado");
            }
            
            // Verificar conflictos de username
            if (!updateUserDto.getUsername().equals(user.getUsername()) 
                    && userRepository.findByUsername(updateUserDto.getUsername()).isPresent()) {
                return badRequest("El nombre de usuario ya existe");
            }
            
            // Verificar conflictos de email
            if (!updateUserDto.getEmail().equals(user.getEmail()) 
                    && userRepository.findByEmail(updateUserDto.getEmail()).isPresent()) {
                return badRequest("El email ya existe");
            }
            
            // Verificar que el rol existe y tiene nombre válido
            Role role = roleRepository.findById(updateUserDto.getRoleId())
                    .orElse(null);
            if (role == null) {
                return badRequest("Rol con ID " + updateUserDto.getRoleId() + " no encontrado");
            }
            if (isNullOrEmpty(role.getName())) {
                return badRequest("El rol no tiene un nombre válido");
            }
            
            // Actualizar entidad desde DTO
            User updatedUser = updateEntityFromDto(user, updateUserDto);
            
            User savedUser = userRepository.save(updatedUser);
            
            // Verificar si el usuario actualizado está logueado y enviar notificación
            verificarYEnviarLogoutForzado(savedUser);
            
            // Enviar notificación WebSocket para refrescar tabla
            userNotificationHandler.enviarActualizacionUsuarios("USER_UPDATED", savedUser.getId(), savedUser.getUsername());
            
            log.info("✅ Usuario YEGO Principal actualizado: {}", savedUser.getUsername());
            
            return ResponseEntity.ok(mapToResponseDto(savedUser));
            
        } catch (Exception e) {
            log.error("❌ Error actualizando usuario: {}", e.getMessage());
            ErrorResponseDto error = ErrorResponseDto.builder()
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(error);
        }
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
        userNotificationHandler.enviarActualizacionUsuarios("USER_DELETED", user.getId(), user.getUsername());
        
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
        userNotificationHandler.enviarActualizacionUsuarios("USER_STATUS_CHANGED", savedUser.getId(), savedUser.getUsername());
        
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
        userNotificationHandler.enviarActualizacionUsuarios("USER_PASSWORD_CHANGED", user.getId(), user.getUsername());
        
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
    
    /**
     * Valida el DTO de actualización de usuario
     */
    private ResponseEntity<?> validateUpdateUserDto(UpdateUserDto updateUserDto) {
        // Validar campos obligatorios
        if (isNullOrEmpty(updateUserDto.getUsername())) {
            return badRequest("El nombre de usuario es obligatorio");
        }
        if (isNullOrEmpty(updateUserDto.getEmail())) {
            return badRequest("El email es obligatorio");
        }
        if (isNullOrEmpty(updateUserDto.getName())) {
            return badRequest("El nombre es obligatorio");
        }
        if (isNullOrEmpty(updateUserDto.getLastName())) {
            return badRequest("El apellido es obligatorio");
        }
        if (isNullOrEmpty(updateUserDto.getDni())) {
            return badRequest("El DNI es obligatorio");
        }
        if (updateUserDto.getRoleId() == null) {
            return badRequest("El rol es obligatorio");
        }
        
        // Validar formato y longitud
        if (updateUserDto.getUsername().length() < 2 || updateUserDto.getUsername().length() > 255) {
            return badRequest("El nombre de usuario debe tener entre 2 y 255 caracteres");
        }
        if (!updateUserDto.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return badRequest("El formato del email no es válido");
        }
        if (updateUserDto.getName().length() > 255) {
            return badRequest("El nombre no puede exceder 255 caracteres");
        }
        if (updateUserDto.getLastName().length() > 255) {
            return badRequest("El apellido no puede exceder 255 caracteres");
        }
        if (updateUserDto.getDni().length() < 8 || updateUserDto.getDni().length() > 12) {
            return badRequest("El documento debe tener entre 8 y 12 caracteres");
        }
        if (updateUserDto.getPassword() != null && updateUserDto.getPassword().length() < 6) {
            return badRequest("La contraseña debe tener al menos 6 caracteres");
        }
        
        return null; // Validación exitosa
    }
    
    /**
     * Helper para verificar si un string es null o vacío
     */
    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    /**
     * Helper para crear respuesta de error
     */
    private ResponseEntity<ErrorResponseDto> badRequest(String message) {
        ErrorResponseDto error = ErrorResponseDto.builder()
                .message(message)
                .build();
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Actualiza la entidad User con los datos del DTO
     */
    private User updateEntityFromDto(User user, UpdateUserDto updateUserDto) {
        if (updateUserDto.getUsername() != null) {
            user.setUsername(updateUserDto.getUsername());
        }
        if (updateUserDto.getEmail() != null) {
            user.setEmail(updateUserDto.getEmail());
        }
        if (updateUserDto.getName() != null) {
            user.setName(updateUserDto.getName());
        }
        if (updateUserDto.getLastName() != null) {
            user.setLastName(updateUserDto.getLastName());
        }
        if (updateUserDto.getDni() != null) {
            user.setDni(updateUserDto.getDni());
        }
        if (updateUserDto.getRoleId() != null) {
            Role role = roleRepository.findById(updateUserDto.getRoleId())
                    .orElseThrow(() -> new EntityNotFoundException("Rol con ID " + updateUserDto.getRoleId() + " no encontrado"));
            user.setRole(role);
        }
        if (updateUserDto.getPassword() != null) {
            if (!validatePassword(updateUserDto.getPassword())) {
                throw new IllegalArgumentException("La contraseña no cumple con los requisitos de seguridad");
            }
            user.setPassword(passwordEncoder.encode(updateUserDto.getPassword()));
        }
        if (updateUserDto.getActive() != null) {
            user.setActive(updateUserDto.getActive());
        }
        return user;
    }
    
    /**
     * Convierte la entidad User a UserResponseDto
     */
    private UserResponseDto mapToResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername() != null ? user.getUsername() : "")
                .email(user.getEmail() != null ? user.getEmail() : "")
                .name(user.getName() != null ? user.getName() : "")
                .lastName(user.getLastName() != null ? user.getLastName() : "")
                .dni(user.getDni() != null ? user.getDni() : "")
                .role(user.getRoleId() != null ? user.getRoleId() : 0L)
                .roleName(user.getRoleName() != null ? user.getRoleName() : "")
                .moduleId(user.getModuleId() != null ? user.getModuleId() : 0L)
                .active(user.getActive() != null ? user.getActive() : false)
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
                .role(user.getRoleName())
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
                userNotificationHandler.enviarLogoutForzado(user.getId(), user.getUsername());
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
            userNotificationHandler.enviarBloqueoCuenta(user.getId(), user.getUsername());
        } catch (Exception e) {
            log.error("❌ Error enviando notificación de bloqueo para usuario {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    @Override
    public ResponseEntity<DniResponseDto> consultarDni(String dni) {
        log.info("🆔 [UserService] Consultando DNI: {}", dni);
        
        try {
            // Validar si es un DNI peruano (exactamente 8 dígitos)
            if (!dni.matches("^\\d{8}$")) {
                // Si no es DNI de 8 dígitos, no consultar API (puede ser CE, pasaporte, etc.)
                log.info("📄 [UserService] Documento {} no es DNI peruano (8 dígitos), no se consulta API", dni);
                return ResponseEntity.ok(DniResponseDto.builder()
                    .success(false)
                    .dni(dni)
                    .error("Solo se pueden consultar DNI peruanos de 8 dígitos")
                    .build());
            }
            
            // Consultar API externa (Factiliza)
            String apiUrl = "https://api.factiliza.com/pe/v1/dni/info/" + dni;
            log.info("🌐 [UserService] Consultando API: {}", apiUrl);
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI2NTkiLCJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL3dzLzIwMDgvMDYvaWRlbnRpdHkvY2xhaW1zL3JvbGUiOiJjb25zdWx0b3IifQ.NaoAXramusCzks7mRCzWFWcMiBaSA0d8rNBgw-OVeYg")
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            log.info("📡 [UserService] Respuesta API status: {}", response.statusCode());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                log.info("📋 [UserService] Respuesta API: {}", responseBody);
                
                try {
                    // Parsear JSON con ObjectMapper
                    JsonNode rootNode = objectMapper.readTree(responseBody);
                    
                    // Verificar si la respuesta es exitosa
                    boolean success = rootNode.has("success") && rootNode.get("success").asBoolean();
                    if (!success) {
                        String message = rootNode.has("message") ? rootNode.get("message").asText() : "Error en la consulta";
                        log.error("❌ [UserService] API retornó success=false: {}", message);
                        return ResponseEntity.ok(DniResponseDto.builder()
                            .success(false)
                            .dni(dni)
                            .error(message)
                            .build());
                    }
                    
                    // Extraer datos del objeto "data"
                    JsonNode dataNode = rootNode.get("data");
                    if (dataNode == null || dataNode.isNull()) {
                        log.error("❌ [UserService] No se encontró el objeto 'data' en la respuesta");
                        return ResponseEntity.ok(DniResponseDto.builder()
                            .success(false)
                            .dni(dni)
                            .error("No se encontraron datos en la respuesta")
                            .build());
                    }
                    
                    String nombres = dataNode.has("nombres") ? dataNode.get("nombres").asText() : "";
                    String apellidoPaterno = dataNode.has("apellido_paterno") ? dataNode.get("apellido_paterno").asText() : "";
                    String apellidoMaterno = dataNode.has("apellido_materno") ? dataNode.get("apellido_materno").asText() : "";
                    
                    return ResponseEntity.ok(DniResponseDto.builder()
                        .success(true)
                        .nombres(nombres)
                        .apellidoPaterno(apellidoPaterno)
                        .apellidoMaterno(apellidoMaterno)
                        .build());
                        
                } catch (Exception e) {
                    log.error("❌ [UserService] Error parseando respuesta JSON: {}", e.getMessage());
                    return ResponseEntity.ok(DniResponseDto.builder()
                        .success(false)
                        .dni(dni)
                        .error("Error parseando respuesta: " + e.getMessage())
                        .build());
                }
                
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
    
    
}

