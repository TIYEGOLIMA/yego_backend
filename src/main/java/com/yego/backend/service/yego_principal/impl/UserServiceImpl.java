package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.entity.yego_principal.entities.Role;
import com.yego.backend.entity.yego_principal.entities.Area;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.repository.yego_principal.RoleRepository;
import com.yego.backend.repository.yego_principal.AreaRepository;
import com.yego.backend.service.yego_principal.UserService;
import com.yego.backend.handler.yego_principal.UserNotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AreaRepository areaRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserNotificationHandler userNotificationHandler;
    private final ObjectMapper objectMapper;

    private static final Set<Long> USER_IDS_EXCLUIDOS_LISTADO = Set.of(1L, 4L, 6L);

    private static final List<String> WEAK_PASSWORDS = Arrays.asList(
            "123456", "admin", "password", "123456789", "qwerty");

    // ── CRUD ──

    @Override
    @Transactional
    public UserResponseDto create(CreateUserDto dto) {
        if (userRepository.existsByUsernameOrEmail(dto.getUsername(), dto.getEmail())) {
            throw new IllegalStateException("El usuario o email ya existe");
        }
        if (!validatePassword(dto.getPassword())) {
            throw new IllegalArgumentException("La contrasena no cumple con los requisitos de seguridad");
        }

        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new EntityNotFoundException("Rol con ID " + dto.getRoleId() + " no encontrado"));

        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .name(dto.getName())
                .lastName(dto.getLastName())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(role)
                .dni(dto.getDni())
                .moduleId(dto.getModuleId())
                .active(true)
                .build();

        User saved = userRepository.save(user);
        userNotificationHandler.enviarActualizacionUsuarios("USER_CREATED", saved.getId(), saved.getUsername());
        log.info("[UserService] Usuario creado: {}", saved.getUsername());
        return mapToResponseDto(saved);
    }

    @Override
    public Object findAll(Integer page, Integer limit, String search, Boolean active) {
        if (page != null && limit != null) {
            return findAllPaginado(page, limit, search, active);
        }
        return findAllSinPaginacion(active);
    }

    @Override
    public List<UsuarioResumenDto> findAllResumen() {
        List<User> users = userRepository.findByActiveWithRole(true);
        AreaMaps areaMaps = loadAreaMaps();
        return users.stream()
                .filter(u -> !USER_IDS_EXCLUIDOS_LISTADO.contains(u.getId()))
                .map(u -> toUsuarioResumenDto(u, areaMaps))
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDto findOne(Long id) {
        return mapToResponseDto(findUserOrThrow(id));
    }

    @Override
    public UserResponseDto findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        return mapToResponseDto(user);
    }

    @Override
    @Transactional
    public ResponseEntity<?> update(Long id, UpdateUserDto dto) {
        try {
            boolean isPartialUpdate = dto.getUsername() == null
                    && dto.getEmail() == null
                    && dto.getName() == null
                    && dto.getLastName() == null
                    && dto.getDni() == null
                    && dto.getRoleId() == null;

            if (isPartialUpdate) {
                User user = userRepository.findById(id).orElse(null);
                if (user == null) return badRequest("Usuario no encontrado");

                User saved = userRepository.save(updateEntityFromDto(user, dto));
                if (Boolean.FALSE.equals(dto.getActive())) {
                    verificarYEnviarLogoutForzado(saved);
                }
                userNotificationHandler.enviarActualizacionUsuarios("USER_UPDATED", saved.getId(), saved.getUsername());
                log.info("[UserService] Usuario actualizado (parcial): {} - areaId={}", saved.getUsername(), saved.getAreaId());
                return ResponseEntity.ok(mapToResponseDto(saved));
            }

            ResponseEntity<?> validationError = validateUpdateUserDto(dto);
            if (validationError != null) return validationError;

            User user = userRepository.findById(id).orElse(null);
            if (user == null) return badRequest("Usuario no encontrado");

            if (!dto.getUsername().equals(user.getUsername())
                    && userRepository.findByUsername(dto.getUsername()).isPresent()) {
                return badRequest("El nombre de usuario ya existe");
            }
            if (!dto.getEmail().equals(user.getEmail())
                    && userRepository.findByEmail(dto.getEmail()).isPresent()) {
                return badRequest("El email ya existe");
            }

            Role role = roleRepository.findById(dto.getRoleId()).orElse(null);
            if (role == null) return badRequest("Rol con ID " + dto.getRoleId() + " no encontrado");
            if (isNullOrEmpty(role.getName())) return badRequest("El rol no tiene un nombre valido");

            User saved = userRepository.save(updateEntityFromDto(user, dto));
            verificarYEnviarLogoutForzado(saved);
            userNotificationHandler.enviarActualizacionUsuarios("USER_UPDATED", saved.getId(), saved.getUsername());
            log.info("[UserService] Usuario actualizado: {}", saved.getUsername());
            return ResponseEntity.ok(mapToResponseDto(saved));

        } catch (Exception e) {
            log.error("[UserService] Error actualizando usuario: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ErrorResponseDto.builder().message(e.getMessage()).build());
        }
    }

    @Override
    @Transactional
    public UserResponseDto updateArea(Long id, Long areaId) {
        User user = findUserOrThrow(id);
        if (areaId != null && areaId != 0) {
            Area area = areaRepository.findById(areaId).orElse(null);
            if (area != null && area.getSupervisorId() != null && area.getSupervisorId().equals(id)) {
                throw new IllegalStateException("El usuario es supervisor de esta área y no puede ser agregado como colaborador");
            }
            if (area != null && area.getManagerId() != null && area.getManagerId().equals(id)) {
                throw new IllegalStateException("El usuario es responsable de esta área y no puede ser agregado como colaborador");
            }
        }
        user.setAreaId(areaId == null || areaId == 0 ? null : areaId);
        User saved = userRepository.save(user);
        userNotificationHandler.enviarActualizacionUsuarios("USER_UPDATED", saved.getId(), saved.getUsername());
        log.info("[UserService] Area del usuario actualizada: {} -> areaId={}", saved.getUsername(), saved.getAreaId());
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public void remove(Long id) {
        User user = findUserOrThrow(id);
        String username = user.getUsername();
        Long userId = user.getId();

        userRepository.delete(user);

        userNotificationHandler.enviarActualizacionUsuarios("USER_DELETED", userId, username);
        log.info("[UserService] Usuario eliminado permanentemente: {}", username);
    }

    @Override
    @Transactional
    public UserResponseDto cambiarEstado(Long id, Boolean activo) {
        User user = findUserOrThrow(id);
        user.setActive(activo);
        User saved = userRepository.save(user);

        if (!activo) {
            enviarNotificacionBloqueo(saved);
        }

        userNotificationHandler.enviarActualizacionUsuarios("USER_STATUS_CHANGED", saved.getId(), saved.getUsername());
        log.info("[UserService] Usuario {}: {}", activo ? "activado" : "desactivado", saved.getUsername());
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public void changePassword(Long id, String newPassword) {
        User user = findUserOrThrow(id);
        if (!validatePassword(newPassword)) {
            throw new IllegalArgumentException("La contrasena no cumple con los requisitos de seguridad");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        userNotificationHandler.enviarActualizacionUsuarios("USER_PASSWORD_CHANGED", user.getId(), user.getUsername());
        log.info("[UserService] Contrasena cambiada para: {}", user.getUsername());
    }

    @Override
    public boolean validatePassword(String password) {
        if (WEAK_PASSWORDS.contains(password.toLowerCase())) return false;
        return password.length() >= 8
                && password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }

    @Override
    public ResponseEntity<DniResponseDto> consultarDni(String dni) {
        try {
            if (!dni.matches("^\\d{8}$")) {
                return ResponseEntity.ok(DniResponseDto.builder()
                        .success(false).dni(dni)
                        .error("Solo se pueden consultar DNI peruanos de 8 digitos")
                        .build());
            }

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.factiliza.com/pe/v1/dni/info/" + dni))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI2NTkiLCJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL3dzLzIwMDgvMDYvaWRlbnRpdHkvY2xhaW1zL3JvbGUiOiJjb25zdWx0b3IifQ.NaoAXramusCzks7mRCzWFWcMiBaSA0d8rNBgw-OVeYg")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("[UserService] Error API DNI: status {}", response.statusCode());
                return ResponseEntity.status(response.statusCode()).body(DniResponseDto.builder()
                        .success(false).dni(dni)
                        .error("Error consultando DNI: status " + response.statusCode())
                        .build());
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!root.has("success") || !root.get("success").asBoolean()) {
                String message = root.has("message") ? root.get("message").asText() : "Error en la consulta";
                return ResponseEntity.ok(DniResponseDto.builder()
                        .success(false).dni(dni).error(message).build());
            }

            JsonNode data = root.get("data");
            if (data == null || data.isNull()) {
                return ResponseEntity.ok(DniResponseDto.builder()
                        .success(false).dni(dni).error("No se encontraron datos").build());
            }

            return ResponseEntity.ok(DniResponseDto.builder()
                    .success(true)
                    .nombres(data.has("nombres") ? data.get("nombres").asText() : "")
                    .apellidoPaterno(data.has("apellido_paterno") ? data.get("apellido_paterno").asText() : "")
                    .apellidoMaterno(data.has("apellido_materno") ? data.get("apellido_materno").asText() : "")
                    .build());

        } catch (Exception e) {
            log.error("[UserService] Error consultando DNI {}: {}", dni, e.getMessage());
            return ResponseEntity.internalServerError().body(DniResponseDto.builder()
                    .success(false).dni(dni).error("Error interno: " + e.getMessage()).build());
        }
    }

    // ── Consultas internas ──

    private UserPageDto findAllPaginado(Integer page, Integer limit, String search, Boolean active) {
        AreaMaps areaMaps = loadAreaMaps();

        String searchTrim = search != null ? search.trim() : "";

        List<Object[]> rows;
        if (!searchTrim.isEmpty()) {
            String pattern = "%" + searchTrim + "%";
            List<User> allUsers = active != null
                    ? userRepository.findBySearchAndActiveWithRole(pattern, active)
                    : userRepository.findBySearchWithRole(pattern);
            int total = allUsers.size();
            int totalPages = (int) Math.ceil((double) total / limit);
            int start = Math.min((page - 1) * limit, total);
            int end = Math.min(start + limit, total);
            List<UserResponseCompleteDto> users = allUsers.subList(start, end).stream()
                    .map(u -> mapToCompleteDto(u, areaMaps))
                    .collect(Collectors.toList());
            return UserPageDto.builder().users(users).total((long) total)
                    .page(page).limit(limit).totalPages(totalPages).search(search).active(active).build();
        }

        rows = active != null
                ? userRepository.findAllLightweightByActive(active)
                : userRepository.findAllLightweight();

        int total = rows.size();
        int totalPages = (int) Math.ceil((double) total / limit);
        int start = Math.min((page - 1) * limit, total);
        int end = Math.min(start + limit, total);

        List<UserResponseCompleteDto> users = rows.subList(start, end).stream()
                .map(row -> mapRowToCompleteDto(row, areaMaps))
                .collect(Collectors.toList());

        return UserPageDto.builder().users(users).total((long) total)
                .page(page).limit(limit).totalPages(totalPages).search(search).active(active).build();
    }

    private List<UserResponseDto> findAllSinPaginacion(Boolean active) {
        List<User> users = active != null
                ? userRepository.findByActive(active)
                : userRepository.findAll();
        return users.stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    // ── Validacion ──

    private ResponseEntity<?> validateUpdateUserDto(UpdateUserDto dto) {
        if (isNullOrEmpty(dto.getUsername())) return badRequest("El nombre de usuario es obligatorio");
        if (isNullOrEmpty(dto.getEmail())) return badRequest("El email es obligatorio");
        if (isNullOrEmpty(dto.getName())) return badRequest("El nombre es obligatorio");
        if (isNullOrEmpty(dto.getLastName())) return badRequest("El apellido es obligatorio");
        if (isNullOrEmpty(dto.getDni())) return badRequest("El DNI es obligatorio");
        if (dto.getRoleId() == null) return badRequest("El rol es obligatorio");

        if (dto.getUsername().length() < 2 || dto.getUsername().length() > 255)
            return badRequest("El nombre de usuario debe tener entre 2 y 255 caracteres");
        if (!dto.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$"))
            return badRequest("El formato del email no es valido");
        if (dto.getName().length() > 255) return badRequest("El nombre no puede exceder 255 caracteres");
        if (dto.getLastName().length() > 255) return badRequest("El apellido no puede exceder 255 caracteres");
        if (dto.getDni().length() < 8 || dto.getDni().length() > 12)
            return badRequest("El documento debe tener entre 8 y 12 caracteres");
        if (dto.getPassword() != null && dto.getPassword().length() < 6)
            return badRequest("La contrasena debe tener al menos 6 caracteres");

        return null;
    }

    // ── Mappers ──

    private User updateEntityFromDto(User user, UpdateUserDto dto) {
        if (dto.getUsername() != null) user.setUsername(dto.getUsername());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getDni() != null) user.setDni(dto.getDni());
        if (dto.getRoleId() != null) {
            Role role = roleRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new EntityNotFoundException("Rol con ID " + dto.getRoleId() + " no encontrado"));
            user.setRole(role);
        }
        if (dto.getPassword() != null) {
            if (!validatePassword(dto.getPassword())) {
                throw new IllegalArgumentException("La contrasena no cumple con los requisitos de seguridad");
            }
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getActive() != null) user.setActive(dto.getActive());
        if (dto.getAreaId() != null) user.setAreaId(dto.getAreaId().longValue() == 0 ? null : dto.getAreaId());
        return user;
    }

    private UserResponseDto mapToResponseDto(User user) {
        Long areaId = user.getAreaId();
        String areaNombre = areaId != null
                ? areaRepository.findById(areaId).map(Area::getName).orElse(null)
                : null;
        return UserResponseDto.builder()
                .id(user.getId())
                .username(orEmpty(user.getUsername()))
                .email(orEmpty(user.getEmail()))
                .name(orEmpty(user.getName()))
                .lastName(orEmpty(user.getLastName()))
                .dni(orEmpty(user.getDni()))
                .role(user.getRoleId() != null ? user.getRoleId() : 0L)
                .roleName(orEmpty(user.getRoleName()))
                .moduleId(user.getModuleId() != null ? user.getModuleId() : 0L)
                .active(user.getActive() != null ? user.getActive() : false)
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .areaId(areaId)
                .areaNombre(areaNombre)
                .build();
    }

    private UserResponseCompleteDto mapRowToCompleteDto(Object[] row, AreaMaps areaMaps) {
        Long id = ((Number) row[0]).longValue();
        String username = (String) row[1];
        String email = (String) row[2];
        String name = (String) row[3];
        String lastName = (String) row[4];
        String roleName = (String) row[5];
        Boolean active = (Boolean) row[6];
        String dni = (String) row[7];
        LocalDateTime createdAt = row[8] != null ? ((java.sql.Timestamp) row[8]).toLocalDateTime() : null;
        LocalDateTime lastLogin = row[9] != null ? ((java.sql.Timestamp) row[9]).toLocalDateTime() : null;
        Long areaId = row[10] != null ? ((Number) row[10]).longValue() : null;

        String areaNombre = null;
        boolean esResponsable = false;
        boolean esSupervisor = false;

        if (areaId != null) {
            Area area = areaMaps.byId.get(areaId);
            if (area != null) {
                areaNombre = area.getName();
                esResponsable = area.getManagerId() != null && area.getManagerId().equals(id);
                if (!esResponsable) {
                    esSupervisor = area.getSupervisorId() != null && area.getSupervisorId().equals(id);
                }
            }
        }
        if (areaNombre == null) {
            List<Area> areasJefe = areaMaps.byManagerId.get(id);
            if (areasJefe != null && !areasJefe.isEmpty()) {
                areaId = areasJefe.get(0).getId();
                areaNombre = areasJefe.stream().map(Area::getName).collect(Collectors.joining(", "));
                esResponsable = true;
            } else {
                List<Area> areasSup = areaMaps.bySupervisorId.get(id);
                if (areasSup != null && !areasSup.isEmpty()) {
                    areaId = areasSup.get(0).getId();
                    areaNombre = areasSup.stream().map(Area::getName).collect(Collectors.joining(", "));
                    esSupervisor = true;
                }
            }
        }

        return UserResponseCompleteDto.builder()
                .id(id).username(username).email(email).name(name).lastName(lastName)
                .role(roleName).dni(dni).active(active).createdAt(createdAt).lastLogin(lastLogin)
                .areaId(areaId).areaNombre(areaNombre).areaEsResponsable(esResponsable).areaEsSupervisor(esSupervisor)
                .build();
    }

    private UserResponseCompleteDto mapToCompleteDto(User user, AreaMaps areaMaps) {
        Long areaId = user.getAreaId();
        String areaNombre = null;
        boolean esResponsable = false;
        boolean esSupervisor = false;

        if (areaId != null) {
            Area area = areaMaps.byId.get(areaId);
            if (area != null) {
                areaNombre = area.getName();
                esResponsable = area.getManagerId() != null && area.getManagerId().equals(user.getId());
                if (!esResponsable) {
                    esSupervisor = area.getSupervisorId() != null && area.getSupervisorId().equals(user.getId());
                }
            }
        }
        if (areaNombre == null) {
            List<Area> areasJefe = areaMaps.byManagerId.get(user.getId());
            if (areasJefe != null && !areasJefe.isEmpty()) {
                areaId = areasJefe.get(0).getId();
                areaNombre = areasJefe.stream().map(Area::getName).collect(Collectors.joining(", "));
                esResponsable = true;
            } else {
                List<Area> areasSup = areaMaps.bySupervisorId.get(user.getId());
                if (areasSup != null && !areasSup.isEmpty()) {
                    areaId = areasSup.get(0).getId();
                    areaNombre = areasSup.stream().map(Area::getName).collect(Collectors.joining(", "));
                    esSupervisor = true;
                }
            }
        }

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
                .areaId(areaId)
                .areaNombre(areaNombre)
                .areaEsResponsable(esResponsable)
                .areaEsSupervisor(esSupervisor)
                .build();
    }

    private UsuarioResumenDto toUsuarioResumenDto(User u, AreaMaps areaMaps) {
        String areaNombre = null;
        if (u.getAreaId() != null) {
            Area a = areaMaps.byId.get(u.getAreaId());
            if (a != null) areaNombre = a.getName();
        }
        if (areaNombre == null) {
            List<Area> areas = areaMaps.byManagerId.get(u.getId());
            if (areas != null && !areas.isEmpty()) {
                areaNombre = areas.stream().map(Area::getName).collect(Collectors.joining(", "));
            }
        }
        if (areaNombre == null) {
            List<Area> areasSup = areaMaps.bySupervisorId.get(u.getId());
            if (areasSup != null && !areasSup.isEmpty()) {
                areaNombre = areasSup.stream().map(Area::getName).collect(Collectors.joining(", "));
            }
        }
        return UsuarioResumenDto.builder()
                .id(u.getId())
                .username(u.getUsername())
                .rol(orEmpty(u.getRoleName()))
                .esJefe(areaMaps.byManagerId.containsKey(u.getId()))
                .esSupervisor(areaMaps.bySupervisorId.containsKey(u.getId()))
                .area(orEmpty(areaNombre))
                .nombre(orEmpty(u.getName()))
                .apellido(orEmpty(u.getLastName()))
                .email(orEmpty(u.getEmail()))
                .dni(orEmpty(u.getDni()))
                .build();
    }

    // ── Notificaciones ──

    private void verificarYEnviarLogoutForzado(User user) {
        try {
            if (user.getLastLogin() != null
                    && user.getLastLogin().isAfter(LocalDateTime.now().minusHours(24))) {
                log.info("[UserService] Usuario {} logueado, enviando logout forzado", user.getUsername());
                userNotificationHandler.enviarLogoutForzado(user.getId(), user.getUsername());
            }
        } catch (Exception e) {
            log.error("[UserService] Error enviando logout forzado para {}: {}", user.getUsername(), e.getMessage());
        }
    }

    private void enviarNotificacionBloqueo(User user) {
        try {
            log.info("[UserService] Usuario {} desactivado, enviando notificacion de bloqueo", user.getUsername());
            userNotificationHandler.enviarBloqueoCuenta(user.getId(), user.getUsername());
        } catch (Exception e) {
            log.error("[UserService] Error enviando notificacion de bloqueo para {}: {}", user.getUsername(), e.getMessage());
        }
    }

    // ── Helpers ──

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
    }

    private AreaMaps loadAreaMaps() {
        List<Area> allAreas = areaRepository.findAll();
        return new AreaMaps(
                allAreas.stream().collect(Collectors.toMap(Area::getId, a -> a)),
                allAreas.stream()
                        .filter(a -> a.getManagerId() != null)
                        .collect(Collectors.groupingBy(Area::getManagerId)),
                allAreas.stream()
                        .filter(a -> a.getSupervisorId() != null)
                        .collect(Collectors.groupingBy(Area::getSupervisorId)));
    }

    private static String orEmpty(String value) {
        return value != null ? value : "";
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static ResponseEntity<ErrorResponseDto> badRequest(String message) {
        return ResponseEntity.badRequest().body(ErrorResponseDto.builder().message(message).build());
    }

    private record AreaMaps(Map<Long, Area> byId, Map<Long, List<Area>> byManagerId, Map<Long, List<Area>> bySupervisorId) {}
}
