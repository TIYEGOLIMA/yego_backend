package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.service.yego_principal.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

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
    public ResponseEntity<?> getProfile(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        UserResponseDto user = userService.findOne(userId);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping
    public ResponseEntity<?> findAll(@RequestParam(required = false) Integer page,
                                     @RequestParam(required = false) Integer limit,
                                     @RequestParam(required = false) String search,
                                     @RequestParam(required = false) Boolean active) {
        Object result = userService.findAll(page, limit, search, active);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> findOne(@PathVariable Long id) {
        UserResponseDto user = userService.findOne(id);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Crear nuevo usuario
     */
    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody CreateUserDto createUserDto) {
        UserResponseDto user = userService.create(createUserDto);
        return ResponseEntity.status(201).body(user);
    }
    
    /**
     * Actualizar usuario
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, 
                                   @RequestBody UpdateUserDto updateUserDto) {
        return userService.update(id, updateUserDto);
    }
    
    /**
     * Eliminar usuario
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> remove(@PathVariable Long id) {
        userService.remove(id);
        return ResponseEntity.ok().build();
    }
    

    /**
     * Cambiar estado de usuario
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, 
                                           @Valid @RequestBody CambiarEstadoDto cambiarEstadoDto) {
        UserResponseDto user = userService.cambiarEstado(id, cambiarEstadoDto.getActivo());
        return ResponseEntity.ok(user);
    }
    
    /**
     * Consultar DNI
     */
    @GetMapping("/dni/{dni}")
    public ResponseEntity<DniResponseDto> consultarDni(@PathVariable String dni) {
        return userService.consultarDni(dni);
    }
}