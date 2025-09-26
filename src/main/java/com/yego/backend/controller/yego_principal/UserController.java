package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.service.yego_principal.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controlador REST para usuarios del sistema YEGO Principal
 * Equivalente a UsersController de NestJS
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * Obtener perfil del usuario actual
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        UserResponseDto user = userService.findOne(userId);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Obtener todos los usuarios
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> findAll() {
        List<UserResponseDto> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }
    
    /**
     * Obtener usuario por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> findOne(@PathVariable Long id) {
        UserResponseDto user = userService.findOne(id);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Crear nuevo usuario
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateUserDto createUserDto) {
        UserResponseDto user = userService.create(createUserDto);
        return ResponseEntity.status(201).body(user);
    }
    
    /**
     * Actualizar usuario
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, 
                                   @Valid @RequestBody UpdateUserDto updateUserDto) {
        UserResponseDto user = userService.update(id, updateUserDto);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Eliminar usuario
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> remove(@PathVariable Long id) {
        userService.remove(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Activar usuario
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> activate(@PathVariable Long id) {
        UserResponseDto user = userService.activate(id);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Desactivar usuario
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        UserResponseDto user = userService.deactivate(id);
        return ResponseEntity.ok(user);
    }
}